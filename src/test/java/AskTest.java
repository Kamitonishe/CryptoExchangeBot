import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AskTest {
    public static void main(String[] args) {
        BinanceApiClientFactory factory;
        BinanceApiRestClient client;

        factory = BinanceApiClientFactory.newInstance("W4E3BQ3xpxvEB3fFV4lFUUU4DEQm437gJpJVwbdeDLI02w4UECY6yh46gC5YU5E3", "UzpoApvzdz0C3ZrWWUTZHzqZ1e22j3WD4YnjA3kVCv1ILjEAG8ggmUTvY2YY8FCd");
        client = factory.newRestClient();


        OrderBook bnbEthBook = client.getOrderBook("BNBETH", 5);


        List<OrderBookEntry> bnbEthAsks = bnbEthBook.getAsks();
        System.out.println(bnbEthAsks);

        // Asks - красные.

    }
}
