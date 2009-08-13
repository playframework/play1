package notifiers;

import play.mvc.*;

import javax.mail.internet.*;

import models.*;

public class Notifier extends Mailer {

    public static boolean welcome(User user) throws Exception {
        setFrom(new InternetAddress("admin@sampleforum.com", "Administrator"));
        setReplyTo(new InternetAddress("help@sampleforum.com", "Help"));
        setSubject("Welcome %s", user.name);
        addRecipient(user.email, new InternetAddress("new-users@sampleforum.com", "New users notice"));
        return sendAndWait(user);
    }
    
}

