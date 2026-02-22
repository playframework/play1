package models;

import javax.persistence.*;
import play.db.jpa.*;

@Entity
public class World extends GenericModel {

    @Id
    public Integer id;

    public Integer randomNumber;

}
