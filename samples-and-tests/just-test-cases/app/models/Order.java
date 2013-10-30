package models;

import play.db.jpa.Model;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity(name="customer_order")
public class Order extends Model {
    public String product;
    public Integer items;

    @ManyToOne
    public Customer customer;
    
    public Order() {}

    public Order(String product, Integer items) {
        this.product = product;
        this.items = items;
    }
}
