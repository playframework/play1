package models;

import play.*;
import play.db.jpa.*;
import javax.persistence.*;
import java.util.*;

@Entity
public class PropertyEnhancerModel extends Model {

    public String text;

    public String getText() {
        return text;
    }
}

