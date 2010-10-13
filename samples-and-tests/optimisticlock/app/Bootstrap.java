import models.Book;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;
 
@OnApplicationStart
public class Bootstrap extends Job {
 
    public void doJob() {
        // Check if the database is empty
        if(Book.count() == 0) {
            Fixtures.load("initial-data.yml");
        }
    }
 
}