package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
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

    public SettleTradeFlow(TradeState counterTradeState) {
        this.counterTradeState = counterTradeState;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        // TODO: if a subflow fails then revert all
        if (counterTradeState.getSellQuantity() != 0) // called by seller/initiatingParty
            subFlow(new MoveStock.Initiator(counterTradeState.getStockName(), counterTradeState.getSellQuantity(), counterTradeState.getCounterParty()));
        else if (counterTradeState.getBuyQuantity() != 0) // called by seller/counterParty
            subFlow(new MoveStock.Initiator(counterTradeState.getStockName(), counterTradeState.getBuyQuantity(), counterTradeState.getInitiatingParty()));
        return subFlow(new CounterTradeFlow.CounterInitiator(counterTradeState));

    }
}