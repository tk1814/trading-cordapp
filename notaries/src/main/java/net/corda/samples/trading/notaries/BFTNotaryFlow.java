package net.corda.samples.trading.notaries;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.AttachmentResolutionException;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.internal.notary.NotaryInternalException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionWithSignatures;
//import net.corda.node.services.transactions.ValidatingNotaryFlow;

import java.security.SignatureException;
import java.util.List;

public class BFTNotaryFlow extends FlowLogic {


    protected final BFTNotary notaryService;
    protected final FlowSession otherPartySession;

    protected BFTNotaryFlow(BFTNotary notaryService, FlowSession otherPartySession) {
        super();
        this.notaryService = notaryService;
        this.otherPartySession = otherPartySession;
    }

    Party getFirstNotary() throws FlowException {
        List<Party> notaries = getServiceHub().getNetworkMapCache().getNotaryIdentities();
        if (notaries.isEmpty()) {
            throw new FlowException("No available notary.");
        }
        return notaries.get(0);
    }


    public void verifyTransaction(NotarisationPayload requestPayload){
        try {
            SignedTransaction stx = requestPayload.getSignedTransaction();
            resolveAndContractVerify(stx);
            verifySignatures(stx);
            customVerify(stx);
        } catch (Exception e) {
            try {
                throw new NotaryInternalException(new NotaryError.TransactionInvalid(e));
            } catch (NotaryInternalException notaryInternalException) {
                notaryInternalException.printStackTrace();
            }
        }
    }


    @Suspendable
    private void resolveAndContractVerify(SignedTransaction stx) throws TransactionVerificationException, SignatureException, AttachmentResolutionException, TransactionResolutionException {
        //subFlow(new ResolveTransactionsFlow(stx, otherPartySession));
        stx.verify(getServiceHub(), false);
    }



    private void verifySignatures(SignedTransaction stx) throws SignatureException {
        TransactionWithSignatures transactionWithSignatures = stx.resolveTransactionWithSignatures(getServiceHub());
        checkSignatures(transactionWithSignatures);
    }

    private void checkSignatures(TransactionWithSignatures tx) throws SignatureException {
        tx.verifySignaturesExcept(notaryService.getNotaryIdentityKey());
    }


    private void customVerify(SignedTransaction stx){

    }

    @Override
    public Object call() throws FlowException {
        return null;
    }
}
