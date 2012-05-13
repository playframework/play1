import java.io.File;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.libs.WS;
import play.libs.WS.FileParam;
import play.libs.WS.HttpResponse;
import play.mvc.Http.Header;
import play.test.UnitTest;

import com.google.gson.JsonObject;
import controllers.Rest;


public class RestTest extends UnitTest {
    static Map<String, Object> params;

    @Before
    public void setUp() {
        params = new HashMap<String, Object>();
        params.put("timestamp", 1200000L);
        params.put("cachable", true);
        params.put("multipleValues", new String[]{ "欢迎",  "dobrodošli",  "ยินดีต้อนรับ"});
    }

    @Test
    public void testGet() throws Exception {
        assertEquals("对!", WS.url("http://localhost:9003/ressource/%s",  "ééééééçççççç汉语漢語").get().getString());
    }

    @Test
    public void testASCIIGet() throws Exception {
        assertEquals("toto", WS.url("http://localhost:9003/ressource/%s", "foobar").get().getString());
    }

    @Test
    public void testPost() throws Exception {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("id", 101);
        assertEquals(jsonResponse.toString(), WS.url("http://localhost:9003/ressource/%s", "名字").params(params).post().getJson().toString());
        File fileToSend = new File(new URLDecoder().decode(getClass().getResource("/kiki.txt").getFile(), "UTF-8"));
        assertTrue(fileToSend.exists());
        assertEquals("POSTED!", WS.url("http://localhost:9003/ressource/file/%s",  "名字").files(new FileParam(fileToSend, "file")).post().getString());
        assertEquals("FILE AND PARAMS POSTED!", WS.url("http://localhost:9003/ressource/fileAndParams/%s", "名字").files(new FileParam(fileToSend, "file")).params(params).post().getString());

    }

    @Test
    public void testHead() throws Exception {
        HttpResponse headResponse = WS.url("http://localhost:9003/ressource/%s", "ééééééçççççç汉语漢語").head();
        List<Header> headResponseHeaders = headResponse.getHeaders();
        assertTrue(headResponse.getStatus() == 200);
        assertEquals("", headResponse.getString());
        HttpResponse getResponse = WS.url("http://localhost:9003/ressource/%s", "ééééééçççççç汉语漢語").get();
        assertTrue(getResponse.getStatus() == 200);
        List<Header> getResponseHeaders = getResponse.getHeaders();
        for (int i = 0; i < getResponseHeaders.size(); i++) {
            if (!"Set-Cookie".equals(getResponseHeaders.get(i).name)) {
                assertEquals(getResponseHeaders.get(i).value(), headResponseHeaders.get(i).value());
            }
        }
    }

    @Test
    public void testPut() throws Exception {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("id", 101);
        assertEquals(jsonResponse.toString(), WS.url("http://localhost:9003/ressource/%s",  "名字").params(params).put().getJson().toString());
        File fileToSend = new File(new URLDecoder().decode(getClass().getResource("/kiki.txt").getFile(), "UTF-8"));
        assertTrue(fileToSend.exists());
        assertEquals("POSTED!", WS.url("http://localhost:9003/ressource/file/%s", "名字").files(new FileParam(fileToSend, "file")).put().getString());
        assertEquals("FILE AND PARAMS POSTED!", WS.url("http://localhost:9003/ressource/fileAndParams/%s",  "名字").files(new FileParam(fileToSend, "file")).params(params).put().getString());
    }

    @Test
    public void testParallelCalls() throws Exception {
        Future<HttpResponse> response = WS.url("http://localhost:9003/ressource/%s", "ééééééçççççç汉语漢語").getAsync();
        Future<HttpResponse> response2 = WS.url("http://localhost:9003/ressource/%s", "foobar").getAsync();
        int success = 0;
        while (success < 2) {
            if (response.isDone()) {
                assertEquals("对!", response.get().getString());
                success++;
            }
            if (response2.isDone()) {
                assertEquals("toto", response2.get().getString());
                success++;
            }
            Thread.sleep(1000);
        }
    }
    
    @Test
    public void testEncodingOfParams() throws Exception {
        // related to #737
        Map<String, Object> params = new HashMap<String, Object>();
        params.put( "paramÆØÅ", "%%%æøåÆØÅ");
        
        String res = WS.url("http://localhost:9003/ressource/returnParam").params(params).get().getString();
        Logger.info("res: " + res);
        assertEquals("param: %%%æøåÆØÅ", res);
        
        // try it again with different encoding
        HttpResponse r = WS.withEncoding("iso-8859-1").url("http://localhost:9003/ressource/returnParam").params(params).get();
        Logger.info("res.contentType: " + r.getContentType());
        assertEquals("param: %%%æøåÆØÅ", r.getString());
        
        // do the same with post..
        res = WS.url("http://localhost:9003/ressource/returnParam").params(params).post().getString();
        Logger.info("res: " + res);
        assertEquals("param: %%%æøåÆØÅ", res);
        
        // try it again with different encoding
        r = WS.withEncoding("iso-8859-1").url("http://localhost:9003/ressource/returnParam").params(params).post();
        Logger.info("res.contentType: " + r.getContentType());
        assertEquals("param: %%%æøåÆØÅ", r.getString());
        
        
    }

    @Test
    public void testEncodingEcho() {
        // verify that we have no encoding regression bugs related to raw urls and params
        if ( play.Play.defaultWebEncoding.equalsIgnoreCase("utf-8") ) {
            assertEquals("æøå|a|æøå|a|x|b|æøå|body||id|æøå", WS.url("http://localhost:9003/encoding/echo/%C3%A6%C3%B8%C3%A5?a=%C3%A6%C3%B8%C3%A5&a=x&b=%C3%A6%C3%B8%C3%A5").get().getString());
        }
	    assertEquals("abc|a|æøå|a|x|b|æøå|body||id|abc", WS.url("http://localhost:9003/encoding/echo/abc?a=æøå&a=x&b=æøå").get().getString());
	 	assertEquals("æøå|a|æøå|a|x|b|æøå|body||id|æøå", WS.url("http://localhost:9003/encoding/echo/%s?a=æøå&a=x&b=æøå", "æøå").get().getString());

        assertEquals("æøå|a|æøå|a|x|b|æøå|body||id|æøå", WS.url("http://localhost:9003/encoding/echo/%s?", "æøå").setParameter("a",new String[]{"æøå","x"}).setParameter("b","æøå").get().getString());
        // test with value including '='
        assertEquals("abc|a|æøå|a|x|b|æøå=|body||id|abc", WS.url("http://localhost:9003/encoding/echo/abc?a=æøå&a=x&b=æøå=").get().getString());
        //test with 'flag'
		
        assertEquals("abc|a|flag|b|flag|body||id|abc", WS.url("http://localhost:9003/encoding/echo/abc?a&b=").get().getString());
        
        // verify url ending with only ? or none
        assertEquals("abc|body||id|abc", WS.url("http://localhost:9003/encoding/echo/abc?").get().getString());
        assertEquals("abc|body||id|abc", WS.url("http://localhost:9003/encoding/echo/abc").get().getString());
    }

    @Test
    public void testWSAsyncWithException() {
        String url = "http://localhost:9003/SlowResponseTestController/testWSAsyncWithException";
        String res = WS.url(url).get().getString();
        assertEquals("ok", res);
    }

    // Test our "Denial of Service through hash table multi-collisions"-protection
    @Test
    public void testPostHashCollisionProtection() {
        // generate some post data with 1000 params
        // PS: these keys does not have hash-colition, but our protection is only looking at the count
        Map<String, Object> manyParams = new HashMap<String, Object>();
        for ( int i=0; i < 1000; i++) {
            manyParams.put("a"+i, ""+i);
        }

        assertEquals("POST", WS.url("http://localhost:9003/Rest/echoHttpMethod").params(manyParams).post().getString());

        // now add one more to push the limit
        manyParams.put("anotherone", "x");
        // 413 Request Entity Too Large
        assertEquals(413, (int)WS.url("http://localhost:9003/Rest/echoHttpMethod").params(manyParams).post().getStatus());
    }

}
