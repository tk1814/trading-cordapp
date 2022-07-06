package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Currency;
import java.util.List;

/*
 * Responder Flow for the stock in exchange for fiat-currency. This flow receives the cost of stocks from
 * the seller and transfer the equivalent amount of fiat currency to the seller.
 */

@InitiatedBy(DvPInitiatorFlow.class)
public class DvPResponderFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession counterpartySession;

    public DvPResponderFlow(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        // Receive the cost of the stocks
        Amount<TokenType> cost = counterpartySession.receive(Amount.class).unwrap(amount -> amount);

        // Generate the move proposal, it returns the input-output pair for the fiat currency transfer, which we need to send to the Initiator
        Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs = new DatabaseTokenSelection(getServiceHub())
                // here we are generating input and output states which send the correct amount to the seller, and any change back to buyer
                .generateMove(Collections.singletonList(new Pair<>(counterpartySession.getCounterparty(), cost)), getOurIdentity());

        // Call SendStateAndRefFlow to send the inputs to the Initiator
        subFlow(new SendStateAndRefFlow(counterpartySession, inputsAndOutputs.getFirst()));

        // Send the output generated from the fiat currency move proposal to the initiator
        counterpartySession.send(inputsAndOutputs.getSecond());
        subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        return subFlow(new ReceiveFinalityFlow(counterpartySession));
    }
}