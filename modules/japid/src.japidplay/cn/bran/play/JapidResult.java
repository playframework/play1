package cn.bran.play;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;
import cn.bran.japid.template.RenderResult;

/**
 * class for use to indicate that the result has been flushed to the response
 * result
 * 
 * The content extraction from the RenderResult is postponed until the apply()
 * if eval() is not called before apply. The eval() will make the JapidResult
 * render the content eagerly and once, therefore any nested cache will effect
 * once. stage so that JapidResult can be cached and still retain dynamic
 * feature of a RenderResultPartial
 * 
 * @author bran
 * 
 */
public class JapidResult extends Result implements Externalizable {
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String CACHE_CONTROL = "Cache-Control";

	private RenderResult renderResult;
	private Map<String, String> headers = new HashMap<String, String>();
	private boolean eager = false;

	String resultContent = "";

	// public JapidResult(String contentType) {
	// super();
	// this.contentType = contentType;
	// }
	//
	// public JapidResult(String contentType2, String string) {
	// this.contentType = contentType2;
	// this.content = string;
	// }

	public JapidResult(RenderResult r) {
		this.renderResult = r;
		this.headers = r.getHeaders();
	}

	public JapidResult() {
	}

	public JapidResult(String description) {
		super(description);
		// TODO Auto-generated constructor stub
	}

	/**
	 * extract content now and once. Eager evaluation of RenderResult
	 */
	public JapidResult eval() {
		this.eager = true;
		this.resultContent = extractContent();
		return this;
	}

	/**
	 * @param r
	 */
	public String extractContent() {
		String content = "";
		StringBuilder sb = renderResult.getContent();
		if (sb != null)
			content = sb.toString();
		return content;
	}

	@Override
	public void apply(Request request, Response response) {
		String content = resultContent;

		if (!eager)
			// late evaluation
			content = extractContent();

		if (content != null)
			try {
				// XXX should it just be response?
//				Response.current().out.write(content.getBytes("UTF-8"));
				response.out.write(content.getBytes("UTF-8"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		Map<String, Header> resHeaders = response.headers;

		if (headers != null) {
			for (String h : headers.keySet()) {
				String value = headers.get(h);
				if (CONTENT_TYPE.equals(h)) {
					setContentTypeIfNotSet(response, value);
				} else {
					if (resHeaders.containsKey(h)) {
						// shall I override it?
						// override it. Consider the value in templates are
						// meant to override
						response.setHeader(h, value);
					} else {
						response.setHeader(h, value);
					}
				}
			}
		}
	}

	public RenderResult getRenderResult() {
		return renderResult;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(renderResult);
		out.writeObject(headers);
		out.writeBoolean(eager);
		out.writeUTF(resultContent);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		renderResult = (RenderResult) in.readObject();
		headers = (Map<String, String>) in.readObject();
		eager = in.readBoolean();
		resultContent = in.readUTF();
	}

	/**
	 * override to save a native call for better performance (35 x better in object creation. 
	 * ~1us after this optimization)
	 */
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

}
