package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;
import models.horse.*;

public class Horses extends Controller {

    public static void index() {
        new Horse().create();
        Horse existing = Horse.all().first();
        render(existing);
    }
    
    public static void dontSave(Horse horse) {
        result();
    }
    
    public static void save(Horse horse) {
        horse.save();
        result();
    }
    
    public static void result() {
        renderText("Blups:" + BLUP.count());
    }
    
}

