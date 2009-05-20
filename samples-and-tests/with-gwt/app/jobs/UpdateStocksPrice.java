package jobs;

import java.util.*;
import play.jobs.*;
import play.*;

import models.*;

@On("0/5 * * * * ?")
public class UpdateStocksPrice extends Job {

    public void doJob() {
        Logger.info("Updating stocks price");
        List<Stock> stocks = Stock.findAll();
        for(Stock stock : stocks) {
            stock.randomlyUpdatePrice();
        }
    }
    
}

