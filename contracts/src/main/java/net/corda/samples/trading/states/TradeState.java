package net.corda.samples.trading.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.flows.FlowLogicRef;
import net.corda.core.flows.FlowLogicRefFactory;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.trading.contracts.TradeContract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@BelongsToContract(TradeContract.class)
public class TradeState implements ContractState, LinearState, SchedulableState {

    public Party initiatingParty;
    @Nullable
    public Party counterParty = null;
    public String orderType;
    public String tradeType;
    public String stockName;
    public double stockPrice;
    public int stockQuantity;
    public LocalDateTime expirationDate;
    public String tradeStatus;
    public LocalDateTime tradeDate;
    public LocalDateTime settlementDate;
    private UniqueIdentifier linearId;

    public TradeState(Party initiatingParty, @Nullable Party counterParty, String orderType, String tradeType, String stockName,
                      double stockPrice, int stockQuantity, LocalDateTime expirationDate, String tradeStatus, LocalDateTime tradeDate, LocalDateTime settlementDate,
                      UniqueIdentifier linearId) {
        this.initiatingParty = initiatingParty;
        if (counterParty != null) {
            this.counterParty = counterParty;
        }
        this.orderType = orderType;
        this.tradeType = tradeType;
        this.stockName = stockName;
        this.stockPrice = stockPrice;
        this.stockQuantity = stockQuantity;
        this.expirationDate = expirationDate;
        this.tradeStatus = tradeStatus;
        this.tradeDate = tradeDate;
        this.settlementDate = settlementDate;
        this.linearId = linearId;
    }

    public Party getInitiatingParty() {
        return initiatingParty;
    }

    @Nullable
    public Party getCounterParty() {
        return counterParty;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public double getStockPrice() {
        return stockPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public String getTradeStatus() {
        return tradeStatus;
    }

    public LocalDateTime getTradeDate() {
        return tradeDate;
    }

    public LocalDateTime getSettlementDate() {
        return settlementDate;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public void setStockPrice(double stockPrice) {
        this.stockPrice = stockPrice;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public void setTradeId(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    public String getStockName() {
        return stockName;
    }

    public void setInitiatingParty(Party initiatingParty) {
        this.initiatingParty = initiatingParty;
    }

    public void setCounterParty(@Nullable Party counterParty) {
        this.counterParty = counterParty;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setTradeStatus(String tradeStatus) {
        this.tradeStatus = tradeStatus;
    }

    public void setTradeDate(LocalDateTime tradeDate) {
        this.tradeDate = tradeDate;
    }

    public void setSettlementDate(LocalDateTime settlementDate) {
        this.settlementDate = settlementDate;
    }

    public void setLinearId(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    /*
     * A participant is any party that should be notified
     * when the state is created or consumed.
     */
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        if (counterParty == null) {
            return ImmutableList.of(initiatingParty);
        } else
            return ImmutableList.of(initiatingParty, counterParty);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Nullable
    @Override // Automatically cancel expired trade
    public ScheduledActivity nextScheduledActivity(@NotNull StateRef thisStateRef, @NotNull FlowLogicRefFactory flowLogicRefFactory) {
        if (tradeStatus.equals("Pending")) {
            FlowLogicRef flowLogicRef = flowLogicRefFactory.create(
                    "net.corda.samples.trading.flows.CancelTradeFlow$CancelInitiator", "Expired", linearId);
            return new ScheduledActivity(flowLogicRef, expirationDate.toInstant(ZoneOffset.UTC));
        } else
            return null;
    }

    @Override
    public String toString() {
        return "{" + "\"initiatingParty\":\"" + initiatingParty + "\"," +
                "\"counterParty\":\"" + counterParty + "\"," +
                "\"orderType\":\"" + orderType + "\"," +
                "\"tradeType\":\"" + tradeType + "\"," +
                "\"stockQuantity\":\"" + stockQuantity + "\"," +
                "\"stockName\":\"" + stockName + "\"," +
                "\"stockPrice\":\"" + stockPrice + "\"," +
                "\"expirationDate\":\"" + expirationDate + "\"," +
                "\"tradeStatus\":\"" + tradeStatus + "\"," +
                "\"tradeDate\":\"" + tradeDate + "\"," +
                "\"settlementDate\":\"" + settlementDate + "\"," +
                "\"linearId\":\"" + linearId + "\"}";
    }
}
