package binance.utils;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.AggTrade;

import java.util.List;

public class Commission {

    private static BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(Account.getApiKey(), Account.getSecret());
    private static BinanceApiRestClient client = factory.newRestClient();
    private static Updater updater = new Updater();

    public static double getCommission(String curr, double value) {
        if (curr.equals("BNB"))
            return value * 0.0005;
        //Exchange ratio of NEO/BNB = NEO/BTC[market price] /(BNB/BTC [market price])
        //BNB = NEO/price
        List<AggTrade> aggTrades = client.getAggTrades(curr + "BTC");
        AggTrade lastTrade = aggTrades.get(0);
        double currBTCPrice;
        currBTCPrice = Double.parseDouble(lastTrade.getPrice());

        aggTrades = client.getAggTrades("BNBBTC");
        lastTrade = aggTrades.get(0);
        double BNBBTCPrice = Double.parseDouble(lastTrade.getPrice());

        double price = currBTCPrice / BNBBTCPrice;

        return value * price * 0.0005;
    }


    private static class Updater extends Thread {

        Updater() {
            this.start();
        }

        @Override
        public void run() {
            System.out.println("Commission updater started");
            while (true) {
                try {
                    sleep(1000 * 60 * 60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                client = factory.newRestClient();
            }
        }
    }

    public static double getCommission(String curr, int value) {
        return getCommission(curr, (double) value);
    }
}
