package binance;

import com.binance.api.client.domain.account.NewOrderResponse;

import com.binance.api.client.exception.BinanceApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static java.lang.Math.min;

public class RiskTradeWayThreadWithCache extends Thread{
    //private double ETH = 0;
    //private double BNB = 0;
    private static Logger log = LoggerFactory.getLogger(RiskTradeWayThreadWithCache.class.getName());
    private String currency;

    private List<NewOrderResponse> orders = new ArrayList<>();

    private OrdersCache currEthCache;
    private OrdersCache currBnbCache;
    private OrdersCache bnbEthCache;

    private boolean  isPaused  = false;


    public RiskTradeWayThreadWithCache(String currency) {
        this.currency = currency;

        currEthCache = new OrdersCache(currency + "ETH");
        currBnbCache = new OrdersCache(currency + "BNB");
        bnbEthCache = new OrdersCache("BNBETH");
    }

    @Override
    public void run() {
        boolean  flag  = true;

        while (flag) {
            if(!isPaused) {
                /*
                 * Bids - зеленые ордера, которым продаем.
                 * Asks - красные ордера, у которых покупаем.
                 */

                Map.Entry<BigDecimal, BigDecimal> buyCurrSellEthBestBid = currEthCache.getBestBid();
                Map.Entry<BigDecimal, BigDecimal> sellCurrBuyBnbBestBid = currBnbCache.getBestBid();
                Map.Entry<BigDecimal, BigDecimal> sellBnbBuyEthBestBid = bnbEthCache.getBestBid();
                Map.Entry<BigDecimal, BigDecimal> buyCurrSellEthBestAsk = currEthCache.getBestAsk();
                Map.Entry<BigDecimal, BigDecimal> sellCurrBuyBnbBestAsk = currBnbCache.getBestAsk();
                Map.Entry<BigDecimal, BigDecimal> sellBnbBuyEthBestAsk = bnbEthCache.getBestAsk();

                //Пусть эфира 0.02, примерно 1000 рублей
                double startETH = 0.015;
                double startBNB = 2;
                double ETH = startETH;
                double BNB = startBNB;

                /*
                 * CURR = ETH/price
                 * Amount of Currency = min(Amount that we can buy, Amount of order)
                 */
                // Key = price
                double buyCurrSellEthPrice = roundPrice((buyCurrSellEthBestBid.getKey().doubleValue() + buyCurrSellEthBestAsk.getKey().doubleValue()) / 2,
                        currency + "ETH");
                //buyCurrSellEthBestAsk.getKey().doubleValue();
                System.out.println("buyCurrSellEthPrice: " + buyCurrSellEthPrice);
                double buyCurrSellEthQty = buyCurrSellEthBestAsk.getValue().doubleValue(); // Value = qty

                double currAmount = roundAmount(ETH / buyCurrSellEthPrice, currency + "ETH");
                System.out.println("currAmount: " + currAmount);

                double sellCurrBuyBnbPrice = roundPrice((sellCurrBuyBnbBestBid.getKey().doubleValue() + sellCurrBuyBnbBestAsk.getKey().doubleValue()) / 2,
                        currency + "BNB");
                System.out.println("sellCurrBuyBnbPrice: " + sellCurrBuyBnbPrice);

                double bnbAmount = roundAmount(currAmount * sellCurrBuyBnbPrice, currency + "BNB"); //Столько BNB предполагаем купить.
                System.out.println("bnbAmount: " + bnbAmount);

                double sellBnbBuyEthPrice = roundPrice((sellBnbBuyEthBestBid.getKey().doubleValue() + sellBnbBuyEthBestAsk.getKey().doubleValue()) / 2,
                        "BNBETH");
                System.out.println("sellBnbBuyEthPrice: " + sellBnbBuyEthPrice);

                double ethAmount = bnbAmount * sellBnbBuyEthPrice;

                System.out.println("ethAmount: " + ethAmount);

                ETH += -currAmount * buyCurrSellEthPrice + ethAmount; //Изменение на счету эфира (расчетное).
                System.out.println("ETH: " + ETH);

                BNB = BNB - 2 * Commission.getCommission(currency, currAmount) - bnbAmount * 0.00015; //Комиссия вычитается со счета BNB.
                System.out.println("BNB: " + BNB);

                double rubProfit = (ETH - startETH) * 53000 + (BNB - startBNB) * 530;   //Примерный рублевый эквивалент профита.

                System.out.println("Currency: " + currency + ",Risk RUB profit: " + rubProfit);

                /*String strCurrAmount = String.format(Locale.US, "%f", currAmount);
                String strBnbAmount = String.format(Locale.US, "%f", bnbAmount);
                System.out.println(strCurrAmount);
                System.out.println(strBnbAmount);*/

                if (rubProfit > 0.1)  //Лог положительного профита в файл.
                    log.info("Currency: " + currency + ", ETH profit: " + (ETH - startETH) + ", Risk RUB profit: " + rubProfit);

                if (rubProfit > 0.2) {

                    log.info("Currency: " + currency + ", Пробуем провести покупки!");
                    try {
                        //Account.newBuyOrder(currency + "ETH", currAmount, buyCurrSellEthPrice, this);
                        //Account.newSellOrder(currency + "BNB", currAmount, sellCurrBuyBnbPrice, this);
                        //Account.newSellOrder("BNBETH", bnbAmount, sellBnbBuyEthPrice, this);
                    } catch (BinanceApiException e) {
                        e.printStackTrace();
                        flag = false;
                    }

                    isPaused = true;

                    try {
                        sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                System.out.println(currency + ": Waiting for orders");

                synchronized (this) {
                    orders.removeIf((NewOrderResponse order) -> Account.isOrderFilled(order));
                }

                if(orders.size() == 0) {
                    isPaused = false;
                    log.info(currency + ": End of waiting");
                }
            }
        }
    }

    private double roundPrice(double value, String pair) {
        return new BigDecimal(value).setScale(Currencies.getPriceLimit(pair), RoundingMode.HALF_DOWN).doubleValue();
    }
    private double roundAmount(double value, String pair) {
        return new BigDecimal(value).setScale(Currencies.getAmountLimit(pair), RoundingMode.FLOOR).doubleValue();
    }


    public static void main(String[] args) {
        RiskTradeWayThreadWithCache rcn = new RiskTradeWayThreadWithCache("DLT");
        rcn.start();
    }

    public void addOrder(NewOrderResponse orderResponse) {
        orders.add(orderResponse);
    }
}
