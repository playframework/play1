package models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import play.db.jpa.GenericModel;

@Entity
@IdClass(CompositeIdPk.class)
public class CompositeIdEntity extends GenericModel {
	@Id
	@JoinColumn(name = "a_id")
	@ManyToOne
	public CompositeIdForeignA compositeIdForeignA;

	@Id
	@JoinColumn(name = "b_id")
	@ManyToOne
	public CompositeIdForeignB compositeIdForeignB;

}
