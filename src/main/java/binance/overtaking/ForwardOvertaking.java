package binance.overtaking;

import binance.utils.Account;
import binance.utils.Commission;
import binance.utils.Currencies;
import binance.utils.OrdersCache;
import binance.ways.TradeWay;

import java.math.BigDecimal;
import java.util.Map;

import static binance.utils.Round.roundPrice;

public class ForwardOvertaking extends Overtaking {

    public ForwardOvertaking(TradeWay tradeWay) {
        super(tradeWay);
    }

    private double startMain, startBase,
            buySmallSellMainPrice, smallAmount,
            sellSmallBuyBasePrice, baseAmount,
            sellBaseBuyMainPrice, mainAmount;

    public void setPrices(double startMain, double startBase,
                          double buySmallSellMainPrice, double smallAmount,
                          double sellSmallBuyBasePrice, double baseAmount,
                          double sellBaseBuyMainPrice, double mainAmount) {
        this.startMain = startMain;
        this.startBase = startBase;
        this.buySmallSellMainPrice = buySmallSellMainPrice;
        this.smallAmount = smallAmount;
        this.sellSmallBuyBasePrice = sellSmallBuyBasePrice;
        this.baseAmount = baseAmount;
        this.sellBaseBuyMainPrice = sellBaseBuyMainPrice;
        this.mainAmount = mainAmount;
    }

    @Override
    public void overtake() {
        switch (mode) {
            case "buySmallSellMain":
                buySmallSellMainOvertaking();
                break;

            case "sellSmallBuyBase":
                sellSmallBuyBaseOvertaking();
                break;

            case "sellBaseBuyMain":
                sellBaseBuyMainOvertaking();
                break;
        }
    }

    @Override
    public void correct() {
        switch (mode) {
            case "buySmallSellMain":
                buySmallSellMainCorrection();
                break;

            case "sellSmallBuyBase":
                sellSmallBuyBaseCorrection();
                break;

            case "sellBaseBuyMain":
                sellBaseBuyMainCorrection();
                break;
        }
    }

    private void buySmallSellMainOvertaking() {
        double mainCurr = startMain;
        double baseCurr = startBase;

        Map.Entry<BigDecimal, BigDecimal> buySmallSellMainBid = tradeWay.getSmallMainCache().getBestBid();

        if (buySmallSellMainPrice < buySmallSellMainBid.getKey().doubleValue()) {
            double newBuySmallSellMainPrice = roundPrice(buySmallSellMainBid.getKey().doubleValue() + (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + mainCurrency))),
                    smallCurrency + mainCurrency);

            mainCurr += -smallAmount * newBuySmallSellMainPrice + mainAmount;
            baseCurr += smallAmount * sellSmallBuyBasePrice - baseAmount;
            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");

            if (rubProfit > overtPriceLimit) {
                synchronized (tradeWay) {
                    if (Account.cancelOrder(order)) {
                        Account.newBuyOrder(smallCurrency + mainCurrency, smallAmount, newBuySmallSellMainPrice, this);
                        buySmallSellMainPrice = newBuySmallSellMainPrice;
                        profitLog.info(tradeWay.getWayName() + ", Overtaken, Rub profit: " + rubProfit);
                    }
                }
            } else {
                System.out.println(tradeWay.getWayName() + ", Overtaking are not profitable, Rub profit: " + rubProfit);
            }
        } else {
            System.out.println(tradeWay.getWayName() + ", Overtaking price is normal");
        }
    }

    private void buySmallSellMainCorrection() {
        OrdersCache cache = tradeWay.getSmallMainCache();
        double firstPrice = cache.getBestBid().getKey().doubleValue();
        double secondPrice = cache.getSecondBid().getKey().doubleValue();

        if (buySmallSellMainPrice == firstPrice &&
                buySmallSellMainPrice - secondPrice > corrLimit * (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + mainCurrency)))) {

            double newBuySmallSellMainPrice = roundPrice(secondPrice + (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + mainCurrency))),
                    smallCurrency + mainCurrency);

            double mainCurr = startMain;
            double baseCurr = startBase;
            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

            mainCurr += -smallAmount * newBuySmallSellMainPrice + mainAmount;
            baseCurr += smallAmount * sellSmallBuyBasePrice - baseAmount;

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");

            synchronized (tradeWay) {
                if (Account.cancelOrder(order)) {
                    Account.newBuyOrder(smallCurrency + mainCurrency, smallAmount, newBuySmallSellMainPrice, this);
                    buySmallSellMainPrice = newBuySmallSellMainPrice;
                    profitLog.info(tradeWay.getWayName() + ", Corrected, Rub profit: " + rubProfit);
                }
            }
        }
    }

    private void sellSmallBuyBaseOvertaking() {
        double mainCurr = startMain;
        double baseCurr = startBase;

        Map.Entry<BigDecimal, BigDecimal> sellSmallBuyBaseAsk = tradeWay.getSmallBaseCache().getBestAsk();

        if (sellSmallBuyBasePrice > sellSmallBuyBaseAsk.getKey().doubleValue()) {
            double newSellSmallBuyBasePrice = roundPrice(sellSmallBuyBaseAsk.getKey().doubleValue() - (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + baseCurrency))),
                    smallCurrency + baseCurrency);

            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.
            mainCurr += -smallAmount * buySmallSellMainPrice + mainAmount;
            baseCurr += smallAmount * newSellSmallBuyBasePrice - baseAmount;

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");

            if (rubProfit > overtPriceLimit) {
                synchronized (tradeWay) {
                    if (Account.cancelOrder(order)) {
                        Account.newSellOrder(smallCurrency + baseCurrency, smallAmount, newSellSmallBuyBasePrice, this);
                        profitLog.info(tradeWay.getWayName() + ", Overtaken, Rub profit: " + rubProfit);
                        sellSmallBuyBasePrice = newSellSmallBuyBasePrice;
                    }
                }
            } else {
                System.out.println(tradeWay.getWayName() + ", Overtaking are not profitable, Rub profit: " + rubProfit);
            }
        } else {
            System.out.println(tradeWay.getWayName() + ", Overtaking price is normal");
        }
    }

    private void sellSmallBuyBaseCorrection() {
        OrdersCache cache = tradeWay.getSmallBaseCache();
        double firstPrice = cache.getBestAsk().getKey().doubleValue();
        double secondPrice = cache.getSecondAsk().getKey().doubleValue();

        if (sellSmallBuyBasePrice == firstPrice &&
                secondPrice - sellSmallBuyBasePrice > corrLimit * (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + baseCurrency)))) {

            double newSellSmallBuyBasePrice = roundPrice(secondPrice - (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + mainCurrency))),
                    smallCurrency + mainCurrency);

            double mainCurr = startMain;
            double baseCurr = startBase;
            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

            mainCurr += -smallAmount * buySmallSellMainPrice + mainAmount;
            baseCurr += smallAmount * newSellSmallBuyBasePrice - baseAmount;

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");

            synchronized (tradeWay) {
                if (Account.cancelOrder(order)) {
                    Account.newSellOrder(smallCurrency + baseCurrency, smallAmount, newSellSmallBuyBasePrice, this);
                    sellSmallBuyBasePrice = newSellSmallBuyBasePrice;
                    profitLog.info(tradeWay.getWayName() + ", Corrected, Rub profit: " + rubProfit);
                }
            }
        }
    }


    private void sellBaseBuyMainOvertaking() {
        double mainCurr = startMain;
        double baseCurr = startBase;

        Map.Entry<BigDecimal, BigDecimal> sellBaseBuyMainAsk = tradeWay.getBaseMainCache().getBestAsk();

        if (sellBaseBuyMainPrice > sellBaseBuyMainAsk.getKey().doubleValue()) {
            double newSellBaseBuyMainPrice = roundPrice(sellBaseBuyMainAsk.getKey().doubleValue() - (1 / Math.pow(10, Currencies.getPriceLimit(baseCurrency + mainCurrency))),
                    baseCurrency + mainCurrency);

            double newMainAmount = baseAmount * newSellBaseBuyMainPrice;

            mainCurr += -smallAmount * buySmallSellMainPrice + newMainAmount;
            baseCurr += smallAmount * sellSmallBuyBasePrice - baseAmount;
            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");

            if (rubProfit > overtPriceLimit) {
                synchronized (tradeWay) {
                    if (Account.cancelOrder(order)) {
                        Account.newSellOrder(baseCurrency + mainCurrency, baseAmount, newSellBaseBuyMainPrice, this);
                        sellBaseBuyMainPrice = newSellBaseBuyMainPrice;
                        mainAmount = newMainAmount;
                        profitLog.info(tradeWay.getWayName() + ", Overtaken, Rub profit: " + rubProfit);
                    }
                }
            } else {
                System.out.println(tradeWay.getWayName() + ", Overtaking are not profitable, Rub profit: " + rubProfit);
            }
        } else {
            System.out.println(tradeWay.getWayName() + ", Overtaking price is normal");
        }
    }

    private void sellBaseBuyMainCorrection() {
        OrdersCache cache = tradeWay.getBaseMainCache();
        double firstPrice = cache.getBestAsk().getKey().doubleValue();
        double secondPrice = cache.getSecondAsk().getKey().doubleValue();

        if (sellBaseBuyMainPrice == firstPrice &&
                secondPrice - sellBaseBuyMainPrice > corrLimit * (1 / Math.pow(10, Currencies.getPriceLimit(smallCurrency + baseCurrency)))) {

            double newSellBaseBuyMainPrice = roundPrice(secondPrice - (1 / Math.pow(10, Currencies.getPriceLimit(baseCurrency + mainCurrency))),
                    baseCurrency + mainCurrency);

            double newMainAmount = baseAmount * newSellBaseBuyMainPrice;

            double mainCurr = startMain;
            double baseCurr = startBase;
            double BNBChange = -2 * Commission.getCommission(smallCurrency, smallAmount) - Commission.getCommission(baseCurrency, baseAmount); //Комиссия вычитается со счета BNB.

            mainCurr += -smallAmount * buySmallSellMainPrice + newMainAmount;
            baseCurr += smallAmount * sellSmallBuyBasePrice - baseAmount;

            rubProfit = (mainCurr - startMain) * Currencies.getRub(mainCurrency) +
                    (baseCurr - startBase) * Currencies.getRub(baseCurrency) +
                    BNBChange * Currencies.getRub("BNB");

            synchronized (tradeWay) {
                if (Account.cancelOrder(order)) {
                    Account.newSellOrder(baseCurrency + mainCurrency, baseAmount, newSellBaseBuyMainPrice, this);
                    sellBaseBuyMainPrice = newSellBaseBuyMainPrice;
                    mainAmount = newMainAmount;
                    profitLog.info(tradeWay.getWayName() + ", Corrected, Rub profit: " + rubProfit);
                }
            }
        }
    }
}
