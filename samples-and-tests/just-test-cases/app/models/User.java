package models;

import play.data.binding.As;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

public class User {
	
	public User(String name) {
		this.name = name;
	}
	
	public User() {
	}
    
    public String name;
    public Boolean b;
    public boolean c;
    public Integer i;
    public int j;
    public long l;
    public Long k;

	@Required
    @As("dd/MM/yyyy")
    public Date birth;

    @Email
    public String email;
    
    @Valid
    @Transient
    public List<MyAddress> addresses;

	public String toString() {
		return name;
	}
	
	public static String yip() {
		return "YIP";
	}
    
}

