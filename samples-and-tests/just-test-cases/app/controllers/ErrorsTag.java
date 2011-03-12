package controllers;

import play.mvc.Controller;

public class ErrorsTag extends Controller {

	public static void noError() {
		render("@error");
	}

	public static void errorGeneral() {
		validation.addError("", "general error 1");
		validation.addError("", "general error 2");
		
		render("@error");
	}

	public static void errorField1() {
		validation.addError("", "general error 1");
		validation.addError("", "general error 2");
		validation.addError("field1", "field 1 error 1");
		validation.addError("field1", "field 1 error 2");
		
		render("@error");
	}

	public static void errorField1andField2() {
		validation.addError("", "general error 1");
		validation.addError("", "general error 2");
		validation.addError("field1", "field 1 error 1");
		validation.addError("field1", "field 1 error 2");
		validation.addError("field2", "field 2 error 1");
		validation.addError("field2", "field 2 error 2");
		validation.addError("field2", "field 2 error 3");
		
		render("@error");
	}

	
}
