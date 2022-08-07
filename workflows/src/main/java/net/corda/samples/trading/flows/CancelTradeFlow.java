package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trading.contracts.TradeContract;
import net.corda.samples.trading.states.TradeState;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.MAX_PAGE_SIZE;

public class CancelTradeFlow {
    @SchedulableFlow
    @InitiatingFlow
    @StartableByRPC
    public static class CancelInitiator extends FlowLogic<SignedTransaction> {

        private final String tradeStatus;
        private final UniqueIdentifier linearId;

        public CancelInitiator(String tradeStatus, UniqueIdentifier linearId) {
            this.tradeStatus = tradeStatus;
            this.linearId = linearId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            PageSpecification pageSpec = new PageSpecification(DEFAULT_PAGE_NUM, MAX_PAGE_SIZE);
            List<UniqueIdentifier> linearIdList = singletonList(linearId);
            QueryCriteria linearCriteriaAll = new LinearStateQueryCriteria(null, linearIdList, Vault.StateStatus.UNCONSUMED, null);

            // Generate a transaction by taking the current state
            Vault.Page<TradeState> results = getServiceHub().getVaultService().queryBy(TradeState.class, linearCriteriaAll, pageSpec);
            List<StateAndRef<TradeState>> inputTradeStateList = results.getStates().stream()
                    .filter(x -> x.getState().getData().getInitiatingParty().equals(getOurIdentity()))
                    .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Pending"))
                    .collect(Collectors.toList());

            if (inputTradeStateList.isEmpty()) {
                throw new RuntimeException("Trade state with trade ID: " + linearId + " was not found in the vault.");
            }
            StateAndRef<TradeState> inputTradeState = inputTradeStateList.get(0);

            TradeState inputState = inputTradeState.getState().getData();
            if (getOurIdentity().equals(inputState.initiatingParty)) { // check that caller is actually the initiating party

                TradeState outputState = new TradeState(inputState.getInitiatingParty(), null,
                        inputState.getOrderType(), inputState.getTradeType(), inputState.getStockName(),
                        inputState.getStockPrice(), inputState.getStockQuantity(), inputState.getExpirationDate(), tradeStatus,
                        inputState.getTradeDate(), null, linearId);

                TradeContract.Commands.CancelTrade command = new TradeContract.Commands.CancelTrade();
                List<PublicKey> requiredSigns = ImmutableList.of(outputState.getInitiatingParty().getOwningKey());

                TransactionBuilder txBuilder = new TransactionBuilder(notary)
                        .addOutputState(outputState, TradeContract.ID)
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
            } else {
                throw new RuntimeException("Flow called by unauthorised party.");
            }
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

