import binance.utils.OrdersCache;

public class OrdersCacheTest {
    public static void main(String[] args) {
        OrdersCache smallMainCache = new OrdersCache("CMTBNB");
        System.out.println(smallMainCache.getSecondAsk());
        System.out.println(smallMainCache.getSecondBid());
    }
}
