package controllers;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import models.Contact;
import play.data.validation.Valid;
import play.db.jpa.JPA;
import cn.bran.play.JapidController;

public class Dummy extends JapidController {

    public static void index() {
        Date now = new Date();
        renderJapid(now);
    }
    
    public static void list() {
        List<Contact> contacts = JPA.find(Contact.class).fetch();
        contacts = contacts.stream().sorted((a, b) -> a.name.compareTo(b.name)).collect(Collectors.toList());
//        dontRedirect();
//        listAll(contacts);
        renderJapidWith("@listAll", contacts);
    }
        
    public static void yahoo(String hi, String a, int b) {
    	renderText("what!" + a + b + hi);
    }
//    
//    /**
//     *  note I don't use public modifier so it won't get enhanced and I don't need call dontRedirect
//     * @param cs
//     */
//    static void listAll(List<Contact> cs) {
//    	renderJapid(cs);
//    }
//    
    public static void form(Long id) {
        if(id == null) {
//            render();
        	renderJapid((Object)null);
        }
        Contact contact = JPA.findById(Contact.class, id);
//        render(contact);
        renderJapid(contact);
    }
    
    public static void save(@Valid Contact contact) {
        if(validation.hasErrors()) {
            if(request.isAjax()) 
            	error("Invalid value received.");
//            render("@form", contact);
            renderJapidWith("@form", contact);
        }
        System.out.println(contact.toString());
        contact.save();

        // cannot use direct all yet...
        redirect("Application.list");
        
    }

}