import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.sun.org.apache.xpath.internal.SourceTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import static java.lang.Math.min;

public class TradeWayThread extends Thread{
    //private double ETH = 0;
    //private double BNB = 0;

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

            /*
             * Bids - зеленые ордера, которым продаем.
             * Asks - красные ордера, у которых покупаем.
             */
            List<OrderBookEntry> currEthAsks = currEthBook.getAsks();
            List<OrderBookEntry> currBnbBids = currBnbBook.getBids();
            List<OrderBookEntry> bnbEthBids = bnbEthBook.getBids();

            //Первый элемент самый выгодный.
            OrderBookEntry buyCurrSellEth = currEthAsks.get(0);
            OrderBookEntry sellCurrBuyBnb = currBnbBids.get(0);
            OrderBookEntry sellBnbBuyEth = bnbEthBids.get(0);

            //Пусть эфира 0.02, примерно 1000 рублей
            final double startETH = 0.02;
            final double startBNB = 10;
            double ETH = startETH;
            double BNB = startBNB;

            /*
             * CURR = ETH/price
             * Amount of Currency = min(Amount that we can buy, Amount of order)
             */
            double currAmount = min(ETH / Double.parseDouble(buyCurrSellEth.getPrice()), Double.parseDouble(buyCurrSellEth.getQty()));
            //System.out.println("Curr amount: " + currAmount + "\n");

            //Если продаваемое количество CURR меньше, чем планировали продать, покупаем CURR столько, сколько можем продать.
            if (currAmount > Double.parseDouble(sellCurrBuyBnb.getQty()))
                currAmount =  Double.parseDouble(sellCurrBuyBnb.getQty());
            double bnbAmount = currAmount * Double.parseDouble(sellCurrBuyBnb.getPrice()); //Столько BNB предполагаем купить.
            //System.out.println("Curr qty: " + Double.parseDouble(sellCurrBuyBnb.getQty()));
            //System.out.println("Curr amount: " + currAmount + "\n");

            /*
             * Если продаваемое количество BNB меньше, чем планировали продать, покупаем BNB столько, сколько можем продать,
             * и покупаем CURR соответственно количеству BNB.
             */
            if (bnbAmount > Double.parseDouble(sellBnbBuyEth.getQty())){
                 bnbAmount = Double.parseDouble(sellBnbBuyEth.getQty());
                 currAmount = bnbAmount/Double.parseDouble(sellCurrBuyBnb.getPrice());
                 bnbAmount = currAmount * Double.parseDouble(sellCurrBuyBnb.getPrice());
             }
            double ethAmount = bnbAmount * Double.parseDouble(sellBnbBuyEth.getPrice());
            //System.out.println("BNB qty: " + Double.parseDouble(sellBnbBuyEth.getQty()));
            //System.out.println("Curr amount: " + currAmount);
            //System.out.println("BNB amount: " + bnbAmount);
            //System.out.println("ETH amount: " + ethAmount + "\n");

            ETH += -currAmount * Double.parseDouble(buyCurrSellEth.getPrice()) + ethAmount; //Изменение на счету эфира (расчетное).

            BNB = BNB - 2*getComission(currency, currAmount) - bnbAmount*0.0005; //Комиссия вычитается со счета BNB.
            //System.out.println("Commission CURR: " + (2*getComission(currency, currAmount)) + "\n");
            //System.out.println("Commission BNB: " + (bnbAmount*0.0005) + "\n");

            double rubProfit = (ETH - startETH)*53000 + (BNB - startBNB)*530;   //Примерный рублевый эквивалент профита.


            System.out.println("Currency: " + currency + ", RUB profit: " + rubProfit);
            if(rubProfit > 0)  //Лог положительного профита в файл.
                log.info("Currency: " + currency + ", RUB profit: " + rubProfit);

        }
    }


    private double getComission(String curr, double value) {
        //Exchange ratio of NEO/BNB = NEO/BTC[market price] /(BNB/BTC [market price])
        //BNB = NEO/price
        List<AggTrade> aggTrades = client.getAggTrades(curr + "BTC");
        AggTrade lastTrade = aggTrades.get(0);
        double currBTCPrice = Double.parseDouble(lastTrade.getPrice());

        aggTrades = client.getAggTrades("BNBBTC");
        lastTrade = aggTrades.get(0);
        double BNBBTCPrice = Double.parseDouble(lastTrade.getPrice());

        double price = currBTCPrice/BNBBTCPrice;

        return value*price*0.0005;
    }

    private double getComission(String curr, int value) {
        return getComission(curr, (double)value);
    }
}