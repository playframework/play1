package play.libs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.Provider;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import play.Logger;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

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

    /**
     * Parse an XML string content to DOM
     * @return null if an error occurs during parsing.
     */
    public static Document getDocument(String xml) {
        InputSource source = new InputSource(new ByteArrayInputStream(xml.getBytes()));
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            return dbf.newDocumentBuilder().parse(source);
        } catch (SAXException e) {
            Logger.warn("Parsing error when building Document object from xml data.", e);
        } catch (IOException e) {
            Logger.warn("Reading error when building Document object from xml data.", e);
        } catch (ParserConfigurationException e) {
            Logger.warn("Parsing error when building Document object from xml data.", e);
        }
        return null;
    }

    /**
     * Check the xmldsig signature of the XML document
     * @param document the document to test
     * @param publicKey the public key corresponding to the key pair the document was signed with
     * @return true if a correct signature is present, false otherwise
     */
    public static boolean validSignature(Document document, Key publicKey) {
        Node signatureNode =  document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
        KeySelector keySelector = KeySelector.singletonKeySelector(publicKey);

        try {
            String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());
            DOMValidateContext valContext = new DOMValidateContext(keySelector, signatureNode);

            XMLSignature signature = fac.unmarshalXMLSignature(valContext);
            return signature.validate(valContext);
        } catch (Exception e) {
            Logger.warn("Exception checking the signature: ", e);
            return false;
        }
    }

}
