package models.vendor.tag;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value="a")
public class AreaTag extends Tag {

}
