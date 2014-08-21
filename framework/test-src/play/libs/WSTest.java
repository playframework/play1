package play.libs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;


/**
 * Tests for {@link WS} class.
 */
public class WSTest {
    
    @Test
    public void getQueryStringTest(){
        TestHttpResponse response = new TestHttpResponse("a=&b= etc");
        Map<String, String>  queryStr = response.getQueryString();
        assertNotNull(queryStr);
        assertEquals("", queryStr.get("a"));
        assertEquals(" etc", queryStr.get("b")); 
        assertEquals(2, queryStr.size());
    }
    
    @Test
    public void getQueryStringTest1(){
        TestHttpResponse response = new TestHttpResponse("a&b= etc&&&d=test toto");
        Map<String, String>  queryStr = response.getQueryString();
        assertNotNull(queryStr);
        assertEquals("", queryStr.get("a"));
        assertEquals(" etc", queryStr.get("b"));
        assertEquals("", queryStr.get(""));
        assertEquals("test toto", queryStr.get("d"));     
        assertEquals(4, queryStr.size());
    }
    
    @Test
    public void getQueryStringTest2(){
        TestHttpResponse response = new TestHttpResponse("&a&b= etc&&d=**");
        Map<String, String>  queryStr = response.getQueryString();
        assertNotNull(queryStr);
        assertEquals("", queryStr.get("a"));
        assertEquals(" etc", queryStr.get("b"));
        assertEquals("", queryStr.get(""));
        assertEquals("**", queryStr.get("d")); 
        assertEquals(4, queryStr.size());
    }
}
