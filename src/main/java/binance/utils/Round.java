package binance.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Round {
    public static double roundPrice(double value, String pair) {
        return new BigDecimal(value).setScale(Currencies.getPriceLimit(pair), RoundingMode.HALF_DOWN).doubleValue();
    }

    public static double roundAmount(double value, String pair) {
        return new BigDecimal(value).setScale(Currencies.getAmountLimit(pair), RoundingMode.FLOOR).doubleValue();
    }
}
