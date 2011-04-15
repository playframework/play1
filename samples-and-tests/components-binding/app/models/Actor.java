package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import models.Address;
import play.db.jpa.Model;

@Entity
public class Actor extends Model {
	public String name;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	public List<Address> addresses;

	@ManyToMany
	public List<Movie> movies;
}
