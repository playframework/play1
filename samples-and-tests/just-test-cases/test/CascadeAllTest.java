import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;

public class CascadeAllTest extends UnitTest {
    @Before
    public void setup() {
        Fixtures.deleteAll();
        Fixtures.load("cascade-all.yml");
    }

    @Test
    public void insert() {
        Customer frank = new Customer("frank");
        frank.orders.add(new Order("coffee", 2));
        frank.orders.add(new Order("tea", 1));
        frank.save();
        assertEquals(2, Customer.count());
        assertEquals(4, Order.count());
        Customer actual = Customer.find("byName", "frank").first();
        assertEquals(frank, actual);
    }
    
    @Test
    public void update() {
        Customer bob = Customer.find("byName", "bob").first();
        bob.orders.clear();
        bob.orders.add(new Order("ice cream", 3));
        bob.save();
        assertEquals(1, Customer.count());
        assertEquals(1, Order.count());
        Customer actual = Customer.find("byName", "bob").first();
        assertEquals(bob, actual);
    }

    @Test
    public void delete() {
        Customer bob = Customer.find("byName", "bob").first();
        bob.delete();
        assertEquals(0, Customer.count());
        assertEquals(0, Order.count());
    }
}
