package binance.utils;

import com.binance.api.client.domain.account.NewOrderResponse;

public interface OrderListener {

    void addOrder(NewOrderResponse orderResponse);

}
