package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trading.contracts.StockContract;
import net.corda.samples.trading.states.StockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This flow is used to build a transaction to issue a stock on the Corda Ledger, which can later be used in trading.
 * It creates a self issued transaction, the state is only issued on the ledger of the party who executes the flows.
 */
@InitiatingFlow
@StartableByRPC
public class IssueStockFlow extends FlowLogic<SignedTransaction> {

    private final StockState stockState;

    public IssueStockFlow(StockState stockState) {
        this.stockState = stockState;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        // Obtain a reference to a notary we wish to use.
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

        // If a stock issuance is requested, find a previous one in the vault (if exists), put it as InputState to the tx builder,
        // create a new StockState with the added amount (new + old amount) and set the new StockState as OutputState

        // Check if a previous stock state was issued
        List<StateAndRef<StockState>> inputStockStateList = getServiceHub().getVaultService().queryBy(StockState.class).getStates().stream()
                .filter(x -> x.getState().getData().getOwner().equals(stockState.getOwner()))
                .collect(Collectors.toList());

        TransactionBuilder transactionBuilder;

        // If a previous stock state was issued: add the new amount to its amount
        if (!inputStockStateList.isEmpty()) {
            StateAndRef<StockState> inputStockState = inputStockStateList.get(0);

            // new amount = newly issued amount + previous amount
            int newStockAmount = stockState.getAmount() + inputStockState.getState().getData().getAmount();
            StockState newStockState = new StockState(stockState.getIssuer(), stockState.getOwner(), newStockAmount);

            transactionBuilder = new TransactionBuilder(notary)
                    .addOutputState(newStockState, StockContract.ID)
                    .addInputState(inputStockState) // input state should be from vault
                    .addCommand(new StockContract.Commands.Issue(),
                            Arrays.asList(getOurIdentity().getOwningKey()));
        } else {
            // Build the transaction, add the output states and the command to the transaction.
            transactionBuilder = new TransactionBuilder(notary)
                    .addOutputState(stockState)
                    .addCommand(new StockContract.Commands.Issue(),
                            Arrays.asList(getOurIdentity().getOwningKey())); // Required Signers
        }

        // Verify the transaction
        transactionBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

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

        // Notarise the transaction and record the states in the ledger.
        return subFlow(new FinalityFlow(signedTransaction, counterPartySessions));
    }

    @InitiatedBy(IssueStockFlow.class)
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