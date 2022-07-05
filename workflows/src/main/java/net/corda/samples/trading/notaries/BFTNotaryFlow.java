package net.corda.samples.trading.notaries;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.NotarisationPayload;
import net.corda.core.flows.NotaryError;
import net.corda.core.internal.ResolveTransactionsFlow;
import net.corda.core.internal.notary.NotaryInternalException;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionWithSignatures;
import net.corda.node.services.transactions.ValidatingNotaryFlow;

import java.security.SignatureException;
import java.time.Duration;


public class BFTNotaryFlow extends ValidatingNotaryFlow {


    protected final com.tutorial.notaries.BFTNotary notaryService;
    protected final FlowSession otherPartySession;

    protected BFTNotaryFlow(com.tutorial.notaries.BFTNotary notaryService, FlowSession otherPartySession) {
        super(otherPartySession,notaryService,Duration.ofSeconds(1000));
        this.notaryService = notaryService;
        this.otherPartySession = otherPartySession;
    }

//    Party getFirstNotary() throws FlowException {
//        List<Party> notaries = getServiceHub().getNetworkMapCache().getNotaryIdentities();
//        if (notaries.isEmpty()) {
//            throw new FlowException("No available notary.");
//        }
//        return notaries.get(0);
//    }

    @Suspendable
    @Override
    public void verifyTransaction(NotarisationPayload requestPayload) {
        SignedTransaction stx = requestPayload.getSignedTransaction();
        try {
            resolveAndContractVerify(stx);
            verifySignatures(stx);
            customVerify(stx);
        } catch (Exception e) {
            e.printStackTrace();

            try {
                throw new NotaryInternalException(new NotaryError.TransactionInvalid(e));
            } catch (NotaryInternalException notaryInternalException) {
                notaryInternalException.printStackTrace();
            }

        }
    }


    @Suspendable
    private void resolveAndContractVerify(SignedTransaction stx) throws FlowException, SignatureException {
        subFlow(new ResolveTransactionsFlow(stx, this.otherPartySession, StatesToRecord.NONE));
        stx.verify(this.notaryService.getServices(), false);
    }


    @Suspendable
    private void verifySignatures(SignedTransaction stx) throws SignatureException {
        TransactionWithSignatures transactionWithSignatures = stx.resolveTransactionWithSignatures(getServiceHub());
        checkSignatures(transactionWithSignatures);
    }

    private void checkSignatures(TransactionWithSignatures tx) throws SignatureException {
        tx.verifySignaturesExcept(this.notaryService.getNotaryIdentityKey());
    }


    private void customVerify(SignedTransaction stx){
        int j=0;
        //custom verify
    }


}
