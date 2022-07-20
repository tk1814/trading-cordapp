package net.corda.samples.trading.notaries;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.util.concurrent.SettableFuture;
import com.typesafe.config.Config;
import kotlin.NoWhenBranchMatchedException;
import kotlin.Pair;
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
import net.corda.core.utilities.NetworkHostAndPort;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public final class BFTNotary extends NotaryService {
    private final NotaryConfig notaryConfig;

    private BFTConfig bftConfig;

    private BFTSMart.Cluster cluster;

    private BFTSMart.Client client;

    private SettableFuture<Replica> replicaHolder;

    private ServiceFlow serviceFlow;

    @NotNull
    private final ServiceHubInternal services;

    @NotNull
    private final PublicKey notaryIdentityKey;

    //private static final Logger log = contextLogger();

    public BFTNotary(@NotNull ServiceHubInternal services, @NotNull PublicKey notaryIdentityKey) throws Throwable {

        this.services = services;
        this.notaryIdentityKey = notaryIdentityKey;
        if (getServices().getConfiguration().getNotary() != null) {
            this.notaryConfig = getServices().getConfiguration().getNotary();

//                this.bftConfig = this.notaryConfig.getBftSMaRt();

//            Config extraConfig = Objects.requireNonNull(
//                    this.notaryConfig.getExtraConfig(),
//                    "required `extraConfig.bft` key in notary config");
            Config extraConfig = this.notaryConfig.getExtraConfig();
            if(extraConfig==null){
                List<NetworkHostAndPort> tmp = new ArrayList<NetworkHostAndPort>() ;
                tmp.add(new NetworkHostAndPort("localhost",10210));
                this.bftConfig = new BFTConfig(0, tmp, true, false);
            }else{
                this.bftConfig = new BFTConfig(extraConfig);
            }



            this.cluster = makeBFTCluster(this.notaryIdentityKey, this.bftConfig);
            this.replicaHolder = SettableFuture.create();
            BFTConfigInternal bftConfigInternal = new BFTConfigInternal(this.bftConfig.getClusterAddresses(), this.bftConfig.getDebug(), this.bftConfig.getExposeRaces());

            //Throwable throwable = (Throwable)null;

            int replicaId = this.bftConfig.getReplicaId();
            BFTConfigInternal configHandle = bftConfigInternal.handle();
            Replica replica = new Replica(configHandle, replicaId, this::createMap, this.services, notaryIdentityKey);
            this.replicaHolder.set(replica);
            this.client = new BFTSMart.Client(configHandle, replicaId, this.cluster, this);

        //TODO
//                Thread thread = new Thread(""){
//
//                };
//                thread.setDaemon(true);



        }else {
            getServices().getConfiguration().getNotary();
            throw new IllegalArgumentException("Failed to register " + BFTNotary.class + ": notary configuration not present");
        }
    }


    @Entity
    @Table(name = "node_notary_committed_states")
    static final class CommittedState extends PersistentUniquenessProvider.BaseComittedState {
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


    @Entity
    @Table(name = "node_notary_committed_txs")
    public static class CommittedTransaction {
        @Id
        @Column(name = "transaction_id", nullable = false, length = 144)
        @NotNull
        private final String transactionId;


        public CommittedTransaction(@NotNull String transactionId) {
            this.transactionId = transactionId;
        }

        @NotNull
        public String getTransactionId() {
            return this.transactionId;
        }
    }

    private static final class Replica extends BFTSMart.Replica {

        public Replica(@NotNull BFTConfigInternal config, int replicaId, @NotNull Callable<AppendOnlyPersistentMap<StateRef, SecureHash, CommittedState, ? extends PersistentStateRef>> createMap, @NotNull ServiceHubInternal services, @NotNull PublicKey notaryIdentityKey) throws SocketException, InterruptedException {
            super(config, replicaId, createMap, services, notaryIdentityKey);
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

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(response);
                out.flush();
                bos.flush();
                out.close();
                bos.close();
                return bos.toByteArray();

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

        private final BFTSMart.ReplicaResponse verifyAndCommitTx(CoreTransaction transaction, Party callerIdentity, NotarisationRequestSignature requestSignature) throws IOException {
            BFTSMart.ReplicaResponse response = null;

            SecureHash id = transaction.getId();
            List<StateRef> inputs = transaction.getInputs();
            List<StateRef> references = transaction.getReferences();
            Party notary = transaction.getNotary();

            TimeWindow timeWindow = null;

            if(transaction instanceof FilteredTransaction) {
                timeWindow = ((FilteredTransaction) transaction).getTimeWindow();
            }

            commitInputStates(inputs, id, callerIdentity.getName(), requestSignature, timeWindow, references);
            //log.debug { "Inputs committed successfully, signing $id" }
            response = new BFTSMart.ReplicaResponse.Signature(sign(id));
            return response;
        }


        private final void verifyRequest(BFTSMart.CommitRequest commitRequest) {
            CoreTransaction transaction = commitRequest.getPayload().getCoreTransaction();
            NotarisationRequest notarisationRequest = new NotarisationRequest(transaction.getInputs(), transaction.getId());
            NotaryUtilsKt.verifySignature(notarisationRequest, commitRequest.getPayload().getRequestSignature(), commitRequest.getCallerIdentity());
        }
    }

    private static final class ServiceFlow extends FlowLogic<Void> {
        @NotNull
        private FlowSession otherSideSession;

        @NotNull
        private final BFTNotary service;

        @NotNull
        public final FlowSession getOtherSideSession() {
            return this.otherSideSession;
        }

        @NotNull
        public final BFTNotary getService() {
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

        private final NotarisationResponse commit(NotarisationPayload payload) throws Throwable {
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


    private final BFTSMart.Cluster makeBFTCluster(PublicKey notaryKey, BFTConfig bftConfig) {
        return new BFTSMart.Cluster(){

            @Override
            public void waitUntilAllReplicasHaveInitialized() {
                //
            }
        };
    }


    public final void waitUntilReplicaHasInitialized() {
        //replicaHolder.getOrThrow();
    }

    @NotNull
    public final BFTSMart.ClusterResponse commitTransaction(@NotNull NotarisationPayload payload, @NotNull Party otherSide) throws InterruptedException, IOException, ClassNotFoundException {
        Intrinsics.checkParameterIsNotNull(payload, "payload");
        Intrinsics.checkParameterIsNotNull(otherSide, "otherSide");
        return this.client.commitTransaction(payload, otherSide);
    }

    @NotNull
    @Override
    public FlowLogic<Void> createServiceFlow(@NotNull FlowSession otherPartySession) {
        Intrinsics.checkParameterIsNotNull(otherPartySession, "otherPartySession");
        return new ServiceFlow(otherPartySession, this);
    }


    private final AppendOnlyPersistentMap<StateRef, SecureHash, CommittedState, PersistentStateRef> createMap() {
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

