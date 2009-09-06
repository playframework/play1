package models;
 
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
 
@Entity
public class Tag extends Model {
 
    public String name;
    
    public static Tag findOrCreateByName(String name) {
        Tag tag = Tag.find("byName", name).one();
        if(tag == null) {
            tag = new Tag().save();
            tag.name = name;
        }
        return tag;
    }
    
    public static List<Map> getCloud() {
        List<Map> result = Tag.find(
            "select new map(t.name as tag, count(p.id) as pound) from Post p join p.tags as t group by t.name"
        ).fetch();
        return result;
    }
    
    public String toString() {
        return name;
    }
 
}