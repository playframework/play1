package controllers;

import play.*;
import play.mvc.*;
import utils.ResultSetMapper;
import play.db.DB;
import java.io.*;

@Check("isAdmin")
@With(Secure.class)
public class DbConsole extends Controller {

	public static void index(String query) {
	    try {
	      renderArgs.put("connection",DB.getConnection());
	    } catch (Exception ex)  {}
	    if(request.method == "POST" && query != null) {
	      try {
		renderArgs.put("results",ResultSetMapper.toList(DB.executeQuery(query)));
	      } catch (Exception ex){
		renderArgs.put("error",ex);
	      }
	    }
	    renderArgs.put("query",query);
	    render("/console/db.html");
	 }
}
