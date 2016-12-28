package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
public class Neighborhood extends Model {
    @Required
    public String name;
    @ManyToOne
    public Municipality municipality = null;

    public String toString() {
        return name;
    }
}
