package notifiers;

import java.util.*;
import play.mvc.*;
import models.*;
import java.util.concurrent.*;

public class Notifier extends Mailer {

	public static boolean welcome(User user) {
		setSubject("Welcome %s", user.name);
		addRecipient(user.email);
		return sendAndWait(user);
	}
    
}

