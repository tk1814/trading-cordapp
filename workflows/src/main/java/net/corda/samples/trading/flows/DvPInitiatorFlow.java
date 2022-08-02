package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilities;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trading.states.FungibleStockState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/*
 * Initiator Flow class to initiate the stock-money transfer. The stock token would be exchanged with an equivalent amount of
 * fiat currency as mentioned in the trade. The flow takes the linearId of the stock token and the buyer party as the input parameters.
 */

@InitiatingFlow
@StartableByRPC
public class DvPInitiatorFlow extends FlowLogic<String> {

    private final String name;
    private final int stockQuantity;
    private final Party buyer;
    private final BigDecimal cost;

    public DvPInitiatorFlow(String name, int stockQuantity, Party buyer, BigDecimal cost) {
        this.name = name;
        this.stockQuantity = stockQuantity;
        this.buyer = buyer;
        this.cost = cost;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        // Obtain a reference to a notary we wish to use.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // To get the transferring stock, we get the StockState from the vault and get its pointer
        TokenPointer<FungibleStockState> stockPointer = CustomQuery.queryStockPointer(name, getServiceHub());

        // With the pointer, we can create an instance of transferring Amount
        Amount<TokenType> amount = new Amount(stockQuantity, stockPointer);

        // Build the transaction builder
        TransactionBuilder txBuilder = new TransactionBuilder(notary);

        // Create a move token proposal for the stock token using the helper function provided by Token SDK. This would
        // create the movement proposal and would be committed in the ledgers of parties once the transaction in finalized
        MoveTokensUtilities.addMoveFungibleTokens(txBuilder, getServiceHub(), ImmutableList.of(new PartyAndAmount<>(buyer, amount)), getOurIdentity());

        // Initiate a flow session with the buyer to send the stock price and transfer of the fiat currency
        FlowSession buyerSession = initiateFlow(buyer);

        TokenType tokenType = FiatCurrency.Companion.getInstance("USD");
        Amount<TokenType> costPrice = Amount.fromDecimal(cost, tokenType);

        // Send the stock price to the buyer
        buyerSession.send(costPrice);

        // Receive inputStatesAndRef for the fiat currency exchange from the buyer, these would be inputs to the fiat currency exchange transaction
        List<StateAndRef<FungibleToken>> inputs = subFlow(new ReceiveStateAndRefFlow<>(buyerSession));

        // Receive output for the fiat currency from the buyer, this would contain the transferred amount from buyer to yourself
        List<FungibleToken> moneyReceived = buyerSession.receive(List.class).unwrap(value -> value);

        // Create a fiat currency proposal for the stock token using the helper function provided by Token SDK
        MoveTokensUtilities.addMoveTokens(txBuilder, inputs, moneyReceived);

        // Sign the transaction
        SignedTransaction initialSignedTrnx = getServiceHub().signInitialTransaction(txBuilder, getOurIdentity().getOwningKey());

        // Call the CollectSignaturesFlow to receive signature of the buyer
        SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(initialSignedTrnx, ImmutableList.of(buyerSession)));

        // Call finality flow to notarise the transaction
        SignedTransaction stx = subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(buyerSession)));

        // Distribution list is a list of identities that should receive updates. For this mechanism to behave correctly we call the UpdateDistributionListFlow flow
        subFlow(new UpdateDistributionListFlow(stx));

        return "\nThe stocks are sold to " + buyer + "\nTransaction ID: " + stx.getId();
    }
}