package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trading.states.TradeState;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

        // Check that the incoming counterTradeState matches with the TradeState in the vault before transferring stocks and money
        List<StateAndRef<TradeState>> inputTradeStateList = getServiceHub().getVaultService().queryBy(TradeState.class).getStates().stream()
                .filter(x -> !x.getState().getData().getInitiatingParty().equals(counterTradeState.getCounterParty()))
                .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Pending"))
                .filter(x -> x.getState().getData().getTradeId().equals(counterTradeState.getTradeId()))
                .filter(x -> x.getState().getData().getInitiatingParty().equals(counterTradeState.getInitiatingParty()))
                .filter(x -> Objects.equals(x.getState().getData().getCounterParty(), null))
                .filter(x -> x.getState().getData().getOrderType().equals(counterTradeState.getOrderType()))
                .filter(x -> x.getState().getData().getTradeType().equals(counterTradeState.getTradeType()))
                .filter(x -> x.getState().getData().getStockName().equals(counterTradeState.getStockName()))
                .filter(x -> x.getState().getData().getStockPrice() == counterTradeState.getStockPrice())
                .filter(x -> x.getState().getData().getStockQuantity() == counterTradeState.getStockQuantity())
                .filter(x -> x.getState().getData().getExpirationDate().equals(counterTradeState.getExpirationDate()))
                .filter(x -> x.getState().getData().getTradeDate().equals(counterTradeState.getTradeDate()))
                .filter(x -> x.getState().getData().getSettlementDate() == null)
                .collect(Collectors.toList());

        if (inputTradeStateList.isEmpty()) {
            throw new RuntimeException("Trade state with trade ID: " + counterTradeState.getTradeId() + " was not found in the vault.");
        }

        if (counterTradeState.getTradeType().equals("Sell")) // called by seller/initiatingParty
            seller = counterTradeState.getCounterParty();
        else if (counterTradeState.getTradeType().equals("Buy"))
            seller = counterTradeState.getInitiatingParty(); // called by seller/counterParty
        subFlow(new DvPInitiatorFlow(counterTradeState.getStockName(), counterTradeState.getStockQuantity(), seller, counterTradeState.getStockQuantity() * counterTradeState.getStockPrice()));
        return subFlow(new CounterTradeFlow.CounterInitiator(counterTradeState));

    }
}