package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class Sample2 extends Application {

    public static void index() {
        render();
    }
    
    public static void handleSubmit(
        String username, 
        String firstname, 
        String lastname,
        Integer age,
        String password,
        String passwordConfirm,
        String email,
        String emailConfirm,
        boolean termsOfUse) {
            
        // Validation rules
        validation.required(username);
        validation.minSize(username, 6);
        validation.required(firstname);
        validation.required(lastname);
        validation.required(age);
        validation.range(age, 16, 120);
        validation.required(password);
        validation.minSize(password, 6);
        validation.required(passwordConfirm);
        validation.equals(passwordConfirm, password);
        validation.required(email);
        validation.email(email);
        validation.required(emailConfirm);
        validation.equals(emailConfirm, email);
        validation.isTrue(termsOfUse);
        
        // Handle errors
        if(validation.hasErrors()) {
            render("@index");
        }
        
        // Ok, display the created user
        render(username, firstname, lastname, age, password, email);
    }
    
}

