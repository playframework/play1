package services;

import com.google.gwt.user.server.rpc.*;
import java.util.*;
import play.modules.gwt.*;
import play.*;

import client.*;
import models.*;

@GWTServicePath("/main/stockPrices")
public class StockPriceServiceImpl extends GWTService implements StockPriceService {

    public StockPrice[] getStocks() {
        List<Stock> stocks = Stock.findAll();
        StockPrice[] prices = new StockPrice[stocks.size()];
        for(int i=0; i<stocks.size(); i++) {
            prices[i] = stocks.get(i).asStockPrice();
        }        
        return prices;        
    }
    
    public void addSymbol(String symbol) {
        Stock stock = Stock.find("bySymbol", symbol).one();
        if(stock == null) {
            stock = new Stock(symbol);
            stock.save();
        }
    }
    
    public void removeSymbol(String symbol) {
        Stock stock = Stock.find("bySymbol", symbol).one();
        if(stock != null) {
            stock.delete();
        }
    }

}