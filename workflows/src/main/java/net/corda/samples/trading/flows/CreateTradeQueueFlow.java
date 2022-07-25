package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trading.contracts.TradeQueueContract;
import net.corda.samples.trading.entity.BuyOrderKey;
import net.corda.samples.trading.entity.SellOrderKey;
import net.corda.samples.trading.states.TradeQueueState;
import net.corda.samples.trading.states.TradeState;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class CreateTradeQueueFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class CreateTradeQueueInitiator extends FlowLogic<SignedTransaction> {

        private final String stockName;

        public CreateTradeQueueInitiator(String stockName) {
            this.stockName = stockName;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            Party creatorParty = getOurIdentity();

            List<Party> participants = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                    .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                    .collect(Collectors.toList());
            participants.remove(creatorParty);
            participants.remove(notary);

//            TreeMap<BuyOrderKey, TradeState> sellQueue = new TreeMap<BuyOrderKey, TradeState>(new Comparator<BuyOrderKey>() {
//                @Override
//                public int compare(BuyOrderKey o1, BuyOrderKey o2) {
//                    //lowest price at first
//                    BigDecimal data1 = new BigDecimal(o1.price);
//                    BigDecimal data2 = new BigDecimal(o2.price);
//                    int result = data1.compareTo(data2);
//                    result = (result == 0) ? o1.tradeDate.compareTo(o2.tradeDate) : result;
//                    return result;
//                }
//            });
//
//            TreeMap<BuyOrderKey, TradeState> buyQueue = new TreeMap<BuyOrderKey, TradeState> (new Comparator<BuyOrderKey>() {
//                @Override
//                public int compare(BuyOrderKey o1, BuyOrderKey o2) {
//                    //highest price as first
//                    BigDecimal data1 = new BigDecimal(o1.price);
//                    BigDecimal data2 = new BigDecimal(o2.price);
//                    int result = data2.compareTo(data1);
//                    result = (result == 0) ? o1.tradeDate.compareTo(o2.tradeDate) : result;
//                    return result;
//                }
//            });

            //create state
            TradeQueueState tradeQueueState = new TradeQueueState(new TreeMap<SellOrderKey, TradeState>(), new TreeMap<BuyOrderKey, TradeState>(),
                    stockName, new UniqueIdentifier(null, UUID.randomUUID()), creatorParty, participants);

            //create txn
            TransactionBuilder builder = new TransactionBuilder(notary)
                    .addOutputState(tradeQueueState)
                    .addCommand(new TradeQueueContract.Commands.createQueue(), Arrays.asList(creatorParty.getOwningKey()));

            //verify txn
            builder.verify(getServiceHub());

            //sign txn
            SignedTransaction selfsignedTransaction = getServiceHub().signInitialTransaction(builder);

            // Call finality Flow to notarise the transaction and record it in all participants ledger.
            List<FlowSession> sessions = new ArrayList<>();
            for (Party party : participants) {
                sessions.add(initiateFlow(party));
            }

            return subFlow(new FinalityFlow(selfsignedTransaction, sessions));
        }
    }

    @InitiatedBy(CreateTradeQueueInitiator.class)
    public static class CreateTradeQueueFlowResponder extends FlowLogic<SignedTransaction> {

        private FlowSession counterSession;

        public CreateTradeQueueFlowResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterSession));
        }
    }
}
