package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trading.contracts.TradeQueueContract;
import net.corda.samples.trading.entity.MatchRecord;
import net.corda.samples.trading.entity.BuyOrderKey;
import net.corda.samples.trading.entity.SellOrderKey;
import net.corda.samples.trading.entity.TradeStateWithFee;
import net.corda.samples.trading.states.TradeQueueState;
import net.corda.samples.trading.states.TradeState;
import org.apache.http.client.utils.CloneUtils;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * create one limited txn, add it into queue
 */
public class MatchOrdersFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class MatchOrdersInitiator extends FlowLogic<List<MatchRecord>> {

        private final TradeState tradeState;

        public MatchOrdersInitiator(TradeState tradeState) {
            this.tradeState = tradeState;
        }

        @Suspendable
        @Override
        public List<MatchRecord> call() throws FlowException {

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party initiateParty = getOurIdentity();

            // Query the vault to fetch a list of all AuctionState states, and filter the results based on the auctionId
            // to fetch the desired AuctionState states from the vault. This filtered states would be used as input to the
            // transaction.
            StateAndRef<TradeQueueState> inputStateAndRef = getServiceHub().getVaultService().queryBy(TradeQueueState.class).getStates()
                    .stream().filter(it -> {
                        TradeQueueState tradeQueueState = it.getState().getData();
                        return tradeQueueState.getStockName().equals(tradeState.getStockName());
                    }).findAny().orElseThrow(() -> new IllegalArgumentException("TradeQueue not found"));

            TradeQueueState input = inputStateAndRef.getState().getData();

            //matchOrders
            TradeQueueState output = CopyUtils.copy(input);
            TradeState newTradeState = CopyUtils.copy(tradeState);
            List<MatchRecord> matchResults = processOrder(newTradeState, output);
            output.setCreateParty(initiateParty);

            List<Party> parties = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                    .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                    .collect(Collectors.toList());
            parties.remove(initiateParty);
            parties.remove(notary);
            output.setParticipantParties(parties);


            // Build the transaction.
            TransactionBuilder builder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(output)
                    .addCommand(new TradeQueueContract.Commands.insertTrade2Queue(), Arrays.asList(initiateParty.getOwningKey()));

            //verify
            builder.verify(getServiceHub());

            //signed
            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(builder);

            List<FlowSession> allSessions = new ArrayList<>();
            for (Party party : parties)
                allSessions.add(initiateFlow(party));

            SignedTransaction signedTransaction = subFlow(new FinalityFlow(selfSignedTransaction, allSessions));

            for (MatchRecord matchRecord : matchResults) {
                subFlow(new SettleTradeFlow(matchRecord));
            }

            return matchResults;
        }

        /**
         * matchOrder
         */
        List<MatchRecord> processOrder(TradeState tradeState, TradeQueueState tradeQueueState) {
            List<MatchRecord> matchResults = new ArrayList<>();
            BigDecimal matchPrice;
            String tradeType = tradeState.getTradeType();
            TradeStateWithFee tradeStateWithFee = new TradeStateWithFee(tradeState, new BigDecimal("0"));
            TreeMap<BuyOrderKey, TradeStateWithFee> counterPartyBuyQueue = tradeQueueState.getBuyStockList();
            TreeMap<SellOrderKey, TradeStateWithFee> counterPartySellQueue = tradeQueueState.getSellStockList();

            for (; ; ) {

                TradeStateWithFee counterPartyTradeStateWithFee = null;
                if (tradeType.equals("Buy")) {
                    tradeStateWithFee.setFee(new BigDecimal("1"));
                    if (counterPartySellQueue == null || counterPartySellQueue.size() == 0) {
                        break;
                    }
                    counterPartyTradeStateWithFee = tradeQueueState.getSellFirst();
                } else {
                    if (counterPartyBuyQueue == null || counterPartyBuyQueue.size() == 0) {
                        break;
                    }
                    counterPartyTradeStateWithFee = tradeQueueState.getBuyFirst();
                }

                TradeState counterPartyTradeState = counterPartyTradeStateWithFee.getTradeState();

                BigDecimal tradePrice = new BigDecimal(Double.toString(tradeState.getStockPrice()));
                BigDecimal counterPartyTradePrice = new BigDecimal(Double.toString(counterPartyTradeState.getStockPrice()));
                if (tradeType.equals("Buy") && tradePrice.compareTo(counterPartyTradePrice) < 0) {
                    // The buy order price is lower than the first leg of the sell order
                    break;
                } else if (tradeType.equals("Sell") && tradePrice.compareTo(counterPartyTradePrice) > 0) {
                    // Sell order price is higher than the first buy order price
                    break;
                }
                //matchPrice
                matchPrice = counterPartyTradePrice;

                //create new tradeState so that the original tradestate won't be changed(obey contractState immutable rule)
                TradeState newCounterPartyTradeState = CopyUtils.copy(counterPartyTradeState);

                //matchQuantity
                BigDecimal matchQuantity = new BigDecimal(tradeState.getStockQuantity()).min(new BigDecimal(counterPartyTradeState.getStockQuantity()));
                //matchResults
                matchResults.add(new MatchRecord(matchPrice, matchQuantity, tradeState, newCounterPartyTradeState));
                // Update the number of orders that have been filled
                tradeState.stockQuantity = (new BigDecimal(tradeState.getStockQuantity()).subtract(matchQuantity)).intValue();
                newCounterPartyTradeState.stockQuantity = (new BigDecimal(newCounterPartyTradeState.getStockQuantity()).subtract(matchQuantity)).intValue();
                // Delete from the order book when the counterparty order is fully filled
                if (tradeType.equals("Buy")) {
                    //if the initial txn is buying, set a fixed fee
                    tradeQueueState.removeSellTrade(counterPartyTradeStateWithFee);
                    if (newCounterPartyTradeState.stockQuantity != 0) {
                        TradeStateWithFee newCounterPartyTradeStateWithFee = new TradeStateWithFee(newCounterPartyTradeState, counterPartyTradeStateWithFee.getFee());
                        tradeQueueState.addSellTrade(newCounterPartyTradeStateWithFee);
                    }
                } else {
                    tradeQueueState.removeBuyTrade(counterPartyTradeStateWithFee);
                    if (newCounterPartyTradeState.stockQuantity != 0) {
                        TradeStateWithFee newCounterPartyTradeStateWithFee = new TradeStateWithFee(newCounterPartyTradeState, counterPartyTradeStateWithFee.getFee());
                        tradeQueueState.addBuyTrade(newCounterPartyTradeStateWithFee);
                    }
                }

                // tradeState are fully filled and exit the cycle
                if (tradeState.stockQuantity == 0) {
                    break;
                }
            }
            // tradeState are placed in the order book when they are not fully filled
            if (tradeState.stockQuantity > 0) {
                if (tradeType.equals("Buy")) {
                      tradeStateWithFee.setFee(new BigDecimal(1+Math.random()).setScale(2,BigDecimal.ROUND_HALF_UP));
                    tradeQueueState.addBuyTrade(tradeStateWithFee);
                } else {
                    tradeQueueState.addSellTrade(tradeStateWithFee);
                }
            }
            return matchResults;
        }


    }

    @InitiatedBy(MatchOrdersInitiator.class)
    public static class matchOrdersInitiatorResponse extends FlowLogic<SignedTransaction> {

        private FlowSession counterSession;

        public matchOrdersInitiatorResponse(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterSession));
        }
    }

    public static class CopyUtils {
        public static TradeQueueState copy(TradeQueueState tradeQueueState) {
//            TreeMap<BuyOrderKey, TradeState> buyStockList = clone(tradeQueueState.getBuyStockList());
//            TreeMap<SellOrderKey, TradeState> sellStockList = clone(tradeQueueState.getSellStockList());

            TradeQueueState output = new TradeQueueState(tradeQueueState.getSellStockList(), tradeQueueState.getBuyStockList(), tradeQueueState.getStockName(), tradeQueueState.getLinearId(),
                    tradeQueueState.getCreateParty(), tradeQueueState.getParticipantParties());
            return output;
        }

        public static TradeState copy(TradeState tradeState) {
            TradeState newTradeState = new TradeState(tradeState.getInitiatingParty(), tradeState.getCounterParty(), tradeState.getOrderType(),
                    tradeState.getTradeType(), tradeState.stockName, tradeState.stockPrice, tradeState.stockQuantity, tradeState.expirationDate,
                    tradeState.tradeStatus, tradeState.getTradeDate(), tradeState.settlementDate, tradeState.getLinearId());
            return newTradeState;
        }

        public static <T> T clone(T obj) {
            T cloneObj = null;
            ObjectOutputStream obs = null;
            ObjectInputStream ois = null;
            try {
                //write
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                obs = new ObjectOutputStream(out);
                obs.writeObject(obj);

                //create new obj
                ByteArrayInputStream ios = new ByteArrayInputStream(out.toByteArray());
                ois = new ObjectInputStream(ios);

                cloneObj = (T) ois.readObject();
            } catch (Throwable throwable) {
                System.out.println(throwable);
            } finally {
                if (obs != null) {
                    try {
                        obs.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return cloneObj;
        }

    }
}
