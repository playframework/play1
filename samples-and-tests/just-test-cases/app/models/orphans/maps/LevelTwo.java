package models.orphans.maps;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity(name = "maps.LevelTwo")
@Table(name = "maps_level_two")
public class LevelTwo extends Model {

    @ManyToOne
    public LevelOne levelOne;
    
    public String mapKey;

}
