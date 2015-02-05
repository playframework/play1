package models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import play.data.binding.As;
import play.data.binding.NoBinding;
import play.db.jpa.*;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class AnEntity extends Model {
	
    @As("yyyy-MM-dd hh:mm:ss:SSS a")
    public Date date;
    
    @As(binder=utils.TestBinder.class) 
    public String yop;
    
    @As(unbinder=utils.TestBinder.class) 
    public String yap;
    
    @As(binder=utils.TestBinder.class, unbinder=utils.TestUnbinder.class) 
    public String yip;
    
    @OneToMany
    public List<AnotherEntity> children = new ArrayList<AnotherEntity>();
    
    @ManyToOne
    public AnotherEntity parent;
    
    public String getFormattedDate(){
	if(date != null){
	    SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS a");   
	    return df.format(date);
	}
	return "";
    }
    
}
