package models.vendor;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;

import models.vendor.tag.Tag;
import play.db.jpa.Model;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class Vendor extends Model {

    @ManyToMany(cascade=CascadeType.PERSIST)
    public Set<Tag> tags;

}
