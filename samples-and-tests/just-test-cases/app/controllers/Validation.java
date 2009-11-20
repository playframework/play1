package controllers;

import play.*;
import play.mvc.*;
import play.data.validation.*;

import java.util.*;

import models.*;

public class Validation extends Controller {

    public static void index(@Valid Bottle bottle) {
        renderText(validation.errors());
    }
    
}

