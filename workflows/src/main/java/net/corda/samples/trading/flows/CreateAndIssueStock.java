package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trading.states.FungibleStockState;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This flow issues a stock to the node itself just to keep things simple
 * i.e. the issuer and the recipient of IssueTokens are the same
 * It first creates a StockState as EvolvableTokenType and then issues some tokens based on this EvolvableTokenType
 * The observer receives a copy of all the transactions and records it in their vault
 */
@InitiatingFlow
@StartableByRPC
public class CreateAndIssueStock extends FlowLogic<String> {

    private final String name;
    private final int issueVol;

    public CreateAndIssueStock(String name, int issueVol) {
        this.name = name;
        this.issueVol = issueVol;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

        // Retrieving the observers
        List<Party> observers = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                .collect(Collectors.toList());
        observers.remove(getOurIdentity());
        observers.remove(notary);

        Party company = getOurIdentity();

        // Construct the output stockState
        final FungibleStockState stockState = new FungibleStockState(
                new UniqueIdentifier(),
                company,
                name
        );

        // The notary provided here will be used in all future actions of this token
        TransactionState<FungibleStockState> transactionState = new TransactionState<>(stockState, notary);

        // Using the build-in flow to create an evolvable token type
        subFlow(new CreateEvolvableTokens(transactionState, observers));

        // Indicate the recipient which is the issuing party itself
        FungibleToken stockToken = new FungibleTokenBuilder()
                .ofTokenType(stockState.toPointer())
                .withAmount(issueVol)
                .issuedBy(getOurIdentity())
                .heldBy(getOurIdentity())
                .buildFungibleToken();

        // Finally, use the build-in flow to issue the stock tokens. Observer parties will record a copy of the transaction
        SignedTransaction stx = subFlow(new IssueTokens(ImmutableList.of(stockToken), observers));
        subFlow(new CreateTradeQueueFlow.CreateTradeQueueInitiator(name));
        return "\nGenerated " + this.issueVol + " stocks." + "\nTransaction ID: " + stx.getId();
    }
}
