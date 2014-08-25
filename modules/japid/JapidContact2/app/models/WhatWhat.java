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
public class WhatWhat extends Model {
	public String name;
	public String name2;
	private Dept dept;
	/**
	 * @return the dept
	 */
	public Dept getDept() {
		return dept;
	}
	/**
	 * @param dept the dept to set
	 */
	@ManyToOne
	public void setDept(Dept dept) {
		this.dept = dept;
	}
	
}
