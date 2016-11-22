package models.orphans.collections;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Model with preinitialized collection (e.g. one element)
 *
 */
@Entity(name = "collections.DefaultModel")
@Table(name = "collections_default_model")
public class DefaultModel extends BaseModel {

	public DefaultModel() {
		super();
		initLevelOnes();
	}

	private void initLevelOnes() {
		LevelOne levelOne = new LevelOne();
		levelOne.baseModel = this;
		levelOnes.add(levelOne);
	}

}
