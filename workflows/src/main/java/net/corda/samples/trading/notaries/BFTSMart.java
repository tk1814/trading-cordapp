package net.corda.samples.trading.notaries;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.communication.client.netty.NettyClientServerSession;
import bftsmart.statemanagement.strategy.StandardStateManager;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.RequestVerifier;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import bftsmart.tom.util.Extractor;

import java.io.*;
import java.net.SocketException;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.*;

import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import kotlin.NotImplementedError;
import kotlin.Pair;
import kotlin.TypeCastException;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Reflection;

import net.corda.core.contracts.StateRef;
import net.corda.core.contracts.TimeWindow;
import net.corda.core.crypto.Crypto;
import net.corda.core.crypto.DigitalSignature;
import net.corda.core.crypto.SecureHash;
import net.corda.core.crypto.SignableData;
import net.corda.core.crypto.SignatureMetadata;
import net.corda.core.crypto.SignedData;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.NotarisationPayload;
import net.corda.core.flows.NotarisationRequestSignature;
import net.corda.core.flows.NotaryError;
import net.corda.core.flows.StateConsumptionDetails;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.internal.DeclaredField;
import net.corda.core.internal.InternalUtils;
import net.corda.core.internal.notary.NotaryInternalException;
import net.corda.core.internal.notary.NotaryUtilsKt;
import net.corda.core.schemas.PersistentStateRef;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.serialization.SerializationAPIKt;
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
import org.slf4j.Logger;

public  class BFTSMart {
    //public static BFTSMart INSTANCE = new BFTSMart();

    @CordaSerializable
    public static  class CommitRequest {
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
    public static abstract class ReplicaResponse {

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
            public  TransactionSignature component1() {
                return this.txSignature;
            }

            @NotNull
            public  Signature copy(@NotNull TransactionSignature txSignature) {
                Intrinsics.checkParameterIsNotNull(txSignature, "txSignature");
                return new Signature(txSignature);
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
    public static abstract class ClusterResponse {

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
    }


    public static  class Client extends SingletonSerializeAsToken {
        private  ServiceProxy proxy;

        private  Map<Integer, NettyClientServerSession> sessionTable;

        private  int clientId;

        private  BFTSMart.Cluster cluster;

        private  BFTNotary notaryService;

        //private static  Logger log = KotlinUtilsKt.contextLogger(Companion);

        public Client(@NotNull BFTConfigInternal config, int clientId, @NotNull BFTSMart.Cluster cluster, @NotNull BFTNotary notaryService) {
            this.clientId = clientId;
            this.cluster = cluster;
            this.notaryService = notaryService;
            this.proxy = new ServiceProxy(this.clientId, config.getPath().toString(), buildResponseComparator(), buildExtractor());
            if (this.proxy.getCommunicationSystem() == null)
                throw new TypeCastException("null cannot be cast to non-null type bftsmart.communication.client.netty.NettyClientServerCommunicationSystemClientSide");
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
        public  BFTSMart.ClusterResponse commitTransaction(@NotNull NotarisationPayload payload, @NotNull Party otherSide) throws InterruptedException, IOException, ClassNotFoundException {
            awaitClientConnectionToCluster();
            this.cluster.waitUntilAllReplicasHaveInitialized();
            BFTSMart.CommitRequest commitRequest = new BFTSMart.CommitRequest(payload, otherSide);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(commitRequest);
            out.flush();
            bos.flush();
            out.close();
            bos.close();
            byte[] requestBytes = bos.toByteArray();
            byte[] responseBytes = this.proxy.invokeOrdered(requestBytes);

            ByteArrayInputStream bis = new ByteArrayInputStream(responseBytes);
            ObjectInput in = new ObjectInputStream(bis);
            BFTSMart.ClusterResponse response = (BFTSMart.ClusterResponse)in.readObject();
            in.close();
            bis.close();
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
                    try {
                        if(!accepted.isEmpty()){
                            List<TransactionSignature> signatureLst = new ArrayList<>();
                            for(ReplicaResponse response:accepted){
                                signatureLst.add(((ReplicaResponse.Signature)response).getTxSignature());

                            }
                            ClusterResponse.Signatures signatures = new ClusterResponse.Signatures(signatureLst);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ObjectOutput out = null;

                            out = new ObjectOutputStream(bos);
                            out.writeObject(signatures);
                            out.flush();
                            bos.flush();
                            out.close();
                            bos.close();
                            bytes = bos.toByteArray();


                        }else{
                            List<SignedData<NotaryError>> errorLst = new ArrayList<>();
                            for(ReplicaResponse response:rejected){
                                errorLst.add(((ReplicaResponse.Error)response).getError());

                            }
                            ClusterResponse.Error errors = new ClusterResponse.Error(errorLst);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ObjectOutput out = new ObjectOutputStream(bos);
                            out.writeObject(errors);
                            out.flush();
                            bos.flush();
                            out.close();
                            bos.close();
                            bytes = bos.toByteArray();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    TOMMessage reply = replies[lastReceived];
                    return new TOMMessage(reply.getSender(),reply.getSession(),reply.getSequence(),bytes,reply.getViewID());
                }
            };
        }

    }

    public static  class CordaServiceReplica extends ServiceReplica {
        private  DeclaredField<TOMLayer> tomLayerField;

        private  DeclaredField<ServerCommunicationSystem> csField;

        public CordaServiceReplica(int replicaId, @NotNull Path configHome, @NotNull DefaultRecoverable owner) {
            super(replicaId, configHome.toString(), true, (Executable)owner, (Recoverable)owner, null);
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


        public Replica(@NotNull BFTConfigInternal config, int replicaId, @NotNull Function1 createMap, @NotNull ServiceHubInternal services, @NotNull PublicKey notaryIdentityKey) throws SocketException, InterruptedException {
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


            this.commitLog = (AppendOnlyPersistentMap<StateRef, SecureHash, BFTNotary.CommittedState, PersistentStateRef>) this.services.getDatabase().transaction((Function1)createMap);


            config.waitUntilReplicaWillNotPrintStackTrace(replicaId);
            this.replica = new BFTSMart.CordaServiceReplica(replicaId, config.getPath(), this);
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

        @NotNull
        @Override
        public byte[][] appExecuteBatch(@NotNull byte[][] command, @NotNull MessageContext[] mcs) {
            List<byte[]> res = new ArrayList<>();

            for(byte[] c:command){
                res.add(this.executeCommand(c));
            }

            return (byte[][]) res.toArray();
        }

        private  void checkConflict(LinkedHashMap<StateRef, StateConsumptionDetails> conflictingStates, List<StateRef> states, StateConsumptionDetails.ConsumedStateType type) {

            for(StateRef stateRef:states){
                if(this.commitLog.get(stateRef)!=null){
                    conflictingStates.put(stateRef,new StateConsumptionDetails(this.commitLog.get(stateRef).reHash(),type));
                }
            }

        }

        protected  void commitInputStates(@NotNull List states, @NotNull SecureHash txId, @NotNull CordaX500Name callerName, @NotNull NotarisationRequestSignature requestSignature, @Nullable TimeWindow timeWindow, @NotNull List references) {

            services.getDatabase().transaction((Function1)(new Function1() {

                public Object invoke(Object var1) {
                    try {
                        this.invoke((DatabaseTransaction)var1);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    return null;
                }

                public  void invoke(@NotNull DatabaseTransaction $receiver) throws Throwable {
                    Replica.this.logRequest(txId, callerName, requestSignature);
                    LinkedHashMap<StateRef, StateConsumptionDetails> conflictingStates = new LinkedHashMap();
                    Replica.this.checkConflict(conflictingStates, states, StateConsumptionDetails.ConsumedStateType.INPUT_STATE);
                    Replica.this.checkConflict(conflictingStates, references, StateConsumptionDetails.ConsumedStateType.REFERENCE_INPUT_STATE);
                    if (!conflictingStates.isEmpty()) {
                        if (states.isEmpty()) {
                            Replica.this.handleReferenceConflicts(txId, conflictingStates);
                        } else {
                            Replica.this.handleConflicts(txId, conflictingStates);
                        }
                    } else {
                        Replica.this.handleNoConflicts(timeWindow, states, txId);
                    }

                }
            }));
        }

        private  boolean previouslyCommitted(SecureHash txId) {
            Session session = DatabaseTransactionKt.currentDBSession();
            return (session.find(BFTNotary.CommittedTransaction.class, txId.toString()) != null);
        }

        private  void handleReferenceConflicts(SecureHash txId, LinkedHashMap<StateRef, StateConsumptionDetails> conflictingStates) throws NotaryInternalException {
            if (!this.previouslyCommitted(txId)) {
                NotaryError.Conflict conflictError = new NotaryError.Conflict(txId, conflictingStates);

                throw new NotaryInternalException((NotaryError)conflictError);
            }

        }

        private  void handleConflicts(SecureHash txId, LinkedHashMap<StateRef, StateConsumptionDetails> conflictingStates) throws NotaryInternalException {

            if (NotaryUtilsKt.isConsumedByTheSameTx(txId.reHash(), conflictingStates)) {
                return;

            } else {
                NotaryError.Conflict conflictError = new NotaryError.Conflict(txId, conflictingStates);
                throw new NotaryInternalException((NotaryError)conflictError);
            }
        }

        private  void handleNoConflicts(TimeWindow timeWindow, List<StateRef> states, SecureHash txId) throws Throwable {
            if (states.isEmpty() && previouslyCommitted(txId))
                return;
            NotaryError.TimeWindowInvalid outsideTimeWindowError = NotaryUtilsKt.validateTimeWindow(this.services.getClock().instant(), timeWindow);

            if (outsideTimeWindowError == null) {
                for(StateRef stateRef:states){
                    commitLog.set(stateRef,txId);
                }
                Session session = DatabaseTransactionKt.currentDBSession();
                session.persist(new BFTNotary.CommittedTransaction(txId.toString()));
            } else {
                throw new NotaryInternalException((NotaryError)outsideTimeWindowError);
            }
        }

        private  void logRequest(SecureHash txId, CordaX500Name callerName, NotarisationRequestSignature requestSignature) {
            Intrinsics.checkExpressionValueIsNotNull(this.services.getClock().instant(), "services.clock.instant()");
            //PersistentUniquenessProvider.Request request = new PersistentUniquenessProvider.Request(null, txId.toString(), callerName.toString(), SerializationAPIKt.serialize(requestSignature, null, null, 3, null).getBytes(), this.services.getClock().instant(), 1, null);
            PersistentUniquenessProvider.Request request = new PersistentUniquenessProvider.Request();
            Session session = DatabaseTransactionKt.currentDBSession();
            session.persist(request);
        }

        @NotNull
        protected  DigitalSignature.WithKey sign(@NotNull byte[] bytes) {
            return (DigitalSignature.WithKey) this.services.getDatabase().transaction((Function1)(new Function1() {
                public Object invoke(Object var1) {
                    return this.invoke((DatabaseTransaction)var1);
                }

                @NotNull
                public  DigitalSignature.WithKey invoke(@NotNull DatabaseTransaction $receiver) {
                    Intrinsics.checkParameterIsNotNull($receiver, "$receiver");
                    return Replica.this.getServices().getKeyManagementService().sign(bytes, Replica.this.getNotaryIdentityKey());
                }
            }));
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
                    List<Pair<StateRef, SecureHash>> var2 =  (List<Pair<StateRef, SecureHash>>)Replica.this.commitLog.getAllPersisted();

                    for (Pair<StateRef, SecureHash> pair : var2 ){
                        committedStates.put(pair.component1(),pair.component2());
                    }

                    CriteriaQuery criteriaQuery = $receiver.getSession().getCriteriaBuilder().createQuery(PersistentUniquenessProvider.Request.class);
                    criteriaQuery.select(criteriaQuery.from(PersistentUniquenessProvider.Request.class));
                    Query var10000 = $receiver.getSession().createQuery(criteriaQuery);

                    try{
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutput out = new ObjectOutputStream(bos);
                        out.writeObject(var10000.getResultList());
                        out.flush();
                        bos.flush();
                        out.close();
                        bos.close();
                        return bos.toByteArray();
                    }catch (IOException e) {
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
                ObjectInput in = new ObjectInputStream(bis);
                Pair pair = (Pair<LinkedHashMap<StateRef, SecureHash>, List<PersistentUniquenessProvider.Request>>)in.readObject();
                LinkedHashMap<StateRef, SecureHash> committedStates = (LinkedHashMap)pair.component1();
                List<PersistentUniquenessProvider.Request> requests = (List)pair.component2();
                this.services.getDatabase().transaction((Function1)(new Function1() {
                    public Object invoke(Object var1) {
                        this.invoke((DatabaseTransaction)var1);
                        return Unit.INSTANCE;
                    }

                    public  void invoke(@NotNull DatabaseTransaction $receiver) {
                        Intrinsics.checkParameterIsNotNull($receiver, "$receiver");
                        commitLog.clear();
                        commitLog.putAll(committedStates);

                        CriteriaDelete deleteQuery = $receiver.getSession().getCriteriaBuilder().createCriteriaDelete(PersistentUniquenessProvider.Request.class);
                        deleteQuery.from(PersistentUniquenessProvider.Request.class);
                        $receiver.getSession().createQuery(deleteQuery).executeUpdate();

                        for(PersistentUniquenessProvider.Request request:requests){
                            $receiver.getSession().persist(request);
                        }

                    }
                }));



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

