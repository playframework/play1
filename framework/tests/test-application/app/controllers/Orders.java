package controllers;

import play.mvc.Controller;

public class Orders extends Controller {

    @play.mvc.Before
    static void check() {
        int b = 9 / 1;
    }

    public static void show(Long id, String name) {
        render(id, name);
    }
    
    public static void test(String name) {
        render();
    }
}