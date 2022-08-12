package net.corda.samples.trading.jmeter;

import com.r3.corda.jmeter.AbstractSampler;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.samples.trading.flows.TradeAndMatchFlow;
import net.corda.samples.trading.states.TradeState;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TradeAndMatchSampler extends AbstractSampler {
    public static final String STOCK_SYMBOL = "SP500";
    static private Argument stockPrice = new Argument("stockPrice", "", "<meta>", "Stock price from csv file.");
    static private Argument sellOperation = new Argument("sellOperation", "", "<meta>", "Sell or Buy operation.");
    static private Argument expirationTimeInMinutes = new Argument("expirationTimeInMinutes", "", "<meta>", "Minutes to add to the expiration time.");

    public double csvStockPrice;
    public Boolean sellOperationBoolean;
    public long expirationMinutes;

    @Override
    public void setupTest(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {
        csvStockPrice = Double.parseDouble(testContext.getParameter(stockPrice.getName(), stockPrice.getValue()));
        sellOperationBoolean = Boolean.valueOf(testContext.getParameter(sellOperation.getName(), sellOperation.getValue()));
        expirationMinutes = Long.parseLong(testContext.getParameter(expirationTimeInMinutes.getName(), expirationTimeInMinutes.getValue()));
    }

    @NotNull
    @Override
    public Set<Argument> getAdditionalArgs() {
        return Stream.of(stockPrice, sellOperation, expirationTimeInMinutes).collect(Collectors.toCollection(HashSet::new));
    }

    @NotNull
    @Override
    public FlowInvoke<?> createFlowInvoke(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {
        CordaX500Name myLegalName = rpcProxy.nodeInfo().getLegalIdentities().get(0).getName();

        TradeState tradeState;
        if (sellOperationBoolean) {
            tradeState = new TradeState(rpcProxy.wellKnownPartyFromX500Name(myLegalName), null,
                    "Pending Order", "Sell", STOCK_SYMBOL, csvStockPrice, 1,
                    LocalDateTime.now(ZoneOffset.UTC).plusMinutes(expirationMinutes), "Pending", LocalDateTime.now(ZoneOffset.UTC),
                    null, new UniqueIdentifier());
        } else {
            tradeState = new TradeState(rpcProxy.wellKnownPartyFromX500Name(myLegalName), null,
                    "Pending Order", "Buy", STOCK_SYMBOL, csvStockPrice, 1,
                    LocalDateTime.now(ZoneOffset.UTC).plusMinutes(expirationMinutes), "Pending", LocalDateTime.now(ZoneOffset.UTC),
                    null, new UniqueIdentifier());
        }
        return new FlowInvoke<>(TradeAndMatchFlow.class, new Object[]{tradeState});
    }

    @Override
    public void teardownTest(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {
    }
}