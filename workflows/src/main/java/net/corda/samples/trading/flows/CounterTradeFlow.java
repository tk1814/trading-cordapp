package net.corda.samples.trading.flows;

import com.google.common.collect.*;
import net.corda.core.flows.*;
import net.corda.core.identity.*;
import net.corda.core.node.services.*;
import net.corda.core.transactions.*;
import net.corda.core.utilities.*;
import net.corda.samples.trading.contracts.*;
import net.corda.samples.trading.states.*;

import java.security.*;
import java.util.*;
import java.util.stream.*;

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the Trade encapsulated
 * within an [TradeState].
 * <p>
 * In our simple trading, the [Acceptor] always accepts a valid Trade.
 * <p>
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 * <p>
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */

@InitiatingFlow
@StartableByRPC
public class CounterTradeFlow extends FlowLogic<SignedTransaction> {

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


    public CounterTradeFlow(TradeState tradeState) {
        this.counterTradeState = tradeState;
    }

    @Override
    public SignedTransaction call() throws FlowException {
        // Obtain a reference to the notary we want to use.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We get a reference to our own identity.
        Party initiatingParty = getOurIdentity();

        // Stage 1.
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        VaultService vaultService = getServiceHub().getVaultService();

        TradeState inputTrade = vaultService.queryBy(TradeState.class).getStates().stream()
                .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("PENDING"))
                .filter(x -> x.getState().getData().getTradeId().equals(this.counterTradeState.getTradeId()))
                .collect(Collectors.toList())
                .get(0).getState().getData();

        counterTradeState.setTradeId(inputTrade.getTradeId());

        TradeContract.CounterTrade command = new TradeContract.CounterTrade();
        List<PublicKey> requiredSigns = ImmutableList.of(initiatingParty.getOwningKey(), counterTradeState.getCounterParty().getOwningKey());

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                .addOutputState(counterTradeState, TradeContract.ID)
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
        FlowSession otherPartyFlow = initiateFlow(counterTradeState.getCounterParty());
        SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partlySignedTx, ImmutableSet.of(otherPartyFlow), GATHERING_SIGS.childProgressTracker()));

        // Stage 5.
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        // Notarise and record the transaction in both parties' vaults.
        return subFlow(new FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()));

    }

}
