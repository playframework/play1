package controllers;

import java.util.List;

import models.Book;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.modules.search.Search;
import play.modules.search.Search.Query;
import play.mvc.Controller;

public class JPASearch extends Controller {

    public static void index(String query, String field) {
        List<Book> books = null;
        if (query==null|| query.equals("")) 
           books = Book.findAll();
        else {
            if (field==null) field ="title";
            Query qr = Search.search(field+":"+query+"*", Book.class);
            books = qr.page(0, 10).reverse().fetch();
        }
        render(books,field,query);
    }
    
    @OnApplicationStart
    public static class Bootstrap extends Job {
        @Override
        public void doJob() throws Exception {
            new Book ("American Gods","Neil Gaiman",
                    "The boundaries of our country, sir? Why sir, on the north" +
                    " we are bounded by the Aurora Borealis, on the east we " +
                    "are bounded by therising sun, on the south we are bounded" +
                    " by the procession of theEquinoxes, and on the west " +
                    "by the Day of Judgment.",1).save();
            
            new Book ("Neverwhere","Neil Gaiman",
                    "She had been running for days now, a harum-scarum tumbling " +
                    "flight through passages and tunnels. She was hungry, and " +
                    "exhausted, and more tired than a body could stand, and " +
                    "each successive door was proving harder to open. After " +
                    "four days of flight, she had found a hiding place, " +
                    "a tiny stone burrow, under the world, where she would be " +
                    "safe, or so she prayed, and at last she slept.",1).save();
            new Book ("The Color Of Magic", "Terry Pratchett" ,
                    "IN A DISTANT AND SECONDHAND SET OF DIMENSIONS, in an astral " +
                    "plane that was never meant to fly, the curling star-mists " +
                    "waver and part… See… Great A’Tuin the turtle comes, " +
                    "swimming slowly through the interstellar gulf",7).save();
            
            new Book ("Bad Monkeys", "Matt Ruff " ,
                    "Jane Charlotte has been arrested for murder. She tells " +
                    "police that she is a member of a secret organization " +
                    "devoted to fighting evil; her division is called the " +
                    "Department for the Final Disposition of Irredeemable " +
                    "Persons—\"Bad Monkeys\" for short. This confession " +
                    "earns Jane a trip to the jail's psychiatric wing, " +
                    "where a doctor attempts to determine whether she is " +
                    "lying, crazy—or playing a different game altogether. " +
                    "What follows is one of the most clever and" +
                    "gripping novels you'll ever read",3).save();
        }
        
    }
}
