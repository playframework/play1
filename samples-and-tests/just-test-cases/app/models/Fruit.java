package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
public class Fruit extends Model {
    @Required
    public String name;
    @ManyToMany
    public List<Farmer> farmers = new ArrayList<Farmer>();

    public String toString() {
        return name;
    }
}
