package models;

import java.util.*;
import siena.*;

public class List extends Model {

    @Id
    public Long id;
    
    public String user;
    public String name;
    public String notes;
    public int nextPosition;
    
    @Filter("list")
    public Query<Item> items;
    
    public List(String user, String name) {
        this.user = user;
        this.name = name;
        this.notes = "";
        this.nextPosition = 0;
    }
    
    static Query<List> all() {
        return Model.all(List.class);
    }
    
    public static List findById(Long id) {
        return all().filter("id", id).get();
    }
    
    public static Collection<List> findByUser(String user) {
        return all().filter("user", user).fetch();
    }
    
    public Collection<Item> items() {
        return items.filter("done", false).order("position").fetch();
    }
    
    public Collection<Item> oldItems() {
        return items.filter("done", true).order("-position").fetch();
    }
    
    public String toString() {
        return name;
    }
    
}

