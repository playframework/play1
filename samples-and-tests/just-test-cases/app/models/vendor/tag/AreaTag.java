package models.vendor.tag;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(value="a")
public class AreaTag extends Tag {

}
