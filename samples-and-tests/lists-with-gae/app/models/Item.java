package models;

import siena.*;

public class Item extends Model {

    @Id
    public Long id;
    
    public String label;
    public boolean done;
    public int position;
    
    @Index("list_index")
    public List list;
    
    public Item(List list, String label) {
        this.label = label;
        this.list = list;
        this.done = done;
        this.position = list.nextPosition++;
    }
    
    static Query<Item> all() {
        return Model.all(Item.class);
    }
    
    public static Item findById(Long id) {
        return all().filter("id", id).get();
    }
    
    public String toString() {
        return label;
    }
    
}

