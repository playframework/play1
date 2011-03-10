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
    
    public static void seleniumTest() {
        String msg = "Selenium";
        setFrom("x@x.com");
        setSubject("Berillium Subject");
        addRecipient("boron@localhost");
        send(msg);
    }
    
    public static void welcome_mailWithUrls(boolean fromJob) {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Yop4");
        if( fromJob ) {
            addRecipient("mailWithUrlsJob@localhost");
        } else {
            addRecipient("mailWithUrls@localhost");
        }

        send(msg);
    }
    
}
