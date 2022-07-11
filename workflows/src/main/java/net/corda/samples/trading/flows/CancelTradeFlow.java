package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trading.contracts.TradeContract;
import net.corda.samples.trading.states.TradeState;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CancelTradeFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class CancelInitiator extends FlowLogic<SignedTransaction> {

        private final TradeState cancelTradeState;

        public CancelInitiator(TradeState tradeState) {
            this.cancelTradeState = tradeState;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Generate a transaction by taking the current state
            List<StateAndRef<TradeState>> inputTradeStateList = getServiceHub().getVaultService().queryBy(TradeState.class).getStates().stream()
                    .filter(x -> !x.getState().getData().getInitiatingParty().equals(this.cancelTradeState.getCounterParty()))
                    .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Pending"))
                    .filter(x -> x.getState().getData().getTradeId().equals(this.cancelTradeState.getTradeId()))
                    .collect(Collectors.toList());

            if (inputTradeStateList.isEmpty()) {
                throw new RuntimeException("Trade state with trade ID: " + this.cancelTradeState.getTradeId() + " was not found in the vault.");
            }
            StateAndRef<TradeState> inputTradeState = inputTradeStateList.get(0);

            TradeContract.Commands.CancelTrade command = new TradeContract.Commands.CancelTrade();
            List<PublicKey> requiredSigns = ImmutableList.of(cancelTradeState.getInitiatingParty().getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(cancelTradeState, TradeContract.ID)
                    .addInputState(inputTradeState)
                    .addCommand(command, requiredSigns);

            txBuilder.verify(getServiceHub());

            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Fetch all parties from the network map and remove the initiating party and notary.
            // All the parties are added as  participants to the trade state so that it's visible to all the parties in the network.
            List<Party> parties = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                    .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                    .collect(Collectors.toList());
            parties.remove(getOurIdentity());
            parties.remove(notary);

            // Call finality Flow to notarise the transaction and record it in all participants' ledger.
            List<FlowSession> counterPartySessions = new ArrayList<>();
            for (Party party : parties) {
                counterPartySessions.add(initiateFlow(party));
            }

            // Notarise and record the transaction in every parties' vaults.
            return subFlow(new FinalityFlow(signedTx, counterPartySessions));
        }
    }

    @InitiatedBy(CancelTradeFlow.CancelInitiator.class)
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
            subFlow(new ReceiveTransactionFlow(counterpartySession, true, StatesToRecord.ALL_VISIBLE));
            return null;
        }
    }
}

