package binance.ways;

import binance.Main;
import binance.utils.Account;
import binance.utils.OrderListener;
import binance.utils.OrdersCache;
import com.binance.api.client.domain.account.NewOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class TradeWay extends Thread implements OrderListener {
    //private double ETH = 0;
    //private double BNB = 0;
    protected static Logger profitLog = LoggerFactory.getLogger("profit");
    protected static Logger orderLog = LoggerFactory.getLogger("order");

    protected String smallCurrency, baseCurrency, mainCurrency;

    private List<NewOrderResponse> orders = new ArrayList<>();

    protected OrdersCache smallMainCache;
    protected OrdersCache smallBaseCache;
    protected OrdersCache baseMainCache;

    protected boolean isPaused = false;
    protected boolean isActive = true;
    protected boolean isForwardingActive = false;

    protected boolean isFinished = false;

    protected long startTime = System.currentTimeMillis(), finishTime; // = System.currentTimeMillis();


    protected double profitLimit = 7;

    private final Updater updater = new Updater(); //Пересоздает кэши каждые полчаса, иначе отваливаются сокеты.

    private double rubProfit = 0;

    protected String wayName;
    private final TradeWayPair pair;

    public TradeWay(String smallCurrency, String baseCurrency, String mainCurrency, TradeWayPair pair) {
        this.smallCurrency = smallCurrency;
        this.baseCurrency = baseCurrency;
        this.mainCurrency = mainCurrency;
        this.pair = pair;
        smallMainCache = new OrdersCache(smallCurrency + mainCurrency);
        smallBaseCache = new OrdersCache(smallCurrency + baseCurrency);
        baseMainCache = new OrdersCache(baseCurrency + mainCurrency);
    }

    @Override
    public void run() {
        while (isActive) {
            if (!isPaused) {
                if (!isFinished) {
                    synchronized (pair) { // Чтобы пути в обе стороны успевали обновить баланс перед высчитыванием профита.
                        synchronized (updater) { // Чтобы пересоздание кэшей и вычисления происходили в разное время.
                            calculateProfit(); //TODO Разделить вычисление профита и покупки.
                            trySleep(1000);
                        }
                    }
                    trySleep(500);
                }
            } else {
                trySleep(10000);

                System.out.println(wayName + ", Waiting for orders");

                synchronized (this) {  //Синхронизация на пути, чтобы добавления в список и удаления из них происходили в разное время.
                    orders.removeIf((NewOrderResponse order) -> Account.isOrderFilled(order)); // Удаление из списка исполнившихся ордеров.
                    if (!isForwardingActive)
                        orders.removeIf((NewOrderResponse order) -> Account.isOrderCanceled(order)); // Удаление из списка отмененных ордеров ордеров, если обгона нет.

                    // Запуск "обгона" - переставления ордера на переднюю позицию
                    if (orders.size() == 1 && !isForwardingActive && !Account.isOrderPartiallyFilled(orders.get(0))) { //Если ордер частично выполнен, обгон не начинается.
                        isForwardingActive = true;
                        startOvertaking(orders.get(0));
                    }
                }

                if (orders.size() == 0) {
                    isPaused = false;
                    finishTime = System.currentTimeMillis();
                    double min = (finishTime - startTime) / 1000 / 60;
                    String minutes = String.format(Locale.US, "%.1f", min);
                    profitLog.info(wayName + ", End of waiting. Time  of work: " + minutes + "min, Rub profit: " + rubProfit);
                    Main.addProfit(rubProfit);     //Добавляем полученный профит к общему в классе Main для отображения.
                    if (isFinished)
                        isActive = false;
                }

            }
        }
        profitLog.info(wayName + ", Finished");
    }

    protected abstract void calculateProfit(); // Должна вычислять профит и проводить путь, если профит удовлетворяет условиям.

    protected abstract void startOvertaking(NewOrderResponse orderResponse); // Должна создавать наследник класса Overtaking и передавать ему необходимые параметры.

    public void endForwarding(double rubProfit) {
        isForwardingActive = false;
        this.rubProfit = rubProfit;
        profitLog.info(wayName + ", End of overtaking. Rub profit: " + rubProfit);
    }


    protected void trySleep(int mls) {
        try {
            sleep(mls);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void addOrder(NewOrderResponse orderResponse) {
        synchronized (this) {
            orders.add(orderResponse);
        }
    }


    private class Updater extends Thread {

        public Updater() {
            this.start();
        }

        @Override
        public void run() {
            while (isActive) {
                try {
                    sleep(1000 * 60 * 45); //45 минут
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (updater) {
                    smallMainCache = new OrdersCache(smallCurrency + mainCurrency);
                    smallBaseCache = new OrdersCache(smallCurrency + baseCurrency);
                    baseMainCache = new OrdersCache(baseCurrency + mainCurrency);
                    profitLog.info(wayName + ", Кэши обновлены.");
                }
            }

        }
    }

    public void finish() {
        isFinished = true;
        if (!isPaused)
            isPaused = true;
    }

    public OrdersCache getSmallMainCache() {
        return smallMainCache;
    }

    public OrdersCache getSmallBaseCache() {
        return smallBaseCache;
    }

    public OrdersCache getBaseMainCache() {
        return baseMainCache;
    }

    public String getWayName() {
        return wayName;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public double getRubProfit() {
        return rubProfit;
    }

    public void setRubProfit(double rubProfit) {
        this.rubProfit = rubProfit;
    }

    @Override
    public String toString() {
        return getWayName();
    }
}
