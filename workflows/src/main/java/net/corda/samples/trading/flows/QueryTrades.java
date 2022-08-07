package net.corda.samples.trading.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.trading.states.TradeState;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.MAX_PAGE_SIZE;

public class QueryTrades {

    @InitiatingFlow
    @StartableByRPC
    public static class GetTradeCount extends FlowLogic<String> {
        private final ProgressTracker progressTracker = new ProgressTracker();

        public GetTradeCount() {
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {

            PageSpecification pageSpec = new PageSpecification(DEFAULT_PAGE_NUM, MAX_PAGE_SIZE);
            long numOfPending = getServiceHub().getVaultService().queryBy(TradeState.class, pageSpec).getStates().stream()
                    .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Pending")).count();
            long numOfExpired = getServiceHub().getVaultService().queryBy(TradeState.class, pageSpec).getStates().stream()
                    .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Expired")).count();
            long numOfAccepted = getServiceHub().getVaultService().queryBy(TradeState.class, pageSpec).getStates().stream()
                    .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Accepted")).count();
            long numOfCancelled = getServiceHub().getVaultService().queryBy(TradeState.class, pageSpec).getStates().stream()
                    .filter(x -> x.getState().getData().getTradeStatus().equalsIgnoreCase("Cancelled")).count();
            long numOfAll = getServiceHub().getVaultService().queryBy(TradeState.class, pageSpec).getStates().size();

            String result = "QueryTrades_results: \nTotal: " + numOfAll + "\nAccepted: " + numOfAccepted
                    + "\nPending: " + numOfPending + "\nExpired: " + numOfExpired + "\nCancelled: " + numOfCancelled;
            System.out.println(result);
            return result;
        }
    }
}
