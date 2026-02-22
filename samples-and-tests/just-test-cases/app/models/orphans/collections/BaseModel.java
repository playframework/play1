package models.orphans.collections;

import play.db.jpa.Model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
