package models.horse;

import play.data.validation.*;
import play.db.jpa.Model;
import javax.persistence.*;

@Entity
public class Horse extends Model {
    @OneToOne(cascade = CascadeType.ALL)
    @Valid
    private BLUP blup;

    public BLUP getBlup() {
        return blup;
    }

    public void setBlup(BLUP blup) {
        this.blup = blup;
    }
}
