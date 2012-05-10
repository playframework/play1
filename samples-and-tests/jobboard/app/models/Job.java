package models;

import java.util.*;

import play.db.jpa.*;
import play.data.validation.*;
import play.templates.*;
import play.mvc.Scope.*;

import javax.persistence.*;

@Entity
public class Job extends Model {

    @Required
    public String title;
    
    public String place;
    
    public Date postedAt = new Date();
    
    @Required
    @MaxSize(20000)
    @Lob
    public String detail;
    
    @Lob
    // Rename to avoir mysql being lost
    @Column(name = "jobboard_index")
    public String index;
    
    @ManyToOne
    @Required
    public Category category;
    
    @ManyToOne
    @Required
    public Company company;
    
    @ManyToMany
    public List<Tag> tags;
    
    public Boolean online = false;

    public static List findByCategoryAndTags(String category, String[] tags) {
        if (tags == null) {
            tags = new String[0];
        }
        if (category == null && tags.length == 0) {
            return find("from Job where online = true order by postedAt DESC").fetch();
        }
        if (tags.length == 0) {
            return find("category.code = ? and online = true order by postedAt DESC", category).fetch();
        }
        String query = "select job from Job job where job.online = true and " + (category != null ? "job.category.code = '" + category + "' and " : "");
        Object[] tagEntities = new Tag[tags.length];

        int i = 1;
        for (String tag : tags) {
            tagEntities[i - 1] = Tag.findByCode(tag);
            query += ((i != 1) ? " and " : "") + "?" + i + " member of job.tags";
            i++;
        }
        query += " order by job.postedAt DESC";
        return find(query, tagEntities).fetch();
    }

    public static List search(String by) {
        return find("from Job job where job.index like ?1 and job.online = true", JavaExtensions.noAccents("%" + by.toLowerCase() + "%")).fetch();
    }

    public String toString() {
        return title;
    }
    
    // ~~~~~~ Indexation
    
    @PreUpdate
    @PrePersist
    void index() {
        this.index = JavaExtensions.noAccents(this.company == null ? "" : this.company.name).toLowerCase() + " ";
        this.index += JavaExtensions.noAccents(this.title).toLowerCase() + " ";
        this.index += JavaExtensions.noAccents(this.detail).toLowerCase() + " ";
        this.index += JavaExtensions.noAccents(this.place).toLowerCase();
    }
    
}

