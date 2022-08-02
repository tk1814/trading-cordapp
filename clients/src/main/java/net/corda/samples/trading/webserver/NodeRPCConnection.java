package net.corda.samples.trading.webserver;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.samples.trading.flows.IssueMoney;
import net.corda.samples.trading.flows.QueryTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutionException;

/**
 * Wraps an RPC connection to a Corda node.
 *
 * The RPC connection is configured using command line arguments.
 */
@Component
public class NodeRPCConnection implements AutoCloseable {
    // The host of the node we are connecting to.
    @Value("${config.rpc.host}")
    private String host;
    // The RPC port of the node we are connecting to.
    @Value("${config.rpc.username}")
    private String username;
    // The username for logging into the RPC client.
    @Value("${config.rpc.password}")
    private String password;
    // The password for logging into the RPC client.
    @Value("${config.rpc.port}")
    private int rpcPort;

    private CordaRPCConnection rpcConnection;
    CordaRPCOps proxy;

    @PostConstruct
    public void initialiseNodeRPCConnection() throws ExecutionException, InterruptedException {
        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, rpcPort);
        CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
        rpcConnection = rpcClient.start(username, password);
        proxy = rpcConnection.getProxy();

        // Issue money to party
        String fiatBalance = proxy.startFlowDynamic(QueryTokens.GetFiatBalance.class, "USD").getReturnValue().get();
        if (fiatBalance.equals("0.0 USD")) {
            Party owner = proxy.nodeInfo().getLegalIdentities().get(0);
            double moneyAmount = 10000;
            String res = proxy.startFlowDynamic(IssueMoney.class, "USD", moneyAmount, owner).getReturnValue().get();
            System.out.println(res);
        }
    }

    @PreDestroy
    public void close() {
        rpcConnection.notifyServerAndClose();
    }
}
