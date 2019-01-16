package binance.ways;

import binance.overtaking.ForwardOvertaking;
import binance.utils.Account;
import binance.utils.Commission;
import binance.utils.Currencies;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.exception.BinanceApiException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import static binance.utils.Round.roundAmount;
import static binance.utils.Round.roundPrice;

public class ForwardTradeWay extends TradeWay {
    private double startMain, startBase,
            buySmallSellMainPrice, smallAmount,
            sellSmallBuyBasePrice, baseAmount,
            sellBaseBuyMainPrice, mainAmount;

    public ForwardTradeWay(String smallCurrency, String baseCurrency, String mainCurrency, TradeWayPair pair) {
        super(smallCurrency, baseCurrency, mainCurrency, pair);
        wayName = "Way: " + smallCurrency + "-" + baseCurrency + "-" + mainCurrency;
    }

    @Override
    protected void calculateProfit() {
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


        startMain = Math.min(Currencies.getMaxAmount(mainCurrency), Account.getAvailableBalance(mainCurrency));
        startBase = Account.getAvailableBalance(baseCurrency);
        double mainCurr = startMain;
        double baseCurr = startBase;

        /*
         * CURR = ETH/price
         * Amount of Currency = min(Amount that we can buy, Amount of order)
         */
        // Key = price
        //System.out.println("price " + buySmallSellMainBid.getKey());
        buySmallSellMainPrice = roundPrice(buySmallSellMainBid.getKey().doubleValue() + (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + mainCurrency))),
                smallCurrency + mainCurrency);

        smallAmount = roundAmount(mainCurr / buySmallSellMainPrice, smallCurrency + mainCurrency);

        //Если на счету меньше, чем мы хотим провести, то проводим столько, сколько есть на счету.
        if (smallAmount > Account.getAvailableBalance(smallCurrency)) {
            smallAmount = roundAmount(Account.getAvailableBalance(smallCurrency), smallCurrency + mainCurrency);
        }

        sellSmallBuyBasePrice = roundPrice(sellSmallBuyBaseAsk.getKey().doubleValue() - (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + baseCurrency))),
                smallCurrency + baseCurrency);

        /*
         * Если количество базовой, которую мы хотим купить, больше, чем у нас есть, покупаем мелкой валюты соответственно этому значению.
         * Округляем все в меньшую сторону.
         */
        baseAmount = roundAmount(smallAmount * sellSmallBuyBasePrice, baseCurrency + mainCurrency); //Столько базовой предполагаем купить.
        if (baseAmount > Account.getAvailableBalance(baseCurrency)) {
            baseAmount = roundAmount(Account.getAvailableBalance(baseCurrency), baseCurrency + mainCurrency);
            smallAmount = roundAmount(baseAmount / sellSmallBuyBasePrice, smallCurrency + mainCurrency);
            baseAmount = roundAmount(smallAmount * sellSmallBuyBasePrice, baseCurrency + mainCurrency);
        }

        sellBaseBuyMainPrice = roundPrice((sellBaseBuyMainAsk.getKey().doubleValue() - (1 / Math.pow(10, Currencies.getPriceLimit(baseCurrency + mainCurrency)))),
                baseCurrency + mainCurrency);

        mainAmount = baseAmount * sellBaseBuyMainPrice;


        /*System.out.println("buySmallSellMainPrice: " + String.format(Locale.US, "%.10f", buySmallSellMainPrice));
        System.out.println("smallAmount: " + String.format(Locale.US, "%.10f", smallAmount));
        System.out.println("sellSmallBuyBasePrice: " + String.format(Locale.US, "%.10f", sellSmallBuyBasePrice));
        System.out.println("baseAmount: " + String.format(Locale.US, "%.10f", baseAmount));
        System.out.println("sellBaseBuyMainPrice: " + String.format(Locale.US, "%.10f", sellBaseBuyMainPrice));
        System.out.println("startMain: " + String.format(Locale.US, "%.10f", startMain));*/

        if (baseAmount < Currencies.getMinTotal(smallCurrency + baseCurrency) || //Проверяем, что больше минимального значения в графе Total
                mainAmount < Currencies.getMinTotal(baseCurrency + mainCurrency) ||
                mainAmount < Currencies.getMinTotal(smallCurrency + mainCurrency)) {
            System.out.println(wayName + ", Too small amount");
            trySleep(10000);
            return;
        }

        mainCurr += -smallAmount * buySmallSellMainPrice + mainAmount; //Изменение на счету эфира (расчетное).
        baseCurr += smallAmount * sellSmallBuyBasePrice - baseAmount;

        double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

        double rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                BNBChange * Currencies.getRub("BNB");   //Примерный рублевый эквивалент профита.


        /*System.out.println("mainCurr: " + String.format(Locale.US, "%.10f", mainCurr));
        System.out.println("startBase: " + String.format(Locale.US, "%.10f", startBase));
        System.out.println("baseCurr: " + String.format(Locale.US, "%.10f", baseCurr));
        System.out.println("BNBChange: " + String.format(Locale.US, "%.10f", BNBChange));*/

        System.out.println(wayName + ", Risk RUB profit: " + String.format(Locale.US, "%.10f", rubProfit));


        /*if (rubProfit > 1)  //Лог положительного профита в файл.
            profitLog.info(wayName + ", Risk RUB profit: " + String.format(Locale.US, "%.10f", rubProfit)); */

        if (rubProfit > profitLimit) { // Закупаемся.
            profitLog.info(wayName + ", Risk RUB profit: " + String.format(Locale.US, "%.10f", rubProfit));
            profitLog.info(wayName + ", Пробуем провести покупки!");
            synchronized (Account.class) {
                try {
                    Account.newBuyOrder(smallCurrency + mainCurrency, smallAmount, buySmallSellMainPrice, this);
                    Account.newSellOrder(smallCurrency + baseCurrency, smallAmount, sellSmallBuyBasePrice, this);
                    Account.newSellOrder(baseCurrency + mainCurrency, baseAmount, sellBaseBuyMainPrice, this);
                    startTime = System.currentTimeMillis();
                } catch (BinanceApiException e) {
                    e.printStackTrace();
                    isActive = false;
                }
                trySleep(1000);
            }

            isPaused = true;

            trySleep(10000);
        }
    }

    @Override
    protected void startOvertaking(NewOrderResponse orderResponse) {
        ForwardOvertaking overtaking = new ForwardOvertaking(this);
        overtaking.setPrices(startMain, startBase,
                buySmallSellMainPrice, smallAmount,
                sellSmallBuyBasePrice, baseAmount,
                sellBaseBuyMainPrice, mainAmount);
        overtaking.setCurrencies(smallCurrency, baseCurrency, mainCurrency);

        if (orderResponse.getSymbol().equals((smallCurrency + mainCurrency)))
            overtaking.setMode("buySmallSellMain", orderResponse);
        else if (orderResponse.getSymbol().equals((baseCurrency + mainCurrency)))
            overtaking.setMode("sellBaseBuyMain", orderResponse);
        else if (orderResponse.getSymbol().equals((smallCurrency + baseCurrency)))
            overtaking.setMode("sellSmallBuyBase", orderResponse);

        overtaking.start();
    }


    public static void main(String[] args) {
        //TradeWay rcn = new ForwardTradeWay("CMT", "BNB", "ETH");
        //rcn.start();
    }
}
