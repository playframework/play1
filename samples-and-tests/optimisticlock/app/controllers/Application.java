package controllers;

import java.util.List;

import models.Book;
import play.mvc.Controller;

public class Application extends Controller {

    public static void index() {
        List<Book> testEntries = Book.all().fetch();
        render(testEntries);
    }
    
    public static void edit(Long id) {
        Book testObj = Book.findById(id);
        notFoundIfNull(testObj);
        render(testObj);
    }

    
    public static void save(long id, Book testObj) {
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