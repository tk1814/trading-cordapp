//package net.corda.samples.trading.flows;
//
//
//import co.paralleluniverse.fibers.*;
//import net.corda.core.contracts.ContractState;
//import net.corda.core.crypto.SecureHash;
//import net.corda.core.flows.*;
//import net.corda.core.transactions.*;
//import net.corda.core.utilities.*;
//
//import static net.corda.core.contracts.ContractsDSL.requireThat;
//
//@InitiatedBy(TradeFlow.class)
//public class TradeFlowResponder extends FlowLogic<SignedTransaction> {
//
//    private final FlowSession counterpartySession;
//
//    public TradeFlowResponder(FlowSession counterpartySession) {
//        this.counterpartySession = counterpartySession;
//    }
//
//    private final ProgressTracker progressTracker = new ProgressTracker();
//
//    @Override
//    public ProgressTracker getProgressTracker() {
//        return progressTracker;
//    }
//
//    @Suspendable
//    @Override
//    public SignedTransaction call() throws FlowException {
//
//        class SignTxFlow extends SignTransactionFlow {
//            private SignTxFlow(FlowSession otherSession, ProgressTracker progressTracker) {
//                super(otherSession, progressTracker);
//            }
//
//            @Override
//            protected void checkTransaction(SignedTransaction stx) throws FlowException {
//                // Whatever checking you want to do...
//            }
//        }
//
//        return subFlow(new SignTxFlow(counterpartySession, SignTransactionFlow.tracker()));
//
//    }
//
//
////
////    @InitiatedBy(TradeFlow.class)
////    public static class Acceptor extends FlowLogic<SignedTransaction> {
////
////        private final FlowSession otherPartySession;
////
////        public Acceptor(FlowSession otherPartySession) {
////            this.otherPartySession = otherPartySession;
////        }
////
////        @Suspendable
////        @Override
////        public SignedTransaction call() throws FlowException {
////            class SignTxFlow extends SignTransactionFlow {
////                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
////                    super(otherPartyFlow, progressTracker);
////                }
////
////                @Override
////                protected void checkTransaction(SignedTransaction stx) {
////                    requireThat(require -> {
////                        ContractState output = stx.getTx().getOutputs().get(0).getData();
////                        require.using("This must be an IOU transaction.", output instanceof IOUState);
////                        IOUState iou = (IOUState) output;
////                        require.using("I won't accept IOUs with a value over 100.", iou.getValue() <= 100);
////                        return null;
////                    });
////                }
////            }
////            final SignTxFlow signTxFlow = new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker());
////            final SecureHash txId = subFlow(signTxFlow).getId();
////
////            return subFlow(new ReceiveFinalityFlow(otherPartySession, txId));
////        }
////    }
//
//
//}