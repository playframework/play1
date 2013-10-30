package play.libs;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Key;
import java.security.Provider;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(writer);
            transformer.transform(domSource, streamResult); 
        } catch (TransformerException e) {
            throw new RuntimeException(
                    "Error when serializing XML document.", e);
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
        InputSource source = new InputSource(new StringReader(xml));
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
     * Check the xmldsig signature of the XML document.
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
            Logger.warn("Error validating an XML signature.", e);
            return false;
        }
    }

    /**
     * Sign the XML document using xmldsig.
     * @param document the document to sign; it will be modified by the method.
     * @param publicKey the public key from the key pair to sign the document.
     * @param privateKey the private key from the key pair to sign the document.
     * @return the signed document for chaining.
     */
    public static Document sign(Document document, RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        KeyInfoFactory keyInfoFactory = fac.getKeyInfoFactory();

        try {
            Reference ref =fac.newReference(
                    "",
                    fac.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null,
                    null);
            SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                                                                            (C14NMethodParameterSpec) null),
                                              fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                                              Collections.singletonList(ref));
            DOMSignContext dsc = new DOMSignContext(privateKey, document.getDocumentElement());
            KeyValue keyValue = keyInfoFactory.newKeyValue(publicKey);
            KeyInfo ki = keyInfoFactory.newKeyInfo(Collections.singletonList(keyValue));
            XMLSignature signature = fac.newXMLSignature(si, ki);
            signature.sign(dsc);
        } catch (Exception e) {
            Logger.warn("Error while signing an XML document.", e);
        }

        return document;
    }

}
