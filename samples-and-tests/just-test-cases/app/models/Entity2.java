package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;

@Entity
public class Entity2 extends Model{
    public String a;
    public Boolean b;
    public int c;
    
    @ManyToOne(fetch = FetchType.LAZY)
    public City city;

    @ManyToMany(fetch = FetchType.LAZY)
    public List<City> cities;

}
