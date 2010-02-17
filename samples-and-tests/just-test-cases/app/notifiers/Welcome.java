package notifiers;

import play.mvc.*;

public class Welcome extends Mailer {
    
    public static void welcome() {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Yop");
        addRecipient("toto@localhost");
        send(msg);
    }    
  
   public static void welcome2() {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Yop3");
        addRecipient("toto@localhost");
        addBcc("nicolas@localhost");
        addCc("guillaume@localhost");
        send(msg);
    }    
    
    public static void welcome3() {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Yop4");
        addRecipient("toto@localhost");
        send(msg);
    }  
}
