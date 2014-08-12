package play.libs;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import play.libs.WS.HttpResponse;
import play.mvc.Http.Header;

public class TestHttpResponse extends HttpResponse {

    public String queryContent;

    TestHttpResponse(String queryContent) {
        this.queryContent = queryContent;
    }

    @Override
    public Integer getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getStatusText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHeader(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Header> getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(this.queryContent.getBytes(StandardCharsets.UTF_8));
    }

}
