package net.corda.samples.trading;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trading.flows.*;
import net.corda.samples.trading.states.FungibleStockState;
import net.corda.samples.trading.states.TradeState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TradeFlowTests {

    protected MockNetwork network;
    protected StartedMockNode notary;
    protected StartedMockNode partyA;
    protected StartedMockNode partyB;
    protected String expirationDate;
    protected String tradeDate;
    protected String settlementDate;
    protected Party notaryParty;

    public static TestIdentity PARTY_A = new TestIdentity(new CordaX500Name("PartyA", "TestVillage", "US"));
    public static TestIdentity PARTY_B = new TestIdentity(new CordaX500Name("PartyB", "TestVillage", "US"));

    public final static String STOCK_SYMBOL = "TEST";
    public final static double STOCK_PRICE = 22.2;
    public final static int ISSUING_STOCK_QUANTITY = 10;
    public final static int TRADING_STOCK_QUANTITY = 4;
    public final static String CURRENCY = "USD";
    public final static Integer ISSUING_MONEY = 300;
    public final static UniqueIdentifier LINEAR_ID = new UniqueIdentifier(null, UUID.fromString("6231f549-9c1b-041f-90dd-1dc728fcbafc"));
    public final static TokenType fiatTokenType = FiatCurrency.Companion.getInstance("USD");
    public static TradeState tradeState = null;
    public static TradeState counterTradeState = null;
    public static TradeState cancelTradeState = null;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withNetworkParameters(new NetworkParameters(
                        4, emptyList(), 1000000000, 1000000000, Instant.now(), 1,
                        emptyMap())).withCordappsForAllNodes(ImmutableList.of(
                        TestCordapp.findCordapp("net.corda.samples.trading.contracts"),
                        TestCordapp.findCordapp("net.corda.samples.trading.flows"),
                        TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                        TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
                )).withThreadPerNode(false)
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB")))));

        partyA = network.createPartyNode(PARTY_A.getName());
        partyB = network.createPartyNode(PARTY_B.getName());
        notary = network.getNotaryNodes().get(0);
        notaryParty = notary.getInfo().getLegalIdentities().get(0);
        expirationDate = "2023-03-03T03:03";
        tradeDate = "2022-07-07T07:07";
        settlementDate = "2022-08-28T08:08";
        network.startNodes();

        tradeState = new TradeState(partyA.getInfo().getLegalIdentities().get(0), null, "Pending Order",
                "Sell", STOCK_SYMBOL, STOCK_PRICE, TRADING_STOCK_QUANTITY, expirationDate, "Pending",
                tradeDate, null, LINEAR_ID);
        cancelTradeState = new TradeState(partyA.getInfo().getLegalIdentities().get(0), null, "Pending Order",
                "Sell", STOCK_SYMBOL, STOCK_PRICE, TRADING_STOCK_QUANTITY, expirationDate, "Cancelled",
                tradeDate, null, LINEAR_ID);
        counterTradeState = new TradeState(partyA.getInfo().getLegalIdentities().get(0), partyB.getInfo().getLegalIdentities().get(0),
                "Pending Order", "Sell", STOCK_SYMBOL, STOCK_PRICE, TRADING_STOCK_QUANTITY, expirationDate, "Accepted",
                tradeDate, settlementDate, LINEAR_ID);
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void issueStockFlowTest() throws ExecutionException, InterruptedException {

        // Issue Stock
        CordaFuture<String> future = partyA.startFlow(new CreateAndIssueStock(STOCK_SYMBOL, ISSUING_STOCK_QUANTITY));
        network.runNetwork();
        String stx = future.get();

        String stxID = stx.substring(stx.lastIndexOf(" ") + 1);
        SecureHash stxIDHash = SecureHash.parse(stxID);

        // Check if stock issuer and observer have recorded the transaction
        SignedTransaction issuerTx = partyA.getServices().getValidatedTransactions().getTransaction(stxIDHash);
        SignedTransaction observerTx = partyB.getServices().getValidatedTransactions().getTransaction(stxIDHash);
        assertNotNull(issuerTx);
        assertNotNull(observerTx);
        assertEquals(issuerTx, observerTx);
    }

    @Test
    public void issueMoneyFlowTest() throws ExecutionException, InterruptedException {

        // Issue Money
        CordaFuture<String> future = partyA.startFlow(new IssueMoney(CURRENCY, ISSUING_MONEY, partyB.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        String stx = future.get();

        String stxID = stx.substring(stx.lastIndexOf(" ") + 1);
        SecureHash stxIDHash = SecureHash.parse(stxID);

        // Check if money issuer and observer have recorded the transaction
        SignedTransaction issuerTx = partyA.getServices().getValidatedTransactions().getTransaction(stxIDHash);
        SignedTransaction observerTx = partyB.getServices().getValidatedTransactions().getTransaction(stxIDHash);
        assertNotNull(issuerTx);
        assertNotNull(observerTx);
        assertEquals(issuerTx, observerTx);
    }

    @Test
    public void tradeFlowTest() throws ExecutionException, InterruptedException {

        // Issue Stock to seller
        CordaFuture<String> future = partyA.startFlow(new CreateAndIssueStock(STOCK_SYMBOL, ISSUING_STOCK_QUANTITY));
        network.runNetwork();
        future.get();

        // Create trade to sell stocks
        CordaFuture<SignedTransaction> futureA = partyA.startFlow(new TradeFlow.Initiator(tradeState));
        network.runNetwork();
        SignedTransaction stx = futureA.get();
        SecureHash stxID = stx.getId();

        // Check if initiator and observer have recorded the transaction
        SignedTransaction initiatorTx = partyA.getServices().getValidatedTransactions().getTransaction(stxID);
        SignedTransaction observerTx = partyB.getServices().getValidatedTransactions().getTransaction(stxID);
        assertNotNull(initiatorTx);
        assertNotNull(observerTx);
        assertEquals(initiatorTx, observerTx);

        // Retrieve trade state from initiator's vault
        List<StateAndRef<TradeState>> remainingTradeStatesPages = partyA.getServices().getVaultService().queryBy(TradeState.class).getStates();
        TradeState remainingTradeState = remainingTradeStatesPages.get(0).getState().getData();

        // Check trade state
        assertEquals(remainingTradeState.toString(), tradeState.toString());
    }

    @Test
    public void counterTradeFlowTest() throws ExecutionException, InterruptedException {

        // Issue Stock to seller
        CordaFuture<String> future = partyA.startFlow(new CreateAndIssueStock(STOCK_SYMBOL, ISSUING_STOCK_QUANTITY));
        network.runNetwork();
        future.get();

        // Create trade to sell stocks
        CordaFuture<SignedTransaction> futureA = partyA.startFlow(new TradeFlow.Initiator(tradeState));
        network.runNetwork();
        futureA.get();

        // Issue Money to buyer
        CordaFuture<String> futureB = partyB.startFlow(new IssueMoney(CURRENCY, ISSUING_MONEY, partyB.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        futureB.get();

        // Counter trade to buy stocks
        CordaFuture<SignedTransaction> futureCounterTrade = partyB.startFlow(new CounterTradeFlow.CounterInitiator(counterTradeState));
        network.runNetwork();
        SignedTransaction stx = futureCounterTrade.get();
        SecureHash stxID = stx.getId();

        // Check if initiator and counterparty have recorded the transaction
        SignedTransaction initiatorTx = partyA.getServices().getValidatedTransactions().getTransaction(stxID);
        SignedTransaction counterPartyTx = partyB.getServices().getValidatedTransactions().getTransaction(stxID);
        assertNotNull(initiatorTx);
        assertNotNull(counterPartyTx);
        assertEquals(initiatorTx, counterPartyTx);

        // Retrieve trade state from counterparty's vault
        List<StateAndRef<TradeState>> remainingTradeStatesPages = partyB.getServices().getVaultService().queryBy(TradeState.class).getStates();
        TradeState remainingTradeState = remainingTradeStatesPages.get(0).getState().getData();

        // Check counter trade state
        assertEquals(remainingTradeState.toString(), counterTradeState.toString());
    }

    @Test
    public void DvPTest() throws ExecutionException, InterruptedException {

        // Issue Stock to seller
        CordaFuture<String> futureA = partyA.startFlow(new CreateAndIssueStock(STOCK_SYMBOL, ISSUING_STOCK_QUANTITY));
        network.runNetwork();
        futureA.get();

        // Issue Money to buyer
        CordaFuture<String> futureB = partyB.startFlow(new IssueMoney(CURRENCY, ISSUING_MONEY, partyB.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        futureB.get();

        // Move Stock to buyer
        futureA = partyA.startFlow(new DvPInitiatorFlow(STOCK_SYMBOL, TRADING_STOCK_QUANTITY, partyB.getInfo().getLegalIdentities().get(0), TRADING_STOCK_QUANTITY * STOCK_PRICE));
        network.runNetwork();
        String moveTx = futureA.get();

        // Retrieve stock states from buyer
        List<StateAndRef<FungibleStockState>> receivedStockStatesPages = partyB.getServices().getVaultService().queryBy(FungibleStockState.class).getStates();
        FungibleStockState receivedStockState = receivedStockStatesPages.get(0).getState().getData();
        Amount<TokenType> receivedAmount = QueryUtilities.tokenBalance(partyB.getServices().getVaultService(), receivedStockState.toPointer(receivedStockState.getClass()));

        // Check buyer's stock amount
        assertEquals(receivedAmount.getQuantity(), TRADING_STOCK_QUANTITY);

        // Check buyer's remaining money balance
        Amount<TokenType> sentMoneyAmount = QueryUtilities.tokenBalance(partyB.getServices().getVaultService(), fiatTokenType);
        assertEquals(sentMoneyAmount.getQuantity() / 100.0, ISSUING_MONEY - (TRADING_STOCK_QUANTITY * STOCK_PRICE), 0.01);

        // Retrieve stock states from seller
        List<StateAndRef<FungibleStockState>> remainingStockStatesPages = partyA.getServices().getVaultService().queryBy(FungibleStockState.class).getStates();
        FungibleStockState remainingStockState = remainingStockStatesPages.get(0).getState().getData();
        Amount<TokenType> remainingAmount = QueryUtilities.tokenBalance(partyA.getServices().getVaultService(), remainingStockState.toPointer(remainingStockState.getClass()));

        // Check seller's remaining stock amount
        assertEquals(remainingAmount.getQuantity(), ISSUING_STOCK_QUANTITY - TRADING_STOCK_QUANTITY);

        // Check seller's money balance
        Amount<TokenType> receivedMoneyAmount = QueryUtilities.tokenBalance(partyA.getServices().getVaultService(), fiatTokenType);
        assertEquals(receivedMoneyAmount.getQuantity() / 100.0, TRADING_STOCK_QUANTITY * STOCK_PRICE, 0.01);
    }

    @Test
    public void cancelTradeFlowTest() throws ExecutionException, InterruptedException {

        // Issue Stock to seller
        CordaFuture<String> future = partyA.startFlow(new CreateAndIssueStock(STOCK_SYMBOL, ISSUING_STOCK_QUANTITY));
        network.runNetwork();
        future.get();

        // Create trade to sell stocks
        CordaFuture<SignedTransaction> futureA = partyA.startFlow(new TradeFlow.Initiator(tradeState));
        network.runNetwork();
        futureA.get();

        // Cancel trade
        CordaFuture<SignedTransaction> futureCancelTrade = partyA.startFlow(new CancelTradeFlow.CancelInitiator(cancelTradeState));
        network.runNetwork();
        SignedTransaction stx = futureCancelTrade.get();
        SecureHash stxID = stx.getId();

        // Check if initiator and observer have recorded the transaction
        SignedTransaction initiatorTx = partyA.getServices().getValidatedTransactions().getTransaction(stxID);
        SignedTransaction observerPartyTx = partyB.getServices().getValidatedTransactions().getTransaction(stxID);
        assertNotNull(initiatorTx);
        assertNotNull(observerPartyTx);
        assertEquals(initiatorTx, observerPartyTx);

        // Retrieve cancelled trade state from observer's vault
        List<StateAndRef<TradeState>> remainingTradeStatesPages = partyB.getServices().getVaultService().queryBy(TradeState.class).getStates();
        TradeState remainingTradeState = remainingTradeStatesPages.get(0).getState().getData();

        // Check cancelled trade state
        assertEquals(remainingTradeState.toString(), cancelTradeState.toString());
    }
}