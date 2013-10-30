package models.orphans.maps;

import play.db.jpa.Model;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity(name = "maps.LevelOne")
@Table(name = "maps_level_one")
public class LevelOne extends Model {
    
    @ManyToOne
    public BaseModel baseModel;
    
    public String mapKey;

    @OneToMany(mappedBy = "levelOne", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "mapKey")
    public Map<String, LevelTwo> levelTwoMap = new HashMap<String, LevelTwo>();

}
