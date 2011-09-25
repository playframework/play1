package models.vendor.tag;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.Model;

@Entity(name="VendorTag")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="tag_type", discriminatorType=DiscriminatorType.STRING)
public abstract class Tag extends Model {

    @Required
    @MaxSize(10)
    @Column(nullable=false, unique=true, length=10)
    public String label;

}
