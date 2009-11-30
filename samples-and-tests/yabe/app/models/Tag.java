package models;
 
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;
 
@Entity
public class Tag extends Model implements Comparable<Tag> {
 
    @Required
    public String name;
    
    private Tag(String name) {
        this.name = name;
    }
    
    public static Tag findOrCreateByName(String name) {
        Tag tag = Tag.find("byName", name).first();
        if(tag == null) {
            tag = new Tag(name);
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
    
    public int compareTo(Tag otherTag) {
        return name.compareTo(otherTag.name);
    }
 
}