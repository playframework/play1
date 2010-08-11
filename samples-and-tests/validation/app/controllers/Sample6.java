package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;
import play.data.validation.*;

public class Sample6 extends Application {

    public static void index() {
        render();
    }
    
    public static void handleSubmit(@Valid ComplicatedUser user) {
        
        // Handle errors
        if(validation.hasErrors()) {
            render("@index", user);
        }
        
        // Ok, display the created user
        render(user);
    }
    
}

