package models.orphans.collections;

import play.db.jpa.Model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "collections.BaseModel")
@Table(name = "collections_base_model")
public class BaseModel extends Model {

    @OneToMany(mappedBy = "baseModel", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<LevelOne> levelOnes = new ArrayList<LevelOne>();
    
    @ManyToOne
    public LevelOne parent;
}
