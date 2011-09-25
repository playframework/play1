package models.vendor;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;

import models.vendor.tag.Tag;
import play.db.jpa.Model;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class Vendor extends Model {

    @ManyToMany(cascade=CascadeType.PERSIST)
    public Set<Tag> tags;

}
