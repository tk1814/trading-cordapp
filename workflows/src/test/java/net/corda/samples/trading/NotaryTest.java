package net.corda.samples.trading;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trading.flows.TradeFlow;
import net.corda.samples.trading.states.TradeState;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Future;

public class NotaryTest {

    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;


    @Before
    public void setUp() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.trading.contracts"),
                TestCordapp.findCordapp("net.corda.samples.trading.flows"),
                TestCordapp.findCordapp("net.corda.samples.trading.notaries")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(
                        new CordaX500Name("NotaryB", "London", "GB"),
                        true,// Can also be validating if preferred.
                        "net.corda.samples.trading.notaries.BFTNotary"

                ))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown(){
        network.stopNodes();
    }

    @Test
    public void notaryTest(){
        TradeFlow.Initiator flow1 = new TradeFlow.Initiator(
                new TradeState(10,"GBP",1,"GBP",this.a.getInfo().getLegalIdentities().get(0),
                        this.b.getInfo().getLegalIdentities().get(0),"s",new UniqueIdentifier()));

        Future<SignedTransaction> future1 = a.startFlow(flow1);
        network.runNetwork();

        //successful query means the state is stored at node b's vault. Flow went through.
        QueryCriteria inputCriteria = new QueryCriteria.VaultQueryCriteria()
                .withStatus(Vault.StateStatus.UNCONSUMED);

        TradeState state = b.getServices().getVaultService()
                .queryBy(TradeState.class,inputCriteria).getStates().get(0).getState().getData();
        assert(state.getTradeStatus().equals("s"));
    }



}
