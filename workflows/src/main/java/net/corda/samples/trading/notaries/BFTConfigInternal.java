package net.corda.samples.trading.notaries;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.*;


import com.typesafe.config.Config;
import kotlin.jvm.internal.Intrinsics;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.node.services.transactions.PathManager;
import org.jetbrains.annotations.NotNull;

public final class BFTConfigInternal extends PathManager<BFTConfigInternal> {
    private final List<NetworkHostAndPort> replicaAddresses;

    private final boolean exposeRaces;

    private int clusterSize;

    private Path path;


    @NotNull
    public static final String portIsClaimedFormat = "Port %s is claimed by another replica: %s";




    public final boolean getExposeRaces() {
        return this.exposeRaces;
    }

    public BFTConfigInternal(@NotNull List<NetworkHostAndPort> replicaAddresses, boolean debug, boolean exposeRaces) throws IOException {
        super(Files.createTempDirectory("bft-smart-config"));
        this.replicaAddresses = replicaAddresses;
        this.exposeRaces = exposeRaces;
        Set claimedPorts = new LinkedHashSet<NetworkHostAndPort>();


        //int replicaId;
        for(int replicaId=0; replicaId<=this.clusterSize;replicaId++){
            List<NetworkHostAndPort> networkHostAndPorts = replicaPorts(replicaId);
            for(NetworkHostAndPort port : networkHostAndPorts){
                claimedPorts.add(port);
            }
        }

//        configWriter("hosts.config", new Function1<PrintWriter, Unit>() {
//            public final void invoke(@NotNull PrintWriter $receiver) {
//                Intrinsics.checkParameterIsNotNull($receiver, "$receiver");
//                Iterable $receiver$iv = BFTConfigInternal.this.replicaAddresses;
//                int index$iv = 0;
//                Iterator iterator = $receiver$iv.iterator();
//                if (iterator.hasNext()) {
//                    Object item$iv = iterator.next();
//                    NetworkHostAndPort networkHostAndPort = (NetworkHostAndPort)item$iv;
//                    int index = index$iv++;
//                    String host = networkHostAndPort.component1();
//                    int port = networkHostAndPort.component2();
//                    Intrinsics.checkExpressionValueIsNotNull(InetAddress.getByName(host), "InetAddress.getByName(host)");
//                    $receiver.println(index + ' ' + InetAddress.getByName(host).getHostAddress() + ' ' + port);
//                }
//            }
//        });
//        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
//        Intrinsics.checkExpressionValueIsNotNull(getClass().getResource("system.config.printf"), "javaClass.getResource(\"system.config.printf\")");
//        URL uRL = getClass().getResource("system.config.printf");
//        Charset charset = Charsets.UTF_8;
//        byte[] arrayOfByte = TextStreamsKt.readBytes(uRL);
//        String str1 = new String(arrayOfByte, charset);
//        Object[] arrayOfObject = { Integer.valueOf(n), Integer.valueOf(BFTConfigInternalKt.maxFaultyReplicas(n)), Integer.valueOf(debug ? 1 : 0), CollectionsKt.joinToString$default((Iterable)RangesKt.until(0, n), ",", null, null, 0, null, null, 62, null) };
//        Intrinsics.checkExpressionValueIsNotNull(String.format(str1, Arrays.copyOf(arrayOfObject, arrayOfObject.length)), "java.lang.String.format(format, *args)");


        //        String systemConfig = String.format(str1, Arrays.copyOf(arrayOfObject, arrayOfObject.length));
//        configWriter("system.config", new Function1<PrintWriter, Unit>(systemConfig) {
//            public final void invoke(@NotNull PrintWriter $receiver) {
//                Intrinsics.checkParameterIsNotNull($receiver, "$receiver");
//                $receiver.print(this.$systemConfig);
//            }
//        });
    }



    public final int getClusterSize() {
        return this.replicaAddresses.size();
    }

//    private final void configWriter(String name, PrintWriter. block) {
//
//        new PrintWriter().
//
//        BufferedWriter bufferedWriter = PathUtilsKt.writer$default(PathUtilsKt.div(getPath(), name), null, new java.nio.file.OpenOption[0], 1, null);
//        Throwable throwable = (Throwable)null;
//        try {
//            BufferedWriter it = bufferedWriter;
//            PrintWriter printWriter = new PrintWriter(it);
//            Throwable throwable1 = (Throwable)null;
//            try {
//                PrintWriter printWriter1 = printWriter;
//                PrintWriter printWriter2 = printWriter1;
//                Function1 function1 = block;
//                function1.invoke(printWriter2);
//                Unit unit1 = Unit.INSTANCE;
//            } catch (Throwable throwable2) {
//                throwable1 = throwable2 = null;
//                throw throwable2;
//            } finally {
//                CloseableKt.closeFinally(printWriter, throwable1);
//            }
//            Unit unit = Unit.INSTANCE;
//        } catch (Throwable throwable1) {
//            throwable = throwable1 = null;
//            throw throwable1;
//        } finally {
//            CloseableKt.closeFinally(bufferedWriter, throwable);
//        }
//    }

    public final void waitUntilReplicaWillNotPrintStackTrace(int contextReplicaId) throws SocketException, InterruptedException {
        int peerId = contextReplicaId - 1;
        if(peerId < 0) return;
        NetworkHostAndPort address = BFTPort.FOR_REPLICAS.ofReplica(replicaAddresses.get(peerId));

        while(!isListening(address)){
            Thread.sleep(200);
        }
    }


    private final List<NetworkHostAndPort> replicaPorts(int replicaId) {
        NetworkHostAndPort base = this.replicaAddresses.get(replicaId);

        //return Arrays.stream(BFTPort.values()).map(n->BFTPort.values()).forEach(n -> BFTPort.n.ofReplica(base));
        List<NetworkHostAndPort> lst = new ArrayList<>();
        lst.add(BFTPort.FOR_REPLICAS.ofReplica(base));
        lst.add(BFTPort.FOR_CLIENTS.ofReplica(base));
        return lst;
    }




    boolean isListening(@NotNull NetworkHostAndPort networkHostAndPort) throws SocketException {

        try {
            Socket socket = new Socket(networkHostAndPort.getHost(), networkHostAndPort.getPort());
            return socket.getKeepAlive();
        } catch (IOException e) {
            return false;
        }

    }




    public static final int maxFaultyReplicas(int clusterSize) {
        return (clusterSize - 1) / 3;
    }

    public static final int minCorrectReplicas(int clusterSize) {
        return (2 * clusterSize + 3) / 3;
    }

    public static final int minClusterSize(int maxFaultyReplicas) {
        return maxFaultyReplicas * 3 + 1;
    }




}

enum BFTPort {
    FOR_CLIENTS (0), FOR_REPLICAS (1);

    private final int off;

    BFTPort(int off) {
        this.off = off;
    }

    @NotNull
    public final NetworkHostAndPort ofReplica(@NotNull NetworkHostAndPort base) {
        Intrinsics.checkParameterIsNotNull(base, "base");
        return new NetworkHostAndPort(base.getHost(), base.getPort() + this.off);
    }
}


final class BFTConfig {
    private final int replicaId;

    @NotNull
    private final List<NetworkHostAndPort> clusterAddresses;

    private final boolean debug;

    private final boolean exposeRaces;

    public BFTConfig(int replicaId, @NotNull List<NetworkHostAndPort> clusterAddresses, boolean debug, boolean exposeRaces) throws Throwable {
        this.replicaId = replicaId;
        this.clusterAddresses = clusterAddresses;
        this.debug = debug;
        this.exposeRaces = exposeRaces;
        if (this.replicaId < 0) {
            String str = "replicaId cannot be negative";
            throw (Throwable)new IllegalArgumentException(str.toString());
        }
    }


    public BFTConfig(Config config) throws Throwable {
        this.replicaId = config.getInt("bft.replicaId");

        this.clusterAddresses = (List<NetworkHostAndPort>)config.getAnyRefList("bft.clusterAddresses");
        this.debug = config.getBoolean("bft.debug");
        this.exposeRaces = config.getBoolean("bft.exposeRaces");;
        if (this.replicaId < 0) {
            String str = "replicaId cannot be negative";
            throw (Throwable)new IllegalArgumentException(str.toString());
        }



    }


    public final int getReplicaId() {
        return this.replicaId;
    }

    @NotNull
    public final List<NetworkHostAndPort> getClusterAddresses() {
        return this.clusterAddresses;
    }

    public final boolean getDebug() {
        return this.debug;
    }

    public final boolean getExposeRaces() {
        return this.exposeRaces;
    }

    public final int component1() {
        return this.replicaId;
    }

    @NotNull
    public final List<NetworkHostAndPort> component2() {
        return this.clusterAddresses;
    }

    public final boolean component3() {
        return this.debug;
    }

    public final boolean component4() {
        return this.exposeRaces;
    }

    @NotNull
    public final BFTConfig copy(int replicaId, @NotNull List<NetworkHostAndPort> clusterAddresses, boolean debug, boolean exposeRaces) throws Throwable {
        Intrinsics.checkParameterIsNotNull(clusterAddresses, "clusterAddresses");
        return new BFTConfig(replicaId, clusterAddresses, debug, exposeRaces);
    }

    @NotNull
    public String toString() {
        return "BFTSmartConfig(replicaId=" + this.replicaId + ", clusterAddresses=" + this.clusterAddresses + ", debug=" + this.debug + ", exposeRaces=" + this.exposeRaces + ")";
    }

    public int hashCode() {
        if (this.debug);
        if (this.exposeRaces);
        return ((Integer.hashCode(this.replicaId) * 31 + ((this.clusterAddresses != null) ? this.clusterAddresses.hashCode() : 0)) * 31 + 1) * 31 + 1;
    }

}






