package controllers;

import models.*;

import play.mvc.*;

@CRUD.For(Category.class)
public class Categories extends Administration {

    @Before
    static void checkSuper() {
        if (!session.contains("superadmin")) {
            Jobs.list();
        }
    }
}

