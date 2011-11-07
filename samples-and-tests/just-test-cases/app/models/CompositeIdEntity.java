package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
