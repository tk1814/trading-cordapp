package net.corda.samples.trading.entity;

import net.corda.core.serialization.CordaSerializable;
import net.corda.samples.trading.states.TradeState;

import java.math.BigDecimal;
import java.util.List;

@CordaSerializable
public class MatchRecord {
    public final BigDecimal price;
    public final BigDecimal quantity;
    public final TradeState currentOrder;
    public final TradeState makerOrder;

    public MatchRecord(BigDecimal price, BigDecimal quantity, TradeState currentOrder, TradeState makerOrder) {
        this.price = price;
        this.quantity = quantity;
        this.currentOrder = currentOrder;
        this.makerOrder = makerOrder;
    }

    @Override
    public String toString() {
        return "MatchRecord{" +
                "price=" + price +
                ", quantity=" + quantity +
                ", currentOrder=" + currentOrder +
                ", makerOrder=" + makerOrder +
                '}';
    }
}
