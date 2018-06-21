package play.libs;

import play.mvc.Http;

import java.io.InputStream;
import java.util.List;

public class TestXMLHttpResponse extends WS.HttpResponse {
    private final String body;

    public TestXMLHttpResponse(final String body) {
        this.body = body;
    }

    @Override
    public Integer getStatus() {
        return null;
    }

    @Override
    public String getStatusText() {
        return null;
    }

    @Override
    public String getHeader(String key) {
        return null;
    }

    @Override
    public List<Http.Header> getHeaders() {
        return null;
    }

    @Override
    public String getString() {
        return body;
    }

    @Override
    public String getString(String encoding) {
        return body;
    }

    @Override
    public InputStream getStream() {
        return null;
    }
}
