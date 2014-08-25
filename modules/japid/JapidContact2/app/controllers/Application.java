package controllers;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import models.Contact;
import play.data.validation.Valid;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import play.mvc.results.Error;
import cn.bran.play.JapidController;

public class Application extends JapidController {

	public static void index() {
		Date now = new Date();
		renderJapid(now);
	}

	public static void list() {
		// List<Contact> contacts = Contact.findAll();
		// // List<Contact> contacts = JPA.find(Contact.class).fetch();
		// contacts = contacts.stream().sorted((b, a) ->
		// a.name.compareTo(b.name)).collect(Collectors.toList());
		List<Contact> contacts = Contact.find("order by name, firstname").fetch();
		// the default template would be named list.html and the derived class
		// name seems to be conflict to the List class
		// for some unknown reason I cannot use "list" as the default template
		// name
		// so I chain it to another one

		dontRedirect();
		listAll(contacts);

		// renderJapidWith("@listAll", contacts);
	}

	public static void listAll(List<Contact> contacts) {
		renderJapid(contacts);
	}

	
	public static void yahoo3(String hi) {
		System.out.println("!!");
	}

	//
	// /**
	// * note I don't use public modifier so it won't get enhanced and I don't
	// need call dontRedirect
	// * @param cs
	// */
	// static void listAll(List<Contact> cs) {
	// renderJapid(cs);
	// }
	//
	public static void newform() {
		dontRedirect();
		form(null);
	}

	public static void form(Long id) {
		if (id == null) {
			// render();
			renderJapid((Object) null);
		}
		Contact contact = Contact.findById(id);
		// Contact contact = JPA.findById(Contact.class, id);
		// render(contact);
		renderJapid(contact);
	}

	public static void save(@Valid Contact contact) throws Error {
		if (validation.hasErrors()) {
			if (request.isAjax())
				error("Invalid value..");
			// render("@form", contact);
			renderJapidWith("@form", contact);
		}
		contact.save();

		// redirect("Application.list");
		// requestRedirect(); // just in case
		list(); // should redirect
	}
	
	public static void hello(String s, boolean b, int c) {
		
	}

}