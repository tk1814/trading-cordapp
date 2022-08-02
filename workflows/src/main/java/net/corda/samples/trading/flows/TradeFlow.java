package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.samples.trading.contracts.TradeContract;
import net.corda.samples.trading.states.TradeState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This flow allows two parties (the [Creator] and the [CounterParty]) to come to an agreement about the Trade.
 * <p>
 * In this simple trading example, the [CounterParty] always accepts a valid Trade.
 * <p>
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 * <p>
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
public class TradeFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */

        private final TradeState tradeState;

        public Initiator(TradeState tradeState) {
            this.tradeState = tradeState;
        }

        private final Step GENERATING_TRANSACTION = new Step("Generating transaction based on new Trade.");
        private final Step VERIFYING_TRANSACTION = new Step("Verifying contract constraints.");
        private final Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.");
        private final Step GATHERING_SIGS = new Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION = new Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION);

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Obtain a reference to a notary we wish to use.
            /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            Party initiatingParty = getOurIdentity();

            final Command<TradeContract.Commands.Create> txCommand = new Command<>(
                    new TradeContract.Commands.Create(),
                    initiatingParty.getOwningKey());

            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(tradeState, TradeContract.ID)
                    .addCommand(txCommand);

            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4.
            // Fetch all parties from the network map and remove the initiating party and notary.
            // All the parties are added as  participants to the trade state so that it's visible to all the parties in the network.
            List<Party> parties = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                    .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                    .collect(Collectors.toList());
            parties.remove(initiatingParty);
            parties.remove(notary);

            // Call finality Flow to notarise the transaction and record it in all participants' ledger.
            List<FlowSession> counterPartySessions = new ArrayList<>();
            for (Party party : parties) {
                counterPartySessions.add(initiateFlow(party));
            }

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in every parties' vaults.
            return subFlow(new FinalityFlow(signedTx, counterPartySessions));

        }
    }

    @InitiatedBy(Initiator.class)
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
