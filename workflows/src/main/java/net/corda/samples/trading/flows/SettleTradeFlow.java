package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trading.states.TradeState;

/**
 * This purpose of these flows is to settle the trade which includes:
 * - Transferring stocks from seller to buyer.
 * - Transferring money from buyer to seller.
 */
@StartableByRPC
public class SettleTradeFlow extends FlowLogic<SignedTransaction> {

    private final TradeState counterTradeState;
    private Party seller;

    public SettleTradeFlow(TradeState counterTradeState) {
        this.counterTradeState = counterTradeState;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        // TODO: if a subflow fails then revert all
        if (counterTradeState.getTradeType().equals("Sell")) // called by seller/initiatingParty
            seller = counterTradeState.getCounterParty();
        else if (counterTradeState.getTradeType().equals("Buy"))
            seller = counterTradeState.getInitiatingParty(); // called by seller/counterParty
        subFlow(new DvPInitiatorFlow(counterTradeState.getStockName(), counterTradeState.getStockQuantity(), seller, counterTradeState.getStockQuantity() * counterTradeState.getStockPrice()));
        return subFlow(new CounterTradeFlow.CounterInitiator(counterTradeState));

    }
}