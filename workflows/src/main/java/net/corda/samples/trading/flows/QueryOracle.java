package net.corda.samples.trading.flows;

import Services.Oracle;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import org.intellij.lang.annotations.Flow;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


public class QueryOracle {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<Double> {

        private final Party oracle;

        private final String stockName;

        public Initiator(Party oracle, String stockName) {
            this.oracle = oracle;
            this.stockName = stockName;
        }

        @Suspendable
        @Override
        public Double call() throws FlowException {
            return initiateFlow(oracle).sendAndReceive(Double.class, stockName).unwrap(it -> it);
        }

    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void> {
        private final FlowSession session;

        public Responder(FlowSession session) {
            this.session = session;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            String stockName = session.receive(String.class).unwrap(it -> it);
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("stockName", stockName);
            BigDecimal response = getServiceHub().cordaService(Oracle.class).query(reqMap);
            session.send(new Double(String.valueOf(response)));
            return null;
        }
    }
}