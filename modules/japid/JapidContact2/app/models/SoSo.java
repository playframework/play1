/**
 * 
 */
package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;

/**
 * @author bran
 *
 */
@Entity
public class SoSo extends Model {
	public String name;
	
	@ManyToOne
	public What what2;
}
