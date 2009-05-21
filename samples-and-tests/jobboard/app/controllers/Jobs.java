package controllers;

import models.*;

import play.*;
import play.mvc.*;

import java.util.*;

public class Jobs extends Administration {

	public static void list() {
		if (session.contains("superadmin")) {
			parent();
		}
		String where = "company.email = '" + session.get("company") + "'";
		parent(where);
	}
	
	public static void show(Long id) {
		if (!((Job)Job.findById(id)).company.email.equals(session.get("company")) && !session.contains("superadmin")) {
			forbidden();
		}
		parent();
	}
	
	public static void create() {
		if(session.contains("superadmin")) {
			parent();
		}
		Company company = Company.find("byEmail", session.get("company")).one();
		params.put("object.company", company.id + "");
		parent();
	}
	
	public static void save(Long id) {
		if(session.contains("superadmin")) {
			parent();
		}
		Company company = Company.find("byEmail", session.get("company")).one();
		params.put("object.company", company.id + "");
		parent();
	}
    
	
}

