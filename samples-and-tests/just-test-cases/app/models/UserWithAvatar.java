package models;

import play.*;
import play.db.jpa.*;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class UserWithAvatar extends Model {

    public String username;
    public Blob avatar;

}

