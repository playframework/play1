package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
public class Municipality extends Model {
    @Required
    public String name;
    @OneToMany(mappedBy="municipality")
    public List<Neighborhood> neighborhoods = new ArrayList<Neighborhood>();

    public String toString() {
        return name;
    }
}
