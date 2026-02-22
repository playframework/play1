package models.zoo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;

import play.db.jpa.Model;

@Entity
public class Zoo extends Model {

    @OneToOne(cascade = CascadeType.ALL, fetch=FetchType.EAGER, orphanRemoval = true)
    public Animal lion;

    public Zoo() {
    }

}
