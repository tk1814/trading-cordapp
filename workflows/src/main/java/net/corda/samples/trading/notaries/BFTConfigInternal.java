package net.corda.samples.trading.notaries;

import com.typesafe.config.Config;
import kotlin.jvm.internal.Intrinsics;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.node.services.transactions.PathManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BFTConfigInternal{
    private List<NetworkHostAndPort> replicaAddresses;

    private boolean exposeRaces;

    private int clusterSize;


    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Path path = Paths.get(new File("config").getCanonicalPath());

    private Boolean debug;


    @NotNull
    public static final String portIsClaimedFormat = "Port %s is claimed by another replica: %s";




    public boolean getExposeRaces() {
        return this.exposeRaces;
    }

    public BFTConfigInternal(@NotNull List<NetworkHostAndPort> replicaAddresses, @NotNull boolean debug, @NotNull boolean exposeRaces) throws IOException {
        //super(Paths.get(new File("config").getCanonicalPath()));
        this.replicaAddresses = replicaAddresses;
        this.exposeRaces = exposeRaces;
        this.debug = debug;
        this.clusterSize = replicaAddresses.size();
        Set claimedPorts = new LinkedHashSet<NetworkHostAndPort>();


        //int replicaId;
        for (int replicaId = 0; replicaId < this.clusterSize; replicaId++) {
            List<NetworkHostAndPort> networkHostAndPorts = replicaPorts(replicaId);
            for (NetworkHostAndPort port : networkHostAndPorts) {
                claimedPorts.add(port);
            }
        }

    }



    public int getClusterSize() {
        return this.clusterSize;
    }



    public void waitUntilReplicaWillNotPrintStackTrace(int contextReplicaId) throws SocketException, InterruptedException {
        int peerId = contextReplicaId - 1;
        if(peerId < 0) return;
//        NetworkHostAndPort address = BFTPort.FOR_REPLICAS.ofReplica(replicaAddresses.get(peerId));
//
//        while(!isListening(address)){
//            Thread.sleep(200);
//        }
    }


    private List<NetworkHostAndPort> replicaPorts(int replicaId) {
        NetworkHostAndPort base = this.replicaAddresses.get(replicaId);
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




    public static int maxFaultyReplicas(int clusterSize) {
        return (clusterSize - 1) / 3;
    }

    public static int minCorrectReplicas(int clusterSize) {
        return (2 * clusterSize + 3) / 3;
    }

    public static int minClusterSize(int maxFaultyReplicas) {
        return maxFaultyReplicas * 3 + 1;
    }




}

enum BFTPort {
    FOR_CLIENTS (0), FOR_REPLICAS (1);

    private int off;

    BFTPort(int off) {
        this.off = off;
    }

    @NotNull
    public NetworkHostAndPort ofReplica(@NotNull NetworkHostAndPort base) {
        Intrinsics.checkParameterIsNotNull(base, "base");
        return new NetworkHostAndPort(base.getHost(), base.getPort() + this.off);
    }
}

class BFTConfig {
   private int replicaId;

   private List<NetworkHostAndPort> clusterAddresses;

   private boolean debug;

   private boolean exposeRaces;

   public BFTConfig(@NotNull int replicaId, @NotNull List<NetworkHostAndPort> clusterAddresses, boolean debug, boolean exposeRaces) throws Throwable {
       this.replicaId = replicaId;
       this.clusterAddresses = clusterAddresses;
       this.debug = debug;
       this.exposeRaces = exposeRaces;
       if (this.replicaId < 0) {
           String str = "replicaId cannot be negative";
           throw new IllegalArgumentException(str.toString());
       }
   }


   public BFTConfig(Config config) throws Throwable {
       this.replicaId = config.getInt("bft.replicaId");

       this.clusterAddresses = new ArrayList<NetworkHostAndPort>();

       for(String str: (List<String>)config.getAnyRefList("bft.clusterAddresses")){
           this.clusterAddresses.add(NetworkHostAndPort.parse(str));
       }

       this.debug = config.getBoolean("bft.debug");
       this.exposeRaces = config.getBoolean("bft.exposeRaces");;
       if (this.replicaId < 0) {
           String str = "replicaId cannot be negative";
           throw (Throwable)new IllegalArgumentException(str.toString());
       }

   }


   public int getReplicaId() {
       return this.replicaId;
   }

   @NotNull
   public List<NetworkHostAndPort> getClusterAddresses() {
       return this.clusterAddresses;
   }

   public boolean getDebug() {
       return this.debug;
   }

   public boolean getExposeRaces() {
       return this.exposeRaces;
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






