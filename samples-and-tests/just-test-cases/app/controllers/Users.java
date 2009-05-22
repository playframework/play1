package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class Users extends Controller {

    public static void index() {
        render();
    }
    
    public static void submit(User u) {
        render(u);
    }
    
}

