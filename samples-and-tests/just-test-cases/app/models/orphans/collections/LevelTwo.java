package models.orphans.collections;

import play.db.jpa.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "collections.LevelTwo")
@Table(name = "collections_level_two")
public class LevelTwo extends Model {

    @ManyToOne
    public LevelOne levelOne;

}
