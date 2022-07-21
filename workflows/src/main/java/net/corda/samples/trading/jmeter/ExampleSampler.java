package net.corda.samples.trading.jmeter;


import com.r3.corda.jmeter.AbstractSampler;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.samples.trading.flows.CreateAndIssueStock;
import net.corda.samples.trading.states.TradeState;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.jetbrains.annotations.NotNull;
import com.r3.corda.lib.tokens.contracts.types.TokenType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ExampleSampler extends AbstractSampler {


    public final static String STOCK_SYMBOL = "TEST";
    public final static double STOCK_PRICE = 22.2;
    public final static int ISSUING_STOCK_QUANTITY = 10;
    public final static int TRADING_STOCK_QUANTITY = 4;
    public final static String CURRENCY = "GBP";
    public final static Integer ISSUING_MONEY = 300;
    public final static UniqueIdentifier LINEAR_ID = new UniqueIdentifier(null, UUID.fromString("6231f549-9c1b-041f-90dd-1dc728fcbafc"));
    public final static TokenType fiatTokenType = FiatCurrency.Companion.getInstance("GBP");
    public static TradeState tradeState = null;
    public static TradeState counterTradeState = null;


    @NotNull
    @Override
    public Set<Argument> getAdditionalArgs() {
        Set<Argument> arguments = new HashSet<>();
        arguments.add(AbstractSampler.getNotary());
        return arguments;
    }

    @NotNull
    @Override
    public FlowInvoke<?> createFlowInvoke(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {
        return new FlowInvoke<>(CreateAndIssueStock.class, new Object[]{STOCK_SYMBOL, ISSUING_STOCK_QUANTITY});
    }

    @Override
    public void setupTest(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {

    }

    @Override
    public void teardownTest(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {

    }
}

