package client;

import java.io.*;

/**
 * Something like a DTO
 */
public class StockPrice implements Serializable {
    
    public String symbol;
    public double price;
    public double change;
    
    public double getChangePercent() {
        return 100.0 * this.change / this.price;
    }
    
}

