/**
 * 
 */
package models;

import javax.persistence.Column;
import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * @author bran
 *
 */
@Entity
public class Dept extends Model {
	@Column(name="dept_name")
	public String name1;
	
}
