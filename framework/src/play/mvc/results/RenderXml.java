package play.mvc.results;

import org.w3c.dom.Document;

import play.exceptions.UnexpectedException;
import play.libs.XML;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayOutputStream;
import play.libs.optimization.Compression;

/**
 * 200 OK with a text/xml
 */
public class RenderXml extends Result {

    String xml;

    public RenderXml(CharSequence xml) {
        this.xml = xml.toString();
    }

    public RenderXml(Document document) {
        this.xml = XML.serialize(document);
    }

    public RenderXml(Object o, XStream xstream) {
        this.xml = xstream.toXML(o);
    }

    public RenderXml(Object o) {
        this(o, new XStream());
    }

    public void apply(Request request, Response response) {
        try {
            if (Boolean.parseBoolean(play.Play.configuration.getProperty("optimization.compressXML"))) {
                xml = Compression.compressXML(xml);
            }

            setContentTypeIfNotSet(response, "text/xml");

            if (gzipIsSupported(request)) {
                final ByteArrayOutputStream gzip = Compression.gzip(xml);
                response.setHeader("Content-Encoding", "gzip");
                response.setHeader("Content-Length", gzip.size() + "");
                response.out = gzip;
            } else {
                response.out.write(xml.getBytes(getEncoding()));
            }
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
