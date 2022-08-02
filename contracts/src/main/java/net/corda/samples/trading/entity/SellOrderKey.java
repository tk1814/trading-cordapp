package net.corda.samples.trading.entity;

import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@CordaSerializable
public class SellOrderKey implements Comparable<SellOrderKey>, Serializable {

    public final LocalDateTime tradeDate;
    public final double price;

    public LocalDateTime getTradeDate() {
        return tradeDate;
    }

    public double getPrice() {
        return price;
    }

    public SellOrderKey(LocalDateTime tradeDate, double price) {
        this.tradeDate = tradeDate;
        this.price = price;
    }

    @Override
    public int compareTo(@NotNull SellOrderKey o) {
        BigDecimal data1 = new BigDecimal(this.price);
        BigDecimal data2 = new BigDecimal(o.price);
        int result = data1.compareTo(data2);
        result = (result == 0) ? this.tradeDate.compareTo(o.tradeDate) : result;
        return result;
    }

}