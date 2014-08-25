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
public class What extends Model {
	public String name;
	public String name3;
	@ManyToOne
	public WhatWhat ww;
}
