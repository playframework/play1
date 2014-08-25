package notifiers;

import models.japidsample.Post;
import cn.bran.play.JapidMailer;

public class TestEmailer extends JapidMailer {
	public static void emailme(Post post) {
		setSubject("Welcome %s", "me");
		addRecipient("me@me.com");
		setFrom("Me <me@me.com>");
		// EmailAttachment attachment = new EmailAttachment();
		// attachment.setDescription("A pdf document");
		// attachment.setPath(Play.getFile("rules.pdf").getPath());
		// addAttachment(attachment);
		
		// NOTE: this command will look for 
		// app/japidviews/_notifiers/TestEmailer/emailme.html to render the content. 
		send(post); 
	}
}
