package net.corda.samples.trading.entity;

import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

@CordaSerializable
public class BuyOrderKey implements Comparable<BuyOrderKey>, Serializable {

    public final String tradeDate;
    public final double price;

    public String getTradeDate() {
        return tradeDate;
    }

    public double getPrice() {
        return price;
    }

    public BuyOrderKey(String tradeDate, double price) {
        this.tradeDate = tradeDate;
        this.price = price;
    }

    @Override
    public int compareTo(@NotNull BuyOrderKey o) {
        BigDecimal data1 = new BigDecimal(this.price);
        BigDecimal data2 = new BigDecimal(o.price);
        int result = data2.compareTo(data1);
        result = (result == 0) ? this.tradeDate.compareTo(o.tradeDate) : result;
        return result;
    }




}