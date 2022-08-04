package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.vault.*;
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria;

import static java.util.Collections.singletonList;

import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.trading.contracts.TradeContract;
import net.corda.samples.trading.states.TradeState;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.MAX_PAGE_SIZE;

/**
 * This flow allows two parties (the [Initiator] and the [Responder]) to come to an agreement about the Trade encapsulated
 * within an [TradeState].
 * <p>
 * In our simple trading, the [Responder] always accepts a valid Trade.
 * <p>
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 * <p>
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
public class CounterTradeFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class CounterInitiator extends FlowLogic<SignedTransaction> {

        private final TradeState tradeState;

        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new Trade.");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };

        ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION);

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        public CounterInitiator(TradeState tradeState) {
            this.tradeState = tradeState;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            // Generate a transaction by taking the current state and check that the incoming counterTradeState matches with the TradeState in the vault
            List<UniqueIdentifier> linearId = singletonList(tradeState.getLinearId());
            QueryCriteria linearCriteriaAll = new LinearStateQueryCriteria(null, linearId, Vault.StateStatus.UNCONSUMED, null);
            PageSpecification pageSpec = new PageSpecification(DEFAULT_PAGE_NUM, MAX_PAGE_SIZE);
            Vault.Page<TradeState> results = getServiceHub().getVaultService().queryBy(TradeState.class, linearCriteriaAll, pageSpec);
            List<StateAndRef<TradeState>> inputTradeStateList = results.getStates().stream()
                    .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Pending"))
                    .collect(Collectors.toList());

            if (inputTradeStateList.isEmpty()) {
                throw new RuntimeException("Trade state with trade ID: " + tradeState.getLinearId() + " was not found in the vault.");
            }
            StateAndRef<TradeState> inputTradeState = inputTradeStateList.get(0);
            TradeState input = inputTradeState.getState().getData();

            TradeContract.Commands.CounterTrade command = new TradeContract.Commands.CounterTrade();
            List<PublicKey> requiredSigns = ImmutableList.of(tradeState.getInitiatingParty().getOwningKey(), tradeState.getCounterParty().getOwningKey());

            TransactionBuilder txBuilder;
            //if the match quantity is not the same as the input quantity, there should be one more output to be match in the later transaction
            if (input.getStockQuantity() > tradeState.getStockQuantity()) {
                int unConsumedQuantity = new BigDecimal(input.getStockQuantity()).subtract(new BigDecimal(tradeState.getStockQuantity())).intValue();

                TradeState unConsumedTradeState = new TradeState(tradeState.getInitiatingParty(), tradeState.getCounterParty(), tradeState.getOrderType(),
                        tradeState.getTradeType(), tradeState.stockName, input.getStockPrice(), unConsumedQuantity, input.getExpirationDate(),
                        input.getTradeStatus(), input.tradeDate, input.settlementDate, input.getLinearId());

                txBuilder = new TransactionBuilder(notary)
                        .addOutputState(tradeState, TradeContract.ID)
                        .addOutputState(unConsumedTradeState, TradeContract.ID)
                        .addInputState(inputTradeState)
                        .addCommand(command, requiredSigns);

            } else if (input.getStockQuantity() == tradeState.getStockQuantity()) {
                txBuilder = new TransactionBuilder(notary)
                        .addOutputState(tradeState, TradeContract.ID)
                        .addInputState(inputTradeState)
                        .addCommand(command, requiredSigns);
            } else {
                throw new RuntimeException("stock name" + input.getStockName() + " encountered error in transaction: " +
                        "intput has " + input.getStockQuantity() + " of stocks while output has " + tradeState.getStockQuantity());
            }


            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            txBuilder.verify(getServiceHub());

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the initiating party, and receive it back with their signature.

            FlowSession flowSession;
//            FlowSession flowSession = initiateFlow(tradeState.getCounterParty());
            if (getOurIdentity().equals(tradeState.getInitiatingParty()))
                flowSession = initiateFlow(tradeState.getCounterParty());
            else if (getOurIdentity().equals(tradeState.getCounterParty()))
                flowSession = initiateFlow(tradeState.getInitiatingParty());
            else
                throw new RuntimeException("Trade settlement flow is called by a third party - not involved in the transaction");

            SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(flowSession), GATHERING_SIGS.childProgressTracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(flowSession), FINALISING_TRANSACTION.childProgressTracker()));
        }
    }

    @InitiatedBy(CounterInitiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartySession;

        public Responder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an trade transaction.", output instanceof TradeState);
                        TradeState trade = (TradeState) output;
                        require.using("I won't accept trade with a negative value", trade.getStockPrice() >= 0);
                        return null;
                    });
                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();

            return subFlow(new ReceiveFinalityFlow(otherPartySession, txId));
        }
    }
}
