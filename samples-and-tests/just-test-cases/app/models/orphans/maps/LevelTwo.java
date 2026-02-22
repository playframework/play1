package models.orphans.maps;

import play.db.jpa.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "maps.LevelTwo")
@Table(name = "maps_level_two")
public class LevelTwo extends Model {

    @ManyToOne
    public LevelOne levelOne;
    
    public String mapKey;

}
