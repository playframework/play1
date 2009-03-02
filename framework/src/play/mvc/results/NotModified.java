package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class NotModified extends Result {
	String etag;
	
	public NotModified () {
        super("NotModified");
    }
	
    public NotModified (String etag) {
    	this.etag = etag;
    }
    
	public void apply(Request request, Response response) {
		response.status=304;
		if (etag!=null)
			response.setHeader("Etag", etag);
	}
}
