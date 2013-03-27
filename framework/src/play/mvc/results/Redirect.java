package play.mvc.results;

import java.util.Map;
import java.util.Map.Entry;

import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 302 Redirect
 */
public class Redirect extends Result {

    public String url;
    public int code = Http.StatusCode.FOUND;

    public Redirect(String url) {
        this.url = url;
    }
    
	/**
	 * Redirects to a given URL with the parameters specified in a {@link Map}
	 * 
	 * @param url
	 *            The URL to redirect to as a {@link String}
	 * @param parameters
	 *            Parameters to be included at the end of the URL as a HTTP GET. This is a map whose entries are written out as key1=value1&key2=value2 etc..
	 */
	public Redirect(String url, Map<String, String> parameters) {
		StringBuffer urlSb = new StringBuffer(url);
		char prepend = '?';

		if (parameters != null && parameters.size() > 0) {

			for (Entry<String, String> parameter : parameters.entrySet()) {
				urlSb.append(prepend).append(parameter.getKey()).append('=').append(parameter.getValue());
				prepend = '&';
			}
		}

		this.url = urlSb.toString();
	}

    public Redirect(String url,boolean permanent) {
        this.url = url;
        if (permanent)
            this.code = Http.StatusCode.MOVED;
    }

    public Redirect(String url,int code) {
        this.url = url;
        this.code=code;
    }

    public void apply(Request request, Response response) {
        try {
	    // do not touch any valid uri: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.30 
	    if (url.matches("^\\w+://.*")) {
            } else if (url.startsWith("/")) {
                url = String.format("http%s://%s%s%s", request.secure ? "s" : "", request.domain, (request.port == 80 || request.port == 443) ? "" : ":" + request.port, url);
            } else {
                url = String.format("http%s://%s%s%s%s", request.secure ? "s" : "", request.domain, (request.port == 80 || request.port == 443) ? "" : ":" + request.port, request.path, request.path.endsWith("/") ? url : "/" + url);
            }
            response.status = code;
            response.setHeader("Location", url);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
