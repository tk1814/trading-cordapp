package net.corda.samples.trading.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.trading.contracts.StockContract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@BelongsToContract(StockContract.class)
public class StockState implements ContractState {

    private final Party issuer;
    private final Party owner;
    private final int amount;

    public StockState(Party issuer, Party owner, int amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(issuer, owner);
    }

//    /**
//     * To issue different types of stocks
//     */
//    @NotNull
//    @Override
//    public UniqueIdentifier getLinearId() {
//        return linearId;
//    }

    public Party getIssuer() {
        return issuer;
    }

    public Party getOwner() {
        return owner;
    }

    public int getAmount() {
        return amount;
    }
}