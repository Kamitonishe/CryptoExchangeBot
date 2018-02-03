import com.binance.api.client.*;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.AllOrdersRequest;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.binance.api.client.domain.market.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SimpleBinance extends Thread {
    private static Logger log = LoggerFactory.getLogger(SimpleBinance.class.getName());
    private static BinanceApiClientFactory factory;
    private static BinanceApiRestClient client;

    SimpleBinance() {
        factory = BinanceApiClientFactory.newInstance("W4E3BQ3xpxvEB3fFV4lFUUU4DEQm437gJpJVwbdeDLI02w4UECY6yh46gC5YU5E3", "UzpoApvzdz0C3ZrWWUTZHzqZ1e22j3WD4YnjA3kVCv1ILjEAG8ggmUTvY2YY8FCd");
        client = factory.newRestClient();
    }




    public static void main(String[] args) {
        SimpleBinance binance = new SimpleBinance();
        binance.start();

    }


    @Override
    public void run() {

        while (true) {
            OrderBook rcnEthBook = client.getOrderBook("RCNETH", 5);
            OrderBook rcnBnbBook = client.getOrderBook("RCNBNB", 5);
            OrderBook bnbEthBook = client.getOrderBook("BNBETH", 5);

            List<OrderBookEntry> rcnEthAsks = rcnEthBook.getAsks();
            List<OrderBookEntry> rcnBnbBids = rcnBnbBook.getBids();
            List<OrderBookEntry> bnbEthBids = bnbEthBook.getBids();

            OrderBookEntry byeRcnEth = rcnEthAsks.get(0);
            OrderBookEntry sellRcnBnb = rcnBnbBids.get(0);
            OrderBookEntry sellBnbEth = bnbEthBids.get(0);

            //Пусть эфира 0.01
            double  ETH = 0.02;


            int rcnAmount = (int)Math.floor(ETH / Double.parseDouble(byeRcnEth.getPrice()));
            ETH = ETH - rcnAmount * Double.parseDouble(byeRcnEth.getPrice()) - rcnAmount * Double.parseDouble(byeRcnEth.getPrice())*0.0005;

            double bnbAmount = rcnAmount * Double.parseDouble(sellRcnBnb.getPrice()) * 0.9995;

            double ethAmount = bnbAmount * Double.parseDouble(sellBnbEth.getPrice()) * 0.9995;

            ETH += ethAmount;

            double profit = ETH - 0.02;
            System.out.println("Profit: " + profit + ", RUB: " + (profit*50000));
            if(profit > 0)
                log.info("Profit: " + profit + ", RUB: " + (profit*50000));

        }
    }
}
