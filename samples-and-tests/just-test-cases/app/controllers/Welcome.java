package controllers;

import org.apache.commons.mail.EmailException;
import play.mvc.*;

public class Welcome extends Mailer {
    
    public static void welcome() {
        String msg = "Welcome2";
        setFrom("x@x.com");
        setSubject("Yop2");
        addRecipient("toto@localhost");
        send(msg);
    }

}
