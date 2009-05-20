package client;

import com.google.gwt.user.client.rpc.*;
import java.util.*;

public interface StockPriceServiceAsync {

    void getStocks(AsyncCallback<StockPrice[]> callback);
    void addSymbol(String symbol, AsyncCallback<Void> callback);
    void removeSymbol(String symbol, AsyncCallback<Void> callback);

}