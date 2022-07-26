package net.corda.samples.trading.notaries;

import net.corda.core.flows.*;
import net.corda.core.serialization.SerializationAPIKt;
import net.corda.core.serialization.SerializationFactory;
import tbftsmart.communication.ServerCommunicationSystem;
import tbftsmart.communication.client.netty.NettyClientServerSession;
import tbftsmart.statemanagement.strategy.StandardStateManager;
import tbftsmart.tom.MessageContext;
import tbftsmart.tom.ServiceProxy;
import tbftsmart.tom.ServiceReplica;
import tbftsmart.tom.core.TOMLayer;
import tbftsmart.tom.core.messages.TOMMessage;
import tbftsmart.tom.server.RequestVerifier;
import tbftsmart.tom.server.defaultservices.DefaultRecoverable;
import tbftsmart.tom.server.defaultservices.DefaultReplier;
import tbftsmart.tom.util.Extractor;
import kotlin.NotImplementedError;
import kotlin.Pair;
import kotlin.TypeCastException;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Reflection;
import net.corda.core.contracts.StateRef;
import net.corda.core.contracts.TimeWindow;
import net.corda.core.crypto.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.internal.DeclaredField;
import net.corda.core.internal.InternalUtils;
import net.corda.core.internal.notary.NotaryInternalException;
import net.corda.core.internal.notary.NotaryUtilsKt;
import net.corda.core.schemas.PersistentStateRef;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.node.services.api.ServiceHubInternal;
import net.corda.node.services.transactions.PersistentUniquenessProvider;
import net.corda.node.utilities.AppendOnlyPersistentMap;
import net.corda.nodeapi.internal.persistence.DatabaseTransaction;
import net.corda.nodeapi.internal.persistence.DatabaseTransactionKt;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import java.io.*;
import java.net.SocketException;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

public  class BFTSMart {

    @CordaSerializable
    public static  class CommitRequest implements Serializable {
        @NotNull
        private  NotarisationPayload payload;

        @NotNull
        private  Party callerIdentity;

        @NotNull
        public  NotarisationPayload getPayload() {
            return this.payload;
        }

        @NotNull
        public  Party getCallerIdentity() {
            return this.callerIdentity;
        }

        public CommitRequest(@NotNull NotarisationPayload payload, @NotNull Party callerIdentity) {
            this.payload = payload;
            this.callerIdentity = callerIdentity;
        }

        @NotNull
        public String toString() {
            return "CommitRequest(payload=" + this.payload + ", callerIdentity=" + this.callerIdentity + ")";
        }

        public int hashCode() {
            return ((this.payload != null) ? this.payload.hashCode() : 0) * 31 + ((this.callerIdentity != null) ? this.callerIdentity.hashCode() : 0);
        }

    }

    @CordaSerializable
    public static abstract class ReplicaResponse implements Serializable {

        public static  class Error extends ReplicaResponse {
            @NotNull
            private  SignedData<NotaryError> error;

            @NotNull
            public  SignedData<NotaryError> getError() {
                return this.error;
            }

            public Error(@NotNull SignedData<NotaryError> error) {
                super();
                this.error = error;
            }


            @NotNull
            public String toString() {
                return "Error(error=" + this.error + ")";
            }

            public int hashCode() {
                return (this.error != null) ? this.error.hashCode() : 0;
            }

        }

        public static  class Signature extends ReplicaResponse {
            @NotNull
            private  TransactionSignature txSignature;

            @NotNull
            public  TransactionSignature getTxSignature() {
                return this.txSignature;
            }

            public Signature(@NotNull TransactionSignature txSignature) {
                super();
                this.txSignature = txSignature;
            }

            @NotNull
            public String toString() {
                return "Signature(txSignature=" + this.txSignature + ")";
            }

            public int hashCode() {
                return (this.txSignature != null) ? this.txSignature.hashCode() : 0;
            }

            public boolean equals(@Nullable Object param2Object) {
                if (this != param2Object) {
                    if (param2Object instanceof Signature) {
                        Signature signature = (Signature)param2Object;
                        if (Intrinsics.areEqual(this.txSignature, signature.txSignature))
                            return true;
                    }
                } else {
                    return true;
                }
                return false;
            }
        }
    }

    @CordaSerializable
    public static abstract class ClusterResponse  implements Serializable{

        public static  class Error extends ClusterResponse {
            @NotNull
            private  List<SignedData<NotaryError>> errors;

            @NotNull
            public  List<SignedData<NotaryError>> getErrors() {
                return this.errors;
            }

            public Error(@NotNull List<SignedData<NotaryError>> errors) {
                super();
                this.errors = errors;
            }

            @NotNull
            public  List<SignedData<NotaryError>> component1() {
                return this.errors;
            }
            @NotNull
            public String toString() {
                return "Error(errors=" + this.errors + ")";
            }

            public int hashCode() {
                return (this.errors != null) ? this.errors.hashCode() : 0;
            }
        }

        public static  class Signatures extends ClusterResponse {
            @NotNull
            private  List<TransactionSignature> txSignatures;

            @NotNull
            public  List<TransactionSignature> getTxSignatures() {
                return this.txSignatures;
            }

            public Signatures(@NotNull List<TransactionSignature> txSignatures) {
                super();
                this.txSignatures = txSignatures;
            }

            @NotNull
            public  List<TransactionSignature> component1() {
                return this.txSignatures;
            }

            @NotNull
            public String toString() {
                return "Signatures(txSignatures=" + this.txSignatures + ")";
            }

            public int hashCode() {
                return (this.txSignatures != null) ? this.txSignatures.hashCode() : 0;
            }
        }
    }

    public interface Cluster {
        void waitUntilAllReplicasHaveInitialized();
    }

    public static  class Verifier implements RequestVerifier {
        public boolean isValidRequest(@Nullable byte[] p0) {
            String str = "Not yet implemented";
            try {
                throw (Throwable)new NotImplementedError("An operation is not implemented: " + str);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            return true;
        }

        @Override
        public boolean isValidRequest(TOMMessage tomMessage) {
            return false;
        }
    }


    public static class Client extends SingletonSerializeAsToken {
        private  ServiceProxy proxy;

        private  Map<Integer, NettyClientServerSession> sessionTable;

        private  int clientId;

        private  Cluster cluster;

        private  BFTNotary notaryService;

        //private static  Logger log = KotlinUtilsKt.contextLogger(Companion);

        public Client(@NotNull BFTConfigInternal config, int clientId, @NotNull Cluster cluster, @NotNull BFTNotary notaryService) {
            this.clientId = clientId;
            this.cluster = cluster;
            this.notaryService = notaryService;
            this.proxy = new ServiceProxy(this.clientId, config.getPath().toString(), buildResponseComparator(), buildExtractor(),null);
            if (this.proxy.getCommunicationSystem() == null)
                throw new TypeCastException("null cannot be cast to non-null type tbftsmart.communication.client.netty.NettyClientServerCommunicationSystemClientSide");
            this.sessionTable = (Map<Integer, NettyClientServerSession>)InternalUtils.declaredField(this.proxy.getCommunicationSystem(), "sessionTable").getValue();

        }

        public void dispose() {
            this.proxy.close();
        }

        private  void awaitClientConnectionToCluster() throws InterruptedException {
            while (true) {
                List<Integer> inactive = new ArrayList<>();
                for(Map.Entry entry:this.sessionTable.entrySet()){
                    if(!((NettyClientServerSession)entry.getValue()).getChannel().isActive()){
                        inactive.add((Integer)entry.getKey());
                    }
                }

                if (inactive.isEmpty())
                    break;
                //log.info("Client-replica channels not yet active: " + this.clientId + " to " + inactive);
                Thread.sleep((inactive.size() * 100));
            }
        }

        @NotNull
        public  ClusterResponse commitTransaction(@NotNull NotarisationPayload payload, @NotNull Party otherSide) throws Exception {
            awaitClientConnectionToCluster();
            this.cluster.waitUntilAllReplicasHaveInitialized();
            CommitRequest commitRequest = new CommitRequest(payload, otherSide);

            byte[] request = SerializationAPIKt.serialize(commitRequest,SerializationFactory.Companion.getDefaultFactory(), SerializationFactory.Companion.getDefaultFactory().getDefaultContext()).getBytes();

            byte[] responseBytes = this.proxy.invokeOrdered(request);

            ByteArrayInputStream bis = new ByteArrayInputStream(responseBytes);
            ObjectInput in = new ObjectInputStream(bis);

            ClusterResponse response = (ClusterResponse)in.readObject();
            in.close();
            bis.close();

            if(response==null){
                throw new Exception();
            }

            return response;
        }

        private  Comparator<byte[]> buildResponseComparator() {
            return (o1, o2) -> {
                try{
                    ByteArrayInputStream bis1 = new ByteArrayInputStream(o1);
                    ObjectInput in1 = new ObjectInputStream(bis1);
                    ReplicaResponse reply1 = (ReplicaResponse)in1.readObject();

                    ByteArrayInputStream bis2 = new ByteArrayInputStream(o1);
                    ObjectInput in2 = new ObjectInputStream(bis2);
                    ReplicaResponse reply2 = (ReplicaResponse)in2.readObject();

                    in1.close();
                    bis1.close();
                    in2.close();
                    bis2.close();
                    if(reply1 instanceof ReplicaResponse.Error && reply2 instanceof ReplicaResponse.Error){
                        return 0;
                    }else if(reply1 instanceof ReplicaResponse.Signature && reply2 instanceof ReplicaResponse.Signature){
                        return 0;
                    }else{
                        return -1;
                    }

                } catch(IOException | ClassNotFoundException e){
                    e.printStackTrace();
                }
                return -1;
            };
        }

        private  Extractor buildExtractor() {
            return new Extractor() {
                @Override
                @NotNull
                public  TOMMessage extractResponse(TOMMessage[] replies, int $noName_1, int lastReceived) {
                    List<ReplicaResponse> responses = new ArrayList<>();
                    List<ReplicaResponse> accepted = new ArrayList<>();
                    List<ReplicaResponse> rejected = new ArrayList<>();
                    for(TOMMessage message:replies){
                        try {
                            ByteArrayInputStream bis = new ByteArrayInputStream(message.serializedMessage);
                            ObjectInput in = new ObjectInputStream(bis);
                            ReplicaResponse response = (ReplicaResponse)in.readObject();
                            //responses.add(response);
                            if(response instanceof ReplicaResponse.Signature){
                                accepted.add(response);
                            }else if(response instanceof ReplicaResponse.Error){
                                rejected.add(response);
                            }
                            in.close();
                            bis.close();
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }


                    byte[] bytes=new byte[]{};
                    if(!accepted.isEmpty()){
                        List<TransactionSignature> signatureLst = new ArrayList<>();
                        for(ReplicaResponse response:accepted){
                            signatureLst.add(((ReplicaResponse.Signature)response).getTxSignature());

                        }
                        ClusterResponse.Signatures signatures = new ClusterResponse.Signatures(signatureLst);

                        bytes = SerializationAPIKt.serialize(signatures,SerializationFactory.Companion.getDefaultFactory(), SerializationFactory.Companion.getDefaultFactory().getDefaultContext()).getBytes();



                    }else{
                        List<SignedData<NotaryError>> errorLst = new ArrayList<>();
                        for(ReplicaResponse response:rejected){
                            errorLst.add(((ReplicaResponse.Error)response).getError());

                        }
                        ClusterResponse.Error errors = new ClusterResponse.Error(errorLst);
                        bytes = SerializationAPIKt.serialize(errors,SerializationFactory.Companion.getDefaultFactory(), SerializationFactory.Companion.getDefaultFactory().getDefaultContext()).getBytes();

                    }

                    TOMMessage reply = replies[lastReceived];
                    return new TOMMessage(reply.getSender(),reply.getSession(),reply.getSequence(),reply.getOperationId(),bytes,reply.getViewID(),reply.getReqType());
                }
            };
        }

    }

    public static  class CordaServiceReplica extends ServiceReplica {
        private  DeclaredField<TOMLayer> tomLayerField;

        private  DeclaredField<ServerCommunicationSystem> csField;

        public CordaServiceReplica(@NotNull int replicaId, @NotNull Path configHome, @NotNull DefaultRecoverable owner) {


            super(replicaId, configHome.toString(), owner, owner,null, new DefaultReplier(),null);
            this.tomLayerField = InternalUtils.declaredField(this, Reflection.getOrCreateKotlinClass(ServiceReplica.class), "tomLayer");
            this.csField = InternalUtils.declaredField(this, Reflection.getOrCreateKotlinClass(ServiceReplica.class), "cs");
        }

        public  void dispose() throws InterruptedException {
            TOMLayer tomLayer = (TOMLayer)this.tomLayerField.getValue();
            tomLayer.shutdown();
            ServerCommunicationSystem cs = (ServerCommunicationSystem)this.csField.getValue();
            cs.join();
            cs.getServersConn().join();
            tomLayer.join();
            tomLayer.getDeliveryThread().join();
        }
    }


    public abstract static class Replica extends DefaultRecoverable {

        private BFTConfigInternal config;
        private  StandardStateManager stateManagerOverride;

        private  AppendOnlyPersistentMap<StateRef, SecureHash, BFTNotary.CommittedState, PersistentStateRef> commitLog;

        private  CordaServiceReplica replica;

        @NotNull
        private  ServiceHubInternal services;

        @NotNull
        private  PublicKey notaryIdentityKey;


        public Replica(@NotNull BFTConfigInternal config, @NotNull int replicaId, @NotNull ServiceHubInternal services, @NotNull PublicKey notaryIdentityKey) throws SocketException, InterruptedException {
            this.config = config;
            this.services = services;
            this.notaryIdentityKey = notaryIdentityKey;
            boolean exposeStartupRace = (config.getExposeRaces() && replicaId < BFTConfigInternal.maxFaultyReplicas(config.getClusterSize()));

            this.stateManagerOverride = new StandardStateManager(){
                @Override
                public void askCurrentConsensusId(){
                    if (exposeStartupRace) {
                        try {
                            Thread.sleep(20000);
                            super.askCurrentConsensusId();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            };


            this.commitLog = new AppendOnlyPersistentMap(
                    getServices().getCacheFactory(),
                    "BFTNonValidatingNotaryService_transactions",
                    (it)->{return new PersistentStateRef(((StateRef)it).getTxhash().toString(), ((StateRef)it).getIndex());},
                    (it)->{
                        String txId = ((BFTNotary.CommittedState)it).id.getTxId();
                        int index = ((BFTNotary.CommittedState)it).id.getIndex();
                        return new Pair(new StateRef(SecureHash.create(txId), index), SecureHash.create(((BFTNotary.CommittedState)it).consumingTxHash));},
                    (stateRef, id)->{
                        return new BFTNotary.CommittedState(
                                new PersistentStateRef(((StateRef) stateRef).getTxhash().toString(), ((StateRef) stateRef).getIndex()),
                                id.toString());
                    },
                    BFTNotary.CommittedState.class);


            config.waitUntilReplicaWillNotPrintStackTrace(replicaId);
            this.replica = new CordaServiceReplica(replicaId, config.getPath(), this);
        }



        @NotNull
        protected  ServiceHubInternal getServices() {
            return this.services;
        }

        @NotNull
        protected  PublicKey getNotaryIdentityKey() {
            return this.notaryIdentityKey;
        }


        @NotNull
        public StandardStateManager getStateManager() {
            return this.stateManagerOverride;
        }




        public  void dispose() throws InterruptedException {
            this.replica.dispose();
        }

        @Nullable
        @Override
        public byte[] appExecuteUnordered(@NotNull byte[] command, @NotNull MessageContext msgCtx) {
            try {
                throw (Throwable)new NotImplementedError("No unordered operations supported");
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            return null;
        }


        private  void checkConflict(LinkedHashMap<StateRef, StateConsumptionDetails> conflictingStates, List<StateRef> states, StateConsumptionDetails.ConsumedStateType type) {

            for(StateRef stateRef:states){
                if(this.commitLog.get(stateRef)!=null){
                    conflictingStates.put(stateRef,new StateConsumptionDetails(this.commitLog.get(stateRef).reHash(),type));
                }
            }

        }

        protected  void commitInputStates(@NotNull List<StateRef> states, @NotNull SecureHash txId, @NotNull CordaX500Name callerName, @NotNull NotarisationRequestSignature requestSignature, @Nullable TimeWindow timeWindow, @NotNull List<StateRef> references) {

            services.getDatabase().transaction(((it) -> {
                try {
                    this.logRequest(txId, callerName, requestSignature);
                    LinkedHashMap<StateRef, StateConsumptionDetails> conflictingStates = new LinkedHashMap();
                    this.checkConflict(conflictingStates, states, StateConsumptionDetails.ConsumedStateType.INPUT_STATE);
                    this.checkConflict(conflictingStates, references, StateConsumptionDetails.ConsumedStateType.REFERENCE_INPUT_STATE);
                    if (!conflictingStates.isEmpty()) {
                        if (states.isEmpty()) {
                            this.handleReferenceConflicts(txId, conflictingStates);
                        } else {
                            this.handleConflicts(txId, conflictingStates);
                        }
                    } else {
                        this.handleNoConflicts(timeWindow, states, txId);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return null;
            }));
        }

        // check whether this transaction has been committed
        private  boolean previouslyCommitted(SecureHash txId) {
            Session session = DatabaseTransactionKt.currentDBSession();
            return (session.find(BFTNotary.CommittedTransaction.class, txId.toString()) != null);
        }

        private void handleReferenceConflicts(SecureHash txId, LinkedHashMap<StateRef, StateConsumptionDetails> conflictingStates) throws NotaryInternalException {
            if (!this.previouslyCommitted(txId)) {
                //this transaction was not previously committed,
                // but One or more input states or referenced states have already been used as input states in other transactions.
                NotaryError.Conflict conflictError = new NotaryError.Conflict(txId, conflictingStates);
                throw new NotaryInternalException(conflictError);
            }

        }

        private  void handleConflicts(SecureHash txId, LinkedHashMap<StateRef, StateConsumptionDetails> conflictingStates) throws NotaryInternalException {
            if (NotaryUtilsKt.isConsumedByTheSameTx(txId.reHash(), conflictingStates)) {
                // transaction already notarised
                return;
            } else {
                NotaryError.Conflict conflictError = new NotaryError.Conflict(txId, conflictingStates);
                throw new NotaryInternalException(conflictError);
            }
        }

        private  void handleNoConflicts(TimeWindow timeWindow, List<StateRef> states, SecureHash txId) throws Throwable {
            if (states.isEmpty() && previouslyCommitted(txId))
                return;
            // verify the timewindow
            NotaryError.TimeWindowInvalid outsideTimeWindowError = NotaryUtilsKt.validateTimeWindow(this.services.getClock().instant(), timeWindow);

            if (outsideTimeWindowError == null) {
                for(StateRef stateRef:states){
                    commitLog.set(stateRef,txId);
                }
                Session session = DatabaseTransactionKt.currentDBSession();
                session.persist(new BFTNotary.CommittedTransaction(txId.toString()));
            } else {
                throw new NotaryInternalException(outsideTimeWindowError);
            }
        }

        private  void logRequest(SecureHash txId, CordaX500Name callerName, NotarisationRequestSignature requestSignature) throws IOException {

//txId.toString(),
//                    callerName.toString(), new byte[]{}, this.services.getClock().instant()
            PersistentUniquenessProvider.Request request = new PersistentUniquenessProvider.Request(null,
                    txId.toString(),callerName.toString(),
                    SerializationAPIKt.serialize(requestSignature,SerializationFactory.Companion.getDefaultFactory(), SerializationFactory.Companion.getDefaultFactory().getDefaultContext()).getBytes(),
                    this.services.getClock().instant());
            Session session = DatabaseTransactionKt.currentDBSession();
            session.persist(request);
        }

        @NotNull
        protected  DigitalSignature.WithKey sign(@NotNull byte[] bytes) {
            return (DigitalSignature.WithKey) this.services.getDatabase().transaction((Function1)((it) ->  {
                    return this.getServices().getKeyManagementService().sign(bytes, Replica.this.getNotaryIdentityKey());
                }
            ));
        }

        @NotNull
        protected  TransactionSignature sign(@NotNull SecureHash txId) {
            Intrinsics.checkParameterIsNotNull(txId, "txId");
            SignableData signableData = new SignableData(txId, new SignatureMetadata(this.services.getMyInfo().getPlatformVersion(), Crypto.findSignatureScheme(this.notaryIdentityKey).getSchemeNumberID()));

            return (TransactionSignature) this.services.getDatabase().transaction((Function1)(new Function1() {

                public Object invoke(Object var1) {
                    return this.invoke((DatabaseTransaction)var1);
                }

                @NotNull
                public  TransactionSignature invoke(@NotNull DatabaseTransaction $receiver) {
                    Intrinsics.checkParameterIsNotNull($receiver, "$receiver");
                    return Replica.this.getServices().getKeyManagementService().sign(signableData, Replica.this.getNotaryIdentityKey());
                }
            }));

        }

        @NotNull
        public byte[] getSnapshot() {
            LinkedHashMap<StateRef, SecureHash> committedStates = new LinkedHashMap<>();

            byte[] requests = (byte[]) this.services.getDatabase().transaction((Function1)(new Function1() {
                public byte[] invoke(Object var1) {
                    return this.invoke((DatabaseTransaction)var1);
                }

                public  byte[] invoke(@NotNull DatabaseTransaction $receiver) {
                    Intrinsics.checkParameterIsNotNull($receiver, "$receiver");
                    List<Pair<StateRef, SecureHash>> var2 = (List<Pair<StateRef, SecureHash>>) Replica.this.commitLog.getAllPersisted().collect(Collectors.toList());

                    for (Pair<StateRef, SecureHash> pair : var2) {
                        committedStates.put(pair.component1(), pair.component2());
                    }

                    CriteriaQuery criteriaQuery = $receiver.getSession().getCriteriaBuilder().createQuery(PersistentUniquenessProvider.Request.class);
                    criteriaQuery.select(criteriaQuery.from(PersistentUniquenessProvider.Request.class));
                    Query var10000 = $receiver.getSession().createQuery(criteriaQuery);

                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutput out = new ObjectOutputStream(bos);
                        out.writeObject(var10000.getResultList());
                        out.flush();
                        bos.flush();
                        out.close();
                        bos.close();
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }));


            return requests;




        }

        public void installSnapshot(@NotNull byte[] bytes) {

            try {
                System.out.println("setState called");
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInput in = null;

                in = new ObjectInputStream(bis);

                Pair pair = (Pair<LinkedHashMap<StateRef, SecureHash>, List<PersistentUniquenessProvider.Request>>)in.readObject();


                LinkedHashMap<StateRef, SecureHash> committedStates = (LinkedHashMap)pair.component1();
                List<PersistentUniquenessProvider.Request> requests = (List)pair.component2();
                this.services.getDatabase().transaction((Function1)(($receiver)-> {
                        commitLog.clear();
                        commitLog.putAll(committedStates);

                        CriteriaDelete deleteQuery = ((DatabaseTransaction)$receiver).getSession().getCriteriaBuilder().createCriteriaDelete(PersistentUniquenessProvider.Request.class);
                        deleteQuery.from(PersistentUniquenessProvider.Request.class);
                        ((DatabaseTransaction)$receiver).getSession().createQuery(deleteQuery).executeUpdate();

                        for(PersistentUniquenessProvider.Request request:requests){
                            ((DatabaseTransaction)$receiver).getSession().persist(request);
                        }
                        return null;
                    }
                ));



                in.close();
                bis.close();
            } catch (Exception e) {
                System.err.println("[ERROR] Error deserializing state: " + e
                        .getMessage());
            }


        }


        @Nullable
        public abstract byte[] executeCommand(@NotNull byte[] param1ArrayOfbyte);


    }



}

