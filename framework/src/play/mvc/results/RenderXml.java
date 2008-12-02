package play.mvc.results;

import org.w3c.dom.Document;
import play.exceptions.UnexpectedException;
import play.libs.XML;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

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

    public void apply(Request request, Response response) {
        try {
            setContentTypeIfNotSet(response, "text/xml");
            response.out.write(xml.getBytes("utf-8"));
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
