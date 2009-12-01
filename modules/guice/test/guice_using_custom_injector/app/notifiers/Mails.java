package notifiers;
 
import play.*;
import play.mvc.*;
import java.util.*;
 
import utils.TestInter;
import javax.inject.Inject;

public class Mails extends Mailer {
   @Inject static TestInter test;  
   public static void welcome() {
      setSubject("Welcome %s", test.printer());
      addRecipient("testsomething@hotmail.com");
	  Logger.info("sending a test email:"+ test.printer());
      send();
   }
 
}
