package models;

import java.util.List;

public class Category {
	public String name;
	public String subname;
	public List<Category> subCategories;
	
	public String n() {
		return "name-: " + name; 
//				+ ". sub name: " + subname;
	}
}
