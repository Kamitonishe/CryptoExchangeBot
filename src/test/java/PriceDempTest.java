import binance.utils.Currencies;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;

import java.util.List;
import java.util.Locale;

public class PriceDempTest {
    public static void main(String[] args) {
        BinanceApiClientFactory factory;
        BinanceApiRestClient client;

        factory = BinanceApiClientFactory.newInstance("W4E3BQ3xpxvEB3fFV4lFUUU4DEQm437gJpJVwbdeDLI02w4UECY6yh46gC5YU5E3", "UzpoApvzdz0C3ZrWWUTZHzqZ1e22j3WD4YnjA3kVCv1ILjEAG8ggmUTvY2YY8FCd");
        client = factory.newRestClient();


        OrderBook bnbEthBook = client.getOrderBook("CMTETH", 5);


        List<OrderBookEntry> bnbEthAsks = bnbEthBook.getAsks();
        System.out.println(bnbEthAsks);

        OrderBookEntry bestAsk = bnbEthAsks.get(0);

        System.out.println(bestAsk.getPrice());
        System.out.println(String.format(Locale.US, "%.15f",Double.parseDouble(bestAsk.getPrice()) - (1/Math.pow(10, Currencies.getPriceLimit("CMTETH")))));

    }
}
