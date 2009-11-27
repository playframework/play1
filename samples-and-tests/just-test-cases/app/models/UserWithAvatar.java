package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class UserWithAvatar extends Model {
    
    public String username;
    
    @Embedded
    public FileAttachment avatar;
    
}

