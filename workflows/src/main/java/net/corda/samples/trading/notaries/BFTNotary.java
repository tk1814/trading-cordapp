package net.corda.samples.trading.notaries;


import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.internal.notary.SinglePartyNotaryService;
import net.corda.core.internal.notary.UniquenessProvider;
import net.corda.core.node.ServiceHub;
import net.corda.node.services.api.ServiceHubInternal;
import net.corda.node.services.config.NotaryConfig;
import net.corda.node.services.transactions.PersistentUniquenessProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.Objects;


public class BFTNotary extends SinglePartyNotaryService {

    private static Logger logger = LoggerFactory.getLogger(BFTNotary.class);

    private ServiceHubInternal serviceHubInternal;
    private PublicKey publicKey;
    private NotaryConfig notaryConfig;
    private UniquenessProvider uniquenessProvider;






    public BFTNotary (ServiceHubInternal serviceHubInternal, PublicKey publicKey) {
        super();
        this.serviceHubInternal = serviceHubInternal;
        this.publicKey = publicKey;
        this.uniquenessProvider = new PersistentUniquenessProvider(
                this.serviceHubInternal.getClock(),
                this.serviceHubInternal.getDatabase(),
                this.serviceHubInternal.getCacheFactory(),
                this::signTransaction);

        this.notaryConfig = Objects.requireNonNull(serviceHubInternal.getConfiguration().getNotary());
//        Config extraConfig = Objects.requireNonNull(
//                this.notaryConfig.getExtraConfig(),
//                "required `extraConfig.bft` key in notary config");
    }



    @NotNull
    @Override
    public FlowLogic<Void> createServiceFlow(@NotNull FlowSession otherPartySession) {
        return new BFTNotaryFlow(this, otherPartySession);
    }


    @Override
    public void start() {
        int i=0;
        // constructor() start()  stop()
    }

    @Override
    public void stop() {
        int j=0;
    }




    @NotNull
    @Override
    public PublicKey getNotaryIdentityKey() {
        return this.publicKey;
    }

    @NotNull
    @Override
    public ServiceHub getServices() {
        return this.serviceHubInternal;
    }

    @NotNull
    @Override
    protected UniquenessProvider getUniquenessProvider() {
        return this.uniquenessProvider;
    }

//    @NotNull
//    @Override
//    protected UniquenessProvider getUniquenessProvider() {
//        //
//        return this.uniquenessProvider;
//    }


}
