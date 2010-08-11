package controllers;

import models.*;
import play.mvc.*;

@CRUD.For(Company.class)
public class Companies extends Administration {

    @Before
    static void checkSuper() {
        if (session.contains("company-id") && (request.action.equals("Companies.show") || request.action.equals("Companies.save"))) {
            if (session.get("company-id").equals(params.get("id"))) {
                return;
            }
        }
        if (!session.contains("superadmin")) {
            Companies.show(Long.parseLong(session.get("company-id")));
        }
    }

    public static void show(Long id) {
        parent();
    }
}

