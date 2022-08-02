package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilities;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trading.states.FungibleStockState;
import net.corda.samples.trading.states.TradeState;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface DvPBuyerFlow {

    @InitiatingFlow
    @StartableByRPC
    class BuyStock extends FlowLogic<String> {

        private final TradeState sellerTradeState;
        private final Party seller;
        private final BigDecimal cost;

        public BuyStock(TradeState sellerTradeState, Party seller, BigDecimal cost) {
            this.sellerTradeState = sellerTradeState;
            this.seller = seller;
            this.cost = cost;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {

            // To get the transferring stock, we get the StockState from the vault and get its pointer
//            TokenPointer<FungibleStockState> stockPointer = CustomQuery.queryStockPointer(name, getServiceHub());
            FlowSession sellerSession = initiateFlow(seller);
            sellerSession.send(sellerTradeState);

            List<StateAndRef<FungibleToken>> stockInputs = subFlow(new ReceiveStateAndRefFlow<>(sellerSession));
            final List<FungibleToken> stockOutputs = sellerSession.receive(List.class).unwrap(it -> it);

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder txBuilder = new TransactionBuilder(notary);
            MoveTokensUtilities.addMoveTokens(txBuilder, stockInputs, stockOutputs);

            TokenType currencyTokenType = FiatCurrency.Companion.getInstance("USD");
            Amount<TokenType> costAmount = Amount.fromDecimal(cost, currencyTokenType);

            MoveTokensUtilities.addMoveFungibleTokens(txBuilder, getServiceHub(), ImmutableList.of(new PartyAndAmount<>(seller, costAmount)), getOurIdentity());

            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder,
                    getOurIdentity().getOwningKey());
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    Collections.singletonList(sellerSession)));

            // Finalise the transaction
            final SignedTransaction notarised = subFlow(new FinalityFlow(
                    fullySignedTx, Collections.singletonList(sellerSession)));

            // Distribute updates of the evolvable token.
            subFlow(new UpdateDistributionListFlow(notarised));

            // Distribution list is a list of identities that should receive updates. For this mechanism to behave correctly we call the UpdateDistributionListFlow flow
            subFlow(new UpdateDistributionListFlow(notarised));
            return "\nThe stocks are sold to " + getOurIdentity() + "\nTransaction ID: " + notarised.getId();
        }
    }

    @InitiatedBy(BuyStock.class)
    class SellStock extends FlowLogic<SignedTransaction> {

        @NotNull
        private FlowSession counterpartySession;

        public SellStock(@NotNull FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            TradeState sellerTradeState = counterpartySession.receive(TradeState.class).unwrap(it -> it);
            int stockQuantity = sellerTradeState.getStockQuantity();
            String stockName = sellerTradeState.getStockName();

            TokenPointer<FungibleStockState> stockPointer = CustomQuery.queryStockPointer(stockName, getServiceHub());
            Amount<TokenType> amount = new Amount<>(stockQuantity, stockPointer);

            // send stock inputandoutput
            Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs = new DatabaseTokenSelection(getServiceHub())
                    .generateMove(Collections.singletonList(new Pair<>(counterpartySession.getCounterparty(), amount)),
                            getOurIdentity());
            subFlow(new SendStateAndRefFlow(counterpartySession, inputsAndOutputs.getFirst()));
            counterpartySession.send(inputsAndOutputs.getSecond());


            final SecureHash signedTxId = subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // Custom Logic to validate transaction.
                }
            }).getId();

            return subFlow(new ReceiveFinalityFlow(counterpartySession, signedTxId));
        }

    }
}
