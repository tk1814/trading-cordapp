package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.trading.contracts.TradeContract;
import net.corda.samples.trading.states.TradeState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

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

        private final TradeState counterTradeState;

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
            this.counterTradeState = tradeState;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            // Generate a transaction by taking the current state and check that the incoming counterTradeState matches with the TradeState in the vault
            List<StateAndRef<TradeState>> inputTradeStateList = getServiceHub().getVaultService().queryBy(TradeState.class).getStates().stream()
                    .filter(x -> !x.getState().getData().getInitiatingParty().equals(counterTradeState.getCounterParty()))
                    .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Pending"))
                    .filter(x -> x.getState().getData().getTradeId().equals(counterTradeState.getTradeId()))
                    .filter(x -> x.getState().getData().getInitiatingParty().equals(counterTradeState.getInitiatingParty()))
                    .filter(x -> Objects.equals(x.getState().getData().getCounterParty(), null))
                    .filter(x -> x.getState().getData().getOrderType().equals(counterTradeState.getOrderType()))
                    .filter(x -> x.getState().getData().getTradeType().equals(counterTradeState.getTradeType()))
                    .filter(x -> x.getState().getData().getStockName().equals(counterTradeState.getStockName()))
                    .filter(x -> x.getState().getData().getStockPrice() == counterTradeState.getStockPrice())
                    .filter(x -> x.getState().getData().getStockQuantity() == counterTradeState.getStockQuantity())
                    .filter(x -> x.getState().getData().getExpirationDate().equals(counterTradeState.getExpirationDate()))
                    .filter(x -> x.getState().getData().getTradeDate().equals(counterTradeState.getTradeDate()))
                    .filter(x -> x.getState().getData().getSettlementDate() == null)
                    .collect(Collectors.toList());

            if (inputTradeStateList.isEmpty()) {
                throw new RuntimeException("Trade state with trade ID: " + counterTradeState.getTradeId() + " was not found in the vault.");
            }
            StateAndRef<TradeState> inputTradeState = inputTradeStateList.get(0);

            TradeContract.Commands.CounterTrade command = new TradeContract.Commands.CounterTrade();
            List<PublicKey> requiredSigns = ImmutableList.of(counterTradeState.getInitiatingParty().getOwningKey(), counterTradeState.getCounterParty().getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(counterTradeState, TradeContract.ID)
                    .addInputState(inputTradeState)
                    .addCommand(command, requiredSigns);

            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            txBuilder.verify(getServiceHub());

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the initiating party, and receive it back with their signature.

            FlowSession otherPartyFlow;
            if (getOurIdentity().equals(counterTradeState.getInitiatingParty()))
                otherPartyFlow = initiateFlow(counterTradeState.getCounterParty());
            else if (getOurIdentity().equals(counterTradeState.getCounterParty()))
                otherPartyFlow = initiateFlow(counterTradeState.getInitiatingParty());
            else
                throw new RuntimeException("Trade settlement flow is called by a third party - not involved in the transaction");

            SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartyFlow), GATHERING_SIGS.childProgressTracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartyFlow), FINALISING_TRANSACTION.childProgressTracker()));
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
