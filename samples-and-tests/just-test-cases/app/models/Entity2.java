package models;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

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
