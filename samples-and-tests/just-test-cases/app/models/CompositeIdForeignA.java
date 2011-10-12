package models;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import play.db.jpa.Model;

@Entity
public class CompositeIdForeignA extends Model {
	@OneToMany(mappedBy = "compositeIdForeignA", fetch = FetchType.LAZY)
	public Set<CompositeIdEntity> a2Bs = new HashSet<CompositeIdEntity>();
	
	public String testId;
}
