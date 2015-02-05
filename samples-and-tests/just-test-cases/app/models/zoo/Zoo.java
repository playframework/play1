package models.zoo;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

import play.db.jpa.Model;

@Entity
public class Zoo extends Model {

    @OneToOne(cascade = CascadeType.ALL, fetch=FetchType.EAGER, orphanRemoval = true)
    public Animal lion;

    public Zoo() {
    }

}
