package controllers;

import play.*;
import play.mvc.*;
import play.data.validation.*;

import java.util.*;

import models.*;

public class Validation extends Controller {

    public static void index(@Required @Valid Bottle bottle) {
        renderText(validation.errorsMap());
    }

     public static void user(@Required @Valid User user) {
         renderText(validation.errorsMap());
    }
    
}

