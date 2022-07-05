package net.corda.samples.trading.flows;

import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.ServiceHub;
import net.corda.samples.trading.states.FungibleStockState;

import java.util.List;

public class CustomQuery {

    /**
     * Retrieve any unconsumed StockState and filter by the given name
     */
    public static StateAndRef<FungibleStockState> queryStock(String name, ServiceHub serviceHub) {
        List<StateAndRef<FungibleStockState>> stateAndRefs = serviceHub.getVaultService().queryBy(FungibleStockState.class).getStates();

        // Match the query result with the symbol. If no results match, throw exception
        StateAndRef<FungibleStockState> stockStateAndRef = stateAndRefs.stream()
                .filter(sf -> sf.getState().getData().getName().equals(name)).findAny()
                .orElseThrow(() -> new IllegalArgumentException("StockState name=\"" + name + "\" not found from vault"));

        return stockStateAndRef;
    }

    /**
     * Retrieve any unconsumed StockState and filter by the given name
     * Then return the pointer to this StockState
     */
    public static TokenPointer<FungibleStockState> queryStockPointer(String name, ServiceHub serviceHub) {
        StateAndRef<FungibleStockState> stockStateStateAndRef = queryStock(name, serviceHub);
        return stockStateStateAndRef.getState().getData().toPointer(FungibleStockState.class);
    }
}