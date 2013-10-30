package models;

import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Tag extends Model {

    @Required
    public String label;
    
    @Required
    public String code;

    public static List<Tag> findByCategory(String category) {
        if (category == null) {
            return Tag.find("select distinct t from Tag t, Job j where t member of j.tags").fetch();
        }
        return Tag.find("select distinct t from Tag t, Job j where j.category.code = ?1 and t member of j.tags and j.online = true", category).fetch();
    }

    public static Tag findByCode(String code) {
        return Tag.find("byCode", code).first();
    }

    public String toString() {
        return label;
    }
    
}

