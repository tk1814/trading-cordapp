package net.corda.samples.trading.webserver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.WireTransaction;
import net.corda.samples.trading.flows.CounterTradeFlow;
import net.corda.samples.trading.flows.TradeFlow;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;

import net.corda.samples.trading.states.TradeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.*;

import javax.ws.rs.QueryParam;


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
    private final CordaX500Name myLegalName; // was "me"
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

    @RequestMapping(value = "/nodes", method = RequestMethod.GET) //@Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getNodes() {

        List<NodeInfo> nodes = proxy.networkMapSnapshot();
        System.out.println("Nodes :" + nodes);
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
        System.out.println("Peers :" + nodes);
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

        double sellValue = convertedObject.get("sellValue").getAsDouble();
        int sellQuantity = convertedObject.get("sellQuantity").getAsInt();
        double buyValue = convertedObject.get("buyValue").getAsDouble();
        int buyQuantity = convertedObject.get("buyQuantity").getAsInt();

        JsonObject resp = new JsonObject();

        if (sellValue < 0) {
            resp.addProperty("Response", "Query parameter 'Sell Value' must be non-negative.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        }
        if (buyValue < 0) {
            resp.addProperty("Response", "Query parameter 'Buy Value' must be non-negative.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else {
            try {
                TradeState tradeState = new TradeState(sellValue,
                        sellQuantity,
                        buyValue,
                        buyQuantity,
                        proxy.wellKnownPartyFromX500Name(myLegalName),
                        null,
                        "Pending",
                        new UniqueIdentifier());

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
        double sellValue = convertedObject.get("sellValue").getAsDouble();
        int sellQuantity = convertedObject.get("sellQuantity").getAsInt();
        double buyValue = convertedObject.get("buyValue").getAsDouble();
        int buyQuantity = convertedObject.get("buyQuantity").getAsInt();
        String tradeStatus = convertedObject.get("tradeStatus").getAsString();
        String tradeID = convertedObject.get("tradeID").getAsString();

        JsonObject resp = new JsonObject();

        if (counterParty == null) {
            resp.addProperty("Response", "Query parameter 'Counter partyName' missing or has wrong format.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else if (proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(counterParty)) == null) {
            resp.addProperty("Response", "Counter Party named \" + counterParty + \" cannot be found.\\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else {
            try {
                UniqueIdentifier linearId = new UniqueIdentifier(null, UUID.fromString(tradeID));
                TradeState counterTradeState = new TradeState(
                        sellValue,
                        sellQuantity,
                        buyValue,
                        buyQuantity,
                        this.proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(initiatingParty)),
                        this.proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(counterParty)),
                        tradeStatus, linearId);

                SignedTransaction signedTx = proxy.startTrackedFlowDynamic(CounterTradeFlow.CounterInitiator.class, counterTradeState).getReturnValue().get();
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
     * Get full Trade details.
     */
    @RequestMapping(value = "/getTrade", method = RequestMethod.GET)
    public Response gettrades(@QueryParam("tradeId") String tradeId) {
        if (tradeId == null) {
            return Response.status(Status.BAD_REQUEST).entity("Query parameter 'linearID' missing or has wrong format.\n").build();
        }
//        QueryCriteria.LinearStateQueryCriteria("tradeId")
//        val idParts = linearID.split('_')
//        val uuid = idParts[idParts.size - 1]
//
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(uuid)),status = Vault.StateStatus.ALL)
//        return try {
//            Response.ok(rpcOps.vaultQueryBy<TradeState>(criteria=criteria).states).build()
//        } catch (ex: Throwable) {
//            logger.error(ex.message, ex)
//            Response.status(BAD_REQUEST).entity(ex.message!!).build()
//        }
        return null;
    }
}
