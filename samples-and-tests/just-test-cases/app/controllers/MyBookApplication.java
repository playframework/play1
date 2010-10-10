package controllers;

import java.util.List;

import models.MyBook;
import play.mvc.Controller;

public class MyBookApplication extends Controller {

    public static void index() {
        List<MyBook> testEntries = MyBook.all().fetch();
        render(testEntries);
    }
    
    public static void edit(Long id) {
        MyBook testObj = MyBook.findById(id);
        notFoundIfNull(testObj);
        render(testObj);
    }

    
    public static void save(long id, MyBook testObj) {
        if (!testObj.isPersistent()) {
            notFound("The object Test with id "+ id + " wasn't found anymore!");
        }
        if (testObj.validateAndSave()) {
            index();
        } else {
            render("Application/edit.html", testObj);
        }
    }
    

}