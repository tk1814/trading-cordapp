package net.corda.samples.trading.jmeter;

import com.r3.corda.jmeter.AbstractSampler;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.samples.trading.flows.CreateAndIssueStock;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class IssueStockSampler extends AbstractSampler {

    public final static String STOCK_SYMBOL = "SP500";
    public final static Long STOCK_AMOUNT = 100000000L;

    @NotNull
    @Override
    public Set<Argument> getAdditionalArgs() {
        return new HashSet<>();
    }

    @NotNull
    @Override
    public FlowInvoke<?> createFlowInvoke(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {
        return new FlowInvoke<>(CreateAndIssueStock.class, new Object[]{STOCK_SYMBOL, STOCK_AMOUNT});
    }

    @Override
    public void setupTest(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {

    }

    @Override
    public void teardownTest(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {

    }
}
