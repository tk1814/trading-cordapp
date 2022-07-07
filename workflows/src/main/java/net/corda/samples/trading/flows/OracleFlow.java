package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.sun.org.apache.xpath.internal.operations.Bool;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.trading.contracts.TradeContract;
import net.corda.samples.trading.states.TradeState;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OracleFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String stockName;
        private final Boolean tradeType;
        private final Double amount;

        private static final ProgressTracker.Step SET_UP = new ProgressTracker.Step("Initialising flows.");
        private static final ProgressTracker.Step MATCHING_COUNTER = new ProgressTracker.Step(" Matching counter party");
        private static final ProgressTracker.Step QUERYING_THE_ORACLE = new ProgressTracker.Step("Querying oracle for the stock value");
        private static final ProgressTracker.Step BUILDING_THE_TX = new ProgressTracker.Step("Building transaction.");
        private static final ProgressTracker.Step VERIFYING_THE_TX = new ProgressTracker.Step("Verifying transaction.");
        private static final ProgressTracker.Step WE_SIGN = new ProgressTracker.Step("signing transaction.");
        private static final ProgressTracker.Step ORACLE_SIGNS = new ProgressTracker.Step("Requesting oracle signature.");
        private static final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising transaction.") {
            @NotNull
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };

        public ProgressTracker progressTracker = new ProgressTracker(SET_UP, MATCHING_COUNTER, QUERYING_THE_ORACLE, BUILDING_THE_TX, VERIFYING_THE_TX,
                WE_SIGN, ORACLE_SIGNS, FINALISING);

        public Initiator(final String stockName, final Boolean tradeType, final Double amount) {
            this.stockName = stockName;
            this.tradeType = tradeType;
            this.amount = amount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            progressTracker.setCurrentStep(SET_UP);

            Party initiatingParty = getOurIdentity();
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2
            CordaX500Name oracleName = new CordaX500Name("Oracle", "New York", "US");
            Party oracle = getServiceHub().getNetworkMapCache().getNodeByLegalName(oracleName)
                    .getLegalIdentities().get(0);
            if (oracle == null) {
                throw new IllegalArgumentException("Requested oracle");
            }

            progressTracker.setCurrentStep(MATCHING_COUNTER);
            Vault.Page<TradeState> results = null;
            try {
                //Query for the asserts of unconsumed counterparties
                //true buy stocks, false sell stocks
                FieldInfo attributeSellValue = null;
                FieldInfo attributeStockName = null;
                FieldInfo attributeBuyValue = null;
                CriteriaExpression valueIndex = null;
                CriteriaExpression stockNameIndex = null;

                QueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
                if (tradeType) {
                    attributeSellValue = QueryCriteriaUtils.getField("sellValue", TradeState.class);
                    valueIndex = Builder.equal(attributeSellValue, amount);
                } else {
                    attributeBuyValue = QueryCriteriaUtils.getField("buyValue", TradeState.class);
                    valueIndex = Builder.equal(attributeBuyValue, amount);
                }
                attributeStockName = QueryCriteriaUtils.getField("stockName", TradeState.class);
                stockNameIndex = Builder.equal(attributeStockName, stockName);
                QueryCriteria customCriteria1 = new QueryCriteria.VaultCustomQueryCriteria(valueIndex);
                QueryCriteria customCriteria2 = new QueryCriteria.VaultCustomQueryCriteria(stockNameIndex);
                QueryCriteria criteria = queryCriteria.and(customCriteria1).and(customCriteria2);
                results = getServiceHub().getVaultService().queryBy(TradeState.class, criteria);
                //undo sort results
                if (results == null || results.getStates().isEmpty()) {
                    throw new IllegalArgumentException("There are no matching counter parties!");
                }
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException(e.getMessage());
            }

            progressTracker.setCurrentStep(QUERYING_THE_ORACLE);
            Double stockPriceFromOracle = subFlow(new QueryOracle.Initiator(oracle, stockName));
            if (this.amount != stockPriceFromOracle) {
                throw new IllegalArgumentException("The preset value doesn't match the stock value");
            }

            progressTracker.setCurrentStep(BUILDING_THE_TX);

            List<StateAndRef<TradeState>> inputTradeStateList;
            // Generate a transaction by taking the current state
            if (tradeType) {
                inputTradeStateList = getServiceHub().getVaultService().queryBy(TradeState.class).getStates().stream()
                        .filter(x -> !x.getState().getData().getInitiatingParty().equals(initiatingParty))
                        .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Pending"))
                        .filter(x -> x.getState().getData().getStockName().equals(this.stockName))
                        .filter(x -> x.getState().getData().getBuyQuantity() > 0)
                        .filter(x -> x.getState().getData().getBuyValue() > 0)
                        .filter(x -> x.getState().getData().getCounterParty() == null)
                        .collect(Collectors.toList());
            } else {
                inputTradeStateList = getServiceHub().getVaultService().queryBy(TradeState.class).getStates().stream()
                        .filter(x -> !x.getState().getData().getInitiatingParty().equals(initiatingParty))
                        .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Pending"))
                        .filter(x -> x.getState().getData().getStockName().equals(this.stockName))
                        .filter(x -> x.getState().getData().getSellQuantity() > 0)
                        .filter(x -> x.getState().getData().getSellValue() > 0)
                        .filter(x -> x.getState().getData().getCounterParty() == null)
                        .collect(Collectors.toList());
            }
            if (inputTradeStateList.isEmpty()) {
                throw new RuntimeException("Trade state with trade initiatingParty " + initiatingParty +
                        " and stockname" + this.stockName + " was not found in the vault.");
            }
            //generate one output
            TradeState outputState = results.getStates().get(0).getState().getData();
            TradeContract.Commands.OracleTrade command = new TradeContract.Commands.OracleTrade(new BigDecimal(String.valueOf(stockPriceFromOracle))
                    ,new BigDecimal(String.valueOf(amount)));

            List<PublicKey> requiredSigns = ImmutableList.of(oracle.getOwningKey(), initiatingParty.getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addCommand(command,requiredSigns)
                    .addInputState(inputTradeStateList.get(0))
                    .addOutputState(outputState);

            progressTracker.setCurrentStep(VERIFYING_THE_TX);
            txBuilder.verify(getServiceHub());

            progressTracker.setCurrentStep(WE_SIGN);
            SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder);

            progressTracker.setCurrentStep(ORACLE_SIGNS);
            FilteredTransaction ftx = ptx.buildFilteredTransaction(o -> {
                if (o instanceof Command && ((Command) o).getSigners().contains(oracle.getOwningKey())
                        && ((Command) o).getValue() instanceof TradeContract.Commands.OracleTrade) {
                    return true;
                }
                return false;
            });

            TransactionSignature oracleSignature = subFlow(new SignOracle.Initiator(oracle, ftx));
            SignedTransaction stx = ptx.withAdditionalSignature(oracleSignature);

            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx, Collections.emptyList()));
        }


    }
}
