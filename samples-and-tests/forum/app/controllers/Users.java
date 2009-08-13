package controllers;

import java.util.*;

import models.*;

public class Users extends Application {

    public static void index(Integer page) {
        List users = User.findAll(page == null ? 1 : page, pageSize);
        Long nbUsers = User.count();
        render(nbUsers, users, page);
    }

    public static void show(Long id) {
        User user = User.findById(id);
        notFoundIfNull(user);
        render(user);
    }
}

