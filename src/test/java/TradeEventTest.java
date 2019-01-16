import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;

public class TradeEventTest {
    public static void main(String[] args) {
        BinanceApiWebSocketClient client = BinanceApiClientFactory.newInstance().newWebSocketClient();

        // Listen for aggregated trade events for ETH/BTC
        client.onAggTradeEvent("btcusdt", response -> {
            System.out.println(response.getPrice());
            // System.out.println(response.getQuantity());

        });
    }
}
