package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;
import play.data.validation.*;

public class Sample3 extends Application {

    public static void index() {
        render();
    }
    
    public static void handleSubmit(
        @Required @MinSize(6) String username, 
        @Required String firstname, 
        @Required String lastname,
        @Required @Range(min=16, max=120) Integer age,
        @Required @MinSize(6) String password,
        @Required @Equals("password") String passwordConfirm,
        @Required @Email String email,
        @Required @Equals("email") String emailConfirm,
        @Required @IsTrue boolean termsOfUse) {
        
        // Handle errors
        if(validation.hasErrors()) {
            render("@index");
        }
        
        // Ok, display the created user
        render(username, firstname, lastname, age, password, email);
    }
    
}

