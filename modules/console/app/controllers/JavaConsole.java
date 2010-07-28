package controllers;

import play.*;
import play.mvc.*;
import bsh.Interpreter;
import bsh.EvalError;

import java.io.*;

@Check("isAdmin")
@With(Secure.class)
public class JavaConsole extends Controller {

    public static void index(String script) {
	renderArgs.put("action","/console/java");
	if(request.method == "POST" && script != null) {
		try {
			Interpreter i = new Interpreter();
			utils.InterpreterHelper.out.set(new java.util.ArrayList<String>());    	
			i.eval("static import utils.InterpreterHelper.*;\n"+script);
			renderArgs.put("results",utils.InterpreterHelper.out.get());
		} catch (EvalError ex) {
			System.out.println(ex);
			ex.printStackTrace();
			renderArgs.put("trace",getStackTrace(ex));
			renderArgs.put("error",ex);
		}	
	} else script = "println(\"hello java!\");";
        render("/console/repl.html",script);
    }

    private static String getStackTrace(Throwable aThrowable) {
    	final Writer result = new StringWriter();
    	final PrintWriter printWriter = new PrintWriter(result);
    	aThrowable.printStackTrace(printWriter);
    	return result.toString();
  }
}
