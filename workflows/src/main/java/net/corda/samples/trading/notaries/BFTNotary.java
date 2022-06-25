package net.corda.samples.trading.notaries;


import net.corda.core.crypto.*;
import net.corda.core.flows.*;
import net.corda.core.internal.notary.NotaryService;
import net.corda.core.node.ServiceHub;
import net.corda.node.services.api.ServiceHubInternal;
import net.corda.node.services.config.NotaryConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.security.PublicKey;
import java.util.Objects;

public class BFTNotary extends NotaryService{

//    class Server extends BFTServer {
//    }


    private static Logger logger = LoggerFactory.getLogger(BFTNotary.class);

    private final ServiceHubInternal serviceHubInternal;
    private final PublicKey publicKey;
    private final NotaryConfig notaryConfig;


    public BFTNotary (ServiceHubInternal serviceHubInternal, PublicKey publicKey) {
        super();
        this.serviceHubInternal = serviceHubInternal;
        this.publicKey = publicKey;
        this.notaryConfig = Objects.requireNonNull(serviceHubInternal.getConfiguration().getNotary());
//        Config extraConfig = Objects.requireNonNull(
//                this.notaryConfig.getExtraConfig(),
//                "required `extraConfig.bft` key in notary config");
    }


    @NotNull
    @Override
    public PublicKey getNotaryIdentityKey() {
        return publicKey;
    }

    @NotNull
    @Override
    public ServiceHub getServices() {
        return serviceHubInternal;
    }

    @NotNull
    @Override
    public FlowLogic<Void> createServiceFlow(@NotNull FlowSession otherPartySession) {
        return new BFTNotaryFlow(this, otherPartySession);
    }



    TransactionSignature signTransaction(SecureHash txId) {
        SignableData signableData = new SignableData(txId, new SignatureMetadata(serviceHubInternal.getMyInfo().getPlatformVersion(), Crypto.findSignatureScheme(publicKey).getSchemeNumberID()));
        return serviceHubInternal.getKeyManagementService().sign(signableData, publicKey);
    }


    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
