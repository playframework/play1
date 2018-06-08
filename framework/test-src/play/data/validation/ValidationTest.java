package play.data.validation;

import org.junit.Test;
import play.Play;
import play.i18n.Messages;
import play.i18n.MessagesBuilder;
import play.mvc.Http;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class ValidationTest {

    @Test
    public void verifyError() {
        new MessagesBuilder().build();

        Validation.current.set(new Validation());

        String field = "f1";

        assertThat(Validation.error(field)).isNull();
        assertThat(Validation.errors(field)).isEmpty();

        String errorMsg = "My errorMessage";

        Validation.addError(field, errorMsg);

        assertThat( Validation.error(field).message).isEqualTo(errorMsg);
        assertThat(Validation.errors(field)).containsOnly(Validation.error(field));

        // ticket [#109] - add an error with null-key
        Validation.addError(null, errorMsg);
        // make sure this null key does not break stuff
        assertThat( Validation.error(field).message).isEqualTo(errorMsg);
        assertThat(Validation.errors(field)).containsOnly(Validation.error(field));

    }
    
    @Test
    public void addErrorTest(){
        new MessagesBuilder().build();

        Validation.current.set(new Validation());

        String field = "f1";
        String field2 = "f1.element";

        String errorMsg = "My errorMessage";
        
        Validation.addError(field, errorMsg);  
        Validation.addError(field, errorMsg); 
        
        Validation.addError(field2, errorMsg);
        
        assertThat( Validation.error(field).message).isEqualTo(errorMsg);
        
        // Test avoid insert duplicate message key
        assertEquals(2, Validation.errors().size());
        
        assertEquals(1, Validation.errors(field).size());
        assertEquals(1, Validation.errors(field2).size());
        
        Validation.clear();
        
        // Test clear empty the list
        assertEquals(0, Validation.errors().size());
        assertEquals(0, Validation.errors(field).size());
        assertEquals(0, Validation.errors(field2).size());
        
        String errorMsgWithParam = "My errorMessage: %2$s";
        
        Validation.addError(field, errorMsgWithParam, "param1");  
        Validation.addError(field, errorMsgWithParam,"param2"); 
                
        assertThat( Validation.error(field).message).isEqualTo(errorMsgWithParam);
        
        // Test avoid insert duplicate message key
        assertEquals(1, Validation.errors().size());
        
        assertEquals(1, Validation.errors(field).size());
        
        assertEquals("My errorMessage: param1", Validation.error(field).message());
    }
    
    @Test
    public void removeErrorTest(){
        new MessagesBuilder().build();

        Validation.current.set(new Validation());

        String field = "f1";
        String field2 = "f1.element";

        String errorMsg = "My errorMessage";
        String errorMsg2 = "My errorMessage2";
        Validation.addError(field, errorMsg); 
        Validation.addError(field, errorMsg2); 
        
        Validation.addError(field2, errorMsg);
        Validation.addError(field2, errorMsg2);

        // Check the first error
        assertThat( Validation.error(field).message).isEqualTo(errorMsg);
        assertEquals(4, Validation.current().errors.size());   
        
        // Remove Errors on field2
        Validation.removeErrors(field2);
        
        assertEquals(2, Validation.errors().size());  
        assertEquals(2, Validation.errors(field).size()); 
        assertEquals(0, Validation.errors(field2).size()); 
        
        // Restore error on field2
        Validation.addError(field2, errorMsg);
        Validation.addError(field2, errorMsg2);
        
        assertEquals(4, Validation.current().errors.size()); 
        
        // Remove Errors on field
        Validation.removeErrors(field);
        
        assertEquals(2, Validation.errors().size());  
        assertEquals(0, Validation.errors(field).size()); 
        assertEquals(2, Validation.errors(field2).size());         
    }
    
    @Test
    public void removeErrorMessageTest(){
        new MessagesBuilder().build();

        Validation.current.set(new Validation());

        String field = "f1";
        String field2 = "f1.element";

        String errorMsg = "My errorMessage";
        String errorMsg2 = "My errorMessage2";
        Validation.addError(field, errorMsg); 
        Validation.addError(field, errorMsg2); 
        
        Validation.addError(field2, errorMsg);
        Validation.addError(field2, errorMsg2);

        // Check the first error
        assertThat( Validation.error(field).message).isEqualTo(errorMsg);
        assertEquals(4, Validation.current().errors.size());   
        
        // Remove Errors on field2
        Validation.removeErrors(field2, errorMsg);
        
        assertEquals(3, Validation.errors().size());  
        assertEquals(2, Validation.errors(field).size()); 
        assertEquals(1, Validation.errors(field2).size()); 
        
        assertThat( Validation.error(field2).message).isEqualTo(errorMsg2);
        
        // Restore error on field2
        Validation.addError(field2, errorMsg);
        Validation.addError(field2, errorMsg2);
        
        assertEquals(4, Validation.current().errors.size()); 
        
        // Remove Errors on field
        Validation.removeErrors(field, errorMsg);
        
        assertEquals(3, Validation.errors().size());  
        assertEquals(1, Validation.errors(field).size()); 
        assertEquals(2, Validation.errors(field2).size()); 
        
        assertThat( Validation.error(field).message).isEqualTo(errorMsg2);
    }
    
    @Test
    public void insertErrorTest(){
        new MessagesBuilder().build();

        Validation.current.set(new Validation());

        String field = "f1";

        String errorMsg = "My errorMessage";
        String errorMsg2 = "My errorMessage2";
        Validation.addError(field, errorMsg); 
        Validation.insertError(0, field, errorMsg2); 
        

        // Check the first error
        assertThat( Validation.error(field).message).isEqualTo(errorMsg2);
        assertEquals(2, Validation.current().errors.size());    
    }

    @Test
    public void restoreEmptyVariable() {
        Messages.defaults = new Properties();
        Messages.defaults.setProperty("validation.error.missingName", "%s is invalid, given: '%s'");
        Validation.current.set(new Validation());
        Http.Response.current.set(new Http.Response());
        Http.Request.current.set(new Http.Request());
        Play.configuration = new Properties();
        Validation.addError("user.name", "validation.error.missingName", "");
        Validation.keep();
        ValidationPlugin.save();

        Http.Request.current().cookies = Http.Response.current().cookies;

        Validation restored = ValidationPlugin.restore();
        assertEquals("user.name is invalid, given: ''", restored.errors.get(0).message());
    }
}
