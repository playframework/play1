package controllers;

import models.*;

import play.*;
import play.mvc.*;

import java.util.*;

public class Tags extends Administration {

	@Before
	static void checkSuper() {
		if(!session.contains("superadmin")) {
			Jobs.list();
		}
	}
    
}

