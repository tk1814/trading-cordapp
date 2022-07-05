package net.corda.samples.trading.notaries;//package net.corda.samples.example.notaries;
//
//import net.corda.client.rpc.CordaRPCClient;
//import net.corda.core.concurrent.CordaFuture;
//import net.corda.core.crypto.CompositeKey;
//import net.corda.core.crypto.Crypto;
//import net.corda.core.crypto.toStringShort;
//import net.corda.core.flows.FlowLogic;
//import net.corda.core.identity.CordaX500Name;
//import net.corda.core.identity.Party;
//import net.corda.core.messaging.CordaRPCOps;
//import net.corda.core.messaging.startFlow;
//import net.corda.core.transactions.SignedTransaction;
//import net.corda.core.utilities.NetworkHostAndPort;
//import net.corda.core.utilities.getOrThrow;
//import net.corda.samples.example.notaries.BFTNotaryFlow;
//
//
//import java.lang.reflect.Proxy;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.Future;
//
//public class NotaryClient {
//
//
//    public static void main(String[] args) {
//        NetworkHostAndPort address = new NetworkHostAndPort("localhost", 10003);
//        System.out.println("Connecting to the recipient node ($address)");
//        for(String arg : args){
//            new CordaRPCClient(address).start("demou", "demop").getProxy() {
//                new NotaryDemoClientApi(new Proxy(arg)).notarise(10);
//            }
//        }
//
//    }
//
//}
//
//class NotaryDemoClientApi {
//
//    private CordaRPCOps rpc;
//    private Party notary;
//
//    public NotaryDemoClientApi(CordaRPCOps rpc){
//        this.rpc = rpc;
//        if (this.rpc.notaryIdentities()==null || this.rpc.notaryIdentities().size()>1){
//            this.notary = null;
//            try {
//                throw new Exception("No unique notary identity, try cleaning the node directories.");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }else{
//            this.notary = this.rpc.notaryIdentities().get(0);
//        }
//    }
//
//
//    private CordaX500Name BOB_NAME = new CordaX500Name("Bob Plc", "Rome", "IT");
//    private Party counterparty = new Party(BOB_NAME, Crypto.generateKeyPair(Crypto.DEFAULT_SIGNATURE_SCHEME).getPublic());
//
//    /** Makes calls to the node rpc to start transaction notarisation. */
//    void notarise(int count) {
//        String keyType = "";
//        if (this.notary.getOwningKey() instanceof CompositeKey){
//            keyType = "composite";
//        }else {
//            keyType = "public";
//        }
//        System.out.println("Notary: \"${notary.name}\", with $keyType key: ${notary.owningKey.toStringShort()}");
//        List<SignedTransaction> transactions = buildTransactions(count);
//        System.out.println("Notarised ${transactions.size} transactions:");
//        notariseTransactions(transactions);
////        transactions.zip(notariseTransactions(transactions)).forEach { (tx, signersFuture) ->
////                System.out.println("Tx [${tx.tx.id.prefixChars()}..] signed by ${signersFuture.getOrThrow().joinToString()}");
////        }
//    }
//
//
//    /**
//     * Builds a number of dummy transactions (as specified by [count]). The party first self-issues a state (asset),
//     * and builds a transaction to transfer the asset to the counterparty. The *move* transaction requires notarisation,
//     * as it consumes the original asset and creates a copy with the new owner as its output.
//     */
//    private List<SignedTransaction> buildTransactions(int count){
//        List<SignedTransaction> res = new ArrayList<SignedTransaction>();
//        List<CordaFuture> flowFutures = new ArrayList<CordaFuture>();
//        for (int i=1; i<=count;i++){
//            CordaFuture future = this.rpc.startFlowDynamic(, notary, counterparty).getReturnValue();
//            future.getOrThrow();
//            flowFutures.add(future);
//        }
//        return res;
//    }
//
//    /**
//     * For every transaction invoke the notary flow and obtains a notary signature.
//     * The signer can be any of the nodes in the notary cluster.
//     *
//     * @return a list of encoded signer public keys - one for every transaction
//     */
//    private List<Future<List<String>>> notariseTransactions(List<SignedTransaction> transactions){
//
//        List<Future<List<String>>> res = new ArrayList<Future<List<String>>>();
//        FlowLogic flow = new BFTNotary(getServiceHub(),this.notary.getOwningKey()).createServiceFlow();
//        for(SignedTransaction transaction : transactions){
//            res.add(this.rpc.startFlowDynamic(flow).getReturnValue().toCompletableFuture().thenApply().toString());
//
//        }
//        return res;
//    }
//
//
//}
