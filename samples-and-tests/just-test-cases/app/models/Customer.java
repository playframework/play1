package models;

import play.db.jpa.Model;
import javax.persistence.*;
import java.util.*;

@Entity
public class Customer extends Model {
    public String name;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    public List<Order> orders;

    public Customer() {}

    public Customer(String name) {
        this.name = name;
        this.orders = new ArrayList();
    }
}
