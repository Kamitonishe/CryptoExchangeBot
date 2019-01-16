package binance.overtaking;

import binance.utils.Account;
import binance.utils.Commission;
import binance.utils.Currencies;
import binance.utils.OrdersCache;
import binance.ways.TradeWay;

import java.math.BigDecimal;
import java.util.Map;

import static binance.utils.Round.roundPrice;

public class BackwardOvertaking extends Overtaking {

    public BackwardOvertaking(TradeWay tradeWay) {
        super(tradeWay);
    }

    private double startMain, startBase,
            buyBaseSellMainPrice, smallAmount,
            buySmallSellBasePrice, baseAmount,
            sellSmallBuyMainPrice, mainAmount;

    public void setPrices(double startMain, double startBase,
                          double buyBaseSellMainPrice, double smallAmount,
                          double buySmallSellBasePrice, double baseAmount,
                          double sellSmallBuyMainPrice, double mainAmount) {
        this.startMain = startMain;
        this.startBase = startBase;
        this.buyBaseSellMainPrice = buyBaseSellMainPrice;
        this.baseAmount = baseAmount;
        this.buySmallSellBasePrice = buySmallSellBasePrice;
        this.smallAmount = smallAmount;
        this.sellSmallBuyMainPrice = sellSmallBuyMainPrice;
        this.mainAmount = mainAmount;
    }

    @Override
    public void overtake() {
        switch (mode) {
            case "buyBaseSellMain":
                buyBaseSellMainOvertaking();
                break;

            case "buySmallSellBase":
                buySmallSellBaseOvertaking();
                break;

            case "sellSmallBuyMain":
                sellSmallBuyMainOvertaking();
                break;
        }
    }

    @Override
    public void correct() {
        switch (mode) {
            case "buyBaseSellMain":
                buyBaseSellMainCorrection();
                break;

            case "buySmallSellBase":
                buySmallSellBaseCorrection();
                break;

            case "sellSmallBuyMain":
                sellSmallBuyMainCorrection();
                break;
        }
    }

    private void buyBaseSellMainOvertaking() {
        double mainCurr = startMain;
        double baseCurr = startBase;

        Map.Entry<BigDecimal, BigDecimal> buyBaseSellMainBid = tradeWay.getBaseMainCache().getBestBid();

        if (buyBaseSellMainPrice < buyBaseSellMainBid.getKey().doubleValue()) {
            double newBuyBaseSellMainPrice = roundPrice(buyBaseSellMainBid.getKey().doubleValue() + (1 / Math.pow(10, Currencies.getPriceLimit(baseCurrency + mainCurrency))),
                    baseCurrency + mainCurrency);

            mainCurr += -baseAmount * newBuyBaseSellMainPrice + mainAmount; //Изменение на счету базовой валюты (расчетное).
            baseCurr += -smallAmount * buySmallSellBasePrice + baseAmount;
            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");   //Примерный рублевый эквивалент профита.
            if (rubProfit > overtPriceLimit) {
                synchronized (tradeWay) {
                    if (Account.cancelOrder(order)) {
                        Account.newBuyOrder(baseCurrency + mainCurrency, baseAmount, newBuyBaseSellMainPrice, this);
                        buyBaseSellMainPrice = newBuyBaseSellMainPrice;
                        profitLog.info(tradeWay.getWayName() + ", Overtaken, Rub profit: " + rubProfit);
                    }
                }
            } else {
                System.out.println(tradeWay + ", Overtaking are not profitable, Rub profit: " + rubProfit);
            }
        } else {
            System.out.println(tradeWay + ", Overtaking price is normal");
        }
    }

    private void buyBaseSellMainCorrection() {
        OrdersCache cache = tradeWay.getBaseMainCache();
        double firstPrice = cache.getBestBid().getKey().doubleValue();
        double secondPrice = cache.getSecondBid().getKey().doubleValue();

        if (buyBaseSellMainPrice == firstPrice &&
                buyBaseSellMainPrice - secondPrice > corrLimit * (1 / Math.pow(10, Currencies.getPriceLimit(baseCurrency + mainCurrency)))) {

            double newBuyBaseSellMainPrice = roundPrice(secondPrice + (1 / Math.pow(10, Currencies.getPriceLimit(baseCurrency + mainCurrency))),
                    baseCurrency + mainCurrency);

            double mainCurr = startMain;
            double baseCurr = startBase;

            mainCurr += -baseAmount * newBuyBaseSellMainPrice + mainAmount; //Изменение на счету базовой валюты (расчетное).
            baseCurr += -smallAmount * buySmallSellBasePrice + baseAmount;
            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");   //Примерный рублевый эквивалент профита.

            synchronized (tradeWay) {
                if (Account.cancelOrder(order)) {
                    Account.newBuyOrder(baseCurrency + mainCurrency, baseAmount, newBuyBaseSellMainPrice, this);
                    buyBaseSellMainPrice = newBuyBaseSellMainPrice;
                    profitLog.info(tradeWay.getWayName() + ", Corrected, Rub profit: " + rubProfit);
                }
            }
        }
    }


    private void buySmallSellBaseOvertaking() {
        double mainCurr = startMain;
        double baseCurr = startBase;

        Map.Entry<BigDecimal, BigDecimal> buySmallSellBaseBid = tradeWay.getSmallBaseCache().getBestBid();

        if (buySmallSellBasePrice < buySmallSellBaseBid.getKey().doubleValue()) {
            double newBuySmallSellBasePrice = roundPrice(buySmallSellBaseBid.getKey().doubleValue() + (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + baseCurrency))),
                    smallCurrency + baseCurrency);

            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.
            mainCurr += -baseAmount * buyBaseSellMainPrice + mainAmount; //Изменение на счету базовой валюты (расчетное).
            baseCurr += -smallAmount * newBuySmallSellBasePrice + baseAmount;

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");   //Примерный рублевый эквивалент профита.
            if (rubProfit > overtPriceLimit) {
                synchronized (tradeWay) {
                    if (Account.cancelOrder(order)) {
                        Account.newBuyOrder(smallCurrency + baseCurrency, smallAmount, newBuySmallSellBasePrice, this);
                        profitLog.info(tradeWay.getWayName() + ", Overtaken, Rub profit: " + rubProfit);
                        buySmallSellBasePrice = newBuySmallSellBasePrice;
                    }
                }
            } else {
                System.out.println(tradeWay + ", Overtaking are not profitable, Rub profit: " + rubProfit);
            }
        } else {
            System.out.println(tradeWay + ", Overtaking price is normal");
        }
    }

    private void buySmallSellBaseCorrection() {
        OrdersCache cache = tradeWay.getSmallBaseCache();
        double firstPrice = cache.getBestBid().getKey().doubleValue();
        double secondPrice = cache.getSecondBid().getKey().doubleValue();

        if (buyBaseSellMainPrice == firstPrice &&
                buyBaseSellMainPrice - secondPrice > corrLimit * (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + baseCurrency)))) {

            double newBuySmallSellBasePrice = roundPrice(secondPrice + (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + baseCurrency))),
                    smallCurrency + baseCurrency);

            double mainCurr = startMain;
            double baseCurr = startBase;

            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.
            mainCurr += -baseAmount * buyBaseSellMainPrice + mainAmount; //Изменение на счету базовой валюты (расчетное).
            baseCurr += -smallAmount * newBuySmallSellBasePrice + baseAmount;

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");   //Примерный рублевый эквивалент профита.

            synchronized (tradeWay) {
                if (Account.cancelOrder(order)) {
                    Account.newBuyOrder(smallCurrency + baseCurrency, smallAmount, newBuySmallSellBasePrice, this);
                    buySmallSellBasePrice = newBuySmallSellBasePrice;
                    profitLog.info(tradeWay.getWayName() + ", Corrected, Rub profit: " + rubProfit);
                }
            }
        }
    }

    private void sellSmallBuyMainOvertaking() {
        double mainCurr = startMain;
        double baseCurr = startBase;

        Map.Entry<BigDecimal, BigDecimal> sellSmallBuyMainAsk = tradeWay.getSmallMainCache().getBestAsk();

        if (sellSmallBuyMainPrice > sellSmallBuyMainAsk.getKey().doubleValue()) {
            double newSellSmallBuyMainPrice = roundPrice(sellSmallBuyMainAsk.getKey().doubleValue() - (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + mainCurrency))),
                    smallCurrency + mainCurrency);

            double newMainAmount = smallAmount * newSellSmallBuyMainPrice;

            mainCurr += -baseAmount * buyBaseSellMainPrice + newMainAmount; //Изменение на счету главной валюты (расчетное).
            baseCurr += -smallAmount * buySmallSellBasePrice + baseAmount;
            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");   //Примерный рублевый эквивалент профита.
            if (rubProfit > overtPriceLimit) {
                synchronized (tradeWay) {
                    if (Account.cancelOrder(order)) {
                        Account.newSellOrder(smallCurrency + mainCurrency, smallAmount, newSellSmallBuyMainPrice, this);
                        sellSmallBuyMainPrice = newSellSmallBuyMainPrice;
                        mainAmount = newMainAmount;
                        profitLog.info(tradeWay.getWayName() + ", Overtaken, Rub profit: " + rubProfit);
                    }
                }
            } else {
                System.out.println(tradeWay + ", Overtaking are not profitable, Rub profit: " + rubProfit);
            }
        } else {
            System.out.println(tradeWay + ", Overtaking price is normal");
        }
    }


    private void sellSmallBuyMainCorrection() {
        OrdersCache cache = tradeWay.getSmallMainCache();
        double firstPrice = cache.getBestAsk().getKey().doubleValue();
        double secondPrice = cache.getSecondAsk().getKey().doubleValue();

        if (sellSmallBuyMainPrice == firstPrice &&
                secondPrice - sellSmallBuyMainPrice > corrLimit * (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + mainCurrency)))) {

            double newSellSmallBuyMainPrice = roundPrice(secondPrice - (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + mainCurrency))),
                    smallCurrency + mainCurrency);

            double newMainAmount = smallAmount * newSellSmallBuyMainPrice;
            double mainCurr = startMain;
            double baseCurr = startBase;
            mainCurr += -baseAmount * buyBaseSellMainPrice + newMainAmount; //Изменение на счету главной валюты (расчетное).
            baseCurr += -smallAmount * buySmallSellBasePrice + baseAmount;
            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");   //Примерный рублевый эквивалент профита.

            synchronized (tradeWay) {
                if (Account.cancelOrder(order)) {
                    Account.newSellOrder(smallCurrency + mainCurrency, smallAmount, newSellSmallBuyMainPrice, this);
                    sellSmallBuyMainPrice = newSellSmallBuyMainPrice;
                    mainAmount = newMainAmount;
                    profitLog.info(tradeWay.getWayName() + ", Corrected, Rub profit: " + rubProfit);
                }
            }
        }
    }
}
