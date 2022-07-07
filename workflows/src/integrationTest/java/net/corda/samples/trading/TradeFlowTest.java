package net.corda.samples.trading;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trading.flows.TradeFlow;
import net.corda.samples.trading.states.TradeState;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;

public class TradeFlowTest {

    private StartedMockNode nodeA;
    private StartedMockNode nodeB;
    private MockNetwork network;


    @Before
    public void setUp() {
        network = new MockNetwork(new MockNetworkParameters().withNotarySpecs(Collections.singletonList(
                new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
                .withCordappsForAllNodes (ImmutableList.of
                (TestCordapp.findCordapp("net.corda.samples.trading.flows"),
                TestCordapp.findCordapp("net.corda.samples.trading.contracts"))));
        nodeA = network.createNode(new CordaX500Name("PartyA", "London", "GB"));
        nodeB = network.createNode(new CordaX500Name("PartyB", "Paris", "FR"));
        for (StartedMockNode node : ImmutableList.of(nodeA, nodeB)) {
            node.registerInitiatedFlow(TradeFlow.Responder.class);
        }

        network.runNetwork();


    }
    @After
    public void cleanup() {
        network.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void tradeFlowTest() throws Exception{
        TradeState tradeState = new TradeState(1,
                100,
                0,
                0,
                nodeA.getInfo().getLegalIdentities().get(0),
                null,
                "Pending",
                new UniqueIdentifier(), "test");
        TradeFlow.Initiator flow = new TradeFlow.Initiator(tradeState);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        future.get();
        //exception.expectCause(instranceOf(TransactionVerificationException.class));

    }





}