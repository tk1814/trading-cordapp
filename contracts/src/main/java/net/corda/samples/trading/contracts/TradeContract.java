package net.corda.samples.trading.contracts;

import net.corda.core.contracts.*;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

import net.corda.samples.trading.states.TradeState;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * This contract enforces rules regarding the creation of a valid [TradeState].
 * <p>
 * For a new [Trade] to be created onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [Trade].
 * - An Create() command with the public keys of both the party.
 * <p>
 * For a counter [Trade] to be created onto the ledger, a transaction is required which takes:
 * - One input states: the old [Trade].
 * - One output state: the new [Trade].
 * - An CounterTrade() command with the public keys of both the lender and the borrower.
 * <p>
 * All contracts must sub-class the [Contract] interface.
 */

public class TradeContract implements Contract {

    public static String ID = "net.corda.samples.trading.contracts.TradeContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();
        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if (commandType instanceof TradeContract.Commands.Create) {
            requireThat(require -> {
                require.using("Transaction must have one command", tx.getCommands().size() == 1);

                // Generic constraints around the Trade transaction.
                // require.using("No inputs should be consumed when issuing a new Trade.", inputs.isEmpty());
                require.using("Transaction must have no input.", inputs.size() == 0);
                require.using("Transaction must have exactly one output.", outputs.size() == 1);
                require.using("Output must be a TradeState.", outputs.get(0) instanceof TradeState);

                // Retrieve the output state of the transaction
                TradeState output = (TradeState) outputs.get(0);

                require.using("The creating party and the counter party cannot be the same entity.", output.initiatingParty != output.counterParty);
                // require.using("All the participants must be signers.", command.signers.containsAll(out.participants.map { it.owningKey }));

                // Trade-specific constraints.
                require.using("InitiatingParty must sign Trade.", requiredSigners.contains(output.getInitiatingParty().getOwningKey()));
                require.using("Transaction must have no counterparty.", output.counterParty == null);
                require.using("The Trade's order type must be a pending or market order.", output.orderType.equals("Pending Order") || output.orderType.equals("Market Order"));
                require.using("The Trade's type must be a buy or sell operation.", output.tradeType.equals("Sell") || output.tradeType.equals("Buy"));
                require.using("The Trade must have a stock name.", !output.stockName.isEmpty());
                require.using("The Trade's stock price must be non-negative.", output.stockPrice > 0);
                require.using("The Trade's stock quantity must be positive.", output.stockQuantity > 0);
                require.using("Transaction must have an expiration date.", output.expirationDate != null);
                require.using("The Trade's status must be Pending.", output.tradeStatus.equals("Pending"));
                require.using("Transaction must have a trade date.", output.tradeDate != null);
                require.using("Transaction must have no settlement date.", output.settlementDate == null);
                return null;
            });

        } else if (commandType instanceof TradeContract.Commands.CounterTrade) {
            requireThat(require -> {
                // Generic constraints around the Trade transaction.
                //require.using("Only one output state should be created.", outputs.size() == 1);
                TradeState output = (TradeState) outputs.get(0);
                require.using("The creating party and the counter party cannot be the same entity.", output.initiatingParty != output.counterParty);

                // Trade-specific constraints.
                require.using("The Trade's stock price must be non-negative.", output.stockPrice > 0);
                require.using("The Trade's stock quantity must be positive.", output.stockQuantity > 0);
                require.using("InitiatingParty must sign Trade.", requiredSigners.contains(output.getInitiatingParty().getOwningKey()));
                require.using("CounterParty must not be empty.", output.counterParty != null);
                require.using("CounterParty must sign Trade.", requiredSigners.contains(output.getCounterParty().getOwningKey()));
                require.using("The Trade's status must be accepted.", output.tradeStatus.equals("Accepted"));
                require.using("Transaction must have a settlement date.", output.settlementDate != null);
                return null;
            });

        } else if (commandType instanceof TradeContract.Commands.CancelTrade) {
            requireThat(require -> {
                // Generic constraints around the Trade transaction.
                require.using("Only one output state should be created.", outputs.size() == 1);
                TradeState output = (TradeState) outputs.get(0);

                require.using("The counter party should be null.", output.counterParty == null);
                require.using("Transaction must have no settlement date.", output.settlementDate == null);

                // Trade-specific constraints.
                require.using("The Trade's stock price must be non-negative.", output.stockPrice > 0);
                require.using("The Trade's stock quantity must be positive.", output.stockQuantity > 0);
                require.using("InitiatingParty must sign Trade.", requiredSigners.contains(output.getInitiatingParty().getOwningKey()));
                require.using("The Trade's status must be cancelled or expired.", output.tradeStatus.equals("Cancelled") || output.tradeStatus.equals("Expired"));
                return null;
            });

        } else {
            throw new IllegalArgumentException("CommandType not recognized");
        }

    }

    /**
     * This contract implements command: Create and CounterTrade.
     */
    public interface Commands extends CommandData {
        class Create implements Commands {
        }

        class CounterTrade implements Commands {
        }

        class CancelTrade implements Commands {
        }
    }
}
