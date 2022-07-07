package net.corda.samples.trading.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.trading.contracts.TradeContract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@BelongsToContract(TradeContract.class)
public class TradeState implements ContractState, LinearState {

    public Party initiatingParty;
    @Nullable
    public Party counterParty = null;
    public String orderType;
    public String tradeType;
    public String stockName;
    public double stockPrice;
    public int stockQuantity;
    public String expirationDate;
    public String tradeStatus;
    public String tradeDate;
    public String settlementDate;
    private UniqueIdentifier linearId;
    // TODO: add timestamps

    public TradeState(Party initiatingParty, @Nullable Party counterParty, String orderType, String tradeType, String stockName,
                      double stockPrice, int stockQuantity, String expirationDate, String tradeStatus, String tradeDate, String settlementDate,
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

    public String getTradeStatus() {
        return tradeStatus;
    }

    public UniqueIdentifier getTradeId() {
        return linearId;
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
