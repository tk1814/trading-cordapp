package Services;

import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.util.Pair;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.ComponentVisibilityException;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;
import net.corda.samples.trading.contracts.TradeContract;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@CordaService
public class Oracle extends SingletonSerializeAsToken {

    private final ServiceHub serviceHub;
    private final PublicKey myKey;

    private final Map<String, BigDecimal> priceMap =  new HashMap<>();

    public Oracle(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        this.myKey = serviceHub.getMyInfo().getLegalIdentities().get(0).getOwningKey();
        loadPrices();
    }

    public BigDecimal query(Map<String,String> reqMap) {
        if (reqMap.get("stockName") != null && !reqMap.get("stockName").equals("")) {
            return getCurrent(reqMap.get("stockName"));
        } else {
            return new BigDecimal("0");
        }
    }

    public BigDecimal getCurrent(String stockName) {
        if (priceMap.get(stockName) != null && !priceMap.get(stockName).equals("")) {
            return priceMap.get(stockName);
        } else {
            return new BigDecimal("0");
        }
    }

    private void loadPrices() {
        this.priceMap.put("Google", new BigDecimal("2291"));
        this.priceMap.put("Tencent",new BigDecimal("43.42"));
    }

    public TransactionSignature sign(FilteredTransaction tx) throws FilteredTransactionVerificationException {
        // Check the partial Merkle tree is valid.
        tx.verify();
        // Is it a Merkle tree we are willing to sign over?
        boolean isValidMerkleTree = tx.checkWithFun(this::isCommandWithCorrectPrimeAndIAmSigner);
        /**
         * Function that checks if all of the commands that should be signed by the input public key are visible.
         * This functionality is required from Oracles to check that all of the commands they should sign are visible.
         */
        try {
            tx.checkCommandVisibility(serviceHub.getMyInfo().getLegalIdentities().get(0).getOwningKey());
        } catch (ComponentVisibilityException e) {
            e.printStackTrace();
        }

        if (isValidMerkleTree) {
            return serviceHub.createSignature(tx, myKey);
        } else {
            throw new IllegalArgumentException("Oracle signature requested over invalid transaction.");
        }

    }

    /**
     * Returns true if the component is an Create command that:
     * - two values are equal
     * - Has the oracle listed as a signer
     */
    private boolean isCommandWithCorrectPrimeAndIAmSigner(Object elem) {
        if (elem instanceof Command && ((Command) elem).getValue() instanceof TradeContract.Commands.OracleTrade) {
            TradeContract.Commands.OracleTrade cmdData = (TradeContract.Commands.OracleTrade) ((Command) elem).getValue();
            return (((Command) elem).getSigners().contains(myKey) && cmdData.getOracleValue().equals(cmdData.getPartyValue()));
        }
        return false;
    }

}
