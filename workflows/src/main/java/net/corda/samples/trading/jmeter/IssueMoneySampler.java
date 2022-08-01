package net.corda.samples.trading.jmeter;

import com.r3.corda.jmeter.AbstractSampler;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.samples.trading.flows.IssueMoney;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class IssueMoneySampler extends AbstractSampler {

    public final static String CURRENCY = "USD";
    public final static double MONEY_AMOUNT = 100000000;

    @NotNull
    @Override
    public Set<Argument> getAdditionalArgs() {
        return new HashSet<>();
    }

    @NotNull
    @Override
    public FlowInvoke<?> createFlowInvoke(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {
       return new FlowInvoke<>(IssueMoney.class, new Object[]{CURRENCY, MONEY_AMOUNT, rpcProxy.nodeInfo().getLegalIdentities().get(0)});
    }

    @Override
    public void setupTest(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {

    }

    @Override
    public void teardownTest(@NotNull CordaRPCOps rpcProxy, @NotNull JavaSamplerContext testContext) {

    }
}
