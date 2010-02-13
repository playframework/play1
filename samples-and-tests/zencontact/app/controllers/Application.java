package controllers;

import play.mvc.*;
import play.data.validation.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        Date now = new Date();
        render(now);
    }
    
    public static void list() {
        List<Contact> contacts = Contact.find("order by name, firstname").fetch();
        render(contacts);
    }
    
    public static void form(Long id) {
        if(id == null) {
            render();
        }
        Contact contact = Contact.findById(id);
        render(contact);
    }
    
    public static void save(@Valid Contact contact) {
        if(validation.hasErrors()) {
            if(request.isAjax()) error("Invalid value");
            render("@form", contact);
        }
        contact.save();
        list();
    }

}