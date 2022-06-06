package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.samples.trading.contracts.TradeContract;
import net.corda.samples.trading.states.TradeState;

import java.security.PublicKey;
import java.util.List;

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
@StartableByRPC
@InitiatingFlow
public class TradeFlow extends FlowLogic<SignedTransaction> {

    /**
     * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
     * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
     */

    private final TradeState tradeState;

    public TradeFlow(TradeState tradeState) {
        this.tradeState = tradeState;
    }

    private final Step GENERATING_TRANSACTION = new Step("Generating transaction based on new Trade.");
    private final Step VERIFYING_TRANSACTION = new Step("Verifying contract constraints.");
    private final Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.");
    private final Step GATHERING_SIGS = new Step("Gathering the counterparty's signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.tracker();
        }
    };
    private final Step FINALISING_TRANSACTION = new Step("Obtaining notary signature and recording transaction.") {
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

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        // Obtain a reference to the notary we want to use.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We get a reference to our own identity.
        Party initiatingParty = getOurIdentity();

        // Stage 1.
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        TradeContract.Create command = new TradeContract.Create();
        List<PublicKey> requiredSigns = ImmutableList.of(initiatingParty.getOwningKey(), tradeState.getCounterParty().getOwningKey());

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                .addOutputState(tradeState, TradeContract.ID)
                .addCommand(command, requiredSigns);
        // Stage 2.
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        transactionBuilder.verify(getServiceHub());

        // Stage 3.
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        SignedTransaction partlySignedTx = getServiceHub().signInitialTransaction(transactionBuilder);

        // Stage 4.
        progressTracker.setCurrentStep(GATHERING_SIGS);
        // Send the state to the counterparty, and receive it back with their signature.
        FlowSession otherPartyFlow = initiateFlow(tradeState.getCounterParty());
        SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partlySignedTx, ImmutableSet.of(otherPartyFlow), GATHERING_SIGS.childProgressTracker()));

        // Stage 5.
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        // Notarise and record the transaction in both parties' vaults.
        return subFlow(new FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()));
    }


//    @InitiatedBy(Initiator::class)
//    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
//        @Suspendable
//        override fun call(): SignedTransaction {
//            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
//                override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                    val output = stx.tx.outputs.single().data
//                    "This must be an Trade transaction." using (output is TradeState)
//                }
//            }
//            return subFlow(signTransactionFlow)
//        }
//    }


}
