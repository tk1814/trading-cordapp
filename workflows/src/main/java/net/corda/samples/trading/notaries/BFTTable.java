package net.corda.samples.trading.notaries;

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;

public class BFTTable extends TreeMap<String, HashMap<String, byte[]>> implements Serializable {
}
