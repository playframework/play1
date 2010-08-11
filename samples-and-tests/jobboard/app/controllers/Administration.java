package controllers;

import models.*;

import play.*;
import play.mvc.*;

public class Administration extends CRUD {

    @Before(unless = {"login", "authenticate"})
    static void checkLogin() {
        if (!session.contains("company") && !session.contains("superadmin")) {
            login();
        }
        renderArgs.put("superadmin", session.contains("superadmin"));
    }
    
    // ~~~~~~~
    
    public static void login() {
        render();
    }

    public static void logout() {
        session.clear();
        login();
    }

    public static void index() {
        if (!session.contains("superadmin")) {
            Jobs.list();
        }
        render("CRUD/index.html");
    }

    public static void authenticate(String email, String password) {
        if (Play.configuration.getProperty("application.superadmin").equals(email) && Play.configuration.getProperty("application.superadminpwd").equals(password)) {
            session.put("superadmin", "yes!");
            index();
        }
        Company company = Company.find("byEmailAndPassword", email, password).first();
        if (company != null) {
            session.put("company", company.email);
            session.put("company-name", company.name);
            session.put("company-id", company.id);
            index();
        }
        params.flash();
        flash.error("Bad email or password");
        login();
    }
    
}

