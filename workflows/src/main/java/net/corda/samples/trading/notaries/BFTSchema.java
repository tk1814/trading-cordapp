package net.corda.samples.trading.notaries;

import net.corda.core.schemas.MappedSchema;
import net.corda.node.services.transactions.PersistentUniquenessProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;


public final class BFTSchema extends MappedSchema {


    public BFTSchema(@NotNull Class<?> schemaFamily, int version, @NotNull Iterable<? extends Class<?>> mappedTypes) {
        super(schemaFamily, version, mappedTypes);
    }

    private static Iterable<? extends Class<?>> lst = Arrays.asList(PersistentUniquenessProvider.BaseComittedState.class,PersistentUniquenessProvider.Request.class,
            BFTNotary.CommittedState.class, BFTNotary.CommittedTransaction.class);


    public BFTSchema() {
        super(
                BFTSchema.class,
                1,
                BFTSchema.lst);
    }
}
