package net.corda.samples.trading.webserver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trading.entity.MatchRecord;
import net.corda.samples.trading.flows.*;
import net.corda.samples.trading.states.TradeState;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.MAX_PAGE_SIZE;

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

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.myLegalName = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /**
     * Returns the node's name.
     */
    @RequestMapping(value = "/node", method = RequestMethod.GET)
    private ImmutableMap<String, String> returnName() {
        return ImmutableMap.of("name", myLegalName.toString());
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @RequestMapping(value = "/nodes", method = RequestMethod.GET)
    public ImmutableMap<String, List<String>> getNodes() {
        List<NodeInfo> nodes = proxy.networkMapSnapshot();
        return ImmutableMap.of("nodes", nodes
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.getOrganisation().contains("Notary"))
                .map(CordaX500Name::toString)
                .collect(Collectors.toList()));
    }

    /**
     * Displays all Trade states that exist in the node's vault.
     */
    @RequestMapping(value = "/trades", method = RequestMethod.GET)
    public List<String> getTrades() {
        PageSpecification pageSpec = new PageSpecification(DEFAULT_PAGE_NUM, MAX_PAGE_SIZE);
        QueryCriteria linearCriteriaAll = new QueryCriteria.LinearStateQueryCriteria(null, null, Vault.StateStatus.UNCONSUMED, null);
        List<StateAndRef<TradeState>> stateAndRefs = proxy.vaultQueryByWithPagingSpec(TradeState.class, linearCriteriaAll, pageSpec).getStates();
        return stateAndRefs.stream().map(x -> x.getState().getData().toString()).collect(Collectors.toList());

//        List<StateAndRef<TradeState>> stateAndRefs = proxy.vaultQuery(TradeState.class).getStates();
//        return stateAndRefs.stream().map(x -> x.getState().getData().toString()).collect(Collectors.toList());
    }

    /**
     * Initiates Create Trade Flow.
     */
    @RequestMapping(value = "/createTrade", method = RequestMethod.POST)
    public ResponseEntity<String> createTrade(@RequestBody String payload) {
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
                        orderType, tradeType, stockName, stockPrice, stockQuantity, LocalDateTime.parse(expirationDate + ":00.00"), "Pending",
                        LocalDateTime.parse(tradeDate), null, new UniqueIdentifier());

                SignedTransaction signedTx = proxy.startTrackedFlowDynamic(TradeFlow.Initiator.class, tradeState).getReturnValue().get();
                System.out.println("signedTx.getId() =  :" + signedTx.getId());
                resp.addProperty("Response", "Transaction id " + signedTx.getId() + " committed to ledger.\n");

                //add match
                List<MatchRecord> matchRecords = proxy.startTrackedFlowDynamic(MatchOrdersFlow.MatchOrdersInitiator.class, tradeState).getReturnValue().get();
                System.out.println("matchSignedTx.matchRecords() =  :" + matchRecords.toArray());


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
        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);

        String counterPartyString = convertedObject.get("counterParty").getAsString();
        String tradeID = convertedObject.get("tradeID").getAsString();
        String settlementDate = convertedObject.get("settlementDate").getAsString();
        JsonObject resp = new JsonObject();

        if (counterPartyString == null) {
            resp.addProperty("Response", "Query parameter 'counterParty' missing or has wrong format.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else if (proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(counterPartyString)) == null) {
            resp.addProperty("Response", "Counter Party named \" + counterParty + \" cannot be found.\\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else if (settlementDate == null || settlementDate.isEmpty() || settlementDate.equals("null")) {
            resp.addProperty("Response", "Query parameter 'settlementDate' missing or has wrong format.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else {
            try {
                UniqueIdentifier linearId = new UniqueIdentifier(null, UUID.fromString(tradeID));
                Party counterParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(counterPartyString));
                SignedTransaction signedTx = proxy.startTrackedFlowDynamic(SettleTradeFlow.class, counterParty, LocalDateTime.parse(settlementDate), linearId).getReturnValue().get();

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
        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);

        String tradeStatus = convertedObject.get("tradeStatus").getAsString();
        String tradeID = convertedObject.get("tradeID").getAsString();
        JsonObject resp = new JsonObject();

        if (!(tradeStatus.equals("Cancelled") || tradeStatus.equals("Expired"))) {
            resp.addProperty("Response", "Query parameter 'tradeStatus' is wrong.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else {
            try {
                UniqueIdentifier linearId = new UniqueIdentifier(null, UUID.fromString(tradeID));
                SignedTransaction signedTx = proxy.startTrackedFlowDynamic(CancelTradeFlow.CancelInitiator.class, tradeStatus, linearId).getReturnValue().get();

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

    @RequestMapping(value = "/issueStock", method = RequestMethod.POST)
    public ResponseEntity<String> issueStock(@RequestBody String payload) {
        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);

        long amount = convertedObject.get("amount").getAsLong();
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
    public ImmutableList<List<String>> getStockList() {
        try {
            List<String> amountOfStocks = proxy.startFlowDynamic(QueryTokens.GetTokenBalance.class).getReturnValue().get();
            return ImmutableList.of(amountOfStocks);
        } catch (Exception e) {
            System.out.println("Exception : " + e.getMessage());
            return ImmutableList.of();
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
