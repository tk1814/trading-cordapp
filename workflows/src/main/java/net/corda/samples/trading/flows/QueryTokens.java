package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.trading.states.FungibleStockState;

import java.util.*;
import java.util.stream.Collectors;

public class QueryTokens {

    @InitiatingFlow
    @StartableByRPC
    public static class GetTokenBalance extends FlowLogic<List<String>> {
        private final ProgressTracker progressTracker = new ProgressTracker();

        public GetTokenBalance() {
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Override
        @Suspendable
        public List<String> call() throws FlowException {

            // return all the issued stocks
            Set<FungibleStockState> evolvableTokenTypeSet = getServiceHub().getVaultService().
                    queryBy(FungibleStockState.class).getStates().stream()
                    .map(StateAndRef::getState)
                    .map(TransactionState::getData).collect(Collectors.toSet());
            if (evolvableTokenTypeSet.isEmpty()) {
                return new ArrayList<>();
            }

            // Save the result
            String result = "";
            List<String> stockAmountsAndNames = new ArrayList<>();

            // The set will have multiple elements, because we retrieve all
            for (FungibleStockState evolvableTokenType : evolvableTokenTypeSet) {
                // get the pointer to the stock state
                TokenPointer<FungibleStockState> tokenPointer = evolvableTokenType.toPointer(FungibleStockState.class);

                // query balance or each different Token
                Amount<TokenType> amount = QueryUtilities.tokenBalance(getServiceHub().getVaultService(), tokenPointer);

                result += "\nYou currently have " + amount.getQuantity() + " " + evolvableTokenType.getName() + " Tokens issued by "
                        + evolvableTokenType.getIssuer().getName().getOrganisation() + "\n";
                stockAmountsAndNames.add(amount.getQuantity() + "=" + evolvableTokenType.getName());
            }
            // System.out.println(result);
            return stockAmountsAndNames;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class GetFiatBalance extends FlowLogic<String> {
        private final ProgressTracker progressTracker = new ProgressTracker();
        private final String currencyCode;

        public GetFiatBalance(String currencyCode) {
            this.currencyCode = currencyCode;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            TokenType fiatTokenType = FiatCurrency.Companion.getInstance(currencyCode);
            Amount<TokenType> amount = QueryUtilities.tokenBalance(getServiceHub().getVaultService(), fiatTokenType);
            return amount.getQuantity() / 100.0 + " " + amount.getToken().getTokenIdentifier();
        }
    }


}
