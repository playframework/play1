package play.libs;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests for {@link XML} class.
 */
public class XMLTest {

    private static final String ORIGINAL_DOCUMENT =
            "<?xml version=\"1.0\"?>\n" +
            "<feed xmlns=\"http://www.w3.org/2005/Atom\">" +
              "<title>Awesome Blog</title>" +
              "<link href=\"http://blog.example.com/\"/>" +
            "</feed>";
    private Document document;
    
    @Before
    public void setUp() throws Exception {
        document = XML.getDocument(ORIGINAL_DOCUMENT); 
    }

    private static String stripPreamble(String text) {
        return text.replaceFirst("<\\?[^?]+\\?>\\s*", "");
    }
    
    @Test
    public void serializeShouldReturnWellFormedXml() throws Exception {
        String outputDocument = XML.serialize(document);
        assertEquals(
                stripPreamble(ORIGINAL_DOCUMENT),
                stripPreamble(outputDocument));
    }
}
