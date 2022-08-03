package net.corda.samples.trading.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import com.r3.corda.lib.tokens.contracts.commands.EvolvableTokenTypeCommand;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.trading.states.FungibleStockState;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class FungibleStockContract extends EvolvableTokenContract implements Contract {

    public static String ID = "net.corda.samples.trading.contracts.FungibleStockContract";


    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        FungibleStockState outputState = (FungibleStockState) tx.getOutput(0);
        if (!(tx.getCommand(0).getSigners().contains(outputState.getIssuer().getOwningKey())))
            throw new IllegalArgumentException("Issuer Signature Required");
        CommandWithParties<EvolvableTokenTypeCommand> command = requireSingleCommand(tx.getCommands(), EvolvableTokenTypeCommand.class);
        if (command.getValue() instanceof com.r3.corda.lib.tokens.contracts.commands.Create) {
            additionalCreateChecks(tx);
            return;
        } else if (command.getValue() instanceof com.r3.corda.lib.tokens.contracts.commands.Update) {
            additionalUpdateChecks(tx);
            return;
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {
        // Number of outputs is guaranteed as 1
        FungibleStockState createdStockState = tx.outputsOfType(FungibleStockState.class).get(0);

        requireThat(req -> {
            // Validations when creating a new stock
            req.using("Stock name must not be empty", (!createdStockState.getName().isEmpty()));
            req.using("Stock issuer must not be empty", (createdStockState.getIssuer() != null));
            return null;
        });
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        // Number of inputs and outputs are guaranteed as 1
        FungibleStockState input = tx.inputsOfType(FungibleStockState.class).get(0);
        FungibleStockState output = tx.outputsOfType(FungibleStockState.class).get(0);

        requireThat(req -> {
            // Validations when a stock is updated
            req.using("Stock Name must not be changed.", input.getName().equals(output.getName()));
            req.using("Stock Issuer must not be changed.", input.getIssuer().equals(output.getIssuer()));
            req.using("Stock FractionDigits must not be changed.", input.getFractionDigits() == output.getFractionDigits());
            return null;
        });
    }

}