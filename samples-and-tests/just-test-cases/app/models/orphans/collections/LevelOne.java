package models.orphans.collections;

import play.db.jpa.Model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "collections.LevelOne")
@Table(name = "collections_level_one")
public class LevelOne extends Model {

    @ManyToOne
    public BaseModel baseModel;

    @OneToMany(mappedBy = "levelOne", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<LevelTwo> levelTwos = new ArrayList<LevelTwo>();

}
