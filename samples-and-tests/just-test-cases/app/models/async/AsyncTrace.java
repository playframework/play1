package models.async;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class AsyncTrace extends Model {
    
    public Long position;

    public String content;

}