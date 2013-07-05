
import play.*;

import org.junit.Test;

import play.test.UnitTest;
import play.i18n.Lang;
import play.i18n.Messages;

public class MessagesTest extends UnitTest {

    @Test
    public void testIncludeMessagesDefault() {
        Lang.set("");
        assertEquals("Username:", Messages.get("login.username"));  
        assertEquals("Hello MessagesTest you are log in.", Messages.get("login.success", "MessagesTest"));    
        assertEquals("Another message", Messages.get("another.message"));  
    }
    
    @Test
    public void testIncludeMessagesUndefinedLang() {
        assertEquals("Username:", Messages.getMessage("ru", "login.username"));  
        assertEquals("Hello MessagesTest you are log in.", Messages.getMessage("ru","login.success", "MessagesTest"));    
        assertEquals("Another message", Messages.getMessage("ru","another.message"));  
    }
    
    @Test
    public void testIncludeMessagesFrench() {
        try{
            Lang.set("fr");
            assertEquals("Nom d'utilisateur&nbsp;:", Messages.get("login.username"));  
            assertEquals("Bonjour MessagesTest vous êtes connecté.", Messages.get("login.success", "MessagesTest"));    
            assertEquals("Un autre message", Messages.get("another.message"));  
	}finally{
	    Lang.set("");
        }	  
    }
}