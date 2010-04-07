package play.libs;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import play.Logger;

/**
 * XML utils
 */
public class XML {

    /**
     * Serialize to XML String
     * @param document The DOM document
     * @return The XML String
     */
    public static String serialize(Document document) {
        StringWriter writer = new StringWriter();
        try {
            new XMLSerializer(writer, null).serialize(document);
        } catch (IOException e) {
            Logger.warn("Error when serializing xml Document.", e);
        }
        return writer.toString();
    }

    /**
     * Parse an XML file to DOM
     * @return null if an error occurs during parsing.
     * 
     */
    public static Document getDocument(File file) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            return dbf.newDocumentBuilder().parse(file);
        } catch (SAXException e) {
            Logger.warn("Parsing error when building Document object from xml file '" + file + "'.", e);
        } catch (IOException e) {
            Logger.warn("Reading error when building Document object from xml file '" + file + "'.", e);
        } catch (ParserConfigurationException e) {
            Logger.warn("Parsing error when building Document object from xml file '" + file + "'.", e);
        }
        return null;
    }
    
}