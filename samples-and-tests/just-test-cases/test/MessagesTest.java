
import play.*;

import org.junit.Test;

import play.test.UnitTest;
import play.i18n.Lang;
import play.i18n.Messages;

public class MessagesTest extends UnitTest {

    @Test
    public void testCountrySpecificMessage() {
        try{
            Lang.set("fr_CA");
            assertEquals("Votre nom:", Messages.get("login.username"));
        }finally{
            Lang.set("");
        }
    }

    @Test
    public void testLanguageFallbackFrench() {
        try{
            Lang.set("fr");
            assertEquals("Your account has expired.", Messages.get("login.accountExpired")); // doesn't exist in french - fallback to default
        }finally{
            Lang.set("");
        }
    }

    @Test
    public void testLanguageFallbackFrenchCanadian() {
        try{
            Lang.set("fr_CA");
            assertEquals("Bonjour MessagesTest vous êtes connecté.", Messages.get("login.success", "MessagesTest")); // doesn't exist in french canadian - fallback to french
            assertEquals("Your account has expired.", Messages.get("login.accountExpired")); // doesn't exist in french and also not in french canadian - fallback to default
        }finally{
            Lang.set("");
        }
    }

    @Test
    public void testLanguageFallbackFrenchUndefinedCountry() {
        try{
            // using change() instead of set() to find closest match
            Lang.change("fr_XY"); // fr_XY was not configured in application.conf, but fr was and therefore falling back to fr
            assertEquals("fr", Lang.get());
            assertEquals("Bonjour MessagesTest vous êtes connecté.", Messages.get("login.success", "MessagesTest"));
        }finally{
            Lang.set("");
        }
    }
}