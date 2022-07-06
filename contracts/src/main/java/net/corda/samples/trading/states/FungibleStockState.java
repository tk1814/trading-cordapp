package net.corda.samples.trading.states;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.schemas.StatePersistable;
import net.corda.core.serialization.CordaSerializable;
import net.corda.samples.trading.contracts.FungibleStockContract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CordaSerializable
@BelongsToContract(FungibleStockContract.class)
public class FungibleStockState extends EvolvableTokenType implements StatePersistable {

    private final UniqueIdentifier linearId;
    private final Party issuer;
    private final int fractionDigits = 0;

    private final String name;

    public FungibleStockState(UniqueIdentifier linearId, Party issuer, String name) {
        this.linearId = linearId;
        this.issuer = issuer;
        this.name = name;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public String getName() {
        return name;
    }

    public Party getIssuer() {
        return issuer;
    }

    @Override
    public int getFractionDigits() {
        return fractionDigits;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return ImmutableList.of(issuer);
    }

    /* This method returns a TokenPointer by using the linear Id of the evolvable state */
    public TokenPointer<FungibleStockState> toPointer() {
        LinearPointer<FungibleStockState> linearPointer = new LinearPointer<>(linearId, FungibleStockState.class);
        return new TokenPointer<>(linearPointer, fractionDigits);
    }
}
