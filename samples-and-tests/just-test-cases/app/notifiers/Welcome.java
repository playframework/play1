package notifiers;

import play.mvc.*;

public class Welcome extends Mailer {
    
    public static void welcome() {
        String msg = "Welcome";
        setSubject("Yop");
        addRecipient("toto@localhost");
        send(msg);
    }    
    
}