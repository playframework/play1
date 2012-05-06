package models.orphans.maps;

import play.db.jpa.Model;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity(name = "maps.BaseModel")
@Table(name = "maps_base_model")
public class BaseModel extends Model {

    @OneToMany(mappedBy = "baseModel", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "mapKey")
    public Map<String, LevelOne> levelOneMap = new HashMap<String, LevelOne>();

}
