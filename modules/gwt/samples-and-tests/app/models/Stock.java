package models;

import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

import client.*;

@Entity
public class Stock extends Model {
    
    final double MAX_PRICE = 100.0; // $100.00
    final double MAX_PRICE_CHANGE = 0.02; // +/- 2%
    
    public String symbol;
    public double price;
    public double change;
    
    public Stock(String symbol) {
        this.symbol = symbol;
        this.randomlyUpdatePrice();
    }
    
    public double getChangePercent() {
        return 100.0 * this.change / this.price;
    }
    
    public StockPrice asStockPrice() {
        StockPrice stockPrice = new StockPrice();
        stockPrice.symbol = this.symbol;
        stockPrice.price = this.price;
        stockPrice.change = this.change;
        return stockPrice;
    }
    
    public void randomlyUpdatePrice() {
        Random rnd = new Random();        
        this.price = rnd.nextDouble() * MAX_PRICE;
        this.change = price * MAX_PRICE_CHANGE * (rnd.nextDouble() * 2.0 - 1.0);
        this.save();
    }
    
    public String toString() {
        return symbol;
    }
    
}

