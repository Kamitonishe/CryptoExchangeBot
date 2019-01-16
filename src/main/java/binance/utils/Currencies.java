package binance.utils;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;

import java.util.HashMap;
import java.util.Map;

public class Currencies {
    /*
     * pairs содержат в себе информацию об ограничениях бинанса для пар.
     * currencies содержат в себе информацию о максимальном стартовом количестве валюты для пути и рублевый эквивалент.
     */
    private static Map<String, Pair> pairs = new HashMap<String, Pair>();
    private static Map<String, Currency> currencies = new HashMap<String, Currency>();
    private static Map<String, Double> usd = new HashMap<String, Double>();

    private static Currencies instance = new Currencies();

    BinanceApiWebSocketClient client = BinanceApiClientFactory.newInstance().newWebSocketClient();


    private Currencies() {
        /*Pairs*/

        //base pairs
        pairs.put("BNBETH", new Pair(6, 2, 0.0101));
        pairs.put("ETHBTC", new Pair(6, 3, 0.00101));

        //DLT
        pairs.put("DLTETH", new Pair(8, 0, 0.0101));
        pairs.put("DLTBNB", new Pair(5, 2, 1.01));
        //KNC
        pairs.put("KNCETH", new Pair(7, 0, 0.0101));
        pairs.put("KNCBTC", new Pair(8, 0, 0.00101));
        //STEEM
        pairs.put("STEEMETH", new Pair(6, 2, 0.0101));
        pairs.put("STEEMBNB", new Pair(5, 2, 1.01));
        //CMT
        pairs.put("CMTETH", new Pair(8, 0, 0.0101));
        pairs.put("CMTBNB", new Pair(5, 2, 1.01));
        //IOTA
        pairs.put("IOTAETH", new Pair(8, 0, 0.0101));
        pairs.put("IOTABNB", new Pair(5, 2, 1.01));
        //XLM
        pairs.put("XLMETH", new Pair(8, 0, 0.0101));
        pairs.put("XLMBNB", new Pair(5, 2, 1.01));
        //NULS
        pairs.put("NULSETH", new Pair(8, 0, 0.0101));
        pairs.put("NULSBNB", new Pair(5, 2, 1.01));
        //WTC
        pairs.put("WTCETH", new Pair(6, 2, 0.0101));
        pairs.put("WTCBNB", new Pair(4, 2, 1.01));

        /*Currencies*/


        BinanceApiRestClient client = BinanceApiClientFactory.newInstance().newRestClient();

        currencies.put("BTC", new Currency(0.00147, Double.parseDouble(client.get24HrPriceStatistics("BTCUSDT").getLastPrice()) * 60));

        currencies.put("ETH", new Currency(0.0135, Double.parseDouble(client.get24HrPriceStatistics("ETHUSDT").getLastPrice()) * 60));
        currencies.put("BNB", new Currency(0.147, Double.parseDouble(client.get24HrPriceStatistics("BNBUSDT").getLastPrice()) * 60));
        currencies.put("DSH", new Currency(0.019, 36000));
        currencies.put("ETC", new Currency(0.47, 1500));
        currencies.put("LTC", new Currency(0.07, Double.parseDouble(client.get24HrPriceStatistics("LTCUSDT").getLastPrice()) * 60));

        new Updater().start();
    }


    public static int getPriceLimit(String pair) {
        return pairs.get(pair).getPriceLimit();
    }

    public static int getAmountLimit(String pair) {
        return pairs.get(pair).getAmountLimit();
    }

    public static double getMinTotal(String pair) {
        return pairs.get(pair).getMinTotal();
    }

    public static double getMaxAmount(String currency) {
        return currencies.get(currency).getMaxAmount();
    }

    public static double getRub(String currency) {
        return currencies.get(currency).getRub();
    }

    private class Pair {
        private int priceLimit;
        private int amountLimit;
        private double minTotal;

        Pair(int priceLimit, int amountLimit, double minTotal) {
            this.priceLimit = priceLimit;
            this.amountLimit = amountLimit;
            this.minTotal = minTotal;
        }

        int getPriceLimit() {
            return priceLimit;
        }

        int getAmountLimit() {
            return amountLimit;
        }

        double getMinTotal() {
            return minTotal;
        }
    }


    private class Currency {
        private double maxAmount;
        private double rub;

        public Currency(double maxAmount, double rub) {
            this.maxAmount = maxAmount;
            this.rub = rub;
        }

        double getMaxAmount() {
            return maxAmount;
        }

        double getRub() {
            return rub;
        }
    }


    private class Updater extends Thread {
        @Override
        public void run() {
            System.out.println("Currencies updater started");
            while (true) {
                try {
                    sleep(1000 * 60 * 15); //15 минут
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                BinanceApiRestClient client = BinanceApiClientFactory.newInstance().newRestClient();

                currencies.put("BTC", new Currency(0.0013, Double.parseDouble(client.get24HrPriceStatistics("BTCUSDT").getLastPrice()) * 60));
                currencies.put("ETH", new Currency(0.0155, Double.parseDouble(client.get24HrPriceStatistics("ETHUSDT").getLastPrice()) * 60));
                currencies.put("BNB", new Currency(0.14, Double.parseDouble(client.get24HrPriceStatistics("BNBUSDT").getLastPrice()) * 60));
                currencies.put("LTC", new Currency(0.07, Double.parseDouble(client.get24HrPriceStatistics("LTCUSDT").getLastPrice()) * 60));
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("BTC: " + Currencies.getRub("BTC"));
        System.out.println("ETH: " + Currencies.getRub("ETH"));
        System.out.println("BNB: " + Currencies.getRub("BNB"));
        System.exit(0);
    }
}
