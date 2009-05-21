package controllers;

import models.*;

import play.*;
import play.mvc.*;

import java.util.*;

@CRUD.For(Category.class)
public class Categories extends Administration {

	@Before
	static void checkSuper() {
		if(!session.contains("superadmin")) {
			Jobs.list();
		}
	}
    
}

