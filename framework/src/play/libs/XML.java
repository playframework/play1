package play.libs;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class XML {
	private static Logger logger = Logger.getLogger(XML.class);

	public static String serialize(Document document) {
		StringWriter writer = new StringWriter();
		try {
			new XMLSerializer(writer, null).serialize(document);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer.toString();
	}

	/**
	 * 
	 * @return null if an error occurs during parsing.
	 * 
	 */
	public static Document getDocument(File file) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			return dbf.newDocumentBuilder().parse(file);
		} catch (SAXException e) {
			logger.warn("Parsing error when building Document objetc from xml file '" + file + "'.", e);
		} catch (IOException e) {
			logger.warn("Reading error when building Document objetc from xml file '" + file + "'.", e);
		} catch (ParserConfigurationException e) {
			logger.warn("Parsing error when building Document objetc from xml file '" + file + "'.", e);
		}
		return null;

	}
}