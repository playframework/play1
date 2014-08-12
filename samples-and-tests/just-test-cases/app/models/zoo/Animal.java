package models.zoo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PreRemove;

import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class Animal extends Model {

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.EAGER , orphanRemoval = true)
    public List<Meal> meals;

    public Animal() {
	this.meals = new ArrayList<Meal>();
	meals.add(new Meal());
    }
    
    @PreRemove
    protected void clean() {
	// hack to force removal of activity instances, otherwise deleting the
	// tiers instance fails because the join table still references
	// the activity instances that have not been correctly deleted =>
	// Hibernate bug that seems to be fixed in Hibernate 4.2.7 and 4.3
	// see https://hibernate.atlassian.net/browse/HHH-6484
	final String cn = this.getClass().getSimpleName();
	final String query = String.format("DELETE FROM %s_%s WHERE %s_id=%s",
		cn, Meal.class.getSimpleName(), cn, id);
	//JPA.em().createNativeQuery(query).executeUpdate(); // but this hack
	// causes a StackOverflowException in Play 1.3RC1
    }

}
