package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trading.contracts.StockContract;
import net.corda.samples.trading.states.StockState;
import net.corda.samples.trading.states.TradeState;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DvPTradeFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class TransferInitiator extends FlowLogic<Void> {

        private final TradeState counterTradeState;

        public TransferInitiator(TradeState counterTradeState) {
            this.counterTradeState = counterTradeState;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            List<PublicKey> requiredSigns = ImmutableList.of(getOurIdentity().getOwningKey());
            StockContract.Commands.Transfer command = new StockContract.Commands.Transfer();

            SignedTransaction signedTxSeller;
            SignedTransaction signedTxBuyer;
            List<FlowSession> counterPartySessionsSeller;
            List<FlowSession> counterPartySessionsBuyer;

            // Initiating Party B sells [SellQuantity] stocks, CounterParty A buys them
            // Money is transferred from CounterParty to Initiating Party
            if (counterTradeState.getSellQuantity() != 0) {

                // Obtain current StockState of seller from vault
                // StockStates from all parties should be there
                List<StateAndRef<StockState>> inputStockStateListSeller = getServiceHub().getVaultService().queryBy(StockState.class).getStates().stream()
                        .filter(x -> x.getState().getData().getOwner().equals(counterTradeState.getInitiatingParty()))
                        .collect(Collectors.toList());
                System.out.println(getServiceHub().getVaultService().queryBy(StockState.class).getStates().size());
                if (inputStockStateListSeller.isEmpty() || inputStockStateListSeller.get(0).getState().getData().getAmount() < counterTradeState.getSellQuantity()) {
                    throw new RuntimeException("Initiating Party does not have enough stocks to sell.");
                }
                int stockAmountSeller = inputStockStateListSeller.get(0).getState().getData().getAmount();

                // Obtain current StockState of buyer from vault
                List<StateAndRef<StockState>> inputStockStateListBuyer = getServiceHub().getVaultService().queryBy(StockState.class).getStates().stream()
                        .filter(x -> x.getState().getData().getOwner().equals(counterTradeState.getCounterParty()))
                        .collect(Collectors.toList());

                int stockAmountBuyer = 0;
                if (!inputStockStateListBuyer.isEmpty()) {
                    stockAmountBuyer = inputStockStateListBuyer.get(0).getState().getData().getAmount();
                }

                // TODO: assuming counterParty/initiatingParty have enough money balance to perform the transaction
                // TODO: 1st: Transfer money: sellValue, 2nd: Transfer stocks
                // TODO: maybe allow owner of stockstate to sign the tx

                // create new StockState for Initiating Party with fewer stocks
                StockState newStockStateSeller = new StockState(counterTradeState.getInitiatingParty(), counterTradeState.getInitiatingParty(),
                        stockAmountSeller - counterTradeState.getSellQuantity());
                // create new StockState for CounterParty with more stocks
                StockState newStockStateBuyer = new StockState(counterTradeState.getCounterParty(), counterTradeState.getCounterParty(),
                        stockAmountBuyer + counterTradeState.getSellQuantity());

                TransactionBuilder txBuilderSeller = new TransactionBuilder(notary)
                        .addOutputState(newStockStateSeller, StockContract.ID)
                        .addInputState(inputStockStateListSeller.get(0))
                        .addCommand(command, requiredSigns);
                TransactionBuilder txBuilderBuyer = new TransactionBuilder(notary)
                        .addOutputState(newStockStateBuyer, StockContract.ID)
                        .addInputState(inputStockStateListBuyer.get(0))
                        .addCommand(command, requiredSigns);

                txBuilderSeller.verify(getServiceHub());
                txBuilderBuyer.verify(getServiceHub());
                signedTxSeller = getServiceHub().signInitialTransaction(txBuilderSeller);
                signedTxBuyer = getServiceHub().signInitialTransaction(txBuilderBuyer);

                // All the parties are added as participants to the stockState state so that it's visible to all the parties in the network.
                List<Party> parties = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                        .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                        .collect(Collectors.toList());
                parties.remove(getOurIdentity());
                parties.remove(notary);

                // Record tx in all participants' ledger.
                counterPartySessionsSeller = new ArrayList<>();
                counterPartySessionsBuyer = new ArrayList<>();
                for (Party party : parties) {
                    counterPartySessionsSeller.add(initiateFlow(party));
                    counterPartySessionsBuyer.add(initiateFlow(party));
                }
            }

            // Initiating Party buys stocks [BuyQuantity], CounterParty sells stocks,
            // Money is transferred from Initiating Party to CounterParty
            else if (counterTradeState.getBuyQuantity() != 0) {

                // Obtain current StockState of seller from vault
                List<StateAndRef<StockState>> inputStockStateListSeller = getServiceHub().getVaultService().queryBy(StockState.class).getStates().stream()
                        .filter(x -> x.getState().getData().getOwner().equals(counterTradeState.getCounterParty()))
                        .collect(Collectors.toList());
                System.out.println(getServiceHub().getVaultService().queryBy(StockState.class).getStates().size());
                if (inputStockStateListSeller.isEmpty() || inputStockStateListSeller.get(0).getState().getData().getAmount() < counterTradeState.getBuyQuantity()) {
                    throw new RuntimeException("CounterParty does not have enough stocks to sell.");
                }
                int stockAmountSeller = inputStockStateListSeller.get(0).getState().getData().getAmount();

                // Obtain current StockState of buyer from vault
                List<StateAndRef<StockState>> inputStockStateListBuyer = getServiceHub().getVaultService().queryBy(StockState.class).getStates().stream()
                        .filter(x -> x.getState().getData().getOwner().equals(counterTradeState.getInitiatingParty()))
                        .collect(Collectors.toList());

                int stockAmountBuyer = 0;
                if (!inputStockStateListBuyer.isEmpty()) {
                    stockAmountBuyer = inputStockStateListBuyer.get(0).getState().getData().getAmount();
                } // if inputStockStateListBuyer is empty, no stocks were issued OR when stocks were issued, the tx was not stored in counterParty's vault

                // create new StockState for CounterParty with fewer stocks
                StockState newStockStateSeller = new StockState(counterTradeState.getCounterParty(), counterTradeState.getCounterParty(),
                        stockAmountSeller - counterTradeState.getBuyQuantity());
                // create new StockState for Initiating Party with more stocks
                StockState newStockStateBuyer = new StockState(counterTradeState.getInitiatingParty(), counterTradeState.getInitiatingParty(),
                        stockAmountBuyer + counterTradeState.getBuyQuantity());

                TransactionBuilder txBuilderSeller = new TransactionBuilder(notary)
                        .addOutputState(newStockStateSeller, StockContract.ID)
                        .addInputState(inputStockStateListSeller.get(0))
                        .addCommand(command, requiredSigns);
                TransactionBuilder txBuilderBuyer = new TransactionBuilder(notary)
                        .addOutputState(newStockStateBuyer, StockContract.ID)
                        .addInputState(inputStockStateListBuyer.get(0)) // TODO: inputStockStateListBuyer it is empty and triggers exception
                        .addCommand(command, requiredSigns);

                txBuilderSeller.verify(getServiceHub());
                txBuilderBuyer.verify(getServiceHub());

                signedTxSeller = getServiceHub().signInitialTransaction(txBuilderSeller);
                signedTxBuyer = getServiceHub().signInitialTransaction(txBuilderBuyer);

                // All the parties are added as participants to the stockState state so that it's visible to all the parties in the network.
                List<Party> parties = getServiceHub().getNetworkMapCache().getAllNodes().stream() // this should not be called at the start of the flow
                        .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                        .collect(Collectors.toList());
                parties.remove(getOurIdentity());
                parties.remove(notary);

                // Record tx in all participants' ledger.
                counterPartySessionsSeller = new ArrayList<>();
                counterPartySessionsBuyer = new ArrayList<>();
                for (Party party : parties) {
                    counterPartySessionsSeller.add(initiateFlow(party));
                    counterPartySessionsBuyer.add(initiateFlow(party));
                }
            } else
                throw new RuntimeException("No BUY or SELL trade operation is happening.");

            subFlow(new FinalityFlow(signedTxSeller, counterPartySessionsSeller));
            subFlow(new FinalityFlow(signedTxBuyer, counterPartySessionsBuyer));
            return null;
        }
    }

    @InitiatedBy(TransferInitiator.class)
    public static class Responder extends FlowLogic<Void> {
        private final FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Receive the transaction and store all its states.
            // If we don't pass `ALL_VISIBLE`, only the states for which the node is one of the `participants` will be stored.
            // subFlow(new ReceiveFinalityFlow(counterpartySession, null, StatesToRecord.ALL_VISIBLE)); // DID NOT BROADCAST
            subFlow(new ReceiveTransactionFlow(counterpartySession, false, StatesToRecord.ALL_VISIBLE));
            return null;
        }
    }
}


