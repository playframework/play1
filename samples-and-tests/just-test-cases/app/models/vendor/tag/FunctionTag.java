package models.vendor.tag;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value="f")
public class FunctionTag extends Tag {

}
