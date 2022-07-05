package net.corda.samples.trading.notaries;



import java.io.*;
import java.util.HashMap;
import java.util.Set;

public class TradingMessage implements Serializable {
    private static final long serialVersionUID = 6198684730704708506L;

    public enum Type {
        CREATE, READ, SCAN, UPDATE, DELETE, SIZE, ERROR, CHECK
    };

    public enum Entity {
        TABLE, RECORD, FIELD
    };

    private Type type;
    private Entity entity;
    private String table;
    private String key;
    private Set<String> fields;
    private HashMap<String, byte[]> values;
    private int result = -1;
    private HashMap<String, byte[]> results;
    private String errorMsg;

    private TradingMessage() {
        super();
        result = -1;
    }

    public static TradingMessage newInsertRequest(String table, String key, HashMap<String, byte[]> values) {
        TradingMessage message = new TradingMessage();
        message.type = Type.CREATE;
        message.entity = Entity.RECORD;
        message.table = table;
        message.key = key;
        message.values = values;
        return message;
    }

    public static TradingMessage newUpdateRequest(String table, String key, HashMap<String, byte[]> values) {
        TradingMessage message = new TradingMessage();
        message.type = Type.UPDATE;
        message.entity = Entity.RECORD;
        message.table = table;
        message.key = key;
        message.values = values;
        return message;
    }

    public static TradingMessage newReadRequest(String table, String key, Set<String> fields, HashMap<String, byte[]> results) {
        TradingMessage message = new TradingMessage();
        message.type = Type.READ;
        message.entity = Entity.RECORD;
        message.table = table;
        message.key = key;
        message.fields = fields;
        message.results = results;
        return message;
    }


    public static TradingMessage newInsertResponse(int result) {
        TradingMessage message = new TradingMessage();
        message.result = result;
        return message;
    }

    public static TradingMessage newUpdateResponse(int result) {
        TradingMessage message = new TradingMessage();
        message.result = result;
        return message;
    }

    public static TradingMessage newReadResponse(HashMap<String, byte[]> results, int result) {
        TradingMessage message = new TradingMessage();
        message.result = result;
        message.results = results;
        return message;
    }

    public static TradingMessage newErrorMessage(String errorMsg) {
        TradingMessage message = new TradingMessage();
        message.errorMsg = errorMsg;
        return message;
    }

    public byte[] getBytes() {
        try {
            byte[] aArray = null;
            ByteArrayOutputStream aBaos = new ByteArrayOutputStream();
            ObjectOutputStream aOos = new ObjectOutputStream(aBaos);
            aOos.writeObject(this);
            aOos.flush();
            aBaos.flush();
            aArray = aBaos.toByteArray();
            aOos.close();
            aBaos.close();
            return aArray;
        } catch (IOException ex) {
            return null;
        }
    }

    public static TradingMessage getObject(byte[] theBytes) {
        try {
            ByteArrayInputStream aBais = new ByteArrayInputStream(theBytes);
            ObjectInputStream aOis = new ObjectInputStream(aBais);
            TradingMessage aMessage = (TradingMessage) aOis.readObject();
            aOis.close();
            aBais.close();
            return aMessage;
        } catch (ClassNotFoundException | IOException ex) {
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(type).append(",").append(entity).append(",");
        sb.append(table).append(",").append(key).append(",").append(values).append(")");
        return sb.toString();
    }

    public int getResult() {
        return result;
    }

    public HashMap<String, byte[]> getResults() {
        return results;
    }

    public Type getType() {
        return type;
    }

    public Entity getEntity() {
        return entity;
    }

    public String getTable() {
        return table;
    }

    public String getKey() {
        return key;
    }

    public Set<String> getFields() {
        return fields;
    }

    public HashMap<String, byte[]> getValues() {
        return values;
    }

    public String getErrorMsg() {
        return errorMsg;
    }


}
