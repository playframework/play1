package models;

import play.db.jpa.*;
import play.data.validation.*;
import javax.persistence.*;
import java.util.*;
import java.text.*;
import java.math.*;

@Entity
public class Booking extends Model {
    
    @Required
    @ManyToOne
    public User user;
    
    @Required
    @ManyToOne
    public Hotel hotel;
    
    @Required
    @Temporal(TemporalType.DATE) 
    public Date checkinDate;
    
    @Required
    @Temporal(TemporalType.DATE)
    public Date checkoutDate;
    
    @Required(message="Credit card number is required")
    @Match(value="^\\d{16}$", message="Credit card number must be numeric and 16 digits long")
    public String creditCard;
    
    @Required(message="Credit card name is required")
    @MinSize(value=3, message="Credit card name is required")
    @MaxSize(value=70, message="Credit card name is required")
    public String creditCardName;
    public int creditCardExpiryMonth;
    public int creditCardExpiryYear;
    public boolean smoking;
    public int beds;

    public Booking(Hotel hotel, User user) {
        this.hotel = hotel;
        this.user = user;
    }
   
    public BigDecimal getTotal() {
        return hotel.price.multiply( new BigDecimal( getNights() ) );
    }

    public int getNights() {
        return (int) ( checkoutDate.getTime() - checkinDate.getTime() ) / 1000 / 60 / 60 / 24;
    }

    public String getDescription() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return hotel==null ? null : hotel.name + 
            ", " + df.format( checkinDate ) + 
            " to " + df.format( checkoutDate );
    }

    public String toString() {
        return "Booking(" + user + ","+ hotel + ")";
    }

}
