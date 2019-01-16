package binance.overtaking;

import binance.utils.Account;
import binance.utils.OrderListener;
import binance.ways.TradeWay;
import com.binance.api.client.domain.account.NewOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Overtaking extends Thread implements OrderListener {
    protected String mode;
    protected boolean isActive = true;
    protected String smallCurrency, baseCurrency, mainCurrency;

    protected static Logger profitLog = LoggerFactory.getLogger("profit");

    protected NewOrderResponse order;
    protected final TradeWay tradeWay;

    protected final static double corrLimit = 5;
    protected final static double overtPriceLimit = -10;

    protected double rubProfit = 0;

    public Overtaking(TradeWay tradeWay) {
        this.tradeWay = tradeWay;
    }

    public void setMode(String mode, NewOrderResponse orderResponse) {
        this.mode = mode;
        order = orderResponse;
    }

    public void setCurrencies(String smallCurrency, String baseCurrency, String mainCurrency) {
        this.smallCurrency = smallCurrency;
        this.baseCurrency = baseCurrency;
        this.mainCurrency = mainCurrency;
    }

    @Override
    public void addOrder(NewOrderResponse orderResponse) {
        order = orderResponse;
        synchronized (tradeWay) {
            tradeWay.addOrder(order);
        }
    }

    @Override
    public void run() {
        profitLog.info(tradeWay.getWayName() + ", Overtaking Started");
        while (isActive) {
            if (Account.isOrderPartiallyFilledOrFilled(order)) {
                isActive = false;
                System.out.println(tradeWay.getWayName() + ", Overtaking order filled or partially filled, overtaking stopped");
                break;
            }

            overtake();
            trySleep(10000);
            correct();
            trySleep(10000);
            tradeWay.setRubProfit(rubProfit);
        }
        tradeWay.endForwarding(rubProfit);
    }

    /*
     * overtake - обгон чужих ордеров для постановки своего ордера на первое место.
     * correct - если наш ордер стоит первым, а цена второго ордера в стакане сильно отличается от его цены,
     * то корректируем цену ближе ко второму ордеру. Это приносит дополнительную прибыль и частично спасает от гонок с другими ботами.
     */
    protected abstract void overtake();

    protected abstract void correct();

    private void trySleep(int mls) {
        try {
            sleep(mls);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
