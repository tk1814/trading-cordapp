package net.corda.samples.trading.webserver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trading.flows.CounterTradeFlow;
import net.corda.samples.trading.flows.TradeFlow;
import net.corda.samples.trading.states.SantaSessionState;
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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status.*;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
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

    private Party getPartyFromNodeInfo(NodeInfo nodeInfo) {
        Party target = nodeInfo.getLegalIdentities().get(0);
        return target;
    }

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.myLegalName = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    @RequestMapping(value = "/me", method = RequestMethod.POST, produces = "application/json")
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

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @RequestMapping(value = "/peers", method = RequestMethod.GET) //@Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {

        List<NodeInfo> nodes = proxy.networkMapSnapshot();
        //System.out.println("Peers :" + nodes);
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
    @RequestMapping(value = "/trades", method = RequestMethod.GET, produces = "application/json")
    public List<StateAndRef<TradeState>> getTrades() {
        return proxy.vaultQuery(TradeState.class).getStates();
    }

    /**
     * Initiates Create Trade Flow.
     */
    @RequestMapping(value = "/createTrade", method = RequestMethod.POST)
    public ResponseEntity<String> createTrade(@RequestBody String payload) {
        System.out.println(payload);

        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);

        String counterParty = convertedObject.get("counterParty").getAsString();
        int sellValue = convertedObject.get("sellValue").getAsInt();
        String sellCurrency = convertedObject.get("sellCurrency").getAsString();
        int buyValue = convertedObject.get("buyValue").getAsInt();
        String buyCurrency = convertedObject.get("buyCurrency").getAsString();

        JsonObject resp = new JsonObject();

        if (counterParty == null) {
            resp.addProperty("Response", "Query parameter 'Counter partyName' missing or has wrong format.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        }
        if (sellValue <= 0) {
            resp.addProperty("Response", "Query parameter 'Sell Value' must be non-negative.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        }
        if (buyValue <= 0) {
            resp.addProperty("Response", "Query parameter 'Buy Value' must be non-negative.\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        }
        if (proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(counterParty)) == null) { // TODO request gets stuck here
            resp.addProperty("Response", "Counter Party named \" + counterParty + \" cannot be found.\\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } else {

            try {
                //UUID.randomUUID().toString(); //UniqueIdentifier.Companion.fromString(convertedObject.get("gameId").getAsString());
                UniqueIdentifier tradeId = new UniqueIdentifier();

                String tradeStatus = "pending";
                TradeState tradeState = new TradeState(sellValue,
                        sellCurrency,
                        buyValue,
                        buyCurrency,
                        proxy.wellKnownPartyFromX500Name(myLegalName),
                        proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(counterParty)),
                        tradeStatus,
                        tradeId);
                System.out.println(6);
//                SignedTransaction signedTx = proxy.startFlowDynamic(TradeFlow.class, tradeState).getReturnValue().get();
                // TODO Exception : net.corda.core.transactions.MissingContractAttachments: Cannot find contract attachments for com.cs.cordapp.contract.TradeContractnull.
                SignedTransaction signedTx = proxy.startTrackedFlowDynamic(TradeFlow.class, tradeState).getReturnValue().get();

//            SantaSessionState output = proxy.startTrackedFlowDynamic(CreateSantaSessionFlow.class, playerNames, playerEmails, elf).getReturnValue().get().getTx().outputsOfType(SantaSessionState.class).get(0);
//            UniqueIdentifier gameId = output.getLinearId();

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
    // TODO COMPLETE TRADEFLOW AND COMPLETE THE CONTROLLER

    /**
     * Initiates Create Trade Flow.
     */
    @RequestMapping(value = "/counterTrade", method = RequestMethod.PUT)
    public Response counterTrade(@QueryParam("sellValue") int sellValue,
                                 @QueryParam("sellCurrency") String sellCurrency,
                                 @QueryParam("buyValue") int buyValue,
                                 @QueryParam("buyCurrency") String buyCurrency,
                                 @QueryParam("counterParty") CordaX500Name counterParty,
                                 @QueryParam("tradeStatus") String tradeStatus) {

        System.out.println("Create trade  command");
        if (this.proxy.wellKnownPartyFromX500Name(counterParty) == null) {
            return Response.status(Status.BAD_REQUEST).entity("Counter Party named " + counterParty + " cannot be found.\n").build();
        } else {
            try {
                TradeState tradestate = new TradeState(
                        sellValue,
                        sellCurrency,
                        buyValue,
                        buyCurrency,
                        this.proxy.wellKnownPartyFromX500Name(myLegalName),
                        this.proxy.wellKnownPartyFromX500Name(counterParty),
                        tradeStatus, null);

                SignedTransaction signedTx = proxy.startFlowDynamic(CounterTradeFlow.class, tradestate)
                        .getReturnValue().get();

                System.out.println("signedTx.getId() =  :" + signedTx.getId());
                return Response.status(Status.CREATED).entity("Transaction id " + signedTx.getId() + " committed to ledger.\n").build();

            } catch (Exception ex) {

                return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
            }
        }

    }

    /**
     * Get full Trade details.
     */
//    @Produces(MediaType.APPLICATION_JSON)
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

    //    SECRETSANTA CONTROLLER
//    @RequestMapping(value = "/node", method = RequestMethod.GET)
//    private ResponseEntity<String> returnName() {
//        JsonObject resp = new JsonObject();
//        resp.addProperty("name", me.toString());
//        return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
//    }
//
//    @RequestMapping(value = "/games/check", method = RequestMethod.POST)
//    public ResponseEntity<String> checkGame(@RequestBody String payload) {
//
//        System.out.println(payload);
//
//        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);
//
//        UniqueIdentifier gameId = UniqueIdentifier.Companion.fromString(convertedObject.get("gameId").getAsString());
//        // NOTE lowercase the name for easy retrieve
//        String playerName = "\"" + convertedObject.get("name").getAsString().toLowerCase().trim() + "\"";
//
//        // optional param
//        Boolean sendEmail = convertedObject.get("sendEmail").getAsBoolean();
//
//        JsonObject resp = new JsonObject();
//
//        try {
//            SantaSessionState output = proxy.startTrackedFlowDynamic(CheckAssignedSantaFlow.class, gameId).getReturnValue().get();
//
//            if (output.getAssignments().get(playerName) == null) {
//                resp.addProperty("target", "target not found in this game");
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
//            }
//
//            List<String> playerNames = output.getPlayerNames();
//            List<String> playerEmails = output.getPlayerEmails();
//
//            String playerEmail = playerEmails.get(playerNames.indexOf(playerName));
//            String targetName = output.getAssignments().get(playerName).replace("\"", "");
//
//            resp.addProperty("target", targetName);
//
//            if (sendEmail) {
//                System.out.println("sending email to "+ playerEmail);
//                boolean b = sendEmail(playerEmail, craftNotice(playerName, targetName, gameId));
//
//                if (!b) {
//                    System.out.println("ERROR: Failed to send email.");
//                }
//            }
//
//            return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }
//
    @RequestMapping(value = "/games", method = RequestMethod.POST)
    public ResponseEntity<String> createGame(@RequestBody String payload) {
        System.out.println(payload);

        JsonObject resp = new JsonObject();
        resp.addProperty("gameId", "one");
        return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
    }
//        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);
//
//        JsonArray pNames = convertedObject.getAsJsonArray("playerNames");
//        JsonArray pMails = convertedObject.getAsJsonArray("playerEmails");
//
//        // optional param
//        Boolean sendEmail = convertedObject.get("sendEmail").getAsBoolean();
//
//        List<String> playerNames = new ArrayList<>();
//        List<String> playerEmails = new ArrayList<>();
//
//        // NOTE: we lowercase all names internally for clarity
//        for (JsonElement jo : pNames) {
//            String newName = jo.toString().toLowerCase();
//
//            if (!playerNames.contains(newName)) {
//                playerNames.add(newName);
//            }
//        }
//        for (JsonElement jo : pMails) {
//            playerEmails.add(jo.toString());
//        }
//
//        try {
//            Party elf = getPartyFromNodeInfo(proxy.networkMapSnapshot().get(2));
//
//            // run the flow to create our game
//            SantaSessionState output = proxy.startTrackedFlowDynamic(CreateSantaSessionFlow.class, playerNames, playerEmails, elf).getReturnValue().get().getTx().outputsOfType(SantaSessionState.class).get(0);
//            UniqueIdentifier gameId = output.getLinearId();
//            LinkedHashMap<String, String> assignments = output.getAssignments();
//
//            System.out.println("Created Secret Santa Game ID# " + output.getLinearId().toString());
//
//            // send email to each player with the assignments
//            for (String p: playerNames) {
//                String t = assignments.get(p).replace("\"", "");
//                String msg = craftNotice(p, t, gameId);
//                int ind = playerNames.indexOf(p);
//                String email = playerEmails.get(ind).replace("\"", "");
//
//                if (sendEmail) {
//                    boolean b = sendEmail(email, msg);
//
//                    if (!b) {
//                        System.out.println("ERROR: Failed to send email.");
//                    }
//                }
//            }
//
//            JsonObject resp = new JsonObject();
//            resp.addProperty("gameId", gameId.toString());
//            return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
//
//        } catch (Exception e) {
//            System.out.println("Exception : " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }
//
//
//    public String craftNotice(String p, String t, UniqueIdentifier gameId) {
//        return "Hello, " + p + "!" + "\n" + "Your super secret santa target for game # " + gameId.toString() + " is " + t + ".";
//    }
//
//    public boolean sendEmail(String recipient, String msg) {
//
//        final String SENDGRID_API_KEY = "SG.xxxxxx_xxxxxxxxxxxxxxx.xxxxxxxxxxxxxxxx_xxxxxxxxxxxxxxxxxx_xxxxxxx";
//
//        if (SENDGRID_API_KEY.equals("SG.xxxxxx_xxxxxxxxxxxxxxx.xxxxxxxxxxxxxxxx_xxxxxxxxxxxxxxxxxx_xxxxxxx")) {
//            System.out.println("You haven't added your sendgrid api key!");
//            return true;
//        }
//
//        // you'll need to specify your sendgrid verified sender identity for this to mail out.
//        Email from = new Email("test@example.com");
//        String subject = "Secret Assignment from the Elves";
//        Email to = new Email(recipient);
//        Content content = new Content("text/plain", msg);
//        Mail mail = new Mail(from, subject, to, content);
//
//        SendGrid sg = new SendGrid(SENDGRID_API_KEY);
//        Request request = new Request();
//
//        try {
//            request.setMethod(Method.POST);
//            request.setEndpoint("mail/send");
//            request.setBody(mail.build());
//            Response response = sg.api(request);
//            System.out.println(response.getStatusCode());
//            System.out.println(response.getBody());
//            System.out.println(response.getHeaders());
//            return true;
//        } catch (IOException ex) {
//            System.out.println(ex.toString());
//            return false;
//        }
//    }
}
