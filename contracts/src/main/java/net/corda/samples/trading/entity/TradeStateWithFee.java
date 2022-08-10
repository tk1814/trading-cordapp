package net.corda.samples.trading.entity;

import net.corda.core.serialization.CordaSerializable;
import net.corda.samples.trading.states.TradeState;

import java.math.BigDecimal;

@CordaSerializable
public class TradeStateWithFee {
    public TradeState tradeState;
    public BigDecimal fee;

    public TradeStateWithFee(TradeState tradeState, BigDecimal fee) {
        this.tradeState = tradeState;
        this.fee = fee;
    }

    public TradeState getTradeState() {
        return tradeState;
    }

    public void setTradeState(TradeState tradeState) {
        this.tradeState = tradeState;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }
}
