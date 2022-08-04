package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.samples.trading.states.TradeState;

@StartableByRPC
public class TradeAndMatchFlow extends FlowLogic<Void> {

    private final TradeState tradeState;

    public TradeAndMatchFlow(TradeState tradeState) {
        this.tradeState = tradeState;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        subFlow(new TradeFlow.Initiator(tradeState));
        subFlow(new MatchOrdersFlow.MatchOrdersInitiator(tradeState));
        return null;
    }
}