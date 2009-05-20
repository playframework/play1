package client;

import com.google.gwt.user.client.rpc.*;

@RemoteServiceRelativePath("stockPrices")
public interface StockPriceService extends RemoteService {

    StockPrice[] getStocks();
    void addSymbol(String symbol);
    void removeSymbol(String symbol);
    
}