package models.vendor.tag;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

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
