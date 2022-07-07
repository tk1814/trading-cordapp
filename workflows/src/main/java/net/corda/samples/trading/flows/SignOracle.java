package net.corda.samples.trading.flows;

import Services.Oracle;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SignOracle {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<TransactionSignature> {

        private final Party oracle;
        private final FilteredTransaction ftx;

        public Initiator(Party oracle, FilteredTransaction ftx) {
            this.oracle = oracle;
            this.ftx = ftx;
        }

        @Suspendable
        @Override
        public TransactionSignature call() throws FlowException {
            FlowSession session = initiateFlow(oracle);
            return session.sendAndReceive(TransactionSignature.class, ftx).unwrap(it -> it);
        }

    }

    @InitiatedBy(SignOracle.Initiator.class)
    public static class Responder extends FlowLogic<Void> {
        private final FlowSession session;

        public Responder(FlowSession session) {
            this.session = session;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            FilteredTransaction request = session.receive(FilteredTransaction.class).unwrap(it -> it);
            TransactionSignature response;
            try {
                response = getServiceHub().cordaService(Oracle.class).sign(request);
            } catch (FilteredTransactionVerificationException e) {
                throw new FlowException(e);
            }
            return null;
        }
    }
}
