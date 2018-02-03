import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;




public class TradeWayThread extends Thread{
    private static Logger log = LoggerFactory.getLogger(SimpleBinance.class.getName());
    private static BinanceApiClientFactory factory;
    private static BinanceApiRestClient client;
    private String currency;
    TradeWayThread(String currency) {
        factory = BinanceApiClientFactory.newInstance("W4E3BQ3xpxvEB3fFV4lFUUU4DEQm437gJpJVwbdeDLI02w4UECY6yh46gC5YU5E3", "UzpoApvzdz0C3ZrWWUTZHzqZ1e22j3WD4YnjA3kVCv1ILjEAG8ggmUTvY2YY8FCd");
        client = factory.newRestClient();
        this.currency = currency;
    }





    @Override
    public void run() {

        while (true) {
            OrderBook currEthBook = client.getOrderBook(currency + "ETH", 5);
            OrderBook currBnbBook = client.getOrderBook(currency + "BNB", 5);
            OrderBook bnbEthBook = client.getOrderBook("BNBETH", 5);

            List<OrderBookEntry> currEthAsks = currEthBook.getAsks();
            List<OrderBookEntry> currBnbBids = currBnbBook.getBids();
            List<OrderBookEntry> bnbEthBids = bnbEthBook.getBids();

            OrderBookEntry byeCurrEth = currEthAsks.get(0);
            OrderBookEntry sellCurrBnb = currBnbBids.get(0);
            OrderBookEntry sellBnbEth = bnbEthBids.get(0);

            //Пусть эфира 0.01
            double  ETH = 0.02;


            int currAmount = (int)Math.floor(ETH / Double.parseDouble(byeCurrEth.getPrice()));
            ETH = ETH - currAmount * Double.parseDouble(byeCurrEth.getPrice()) - currAmount * Double.parseDouble(byeCurrEth.getPrice())*0.0005;

            double bnbAmount = currAmount * Double.parseDouble(sellCurrBnb.getPrice()) * 0.9995;

            double ethAmount = bnbAmount * Double.parseDouble(sellBnbEth.getPrice()) * 0.9995;

            ETH += ethAmount;

            double profit = ETH - 0.02;


            System.out.println("Currency: " + currency + ", Profit: " + profit + ", RUB: " + (profit*50000));

            if(profit > 0)
                log.info("Currency: " + currency + ", Profit: " + profit + ", Qty1: " + byeCurrEth.getQty() + ", Qty2: " + sellCurrBnb.getQty() + ", RUB: " + (profit*50000));

        }
    }
}