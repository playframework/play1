package models;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class UserId implements Serializable {

    public String firstName;
    public String lastName;

}