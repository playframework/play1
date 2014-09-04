package controllers;

import play.*;
import play.libs.IO;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import models.*;

public class WSApplication extends Controller {

    public static void index() {
	String url = "http://google.com";
	HttpResponse response = WS.url(url).post();

	InputStream is = response.getStream();
	String resp1 = IO.readContentAsString(is, response.getEncoding());

	try {
	    is.reset();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	String resp2 = IO.readContentAsString(is, response.getEncoding());

	renderArgs.put("resp1", resp1);
	renderArgs.put("resp2", resp2);
	render();
    }

    public static void index2() {
	String url = "http://google.com";
	HttpResponse response = WS.url(url).post();

	String resp1 = response.getString();
	String resp2 = response.getString();

	renderArgs.put("resp1", resp1);
	renderArgs.put("resp2", resp2);
	renderTemplate("WSApplication/index.html");
    }
}