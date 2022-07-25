package net.corda.samples.trading.contracts;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.trading.states.TradeQueueState;
import org.bouncycastle.crypto.Signer;
import org.jetbrains.annotations.NotNull;

import javax.sql.CommonDataSource;
import java.security.PublicKey;
import java.util.List;

public class TradeQueueContract implements Contract {

    public static String ID = "net.corda.samples.trading.contracts.TradeQueueContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        if (tx.getCommands().size() == 0) {
            throw new IllegalArgumentException("One command Expected");
        }

        Command command = tx.getCommand(0);
        if (command.getValue() instanceof Commands.createQueue) {
            verifyCreateQueue(tx);
        } else if (command.getValue() instanceof Commands.insertTrade2Queue) {
            insertTrade2Queue(tx);
        } else {
            throw new IllegalArgumentException("unrecognized command");
        }

    }

    private void verifyCreateQueue(LedgerTransaction tx) {
        Command command = tx.getCommand(0);
        //shape
        if (tx.getOutputStates().size() != 1) {
            throw new IllegalArgumentException("One Output Expected");
        }
        //content

        //signer
        List<PublicKey> requiredSigners = command.getSigners();
        TradeQueueState tradeQueueState = (TradeQueueState) tx.getOutput(0);
        PublicKey creatorKey = tradeQueueState.getCreateParty().getOwningKey();
        if (!requiredSigners.contains(creatorKey)) {
            throw new IllegalArgumentException("Creator must be required signer");
        }

    }

    private void insertTrade2Queue(LedgerTransaction tx) {
        Command command = tx.getCommand(0);
        //shape
        if (tx.getInputStates().size() != 1) {
            throw new IllegalArgumentException("One Input Expected");
        }
        if (tx.getOutputStates().size() != 1) {
            throw new IllegalArgumentException("One Output Expected");
        }
        //content

        //signer
        List<PublicKey> requiredSigners = command.getSigners();
        TradeQueueState tradeQueueState = (TradeQueueState) tx.getOutput(0);
        PublicKey creatorKey = tradeQueueState.getCreateParty().getOwningKey();
        if (!requiredSigners.contains(creatorKey)) {
            throw new IllegalArgumentException("Create trade party must be required signer");
        }
    }

    public interface Commands extends CommandData {
        class createQueue implements Commands {
        }

        class insertTrade2Queue implements Commands {
        }

        class deleteTradeFromQueue implements Commands {
        }
    }
}
