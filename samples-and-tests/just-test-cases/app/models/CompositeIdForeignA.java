package models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import play.db.jpa.Model;

@Entity
public class CompositeIdForeignA extends Model {
	@OneToMany(mappedBy = "compositeIdForeignA", fetch = FetchType.LAZY)
	public Set<CompositeIdEntity> a2Bs = new HashSet<CompositeIdEntity>();
	
	public String testId;
}
