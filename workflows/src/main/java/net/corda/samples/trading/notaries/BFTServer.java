//package net.corda.samples.trading.notaries;
//
//
//import bftsmart.tom.MessageContext;
//import bftsmart.tom.server.defaultservices.DefaultRecoverable;
//import net.corda.samples.trading.BFTTable;
//
//import java.io.*;
//import java.util.TreeMap;
//
//public class BFTServer extends DefaultRecoverable {
//
//    private static final boolean _debug = false;
//    private boolean logPrinted = false;
//    private TreeMap<String, BFTTable> mTables;
//
//    @Override
//    public void installSnapshot(byte[] state) {
//        try {
//            ByteArrayInputStream bis = new ByteArrayInputStream(state);
//            ObjectInput in = new ObjectInputStream(bis);
//            mTables = (TreeMap<String, BFTTable>) in.readObject();
//            in.close();
//            bis.close();
//        } catch (IOException | ClassNotFoundException e) {
//            System.err.println("[ERROR] Error deserializing state: "
//                    + e.getMessage());
//        }
//    }
//
//    @Override
//    public byte[] getSnapshot() {
//        try {
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            ObjectOutput out = new ObjectOutputStream(bos);
//            out.writeObject(mTables);
//            out.flush();
//            bos.flush();
//            out.close();
//            bos.close();
//            return bos.toByteArray();
//        } catch (IOException ioe) {
//            System.err.println("[ERROR] Error serializing state: "
//                    + ioe.getMessage());
//            return "ERROR".getBytes();
//        }
//    }
//
//
//
//    @Override
//    public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtx) {
//        byte[][] replies = new byte[commands.length][];
//        int index = 0;
//        for (byte[] command : commands) {
//            if (msgCtx != null && msgCtx[index] != null && msgCtx[index].getConsensusId() % 1000 == 0 && !logPrinted) {
//                System.out.println("YCSBServer executing CID: " + msgCtx[index].getConsensusId());
//                logPrinted = true;
//            } else {
//                logPrinted = false;
//            }
//
//            TradingMessage aRequest = TradingMessage.getObject(command);
//            TradingMessage reply = TradingMessage.newErrorMessage("");
//            if (aRequest == null) {
//                replies[index] = reply.getBytes();
//                continue;
//            }
//            if (_debug) {
//                System.out.println("[INFO] Processing an ordered request");
//            }
//            switch (aRequest.getType()) {
//                case CREATE: { // ##### operation: create #####
//                    switch (aRequest.getEntity()) {
//                        case RECORD: // ##### entity: record #####
//                            if (!mTables.containsKey(aRequest.getTable())) {
//                                mTables.put((String) aRequest.getTable(), new BFTTable());
//                            }
//                            if (!mTables.get(aRequest.getTable()).containsKey(aRequest.getKey())) {
//                                mTables.get(aRequest.getTable()).put(aRequest.getKey(), aRequest.getValues());
//                                reply = TradingMessage.newInsertResponse(0);
//                            }
//                            break;
//                        default: // Only create records
//                            break;
//                    }
//                    break;
//                }
//
//                case UPDATE: { // ##### operation: update #####
//                    switch (aRequest.getEntity()) {
//                        case RECORD: // ##### entity: record #####
//                            if (!mTables.containsKey(aRequest.getTable())) {
//                                mTables.put((String) aRequest.getTable(), new BFTTable());
//                            }
//                            mTables.get(aRequest.getTable()).put(aRequest.getKey(), aRequest.getValues());
//                            reply = TradingMessage.newUpdateResponse(1);
//                            break;
//                        default: // Only update records
//                            break;
//                    }
//                    break;
//                }
//            }
//            if (_debug) {
//                System.out.println("[INFO] Sending reply");
//            }
//            replies[index++] = reply.getBytes();
//        }
////		System.out.println("RETURNING REPLY");
//        return replies;
//    }
//
//    @Override
//    public byte[] appExecuteUnordered(byte[] theCommand, MessageContext theContext) {
//        TradingMessage aRequest = TradingMessage.getObject(theCommand);
//        TradingMessage reply = TradingMessage.newErrorMessage("");
//        if (aRequest == null) {
//            return reply.getBytes();
//        }
//        if (_debug) {
//            System.out.println("[INFO] Processing an unordered request");
//        }
//
//        switch (aRequest.getType()) {
//            case READ: { // ##### operation: read #####
//                switch (aRequest.getEntity()) {
//                    case RECORD: // ##### entity: record #####
//                        if (!mTables.containsKey(aRequest.getTable())) {
//                            reply = TradingMessage.newErrorMessage("Table not found");
//                            break;
//                        }
//                        if (!mTables.get(aRequest.getTable()).containsKey(aRequest.getKey())) {
//                            reply = TradingMessage.newErrorMessage("Record not found");
//                            break;
//                        } else {
//                            reply = TradingMessage.newReadResponse(mTables.get(aRequest.getTable()).get(aRequest.getKey()), 0);
//                            break;
//                        }
//                }
//            }
//        }
//        if (_debug) {
//            System.out.println("[INFO] Sending reply");
//        }
//        return reply.getBytes();
//    }
//}
