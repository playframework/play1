package controllers;

import play.mvc.*;

public class Welcome extends Mailer {
    
    public static void welcome() {
        String msg = "Welcome2";
        setSubject("Yop2");
        addRecipient("toto@localhost");
        send(msg);
    }    
    
}