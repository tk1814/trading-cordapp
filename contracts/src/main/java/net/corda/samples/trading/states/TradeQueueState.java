package net.corda.samples.trading.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.trading.contracts.TradeQueueContract;
import net.corda.samples.trading.entity.BuyOrderKey;
import net.corda.samples.trading.entity.SellOrderKey;
import net.corda.samples.trading.entity.TradeStateWithFee;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@BelongsToContract(TradeQueueContract.class)
public class TradeQueueState implements ContractState {

    public TreeMap<SellOrderKey, TradeStateWithFee> sellStockList;
    public TreeMap<BuyOrderKey, TradeStateWithFee> buyStockList;
    public String stockName;
    private UniqueIdentifier linearId;
    public Party createParty;
    private List<Party> participantParties;
    public TradeQueueState(TreeMap<SellOrderKey, TradeStateWithFee> sellStockList, TreeMap<BuyOrderKey, TradeStateWithFee> buyStockList, String stockName,
                           UniqueIdentifier linearId, Party createParty, List<Party> participantParties) {
        this.sellStockList = sellStockList;
        this.buyStockList = buyStockList;
        this.stockName = stockName;
        this.linearId = linearId;
        this.createParty = createParty;
        this.participantParties = participantParties;
    }

    public TreeMap<SellOrderKey, TradeStateWithFee> getSellStockList() {
        return sellStockList;
    }

    public TreeMap<BuyOrderKey, TradeStateWithFee> getBuyStockList() {
        return buyStockList;
    }

    public String getStockName() {
        return stockName;
    }

    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public Party getCreateParty() {
        return createParty;
    }

    public List<Party> getParticipantParties() {
        return participantParties;
    }

    public void setSellStockList(TreeMap<SellOrderKey, TradeStateWithFee> sellStockList) {
        this.sellStockList = sellStockList;
    }

    public void setBuyStockList(TreeMap<BuyOrderKey, TradeStateWithFee> buyStockList) {
        this.buyStockList = buyStockList;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public void setLinearId(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    public void setCreateParty(Party createParty) {
        this.createParty = createParty;
    }

    public void setParticipantParties(List<Party> participantParties) {
        this.participantParties = participantParties;
    }

    public TradeStateWithFee getBuyFirst() {
        return this.buyStockList.isEmpty() ? null : this.getBuyStockList().firstEntry().getValue();
    }

    public boolean removeBuyTrade(TradeStateWithFee tradeStateWithFee) {
        return this.buyStockList.remove(new BuyOrderKey(tradeStateWithFee.getTradeState().getTradeDate(), tradeStateWithFee.getTradeState().getStockPrice(),tradeStateWithFee.getFee())) != null;
    }

    public boolean addBuyTrade(TradeStateWithFee tradeStateWithFee) {
        return this.buyStockList.put(new BuyOrderKey(tradeStateWithFee.getTradeState().getTradeDate(), tradeStateWithFee.getTradeState().getStockPrice(),tradeStateWithFee.getFee()), tradeStateWithFee) == null;
    }

    public TradeStateWithFee getSellFirst() {
        return this.sellStockList.isEmpty() ? null : this.getSellStockList().firstEntry().getValue();
    }

    public boolean removeSellTrade(TradeStateWithFee tradeStateWithFee) {
        return this.sellStockList.remove(new SellOrderKey(tradeStateWithFee.getTradeState().getTradeDate(), tradeStateWithFee.getTradeState().getStockPrice())) != null;
    }

    public boolean addSellTrade(TradeStateWithFee tradeStateWithFee) {
        return this.sellStockList.put(new SellOrderKey(tradeStateWithFee.getTradeState().getTradeDate(), tradeStateWithFee.getTradeState().getStockPrice()), tradeStateWithFee) == null;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> allParties = new ArrayList<>(participantParties);
        allParties.add(createParty);
        return allParties;
    }

    @Override
    public String toString() {
        return "TradeQueueState{" +
                "sellStockList=" + sellStockList +
                ", buyStockList=" + buyStockList +
                ", stockName='" + stockName + '\'' +
                ", linearId=" + linearId +
                ", createParty=" + createParty +
                ", participantParties=" + participantParties +
                '}';
    }
}
