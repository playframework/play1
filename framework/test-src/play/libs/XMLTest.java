package play.libs;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    @Test
    public void shouldFindNodesWithXpath() {
        final Node titleNode = XPath.selectNode("/feed/title", document);
        assertEquals("title", titleNode.getNodeName());
    }

    @Test
    public void shouldNotFindNodesWithXPathAndNamespacesByDefault() {
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("x", "http://www.w3.org/2005/Atom");
        final Node titleNode = XPath.selectNode("/x:feed/x:title", document, namespaces);
        assertNull(titleNode);
    }

    @Test
    public void shouldFindNodesWithXpathAndNamespacesFromString() {
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("x", "http://www.w3.org/2005/Atom");
        final Document namespaceDocument = XML.getDocument(ORIGINAL_DOCUMENT, true);
        final Node titleNode = XPath.selectNode("/x:feed/x:title", namespaceDocument, namespaces);
        assertEquals("title", titleNode.getNodeName());
    }

    @Test
    public void shouldFindNodesWithXpathAndNamespacesFromStream() {
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("x", "http://www.w3.org/2005/Atom");
        final Document namespaceDocument = XML.getDocument(
                new ByteArrayInputStream(ORIGINAL_DOCUMENT.getBytes(StandardCharsets.UTF_8)), true);
        final Node titleNode = XPath.selectNode("/x:feed/x:title", namespaceDocument, namespaces);
        assertEquals("title", titleNode.getNodeName());
    }

    @Test
    public void shouldParseAnXMLResponseBody() {
        TestXMLHttpResponse  response = new TestXMLHttpResponse(ORIGINAL_DOCUMENT);
        final Node titleNode = XPath.selectNode("/feed/title", response.getXml());
        assertEquals("title", titleNode.getNodeName());
    }

    @Test
    public void shouldNotParseAnXMLResponseBodyWithNamespaces() {
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("x", "http://www.w3.org/2005/Atom");
        TestXMLHttpResponse  response = new TestXMLHttpResponse(ORIGINAL_DOCUMENT);
        final Node titleNode = XPath.selectNode("/x:feed/x:title", response.getXml(), namespaces);
        assertNull(titleNode);
    }


    @Test
    public void shouldParseAnXMLResponseBodyWithNamespaces() {
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("x", "http://www.w3.org/2005/Atom");
        TestXMLHttpResponse  response = new TestXMLHttpResponse(ORIGINAL_DOCUMENT);
        final Node titleNode = XPath.selectNode("/x:feed/x:title", response.getXml(true), namespaces);
        assertEquals("title", titleNode.getNodeName());
    }
}
