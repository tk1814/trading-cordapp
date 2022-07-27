package net.corda.samples.trading.notaries;

import kotlin.reflect.jvm.internal.impl.load.kotlin.DeserializationComponentsForJava;
import net.corda.core.serialization.SerializationAPIKt;
import net.corda.core.serialization.SerializationFactory;
import tbftsmart.tom.MessageContext;
import co.paralleluniverse.fibers.Suspendable;
import com.google.common.util.concurrent.SettableFuture;
import com.typesafe.config.Config;
import kotlin.NoWhenBranchMatchedException;
import kotlin.Pair;
import kotlin.Suppress;
import kotlin.jvm.internal.Intrinsics;
import net.corda.core.contracts.StateRef;
import net.corda.core.contracts.TimeWindow;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.internal.notary.NotaryService;
import net.corda.core.internal.notary.NotaryUtilsKt;
import net.corda.core.schemas.PersistentStateRef;
import net.corda.core.transactions.CoreTransaction;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.node.services.api.ServiceHubInternal;
import net.corda.node.services.config.NotaryConfig;
import net.corda.node.services.transactions.PersistentUniquenessProvider;
import net.corda.node.utilities.AppendOnlyPersistentMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.*;
import java.net.SocketException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public  class BFTNotary extends NotaryService {
    private  NotaryConfig notaryConfig;

    private BFTConfig bftConfig;

    private BFTSMart.Cluster cluster;

    private BFTSMart.Client client;

    private SettableFuture<Replica> replicaHolder;

    private ServiceFlow serviceFlow;

    @NotNull
    private  ServiceHubInternal services;

    @NotNull
    private  PublicKey notaryIdentityKey;

    //private static  Logger log = contextLogger();

    public BFTNotary(@NotNull ServiceHubInternal services, @NotNull PublicKey notaryIdentityKey) throws Throwable {

        this.services = services;
        this.notaryIdentityKey = notaryIdentityKey;
        if (getServices().getConfiguration().getNotary() != null) {
            this.notaryConfig = getServices().getConfiguration().getNotary();

//                this.bftConfig = this.notaryConfig.getBftSMaRt();

            Config extraConfig = Objects.requireNonNull(
                    this.notaryConfig.getExtraConfig(),
                    "required `extraConfig.bft` key in notary config");
//            Config extraConfig = this.notaryConfig.getExtraConfig();
//            if(extraConfig==null){
//                List<NetworkHostAndPort> tmp = new ArrayList<NetworkHostAndPort>() ;
//                tmp.add(new NetworkHostAndPort("127.0.0.1",11000));
//                this.bftConfig = new BFTConfig(0, tmp, true, false);
//            }else{
//                this.bftConfig = new BFTConfig(extraConfig);
//            }
            this.bftConfig = new BFTConfig(extraConfig);


            this.cluster = makeBFTCluster(this.notaryIdentityKey, this.bftConfig);
            this.replicaHolder = SettableFuture.create();
            BFTConfigInternal bftConfigInternal = new BFTConfigInternal(this.bftConfig.getClusterAddresses(), this.bftConfig.getDebug(), this.bftConfig.getExposeRaces());

            //Throwable throwable = (Throwable)null;

            int replicaId = this.bftConfig.getReplicaId();
            //BFTConfigInternal configHandle = bftConfigInternal.handle();


            Thread thread = new Thread("BFT SMaRt replica "+replicaId+" init") {

                public void run() {
                    try {
                        Replica replica = new Replica(bftConfigInternal, replicaId, services, notaryIdentityKey);
                        replicaHolder.set(replica);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            thread.setDaemon(true);
            thread.start();

            this.client = new BFTSMart.Client(bftConfigInternal, replicaId, this.cluster, this);


        }else {
            getServices().getConfiguration().getNotary();
            throw new IllegalArgumentException("Failed to register " + BFTNotary.class + ": notary configuration not present");
        }
    }

    @Entity
    @Table(name = "node_bft_committed_states")
    static class CommittedState extends PersistentUniquenessProvider.BaseComittedState {
        public PersistentStateRef id;
        public String consumingTxHash;

        public CommittedState(PersistentStateRef id, String consumingTxHash) {
            super(id, consumingTxHash);
            this.id = id;
            this.consumingTxHash = consumingTxHash;
        }

        @NotNull
        @Override
        public PersistentStateRef getId() {
            return id;
        }

        public void setId(PersistentStateRef id) {
            this.id = id;
        }

        @Nullable
        @Override
        public String getConsumingTxHash() {
            return consumingTxHash;
        }

        public void setConsumingTxHash(String consumingTxHash) {
            this.consumingTxHash = consumingTxHash;
        }
    }


    @Suppress(names = "MagicNumber") // database column length
    @Entity
    @Table(name = "node_bft_committed_txs")
    public static class CommittedTransaction {
        @Id
        @Column(name = "transaction_id", nullable = false, length = 144)
        @NotNull
        private String transactionId;


        public CommittedTransaction(@NotNull String transactionId) {
            this.transactionId = transactionId;
        }

        @NotNull
        public String getTransactionId() {
            return this.transactionId;
        }
    }

    private static  class Replica extends BFTSMart.Replica {

        public Replica(@NotNull BFTConfigInternal config, @NotNull int replicaId, @NotNull ServiceHubInternal services, @NotNull PublicKey notaryIdentityKey) throws SocketException, InterruptedException {
            super(config, replicaId, services, notaryIdentityKey);
        }

        @NotNull
        public byte[] executeCommand(@NotNull byte[] command) {
            Intrinsics.checkParameterIsNotNull(command, "command");
            try{
                ByteArrayInputStream bis = new ByteArrayInputStream(command);
                ObjectInput in = new ObjectInputStream(bis);
                BFTSMart.CommitRequest commitRequest = (BFTSMart.CommitRequest)in.readObject();
                verifyRequest(commitRequest);
                BFTSMart.ReplicaResponse response = verifyAndCommitTx(commitRequest.getPayload().getCoreTransaction(), commitRequest.getCallerIdentity(), commitRequest.getPayload().getRequestSignature());
                in.close();
                bis.close();
                //SerializationAPIKt.deserialize(command,SerializationFactory.Companion.getDefaultFactory(), SerializationFactory.Companion.getDefaultFactory().getDefaultContext());


                return SerializationAPIKt.serialize(response, SerializationFactory.Companion.getDefaultFactory(), SerializationFactory.Companion.getDefaultFactory().getDefaultContext()).getBytes();

            }catch (IOException e) {
                e.printStackTrace();
                System.err.println("[ERROR] Error serializing state: " + command);
                return "ERROR".getBytes();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.err.println("[ERROR] Error serializing state: " + command);
                return "ERROR".getBytes();
            }
        }


        private  BFTSMart.ReplicaResponse verifyAndCommitTx(CoreTransaction transaction, Party callerIdentity, NotarisationRequestSignature requestSignature) throws IOException {
            BFTSMart.ReplicaResponse response = null;

            SecureHash id = transaction.getId();
            List<StateRef> inputs = transaction.getInputs();
            List<StateRef> references = transaction.getReferences();

            TimeWindow timeWindow = null;

            if(transaction instanceof FilteredTransaction) {
                timeWindow = ((FilteredTransaction) transaction).getTimeWindow();
            }

            commitInputStates(inputs, id, callerIdentity.getName(), requestSignature, timeWindow, references);
            //log.debug { "Inputs committed successfully, signing $id" }
            response = new BFTSMart.ReplicaResponse.Signature(sign(id));
            return response;
        }


        private  void verifyRequest(BFTSMart.CommitRequest commitRequest) {
            CoreTransaction transaction = commitRequest.getPayload().getCoreTransaction();
            NotarisationRequest notarisationRequest = new NotarisationRequest(transaction.getInputs(), transaction.getId());
            NotaryUtilsKt.verifySignature(notarisationRequest, commitRequest.getPayload().getRequestSignature(), commitRequest.getCallerIdentity());
        }

        @Override
        public byte[][] appExecuteBatch(byte[][] command, MessageContext[] messageContexts, boolean b) {
            List<byte[]> res = new ArrayList<>();

            for(byte[] c:command){
                res.add(this.executeCommand(c));
            }

            return (byte[][]) res.toArray();
        }
    }

    private static  class ServiceFlow extends FlowLogic<Void> {
        @NotNull
        private FlowSession otherSideSession;

        @NotNull
        private  BFTNotary service;

        @NotNull
        public  FlowSession getOtherSideSession() {
            return this.otherSideSession;
        }

        @NotNull
        public  BFTNotary getService() {
            return this.service;
        }

        public ServiceFlow(@NotNull FlowSession otherSideSession, @NotNull BFTNotary service) {
            this.otherSideSession = otherSideSession;
            this.service = service;
        }

        @Suspendable
        @Nullable
        @Override
        public Void call() throws FlowException {

            NotarisationPayload payload = this.otherSideSession.receive(NotarisationPayload.class)
                    .unwrap(p -> p);

            NotarisationResponse response = null;

            try {
                response = commit(payload);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            this.otherSideSession.send(response);
            return null;
        }

        private  NotarisationResponse commit(NotarisationPayload payload) throws Throwable {
            BFTSMart.ClusterResponse response = this.service.commitTransaction(payload, this.otherSideSession.getCounterparty());
            if (response instanceof BFTSMart.ClusterResponse.Error) {
                NotaryError responseError = ((BFTSMart.ClusterResponse.Error) response).getErrors().get(0).verified();
                throw new NotaryException(responseError, payload.getCoreTransaction().getId());
            }
            if (response instanceof BFTSMart.ClusterResponse.Signatures) {
                //BFTNotary.log.debug("All input states of transaction " + payload.getCoreTransaction().getId() + " have been committed");

                //Party notary2 = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(4);

                return new NotarisationResponse(((BFTSMart.ClusterResponse.Signatures)response).getTxSignatures());
            }
            throw new NoWhenBranchMatchedException();
        }
    }


    @NotNull
    public ServiceHubInternal getServices() {
        return this.services;
    }

    @NotNull
    public PublicKey getNotaryIdentityKey() {
        return this.notaryIdentityKey;
    }


    private  BFTSMart.Cluster makeBFTCluster(PublicKey notaryKey, BFTConfig bftConfig) {
        return new BFTSMart.Cluster(){

            @Override
            public void waitUntilAllReplicasHaveInitialized() {
                //
            }
        };
    }


    public  void waitUntilReplicaHasInitialized() {
        //replicaHolder.getOrThrow();
    }

    @NotNull
    public  BFTSMart.ClusterResponse commitTransaction(@NotNull NotarisationPayload payload, @NotNull Party otherSide) throws Exception {
        Intrinsics.checkParameterIsNotNull(payload, "payload");
        Intrinsics.checkParameterIsNotNull(otherSide, "otherSide");
        return this.client.commitTransaction(payload, otherSide);
    }

    @NotNull
    public FlowLogic<Void> createServiceFlow(@NotNull FlowSession otherPartySession) {
        Intrinsics.checkParameterIsNotNull(otherPartySession, "otherPartySession");
        return new ServiceFlow(otherPartySession, this);
    }


    private AppendOnlyPersistentMap<StateRef, SecureHash, CommittedState, PersistentStateRef> createMap() {
        return new AppendOnlyPersistentMap(
                getServices().getCacheFactory(),
                "BFTNonValidatingNotaryService_transactions",
                (it)->{return new PersistentStateRef(((StateRef)it).getTxhash().toString(), ((StateRef)it).getIndex());},
                (it)->{
                    String txId = ((CommittedState)it).id.getTxId();
                    int index = ((CommittedState)it).id.getIndex();
                    return new Pair(new StateRef(SecureHash.create(txId), index), SecureHash.create(((CommittedState)it).consumingTxHash));},
                (stateRef, id)->{
                    return new CommittedState(
                            new PersistentStateRef(((StateRef) stateRef).getTxhash().toString(), ((StateRef) stateRef).getIndex()),
                            id.toString());
                    },
                CommittedState.class);
    }


    public void start() {}

    public void stop() {
        Intrinsics.checkExpressionValueIsNotNull(this.replicaHolder, "replicaHolder");
        try {
            this.replicaHolder.get().dispose();  //getOrThrow
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        this.client.dispose();
    }


}

