import org.junit.*;
import play.test.*;

import models.*;

public class StockTest extends UnitTest {
    
    @Before
    public void loadData() {
        Fixtures.deleteAll();
        Fixtures.load("stocks.yml");
    }

    @Test
    public void count() {
        assertEquals(3, Stock.count());
    }
    
    @Test
    public void checkFind() {
        assertNotNull(Stock.find("bySymbol", "DCE").one());
    }
    
    @Test
    public void checkUpdate() {
        Stock stock = Stock.find("bySymbol", "DCE").one();
        assertNotNull(stock);
        double price = stock.price;
        double change = stock.change;
        assertNotNull(price);
        assertNotNull(change);
        pause(6000);
        clearJPASession();
        stock = Stock.find("bySymbol", "DCE").one();
        assertFalse(price == stock.price);
        assertFalse(change == stock.change);
    }
    
}

