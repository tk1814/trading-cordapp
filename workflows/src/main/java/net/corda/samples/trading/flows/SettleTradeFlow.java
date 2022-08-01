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

import java.time.LocalDateTime;
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

    private final Party counterParty;
    private final LocalDateTime settlementDate;
    private final UniqueIdentifier linearId;
    private Party seller;
    private Party buyer;

    private MatchRecord matchRecord;

    public SettleTradeFlow(MatchRecord matchRecord) {
        this.matchRecord = matchRecord;
        this.settlementDate = settlementDate;
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

        TradeState inputState = inputTradeStateList.get(0).getState().getData();
        TradeState counterTradeState = new TradeState(inputState.getInitiatingParty(), counterParty, inputState.getOrderType(),
                inputState.getTradeType(), inputState.getStockName(), inputState.getStockPrice(),
                inputState.getStockQuantity(), inputState.getExpirationDate(), "Accepted",
                inputState.getTradeDate(), settlementDate, linearId);

        if (inputState.getTradeType().equals("Sell")) { // called by seller:initiatingParty
            buyer = counterTradeState.getCounterParty();
            seller = counterTradeState.getInitiatingParty();
        }
        else if (inputState.getTradeType().equals("Buy")) { // called by seller:counterParty
            buyer = counterTradeState.getInitiatingParty();
            seller = counterTradeState.getCounterParty();
        }

        if (getOurIdentity().equals(seller)) { // check that seller is actually the caller
            subFlow(new DvPInitiatorFlow(counterTradeState.getStockName(), counterTradeState.getStockQuantity(), buyer, counterTradeState.getStockQuantity() * counterTradeState.getStockPrice()));
            return subFlow(new CounterTradeFlow.CounterInitiator(counterTradeState));
        } else {
            throw new RuntimeException("Flow called by unauthorised party.");
        }
        SignedTransaction sigInit = subFlow(new CounterTradeFlow.CounterInitiator(initialTradeState));
        SignedTransaction sig = subFlow(new CounterTradeFlow.CounterInitiator(counterPartyTradeState));
        return sig;

    }
}