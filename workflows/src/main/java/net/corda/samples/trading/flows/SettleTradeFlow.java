package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trading.entity.MatchRecord;
import net.corda.samples.trading.states.TradeState;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This purpose of these flows is to settle the trade which includes:
 * - Transferring stocks from seller to buyer.
 * - Transferring money from buyer to seller.
 */
@StartableByRPC
public class SettleTradeFlow extends FlowLogic<SignedTransaction> {

    private Party seller;
    private Party buyer;

    private MatchRecord matchRecord;

    public SettleTradeFlow(MatchRecord matchRecord) {
        this.matchRecord = matchRecord;
    }


    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        TradeState initialTradeState = matchRecord.currentOrder;
        TradeState counterPartyTradeState = matchRecord.makerOrder;

        initialTradeState.setCounterParty(counterPartyTradeState.getInitiatingParty());
        initialTradeState.setStockPrice((matchRecord.price).doubleValue());
        initialTradeState.setStockQuantity(matchRecord.quantity.intValue());
        initialTradeState.setTradeStatus("Accepted");
        //undo
        initialTradeState.setSettlementDate(LocalDateTime.now(ZoneOffset.UTC).toString());

        counterPartyTradeState.setCounterParty(initialTradeState.getInitiatingParty());
        counterPartyTradeState.setStockPrice(matchRecord.price.doubleValue());
        counterPartyTradeState.setStockQuantity(matchRecord.quantity.intValue());
        counterPartyTradeState.setTradeStatus("Accepted");
        //undo
        counterPartyTradeState.setSettlementDate(LocalDateTime.now(ZoneOffset.UTC).toString());

        BigDecimal cost = matchRecord.price.multiply(matchRecord.quantity);

        if (initialTradeState.getTradeType().equals("Sell")) { // called by seller:initiatingParty
            buyer = initialTradeState.getCounterParty();
            seller = initialTradeState.getInitiatingParty();
            subFlow(new DvPInitiatorFlow(initialTradeState.getStockName(), initialTradeState.getStockQuantity(), buyer, cost));
        } else if (initialTradeState.getTradeType().equals("Buy")) {
            buyer = initialTradeState.getInitiatingParty();
            seller = initialTradeState.getCounterParty();
            subFlow(new DvPBuyerFlow.BuyStock(counterPartyTradeState, seller, cost));
        }
        SignedTransaction sigInit = subFlow(new CounterTradeFlow.CounterInitiator(initialTradeState));
        SignedTransaction sig = subFlow(new CounterTradeFlow.CounterInitiator(counterPartyTradeState));
        return sig;

    }
}