package models.vendor.tag;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(value="f")
public class FunctionTag extends Tag {

}
