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

import java.util.*;

@BelongsToContract(TradeContract.class)
public class TradeState implements ContractState, LinearState {

    public double sellValue; // sell value of the Trade
    public int sellQuantity; // sell quantity for the Trade
    public double buyValue; // buy value of the Trade

    public double getSellValue() {
        return sellValue;
    }

    public int getSellQuantity() {
        return sellQuantity;
    }

    public int getBuyQuantity() {
        return buyQuantity;
    }

    public int buyQuantity; // buy quantity for the Trade
    public Party initiatingParty; // the party initiating the Trade
    @Nullable
    public Party counterParty = null; // the Trade Counterparty
    public String tradeStatus; // the Trade Status
    private UniqueIdentifier linearId;// = new UniqueIdentifier(); // Unique ID for the Trade
    private String timestamp; // TODO
    public String stockName;

    public TradeState(double sellValue, int sellQuantity, double buyValue, int buyQuantity, Party initiatingParty, Party counterParty, String tradeStatus, UniqueIdentifier linearId, String stockName) {
        this.sellValue = sellValue;
        this.sellQuantity = sellQuantity;
        this.buyValue = buyValue;
        this.buyQuantity = buyQuantity;
        this.initiatingParty = initiatingParty;
        if (counterParty != null) {
            this.counterParty = counterParty;
        }
        this.tradeStatus = tradeStatus;
        this.linearId = linearId;
        this.stockName = stockName;
    }

    public double getBuyValue() {
        return buyValue;
    }

    public Party getInitiatingParty() {
        return initiatingParty;
    }

    @Nullable
    public Party getCounterParty() {
        return counterParty;
    }

    public String getTradeStatus() {
        return tradeStatus;
    }

    public UniqueIdentifier getTradeId() {
        return linearId;
    }

    public void setTradeId(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
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
        return initiatingParty + "|" + counterParty + "|" + sellValue + "|" + sellQuantity
                + "|" + buyValue + "|" + buyQuantity + "|" + stockName + "|" + tradeStatus + "|" + linearId;
    }
}
