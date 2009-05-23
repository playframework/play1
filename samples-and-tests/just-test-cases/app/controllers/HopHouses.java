package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class HopHouses extends Controller {

    public static void index() {
        render();
    }
    
    public static void submit(House h) {
        render(h);
    }
    
}

