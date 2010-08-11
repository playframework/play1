package controllers;

import models.*;

public class Jobs extends Administration {

    public static void list() {
        if (session.contains("superadmin")) {
            parent();
        }
        request.args.put("where", "company.email = '" + session.get("company") + "'");
        parent();
    }

    public static void show(Long id) {
        if (!((Job) Job.findById(id)).company.email.equals(session.get("company")) && !session.contains("superadmin")) {
            forbidden();
        }
        parent();
    }

    public static void create() {
        if (session.contains("superadmin")) {
            parent();
        }
        Company company = Company.find("byEmail", session.get("company")).first();
        params.put("object.company.id", company.id + "");
        parent();
    }

    public static void save(Long id) {
        if (session.contains("superadmin")) {
            parent();
        }
        Company company = Company.find("byEmail", session.get("company")).first();
        params.put("object.company.id", company.id + "");
        parent();
    }
    
}

