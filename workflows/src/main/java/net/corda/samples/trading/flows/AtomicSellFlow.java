package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.selection.TokenQueryBy;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilities;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import kotlin.Pair;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trading.states.FungibleStockState;
import net.corda.samples.trading.states.TradeQueueState;
import net.corda.samples.trading.states.TradeState;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Initiator Flow class to initiate the stock-money transfer. The stock token would be exchanged with an equivalent amount of
 * fiat currency as mentioned in the trade. The flow takes the linearId of the stock token and the buyer party as the input parameters.
 */


public interface AtomicSellFlow {

    @InitiatingFlow
    @StartableByRPC
    class SellStock extends FlowLogic<String> {

        private final TradeState sellerTrade;
        private final TradeState buyerTrade;
        private final BigDecimal cost;

        public SellStock(TradeState sellerTrade, TradeState buyerTrade, BigDecimal cost) {
            this.sellerTrade = sellerTrade;
            this.buyerTrade = buyerTrade;
            this.cost = cost;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {

            String name = sellerTrade.getStockName();
            Party buyer = sellerTrade.getCounterParty();

            // To get the transferring stock, we get the StockState from the vault and get its pointer
            final TokenPointer<FungibleStockState> stockPointer = CustomQuery.queryStockPointer(name, getServiceHub());
            final StateAndRef<FungibleStockState> stockTokenTypeInfo = stockPointer.getPointer().resolve(getServiceHub());


            //send the latest state of the stock to buyer
            final FlowSession buyerSession = initiateFlow(buyer);
            subFlow(new SendStateAndRefFlow(buyerSession, Collections.singletonList(stockTokenTypeInfo)));
            buyerSession.send(buyerTrade);

            //send the proof to stocks
            final QueryCriteria tokenCriteria = QueryUtilities.heldTokenAmountCriteria(stockPointer, getOurIdentity());
            final List<StateAndRef<FungibleToken>> heldStockTokens = getServiceHub().getVaultService().
                    queryBy(FungibleToken.class, tokenCriteria).getStates();
            if (heldStockTokens.get(0).getState().getData().getAmount().getQuantity() < sellerTrade.getStockQuantity()) {
                throw new FlowException("FungibleToken quantity is not enough");
            }
            subFlow(new SendStateAndRefFlow(buyerSession, heldStockTokens));

            //send the currency desired
            TokenType currencyTokenType = FiatCurrency.Companion.getInstance("USD");
            buyerSession.send(currencyTokenType);
            Amount<TokenType> costAmount = Amount.fromDecimal(cost, currencyTokenType);

            //move stock tokens
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder txBuilder = new TransactionBuilder(notary);

            Amount<TokenType> amount = new Amount<>(sellerTrade.getStockQuantity(), stockPointer);
            PartyAndAmount<TokenType> stock = new PartyAndAmount<>(buyer, amount);

            MoveTokensUtilities.addMoveFungibleTokens(txBuilder, getServiceHub(), ImmutableList.of(stock), getOurIdentity());

            // Let's make sure the buyer is not trying to pass off some of our own dollars as payment... After all, we
            // are going to sign this transaction.
            List<StateAndRef<FungibleToken>> currencyInputs = subFlow(new ReceiveStateAndRefFlow<>(buyerSession));
            long ourCurrencyInputCount = currencyInputs.stream()
                    .filter(it -> it.getState().getData().getHolder().equals(getOurIdentity()))
                    .count();
            if (ourCurrencyInputCount != 0)
                throw new FlowException("The buyer sent us some of our token states: " + ourCurrencyInputCount);
            // Receive the currency states that will go in output.
            // noinspection unchecked
            final List<FungibleToken> currencyOutputs = buyerSession.receive(List.class).unwrap(it -> it);
            final long sumPaid = currencyOutputs.stream()
                    // Are they owned by the seller (in the future)? We don't care about the "change".
                    .filter(it -> it.getHolder().equals(getOurIdentity()))
                    .map(FungibleToken::getAmount)
                    // Are they of the expected currency?
                    .filter(it -> it.getToken().getTokenType().equals(currencyTokenType))
                    .map(Amount::getQuantity)
                    .reduce(0L, Math::addExact);

            if (sumPaid < AmountUtilities.amount(cost, currencyTokenType).getQuantity())
                throw new FlowException("We were paid only " +
                        sumPaid / AmountUtilities.amount(1L, currencyTokenType).getQuantity() +
                        " instead of the expected " + cost);

            MoveTokensUtilities.addMoveTokens(txBuilder, currencyInputs, currencyOutputs);

            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder,
                    getOurIdentity().getOwningKey());
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    Collections.singletonList(buyerSession)));

            // Finalise the transaction
            final SignedTransaction notarised = subFlow(new FinalityFlow(
                    fullySignedTx, Collections.singletonList(buyerSession)));

            // Distribute updates of the evolvable token.
            subFlow(new UpdateDistributionListFlow(notarised));

            return "\nThe stocks are sold to " + buyer + "\nTransaction ID: " + notarised.getId();
        }

    }

    @InitiatedBy(SellStock.class)
    class BuyStock extends FlowLogic<SignedTransaction> {
        @NotNull
        private final FlowSession sellerSession;

        public BuyStock(@NotNull final FlowSession sellerSession) {
            this.sellerSession = sellerSession;
        }

        @Suspendable
        @Override
        @NotNull
        public SignedTransaction call() throws FlowException {

            //step1 verify the buyer's TradeState in the transaction
            final List<StateAndRef<FungibleStockState>> buyerTradeStateStateAndRef = subFlow(new ReceiveStateAndRefFlow<>(sellerSession));
            TradeState buyerTradeState = sellerSession.receive(TradeState.class).unwrap(it -> it);

            StateAndRef<FungibleStockState> stockTokenTypeInfo = buyerTradeStateStateAndRef.get(0);
            String stockName = stockTokenTypeInfo.getState().getData().getName();

            StateAndRef<TradeState> inputBuyerTradeState = getServiceHub().getVaultService().queryBy(TradeState.class).getStates().stream()
                    .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Pending"))
                    .filter(x -> x.getState().getData().getStockName().equals(stockName))
                    .filter(x -> x.getState().getData().getLinearId().equals(buyerTradeState.getLinearId())).findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Trade not found"));
            if (buyerTradeState.getStockQuantity() > inputBuyerTradeState.getState().getData().getStockQuantity() ||
                    buyerTradeState.getStockPrice() > inputBuyerTradeState.getState().getData().getStockPrice()) {
                throw new IllegalArgumentException("The price in the transaction is greater than the bid price or the stock quantity is less than" +
                        "the required quantity");
            }
            //verify the stock
            final List<StateAndRef<FungibleToken>> fungibleStockStateAndRefList = subFlow(new ReceiveStateAndRefFlow<>(sellerSession));
            if (!((TokenPointer<FungibleStockState>) fungibleStockStateAndRefList.get(0).getState().getData().getTokenType()).getPointer().getPointer()
                    .equals(stockTokenTypeInfo.getState().getData().getLinearId())){
                throw new FlowException("The seller does not correspond to the earlier stock info.");
            }

            //step2 get the required currency from seller
            final TokenType currencyTokenType = sellerSession.receive(TokenType.class).unwrap(it -> it);
            BigDecimal amount = new BigDecimal(String.valueOf(buyerTradeState.getStockPrice())).multiply(new BigDecimal(buyerTradeState.getStockQuantity()));
            Amount<TokenType> tokenTypeAmount = Amount.fromDecimal(amount, currencyTokenType);

            final QueryCriteria heldByMe = QueryUtilities.heldTokenAmountCriteria(currencyTokenType, getOurIdentity()).and(QueryUtilities.sumTokenCriteria());
            List<Object> sum = getServiceHub().getVaultService().queryBy(FungibleToken.class, heldByMe).component5();
            if (sum.size() == 0 || (Long) sum.get(0) < AmountUtilities.amount(amount, currencyTokenType).getQuantity()) {
                throw new FlowException("Available token balance of " + getOurIdentity() + " is less than the cost of the ticket. Please ask the Bank to issue some cash if you wish to buy the ticket ");
            }

            //step3 send inputandoutput
            Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs = new DatabaseTokenSelection(getServiceHub())
                    .generateMove(Collections.singletonList(new Pair<>(sellerSession.getCounterparty(), tokenTypeAmount)),
                            getOurIdentity());

            // Send the currency states that will go in input, along with their history.
            subFlow(new SendStateAndRefFlow(sellerSession, inputsAndOutputs.getFirst()));

            // Send the currency states that will go in output.
            sellerSession.send(inputsAndOutputs.getSecond());

            // Sign the received transaction.
            final SecureHash signedTxId = subFlow(new SignTransactionFlow(sellerSession) {
                @Override
                // There is an opportunity for a malicious seller to ask the buyer for information, and then ask them
                // to sign a different transaction. So we have to be careful.
                // Make sure this is the transaction we expect: car, price and states we sent.
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // Recall the inputs we prepared in the first part of the flow.
                    // We can use a Set because all StateRef are truly unique.
                    final Set<StateRef> allKnownInputs = inputsAndOutputs.getFirst().stream()
                            .map(StateAndRef::getRef)
                            .collect(Collectors.toSet());
                    // There should be no extra inputs, other than the car.
                    allKnownInputs.addAll(fungibleStockStateAndRefList.stream().map(StateAndRef::getRef).collect(Collectors.toSet()));
                    final Set<StateRef> allInputs = new HashSet<>(stx.getInputs());
                    if (!allInputs.equals(allKnownInputs))
                        throw new FlowException("Inconsistency in input refs compared to expectation");

                    // Moving on to the outputs.
                    final List<ContractState> allOutputs = stx.getCoreTransaction().getOutputStates();

                    // If we keep only those of the proper currency. We have to use a List and cannot use a Set
                    // because 2 "quarters" are equal to each other.
                    final List<FungibleToken> allCurrencyOutputs = allOutputs.stream()
                            .filter(it -> it instanceof FungibleToken)
                            .map(it -> (FungibleToken) it)
                            .filter(it -> it.getTokenType().equals(currencyTokenType))
                            .sorted(Comparator.comparing(FungibleToken::getAmount))
                            .collect(Collectors.toList());
                    // Let's not pass if we don't recognise the states we gave, with the additional constraint that
                    // they have to be in the same order.
                    if (!inputsAndOutputs.getSecond().stream().sorted(Comparator.comparing(FungibleToken::getAmount)).collect(Collectors.toList())
                            .equals(allCurrencyOutputs))
                        throw new FlowException("Inconsistency in FungibleToken outputs compared to expectation");

                    // If we keep only the stock tokens.
                    final List<FungibleToken> allStockOutputs = allOutputs.stream()
                            .filter(it -> it instanceof FungibleToken)
                            .filter(it -> ((FungibleToken) it).getTokenType().equals(fungibleStockStateAndRefList.get(0).getState().getData().getTokenType()))
                            .map(it -> (FungibleToken) it)
                            .collect(Collectors.toList());
                    // Let's not pass if there is not exactly
                    if (allStockOutputs.size() < 1)
                        throw new FlowException("Wrong count of stock outputs");
                    // And it has to be the stock
                    final FungibleToken outputHeldStock = allStockOutputs.get(0);
                    if (!outputHeldStock.getTokenType().getTokenIdentifier().equals((fungibleStockStateAndRefList.get(0).getState().getData().getTokenType().getTokenIdentifier())))
                        throw new FlowException("This is not the stock we expected");

                }
            }).getId();

            return subFlow(new ReceiveFinalityFlow(sellerSession, signedTxId));
        }
    }


}