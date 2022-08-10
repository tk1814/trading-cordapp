package net.corda.samples.trading.entity;

import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@CordaSerializable
public class BuyOrderKey implements Comparable<BuyOrderKey>, Serializable {

    public final LocalDateTime tradeDate;
    public final double price;
    public final BigDecimal fee;

    public LocalDateTime getTradeDate() {
        return tradeDate;
    }

    public double getPrice() {
        return price;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public BuyOrderKey(LocalDateTime tradeDate, double price, BigDecimal fee) {
        this.tradeDate = tradeDate;
        this.price = price;
        this.fee = fee;
    }

    @Override
    public int compareTo(@NotNull BuyOrderKey o) {
        int finalResult = 0;
        BigDecimal data1 = new BigDecimal(this.price);
        BigDecimal data2 = new BigDecimal(o.price);
        int result = data2.compareTo(data1);
        if (result == 0) {
            BigDecimal fee2 = o.fee;
            int feeResult = fee2.compareTo(this.fee);
            finalResult = (feeResult == 0) ? this.tradeDate.compareTo(o.tradeDate) : feeResult;
        }else {
            finalResult = result;
        }
        return finalResult;

    }


}