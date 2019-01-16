package binance.utils;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.exception.BinanceApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.limitSell;
import static com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType.ACCOUNT_UPDATE;

public class Account {
    private static BinanceApiClientFactory clientFactory;
    private static BinanceApiRestClient restClient;

    private static Account instance = new Account();

    private static Updater updater = new Updater();

    private static Logger accountLog = LoggerFactory.getLogger("account");
    private static Logger orderLog = LoggerFactory.getLogger("order");

    private static final String apiKey = "W4E3BQ3xpxvEB3fFV4lFUUU4DEQm437gJpJVwbdeDLI02w4UECY6yh46gC5YU5E3";
    private static final String apiSecret = "UzpoApvzdz0C3ZrWWUTZHzqZ1e22j3WD4YnjA3kVCv1ILjEAG8ggmUTvY2YY8FCd";
    /**
     * Key is the symbol, and the value is the balance of that symbol on the account.
     */
    private static Map<String, AssetBalance> accountBalanceCache;

    /**
     * Listen key used to interact with the user data streaming API.
     */
    private final String listenKey;

    private Account() {
        clientFactory = BinanceApiClientFactory.newInstance(apiKey, apiSecret);
        restClient = clientFactory.newRestClient();
        this.listenKey = initializeAssetBalanceCacheAndStreamSession();
        startAccountBalanceEventStreaming(listenKey);
        instance = this;
    }

    /**
     * Initializes the asset balance cache by using the REST API and starts a new user data streaming session.
     *
     * @return a listenKey that can be used with the user data streaming API.
     */
    private String initializeAssetBalanceCacheAndStreamSession() {
        BinanceApiRestClient client = clientFactory.newRestClient();
        com.binance.api.client.domain.account.Account account = client.getAccount();

        accountBalanceCache = new TreeMap<>();
        for (AssetBalance assetBalance : account.getBalances()) {
            accountBalanceCache.put(assetBalance.getAsset(), assetBalance);
        }

        return client.startUserDataStream();
    }

    /**
     * Begins streaming of agg trades events.
     */
    private void startAccountBalanceEventStreaming(String listenKey) {
        BinanceApiWebSocketClient client = clientFactory.newWebSocketClient();

        client.onUserDataUpdateEvent(listenKey, response -> {
            if (response.getEventType() == ACCOUNT_UPDATE) {
                // Override cached asset balances
                for (AssetBalance assetBalance : response.getAccountUpdateEvent().getBalances()) {
                    accountBalanceCache.put(assetBalance.getAsset(), assetBalance);
                }
                accountLog.info(accountBalanceCache.toString());
                //accountLog.info("ETH: " + accountBalanceCache.get("ETH").toString());
                //accountLog.info("BNB: " + accountBalanceCache.get("BNB").toString());
                //accountLog.info("BTC: " + accountBalanceCache.get("BTC").toString());
            }
        });
    }

    /**
     * @return an account balance cache, containing the balance for every asset in this account.
     */
    public Map<String, AssetBalance> getAccountBalanceCache() {
        return accountBalanceCache;
    }


    public static void newBuyOrder(String symbol, double quantity, double price, OrderListener listener) {
        String strQuantity = String.format(Locale.US, "%." + Currencies.getAmountLimit(symbol) + "f", quantity);
        String strPrice = String.format(Locale.US, "%." + Currencies.getPriceLimit(symbol) + "f", price);

        NewOrderResponse response = restClient.newOrder(limitBuy(symbol, TimeInForce.GTC, strQuantity, strPrice));
        orderLog.info(response.toString());
        listener.addOrder(response);
    }

    public static void newSellOrder(String symbol, double quantity, double price, OrderListener listener) {
        String strQuantity = String.format(Locale.US, "%." + Currencies.getAmountLimit(symbol) + "f", quantity);
        String strPrice = String.format(Locale.US, "%." + Currencies.getPriceLimit(symbol) + "f", price);

        NewOrderResponse response = restClient.newOrder(limitSell(symbol, TimeInForce.GTC, strQuantity, strPrice));
        orderLog.info(response.toString());
        listener.addOrder(response);
    }

    public static boolean isOrderFilled(NewOrderResponse response) {
        //Order order = orderClient.getOrderStatus(new OrderStatusRequest("LINKETH", 751698L));
        Order order = restClient.getOrderStatus(new OrderStatusRequest(response.getSymbol(), response.getOrderId()));
        return (order.getStatus() == OrderStatus.FILLED);
    }

    public static boolean isOrderPartiallyFilled(NewOrderResponse response) {
        //Order order = orderClient.getOrderStatus(new OrderStatusRequest("LINKETH", 751698L));
        Order order = restClient.getOrderStatus(new OrderStatusRequest(response.getSymbol(), response.getOrderId()));
        return (order.getStatus() == OrderStatus.PARTIALLY_FILLED);
    }

    public static boolean isOrderPartiallyFilledOrFilled(NewOrderResponse response) {
        //Order order = orderClient.getOrderStatus(new OrderStatusRequest("LINKETH", 751698L));
        Order order = restClient.getOrderStatus(new OrderStatusRequest(response.getSymbol(), response.getOrderId()));
        return (order.getStatus() == OrderStatus.PARTIALLY_FILLED || order.getStatus() == OrderStatus.FILLED);
    }

    public static boolean isOrderCanceled(NewOrderResponse response) {
        //Order order = orderClient.getOrderStatus(new OrderStatusRequest("LINKETH", 751698L));
        Order order = restClient.getOrderStatus(new OrderStatusRequest(response.getSymbol(), response.getOrderId()));
        return (order.getStatus() == OrderStatus.CANCELED);
    }

    public static boolean cancelOrder(NewOrderResponse response) {
        try {
            restClient.cancelOrder(new CancelOrderRequest(response.getSymbol(), response.getOrderId()));
            return true;
        } catch (BinanceApiException e) {
            System.out.println(e.getError().getMsg());
            return false;
        }
    }

    public static double getAvailableBalance(String currency) {
        AssetBalance ab = instance.getAccountBalanceCache().get(currency);
        return Double.parseDouble(ab.getFree());
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static String getSecret() {
        return apiSecret;
    }

    private static class Updater extends Thread {
        Updater() {
            this.start();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    sleep(1000 * 60 * 60); //60 минут
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //restClient.ping();
                //restClient.keepAliveUserDataStream(listenKey);
                instance = new Account();
                accountLog.info("Аккаунт обновлен");
                System.out.println("Аккаунт обновлен");
            }
        }
    }
}
