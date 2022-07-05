package net.corda.samples.trading.notaries;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import javax.annotation.Nullable;

public class BFTConfig {
    /**
     * The account ID that will be sending HCS messages (and will create the topic if it doesn't
     * exist)
     */
    //public final AccountId accountId;

    /**
     * The private key for the account.
     */
    public final byte[] privateKey=null;

    /**
     * The topic ID to use for HCS. If not given it will be created.
     */
    //@Nullable
    //public final ConsensusTopicId topicId;

    /**
     * The submit key to use with the given topic, or if it doesn't exist, the submit key to use
     * with the new topic.
     */
    @Nullable
    public final byte[] submitKey=null;

    public BFTConfig(Config config) {

        String privateKey = null;

        try {
            privateKey = config.getString("pbft.privateKey");
            //this.privateKey = Hex.decode(privateKey.substring(32, 96));
        } catch (ConfigException.Missing e) {
            // ignored
        }


        String submitKey = null;

        try {
            submitKey = config.getString("bft.submitKey");
            //this.submitKey = Hex.decode(submitKey.substring(32, 96));
        } catch (ConfigException.Missing e) {
            // ignored
        }
    }
}
