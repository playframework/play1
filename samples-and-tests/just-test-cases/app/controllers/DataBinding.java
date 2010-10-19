package controllers;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import models.Person;
import play.Logger;
import play.data.binding.As;
import play.i18n.Lang;
import play.mvc.Controller;

public class DataBinding extends Controller {
    
    @play.mvc.Before static void lang(String lang) {
        System.out.println(lang);
    }

    public static void showDefaultDateFormat(Date date) {
        renderText(date);
    }

    public static void changeLanguage(String lang) {
        Lang.change(lang);
    }

    public static void showLocaleDateFormat(Date date) {
        renderText(date);
    }

    public static void showDefaultLocaleDateFormat(Date date) {
        renderText(date);
    }

    public static void showDateFormat(@As("MM-dd-yyyy'T'HH:mm:ss") Date date) {
        renderText(date);
    }

    public static void showList(@As("/") List<String> mailboxes) {
        renderText(mailboxes);
    }

    public static void showCalendar(@As("dd-MMM-yyyy") Calendar cal) {
        renderText(cal.getTime());
    }

    public static void signinPage() {
        render();
    }
    
    public static void customeBinding(@As(binder=utils.TestBinder.class) String yop) {
        renderText(yop);
    }
    
    public static void globalBinder(java.awt.Point p) {
        if(validation.hasErrors()) {
            renderText(validation.errors());
        }
        renderText(p.x + "|" + p.y);
    }

    public static void signin(@As("secure") Person person) {
        Person verifyPerson = get();
        flash.clear();
        if (verifyPerson.userName.equals(person.userName) && verifyPerson.password.equals(person.password)) {
           flash.success("Authentication successful!");
        } else {
           flash.error("Authentication failed!"); 
        }
        render("/DataBinding/signinPage.html", person);
    }

    static Person get() {
        Person person = new Person();
        person.userName = "nicolas";
        person.password = "nicolas";
        return person;
    }
    
    public static void createFactory(@play.data.validation.Valid models.Factory factory) {
        renderText(validation.hasErrors() + " -> " + factory.name + "," + factory.color);
    }
}

