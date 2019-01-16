package binance.ways;

public class TradeWayPair {

    private  TradeWay[] ways = new TradeWay[2];

    public TradeWayPair(String smallCurrency, String baseCurrency, String mainCurrency) {
        ways[0] = new ForwardTradeWay(smallCurrency, baseCurrency, mainCurrency, this);
        ways[1] = new BackwardTradeWay(smallCurrency, baseCurrency, mainCurrency, this);
    }

    public void start() {
        for(TradeWay way : ways)
            way.start();
    }

    public void finish() {
        for(TradeWay way : ways)
            way.finish();
    }

    public TradeWay[] getTradeWays() {
        return ways;
    }
}
