package binance;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.lang.Math.min;

public class RiskTradeWayThread extends Thread{
    //private double ETH = 0;
    //private double BNB = 0;
    private static Logger log = LoggerFactory.getLogger(RiskTradeWayThread.class.getName());
    private static BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
    private static BinanceApiRestClient client = factory.newRestClient();
    private String currency;


    public RiskTradeWayThread(String currency) {
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
            List<OrderBookEntry> currBnbAsks = currBnbBook.getAsks();
            List<OrderBookEntry> bnbEthAsks = bnbEthBook.getAsks();

            //Первый элемент самый выгодный
            OrderBookEntry buyCurrSellEthAsk = currEthAsks.get(0);
            OrderBookEntry sellCurrBuyBnbBid = currBnbBids.get(0);
            OrderBookEntry sellBnbBuyEthBid = bnbEthBids.get(0);
            OrderBookEntry sellCurrBuyBnbAsk = currBnbAsks.get(0);
            OrderBookEntry sellBnbBuyEthAsk = bnbEthAsks.get(0);

            //Пусть эфира 0.02, примерно 1000 рублей
            final double startETH = 0.02;
            final double startBNB = 10;
            double ETH = startETH;
            double BNB = startBNB;

            /*
             * CURR = ETH/price
             * Amount of Currency = min(Amount that we can buy, Amount of order)
             */

            double currAmount = min(ETH / Double.parseDouble(buyCurrSellEthAsk.getPrice()), Double.parseDouble(buyCurrSellEthAsk.getQty()));
            //System.out.println("currAmount: " + currAmount);
            double sellCurrBuyBnbPrice = (Double.parseDouble(sellCurrBuyBnbBid.getPrice()) + Double.parseDouble(sellCurrBuyBnbAsk.getPrice()))/2;
            //System.out.println("sellCurrBuyBnbPrice: " + sellCurrBuyBnbPrice);
            double bnbAmount = currAmount * sellCurrBuyBnbPrice; //Столько BNB предполагаем купить.
            //System.out.println("bnbAmount: " + bnbAmount);
            double sellBnbBuyEthPrice = (Double.parseDouble(sellBnbBuyEthBid.getPrice()) + Double.parseDouble(sellBnbBuyEthAsk.getPrice()))/2;
            //System.out.println("sellBnbBuyEthPrice: " + sellBnbBuyEthPrice);
            double ethAmount = bnbAmount * sellBnbBuyEthPrice;
            //System.out.println("ethAmount: " + ethAmount);

            ETH += -currAmount * Double.parseDouble(buyCurrSellEthAsk.getPrice()) + ethAmount; //Изменение на счету эфира (расчетное).
            //System.out.println("ETH: " + ETH);
            BNB = BNB - 2*Commission.getCommission(currency, currAmount) - bnbAmount*0.0005; //Комиссия вычитается со счета BNB.
            //System.out.println("BNB: " + BNB);
            double rubProfit = (ETH - startETH)*53000 + (BNB - startBNB)*530;   //Примерный рублевый эквивалент профита.


            System.out.println("Currency: " + currency + ",Risk RUB profit: " + rubProfit);
            if(rubProfit > 0)  //Лог положительного профита в файл.
                log.info("Currency: " + currency + ",Risk RUB profit: " + rubProfit);
            try {
                sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        RiskTradeWayThread rcn = new RiskTradeWayThread("DLT");
        rcn.start();
    }
}