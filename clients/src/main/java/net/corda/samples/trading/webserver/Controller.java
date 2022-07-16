package net.corda.samples.trading.webserver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trading.flows.*;
import net.corda.samples.trading.states.TradeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Define your API endpoints here.
 * <p>
 * Note we allow all origins for convenience, this is NOT a good production practice.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
@CrossOrigin(origins = "*")
public class Controller {
    private final CordaRPCOps proxy;
    private final CordaX500Name myLegalName;
    private final List<String> SERVICE_NAMES = ImmutableList.<String>of("Notary", "Controller");
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    private boolean isMe(NodeInfo nodeInfo) {
        return nodeInfo.getLegalIdentities().get(0).getName().equals(myLegalName);
    }

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.myLegalName = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Returns the node's name.
     */
    @RequestMapping(value = "/node", method = RequestMethod.GET)
    private ResponseEntity<String> returnName() {
        JsonObject resp = new JsonObject();
        resp.addProperty("name", myLegalName.toString());
        return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
    }

    @RequestMapping(value = "/nodes", method = RequestMethod.GET)
    public Map<String, List<CordaX500Name>> getNodes() {

        List<NodeInfo> nodes = proxy.networkMapSnapshot();
        return ImmutableMap.of("nodes", nodes
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !SERVICE_NAMES.contains(name.getOrganisation()))
                .collect(Collectors.toList()));
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @RequestMapping(value = "/peers", method = RequestMethod.GET)
    public Map<String, List<CordaX500Name>> getPeers() {

        List<NodeInfo> nodes = proxy.networkMapSnapshot();
        return ImmutableMap.of("peers", nodes
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !SERVICE_NAMES.contains(name.getOrganisation()))
                .collect(Collectors.toList()));
        //      .filter(name -> !name.getOrganisation().equals(myLegalName.getOrganisation()))
    }

    /**
     * Displays all Trade states that exist in the node's vault.
     */
    @RequestMapping(value = "/trades", method = RequestMethod.GET)
    public List<String> getTrades() {
        List<StateAndRef<TradeState>> stateAndRefs = proxy.vaultQuery(TradeState.class).getStates();
        List<String> tradeStates = stateAndRefs.stream().map(x -> x.getState().getData().toString()).collect(Collectors.toList());
        return tradeStates;
    }

    /**
     * Initiates Create Trade Flow.
     */
    @RequestMapping(value = "/createTrade", method = RequestMethod.POST)
    public ResponseEntity<String> createTrade(@RequestBody String payload) {
        System.out.println(payload);

        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);

        String orderType = convertedObject.get("orderType").getAsString();
        String tradeType = convertedObject.get("tradeType").getAsString();
        String stockName = convertedObject.get("stockName").getAsString();
        double stockPrice = convertedObject.get("stockPrice").getAsDouble();
        int stockQuantity = convertedObject.get("stockQuantity").getAsInt();
        String expirationDate = convertedObject.get("expirationDate").getAsString();
        String tradeDate = convertedObject.get("tradeDate").getAsString();

        JsonObject resp = new JsonObject();

        if (stockPrice <= 0) {
            resp.addProperty("Response", "Query parameter 'Stock Price' must be non-negative.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        }
        if (stockQuantity <= 0) {
            resp.addProperty("Response", "Query parameter 'Stock Quantity' must be non-negative.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else {
            try {
                TradeState tradeState = new TradeState(proxy.wellKnownPartyFromX500Name(myLegalName), null,
                        orderType, tradeType, stockName, stockPrice, stockQuantity, expirationDate, "Pending",
                        tradeDate, null, new UniqueIdentifier());

                SignedTransaction signedTx = proxy.startTrackedFlowDynamic(TradeFlow.Initiator.class, tradeState).getReturnValue().get();
                System.out.println("signedTx.getId() =  :" + signedTx.getId());
                resp.addProperty("Response", "Transaction id " + signedTx.getId() + " committed to ledger.\n");
                return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());

            } catch (Exception ex) {
                System.out.println("Exception : " + ex.getMessage());
                resp.addProperty("Response", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
            }
        }

    }

    /**
     * Initiates Counter Trade Flow.
     */
    @RequestMapping(value = "/counterTrade", method = RequestMethod.POST)
    public ResponseEntity<String> counterTrade(@RequestBody String payload) {
        System.out.println(payload);
        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);

        String initiatingParty = convertedObject.get("initiatingParty").getAsString();
        String counterParty = convertedObject.get("counterParty").getAsString();
        String orderType = convertedObject.get("orderType").getAsString();
        String tradeType = convertedObject.get("tradeType").getAsString();
        String stockName = convertedObject.get("stockName").getAsString();
        double stockPrice = convertedObject.get("stockPrice").getAsDouble();
        int stockQuantity = convertedObject.get("stockQuantity").getAsInt();
        String expirationDate = convertedObject.get("expirationDate").getAsString();
        String tradeStatus = convertedObject.get("tradeStatus").getAsString();
        String tradeID = convertedObject.get("tradeID").getAsString();
        String tradeDate = convertedObject.get("tradeDate").getAsString();
        String settlementDate = convertedObject.get("settlementDate").getAsString();

        JsonObject resp = new JsonObject();

        if (counterParty == null) {
            resp.addProperty("Response", "Query parameter 'counterParty' missing or has wrong format.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else if (proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(counterParty)) == null) {
            resp.addProperty("Response", "Counter Party named \" + counterParty + \" cannot be found.\\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else if (settlementDate == null || settlementDate.isEmpty() || settlementDate.equals("null")) {
            resp.addProperty("Response", "Query parameter 'settlementDate' missing or has wrong format.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else {
            try {
                UniqueIdentifier linearId = new UniqueIdentifier(null, UUID.fromString(tradeID));
                TradeState counterTradeState = new TradeState(this.proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(initiatingParty)),
                        this.proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(counterParty)), orderType,
                        tradeType, stockName, stockPrice, stockQuantity, expirationDate, tradeStatus, tradeDate,
                        settlementDate, linearId);

                SignedTransaction signedTx = proxy.startTrackedFlowDynamic(SettleTradeFlow.class, counterTradeState).getReturnValue().get();

                System.out.println("signedTx.getId() =  :" + signedTx.getId());
                resp.addProperty("Response", "Transaction id " + signedTx.getId() + " committed to ledger.\n");
                return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
            } catch (Exception ex) {
                System.out.println("Exception : " + ex.getMessage());
                resp.addProperty("Response", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
            }
        }
    }

    /**
     * Initiates Cancel Trade Flow.
     */
    @RequestMapping(value = "/cancelTrade", method = RequestMethod.POST)
    public ResponseEntity<String> cancelTrade(@RequestBody String payload) {
        System.out.println(payload);
        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);

        String initiatingParty = convertedObject.get("initiatingParty").getAsString();
        String orderType = convertedObject.get("orderType").getAsString();
        String tradeType = convertedObject.get("tradeType").getAsString();
        String stockName = convertedObject.get("stockName").getAsString();
        double stockPrice = convertedObject.get("stockPrice").getAsDouble();
        int stockQuantity = convertedObject.get("stockQuantity").getAsInt();
        String expirationDate = convertedObject.get("expirationDate").getAsString();
        String tradeStatus = convertedObject.get("tradeStatus").getAsString();
        String tradeID = convertedObject.get("tradeID").getAsString();
        String tradeDate = convertedObject.get("tradeDate").getAsString();

        JsonObject resp = new JsonObject();

        try {
            UniqueIdentifier linearId = new UniqueIdentifier(null, UUID.fromString(tradeID));
            TradeState cancelTradeState = new TradeState(this.proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(initiatingParty)),
                    null, orderType, tradeType, stockName, stockPrice, stockQuantity, expirationDate, tradeStatus,
                    tradeDate, null, linearId);

            SignedTransaction signedTx = proxy.startTrackedFlowDynamic(CancelTradeFlow.CancelInitiator.class, cancelTradeState).getReturnValue().get();

            System.out.println("signedTx.getId() =  :" + signedTx.getId());
            resp.addProperty("Response", "Transaction id " + signedTx.getId() + " committed to ledger.\n");
            return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } catch (Exception ex) {
            System.out.println("Exception : " + ex.getMessage());
            resp.addProperty("Response", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        }

    }

    @RequestMapping(value = "/issueStock", method = RequestMethod.POST)
    public ResponseEntity<String> issueStock(@RequestBody String payload) {
        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);

        int amount = convertedObject.get("amount").getAsInt();
        String name = convertedObject.get("name").getAsString();
        JsonObject resp = new JsonObject();

        if (amount <= 0) {
            resp.addProperty("Response", "Query parameter 'Amount' must be non-negative.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else {
            try {
                String answer = proxy.startFlowDynamic(CreateAndIssueStock.class, name, amount).getReturnValue().get();
                System.out.println(answer);

                resp.addProperty("Response", "Success");
                resp.addProperty("Amount", amount);
                resp.addProperty("Name", name);
                return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());

            } catch (Exception e) {
                System.out.println("Exception : " + e.getMessage());
                resp.addProperty("Exception : ", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
            }
        }
    }

    @RequestMapping(value = "/getStockList", method = RequestMethod.GET)
    public ResponseEntity<String> getStockList() {
        JsonObject resp = new JsonObject();
        try {
            List<String> amountOfStocks = proxy.startFlowDynamic(QueryTokens.GetTokenBalance.class).getReturnValue().get();
            resp.addProperty("Response", "Success");
            resp.addProperty("StockList", String.valueOf(amountOfStocks));
            return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());

        } catch (Exception e) {
            System.out.println("Exception : " + e.getMessage());
            resp.addProperty("Exception : ", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        }
    }

    /**
     * Initiates a flow that self-issues cash and then send this to a recipient.
     */
    @RequestMapping(value = "/issueMoney", method = RequestMethod.POST)
    public ResponseEntity<String> issueMoney(@RequestBody String payload) {
        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);
        double amount = convertedObject.get("amount").getAsDouble();

        JsonObject resp = new JsonObject();
        try {
            String result = proxy.startFlowDynamic(IssueMoney.class, "USD", amount, proxy.wellKnownPartyFromX500Name(myLegalName)).getReturnValue().get();
            System.out.println(result);

            resp.addProperty("Response", "Success");
            resp.addProperty("Amount", amount);
            resp.addProperty("Result", result);
            return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());

        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause().getClass().equals(TransactionVerificationException.ContractRejection.class)) {
                resp.addProperty("ExecutionException : ", e.getCause().getMessage());
            } else {
                resp.addProperty("ExecutionException : ", e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } catch (Exception e) {
            resp.addProperty("Exception : ", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        }
    }

    @RequestMapping(value = "/getMoneyBalance", method = RequestMethod.GET)
    public ResponseEntity<String> getMoneyBalance() {
        JsonObject resp = new JsonObject();

        try {
            String fiatBalance = proxy.startFlowDynamic(QueryTokens.GetFiatBalance.class, "USD").getReturnValue().get();
            resp.addProperty("Response", "Success");
            resp.addProperty("Amount", fiatBalance);
            return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());

        } catch (Exception e) {
            resp.addProperty("Exception : ", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        }
    }
}
