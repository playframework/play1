package controllers;

import play.mvc.*;
import play.db.jpa.*;

import models.*;


@play.db.jpa.NoTransaction
public class Transactional2 extends Controller {

	//This should be excluded from any transactions.
	public static void disabledTransactionTest() {
		renderText("isInsideTransaction: " + JPA.isInsideTransaction());
	}
	
}

