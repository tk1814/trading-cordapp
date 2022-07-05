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

import java.util.*;

@BelongsToContract(TradeContract.class)
public class TradeState implements ContractState, LinearState {

    public int sellValue; // sell value of the Trade
    public String sellCurrency; // sell currency for the Trade

    public int getBuyValue() {
        return buyValue;
    }

    public int buyValue; // buy value of the Trade
    public String buyCurrency; // buy currency for the Trade
    public Party initiatingParty; // the party initiating the Trade
    public Party counterParty; // the Trade Counterparty
    public String tradeStatus; // the Trade Status
    private UniqueIdentifier linearId = new UniqueIdentifier(); // Unique ID for the Trade

    public TradeState(int sellValue, String sellCurrency, int buyValue, String buyCurrency, Party initiatingParty, Party counterParty, String tradeStatus, UniqueIdentifier linearId) {
        this.sellValue = sellValue;
        this.sellCurrency = sellCurrency;
        this.buyValue = buyValue;
        this.buyCurrency = buyCurrency;
        this.initiatingParty = initiatingParty;
        this.counterParty = counterParty;
        this.tradeStatus = tradeStatus;
        this.linearId = linearId;
    }

    public Party getInitiatingParty() {
        return initiatingParty;
    }

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

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(initiatingParty, counterParty);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public String toString() {
        return initiatingParty + "|" + counterParty + "|" + sellValue + " " + sellCurrency
                + "|" + buyValue + " " + buyCurrency + "|" + tradeStatus + "|" + linearId;
    }
}
