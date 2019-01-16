package binance.ways;

import binance.overtaking.BackwardOvertaking;
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

public class BackwardTradeWay extends TradeWay {
    private double startMain, startBase,
            buyBaseSellMainPrice, baseAmount,
            buySmallSellBasePrice, smallAmount,
            sellSmallBuyMainPrice, mainAmount;

    public BackwardTradeWay(String smallCurrency, String baseCurrency, String mainCurrency, TradeWayPair pair) {
        super(smallCurrency, baseCurrency, mainCurrency, pair);
        wayName = "Way: " + baseCurrency + "-" + smallCurrency + "-" + mainCurrency;
    }

    @Override
    protected void calculateProfit() {
        /*
         * Bids - зеленые ордера, которым продаем.
         * Asks - красные ордера, у которых покупаем.
         */
        Map.Entry<BigDecimal, BigDecimal> sellSmallBuyMainAsk = smallMainCache.getBestAsk();
        Map.Entry<BigDecimal, BigDecimal> sellSmallBuyMainBid = smallMainCache.getBestBid();
        Map.Entry<BigDecimal, BigDecimal> buySmallSellBaseAsk = smallBaseCache.getBestAsk();
        Map.Entry<BigDecimal, BigDecimal> buySmallSellBaseBid = smallBaseCache.getBestBid();
        Map.Entry<BigDecimal, BigDecimal> buyBaseSellMainAsk = baseMainCache.getBestAsk();
        Map.Entry<BigDecimal, BigDecimal> buyBaseSellMainBid = baseMainCache.getBestBid();


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
        buyBaseSellMainPrice = roundPrice(buyBaseSellMainBid.getKey().doubleValue() + (1 / Math.pow(10, Currencies.getPriceLimit(baseCurrency + mainCurrency))),
                baseCurrency + mainCurrency);

        baseAmount = roundAmount(mainCurr / buyBaseSellMainPrice, baseCurrency + mainCurrency);

        //Если на счету меньше, чем мы хотим провести, то проводим столько, сколько есть на счету.
        if (baseAmount > Account.getAvailableBalance(baseCurrency)) {
            baseAmount = roundAmount(Account.getAvailableBalance(baseCurrency), baseCurrency + mainCurrency);
        }

        buySmallSellBasePrice = roundPrice((buySmallSellBaseBid.getKey().doubleValue() + (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + baseCurrency)))),
                smallCurrency + baseCurrency);

        /*
         * Если количество базовой, которую мы хотим купить, больше, чем у нас есть, покупаем мелкой валюты соответственно этому значению.
         * Округляем все в меньшую сторону.
         */
        smallAmount = roundAmount(baseAmount / buySmallSellBasePrice, smallCurrency + mainCurrency); //Столько базовой предполагаем купить.
        if (smallAmount > Account.getAvailableBalance(smallCurrency)) {
            smallAmount = roundAmount(Account.getAvailableBalance(smallCurrency), smallCurrency + mainCurrency);
            baseAmount = roundAmount(smallAmount * buySmallSellBasePrice, baseCurrency + mainCurrency);
            smallAmount = roundAmount(baseAmount / buySmallSellBasePrice, smallCurrency + mainCurrency);
        }

        sellSmallBuyMainPrice = roundPrice((sellSmallBuyMainAsk.getKey().doubleValue() - (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + mainCurrency)))),
                smallCurrency + mainCurrency);

        mainAmount = smallAmount * sellSmallBuyMainPrice;

        if (baseAmount < Currencies.getMinTotal(smallCurrency + baseCurrency) || //Проверяем, что больше минимального значения в графе Total
                mainAmount < Currencies.getMinTotal(baseCurrency + mainCurrency) ||
                mainAmount < Currencies.getMinTotal(smallCurrency + mainCurrency)) {
            System.out.println(wayName + ", Too small amount");
            trySleep(10000);
            return;
        }

        mainCurr += -baseAmount * buyBaseSellMainPrice + mainAmount; //Изменение на счету базовой валюты (расчетное).
        baseCurr += -smallAmount * buySmallSellBasePrice + baseAmount;
        double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

        double rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                BNBChange * Currencies.getRub("BNB");   //Примерный рублевый эквивалент профита.

        System.out.println(wayName + ", Risk RUB profit: " + String.format(Locale.US, "%.10f", rubProfit));


        /*System.out.println("buyBaseSellMainPrice: " + String.format(Locale.US, "%.10f", buyBaseSellMainPrice));
        System.out.println("baseAmount: " + String.format(Locale.US, "%.10f", baseAmount));
        System.out.println("buySmallSellBasePrice: " + String.format(Locale.US, "%.10f", buySmallSellBasePrice));
        System.out.println("smallAmount: " + String.format(Locale.US, "%.10f", smallAmount));
        System.out.println("sellSmallBuyMainPrice: " + String.format(Locale.US, "%.10f", sellSmallBuyMainPrice));
        System.out.println("mainAmount: " + String.format(Locale.US, "%.10f", mainAmount));
        System.out.println("startMain: " + String.format(Locale.US, "%.10f", startMain));
        System.out.println("mainCurr: " + String.format(Locale.US, "%.10f", mainCurr));
        System.out.println("startBase: " + String.format(Locale.US, "%.10f", startBase));
        System.out.println("baseCurr: " + String.format(Locale.US, "%.10f", baseCurr));
        System.out.println("BNBChange: " + String.format(Locale.US, "%.10f", BNBChange));*/


        /*if (rubProfit > 1)  //Лог положительного профита в файл.
            profitLog.info(wayName + ", Risk RUB profit: " + String.format(Locale.US, "%.10f", rubProfit));
        */
        if (rubProfit > profitLimit) { // Закупаемся.
            profitLog.info(wayName + ", Risk RUB profit: " + String.format(Locale.US, "%.10f", rubProfit));
            profitLog.info(wayName + ", Пробуем провести покупки!");

            synchronized (Account.class) {
                try {
                    Account.newBuyOrder(baseCurrency + mainCurrency, baseAmount, buyBaseSellMainPrice, this);
                    Account.newBuyOrder(smallCurrency + baseCurrency, smallAmount, buySmallSellBasePrice, this);
                    Account.newSellOrder(smallCurrency + mainCurrency, smallAmount, sellSmallBuyMainPrice, this);
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
        BackwardOvertaking overtaking = new BackwardOvertaking(this);
        overtaking.setPrices(startMain, startBase,
                buyBaseSellMainPrice, smallAmount,
                buySmallSellBasePrice, baseAmount,
                sellSmallBuyMainPrice, mainAmount);
        overtaking.setCurrencies(smallCurrency, baseCurrency, mainCurrency);

        if (orderResponse.getSymbol().equals((smallCurrency + mainCurrency)))
            overtaking.setMode("sellSmallBuyMain", orderResponse);
        else if (orderResponse.getSymbol().equals((baseCurrency + mainCurrency)))
            overtaking.setMode("buyBaseSellMain", orderResponse);
        else if (orderResponse.getSymbol().equals((smallCurrency + baseCurrency)))
            overtaking.setMode("buySmallSellBase", orderResponse);

        overtaking.start();
    }

    public static void main(String[] args) {
        //TradeWay rcn = new BackwardTradeWay("CMT", "BNB", "ETH");
        //rcn.start();
    }
}
