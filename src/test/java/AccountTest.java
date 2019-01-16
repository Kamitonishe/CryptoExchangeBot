import binance.utils.Currencies;

import java.util.Locale;

public class AccountTest {
    public static void main(String[] args) {
        String strPrice = String.format(Locale.US, "%." + Currencies.getPriceLimit("CMTETH") + "f", 0.00018601);
        System.out.println(strPrice);

    }



}
