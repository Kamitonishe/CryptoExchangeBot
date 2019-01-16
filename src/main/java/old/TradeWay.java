package binance;

import com.binance.api.client.domain.account.NewOrderResponse;

import com.binance.api.client.exception.BinanceApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static java.lang.Math.min;

public class TradeWay extends Thread{
    //private double ETH = 0;
    //private double BNB = 0;
    private static Logger profitLog = LoggerFactory.getLogger("profit");
    private static Logger orderLog = LoggerFactory.getLogger("order");

    private String smallCurrency, baseCurrency, mainCurrency;

    private List<NewOrderResponse> orders = new ArrayList<>();

    private OrdersCache smallMainCache;
    private OrdersCache smallBaseCache;
    private OrdersCache baseMainCache;

    private boolean  isPaused  = false;
    private boolean isActive = true;

    private final Updater updater = new Updater();

    public TradeWay(String smallCurrency, String baseCurrency, String mainCurrency) {
        this.smallCurrency = smallCurrency;
        this.baseCurrency = baseCurrency;
        this.mainCurrency = mainCurrency;

        smallMainCache = new OrdersCache(smallCurrency + mainCurrency);
        smallBaseCache = new OrdersCache(smallCurrency + baseCurrency);
        baseMainCache = new OrdersCache(baseCurrency + mainCurrency);

        updater.start(); //Пересоздает кэши каждые полчаса, иначе отваливаются сокеты.
    }

    @Override
    public void run() {

        while (isActive) {
            if(!isPaused) {
                synchronized (updater) { //Синхронизация на апдейтере, необходимо, чтобы пересоздание кэшей и вычисления происходили в разное время.
                    calculateProfit();
                }
            } else {
                trySleep(10000);

                System.out.println("Way: " + smallCurrency + "-" + baseCurrency + "-" + mainCurrency + ", Waiting for orders");

                synchronized (this) {  //Синхронизация на пути, чтобы добавления в список и удаления из них происходили в разное время.
                    orders.removeIf((NewOrderResponse order) -> Account.isOrderFilled(order)); // Удаление из списка исполнившихся ордеров.
                }

                if(orders.size() == 0) {
                    isPaused = false;
                    orderLog.info("Way: " + smallCurrency + "-" + baseCurrency + "-" + mainCurrency + ", End of waiting");
                }
            }
        }
    }


    private void calculateProfit() {
        /*
         * Bids - зеленые ордера, которым продаем.
         * Asks - красные ордера, у которых покупаем.
         */
        Map.Entry<BigDecimal, BigDecimal> buySmallSellMainAsk = smallMainCache.getBestAsk();
        Map.Entry<BigDecimal, BigDecimal> buySmallSellMainBid = smallMainCache.getBestBid();
        Map.Entry<BigDecimal, BigDecimal> sellSmallBuyBaseAsk = smallBaseCache.getBestAsk();
        Map.Entry<BigDecimal, BigDecimal> sellSmallBuyBaseBid = smallBaseCache.getBestBid();
        Map.Entry<BigDecimal, BigDecimal> sellBaseBuyMainAsk = baseMainCache.getBestAsk();
        Map.Entry<BigDecimal, BigDecimal> sellBaseBuyMainBid = baseMainCache.getBestBid();


        double startMain = min(Currencies.getMaxAmount(mainCurrency), Account.getAvailableBalance(mainCurrency));
        double startBase = Account.getAvailableBalance(baseCurrency);
        double mainCurr = startMain;
        double baseCurr = startBase;

        /*
         * CURR = ETH/price
         * Amount of Currency = min(Amount that we can buy, Amount of order)
         */
        // Key = price
        //System.out.println("price " + buySmallSellMainBid.getKey());
        double buySmallSellMainPrice = roundPrice((buySmallSellMainBid.getKey().doubleValue() + buySmallSellMainAsk.getKey().doubleValue()) / 2,
                smallCurrency + mainCurrency);

        double smallCurrAmount = roundAmount(mainCurr / buySmallSellMainPrice, smallCurrency + mainCurrency);

        //Если на счету меньше, чем мы хотим провести, то проводим столько, сколько есть на счету.
        if (smallCurrAmount > Account.getAvailableBalance(smallCurrency)) {
            smallCurrAmount = roundAmount(Account.getAvailableBalance(smallCurrency), smallCurrency + mainCurrency);
        }

        double sellSmallBuyBasePrice = roundPrice((sellSmallBuyBaseBid.getKey().doubleValue() + sellSmallBuyBaseAsk.getKey().doubleValue()) / 2,
                smallCurrency + baseCurrency);

        /*
         * Если количество базовой, которую мы хотим купить, больше, чем у нас есть, покупаем мелкой валюты соответственно этому значению.
         * Округляем все в меньшую сторону.
         */
        double baseAmount = roundAmount(smallCurrAmount * sellSmallBuyBasePrice, baseCurrency + mainCurrency); //Столько базовой предполагаем купить.
        if(baseAmount > Account.getAvailableBalance(baseCurrency)) {
            baseAmount = roundAmount(Account.getAvailableBalance(baseCurrency), baseCurrency + mainCurrency);
            smallCurrAmount = roundAmount(baseAmount / sellSmallBuyBasePrice, smallCurrency + mainCurrency);
            baseAmount = roundAmount(smallCurrAmount * sellSmallBuyBasePrice, baseCurrency + mainCurrency);
        }

        double sellBaseBuyMainPrice = roundPrice((sellBaseBuyMainAsk.getKey().doubleValue() - Math.pow(1/10, Currencies.getPriceLimit(baseCurrency + mainCurrency))),
                baseCurrency + mainCurrency);

        double mainAmount = baseAmount * sellBaseBuyMainPrice;

        if(baseAmount < Currencies.getMinTotal(smallCurrency + baseCurrency) || //Проверяем, что больше минимального значения в графе Total
                mainAmount < Currencies.getMinTotal(baseCurrency + mainCurrency) ||
                mainAmount < Currencies.getMinTotal(smallCurrency + mainCurrency)) {
            System.out.println("Way: " + smallCurrency + "-" + baseCurrency + "-" + mainCurrency + ", Too small amount");
            trySleep(10000);
            return;
        }

        mainCurr += -smallCurrAmount * buySmallSellMainPrice + mainAmount; //Изменение на счету эфира (расчетное).
        baseCurr += smallCurrAmount * sellSmallBuyBasePrice - baseAmount;

        double BNBChange = - 2 * Commission.getCommission(smallCurrency, smallCurrAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

        double rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                (baseCurr - startBase)*Currencies.getRub(baseCurrency) +
                BNBChange * Currencies.getRub("BNB");   //Примерный рублевый эквивалент профита.

        System.out.println("Way: " + smallCurrency + "-" + baseCurrency + "-" + mainCurrency + ", Risk RUB profit: " + rubProfit);


        System.out.println("buySmallSellMainPrice: " + String.format(Locale.US, "%f", buySmallSellMainPrice));
        System.out.println("smallCurrAmount: " + String.format(Locale.US, "%f", smallCurrAmount));
        System.out.println("sellSmallBuyBasePrice: " + String.format(Locale.US, "%f", sellSmallBuyBasePrice));
        System.out.println("baseAmount: " + String.format(Locale.US, "%f", baseAmount));
        System.out.println("sellBaseBuyMainPrice: " + String.format(Locale.US, "%f", sellBaseBuyMainPrice));
        System.out.println("mainAmount: " + String.format(Locale.US, "%f", mainAmount));
        System.out.println("startMain: " + String.format(Locale.US, "%f", startMain));
        System.out.println("mainCurr: " + String.format(Locale.US, "%f", mainCurr));
        System.out.println("startBase: " + String.format(Locale.US, "%f", startBase));
        System.out.println("baseCurr: " + String.format(Locale.US, "%f", baseCurr));
        System.out.println("BNBChange: " + String.format(Locale.US, "%f", BNBChange));


        if (rubProfit > 0)  //Лог положительного профита в файл.
            profitLog.info("Way: " + smallCurrency + "-" + baseCurrency + "-" + mainCurrency + ", Risk RUB profit: " + rubProfit);

        if (rubProfit > 0.4) {

            profitLog.info("Way: " + smallCurrency + "-" + baseCurrency + "-" + mainCurrency + ", Пробуем провести покупки!");
            try {
                Account.newBuyOrder(smallCurrency + mainCurrency, smallCurrAmount, buySmallSellMainPrice, this);
                Account.newSellOrder(smallCurrency + baseCurrency, smallCurrAmount, sellSmallBuyBasePrice, this);
                Account.newSellOrder(baseCurrency + mainCurrency, baseAmount, sellBaseBuyMainPrice, this);
            } catch (BinanceApiException e) {
                e.printStackTrace();
                isActive = false;
            }

            isPaused = true;

            trySleep(10000);
        }
        trySleep(200);
    }


    private void trySleep(int mls) {
        try {
            sleep(mls);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private double roundPrice(double value, String pair) {
        return new BigDecimal(value).setScale(Currencies.getPriceLimit(pair), RoundingMode.HALF_DOWN).doubleValue();
    }
    private double roundAmount(double value, String pair) {
        return new BigDecimal(value).setScale(Currencies.getAmountLimit(pair), RoundingMode.FLOOR).doubleValue();
    }

    public void addOrder(NewOrderResponse orderResponse) {
        orders.add(orderResponse);
    }


    private class Updater extends Thread {
        @Override
        public void run() {
            while(isActive) {
                try {
                    sleep(1000 * 60 * 30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (updater) {
                    smallMainCache = new OrdersCache(smallCurrency + mainCurrency);
                    smallBaseCache = new OrdersCache(smallCurrency + baseCurrency);
                    baseMainCache = new OrdersCache(baseCurrency + mainCurrency);
                    profitLog.info("Way: " + smallCurrency + "-" + baseCurrency + "-" + mainCurrency + ", Кэши обновлены.");
                }
            }

        }
    }


    public static void main(String[] args) {
        TradeWay rcn = new TradeWay("XLM", "BNB", "ETH");
        rcn.start();
    }
}
