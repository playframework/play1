import static org.junit.Assert.assertEquals;

import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.ws.WSUrlFetch;
import play.test.UnitTest;

import java.lang.reflect.Field;


public class WSUrlFetchTest extends UnitTest {

    private static WS.WSImpl orgWsImpl;
    private static Field field;


    @BeforeClass
    public static void beforeClass() throws Exception {
        // make sure we use WSUrlFetch as WS impl

        // store org impl
        // call WS.url() to make sure it is initialized by default
        WS.url("");
        field = WS.class.getDeclaredField("wsImpl");
        field.setAccessible(true);
        orgWsImpl = (WS.WSImpl)field.get(null);

        // set WSUrlFetch impl
        field.set(null, new WSUrlFetch());

    }

    @AfterClass
    public static void afterClass() throws Exception {
        // Restore to default WS impl
        field.set(null, orgWsImpl);
    }


    @Test
    public void test() throws Exception {
        final String url = "http://localhost:9003/Rest/echoHttpMethod";
        assertEquals("GET", WS.url(url).get().getString());
        assertEquals("GET", WS.url(url).setParameter("name", "value").get().getString());
        assertEquals("GET a=1 b=2", WS.url(url+"?a=1&b=2").get().getString());
        assertEquals("GET a=1 b=2", WS.url(url+"?a=1").setParameter("b", "2").get().getString());

        assertEquals("POST", WS.url(url).post().getString());
        assertEquals("POST", WS.url(url).setParameter("name", "value").post().getString());
        assertEquals("POST a=1 b=2", WS.url(url+"?a=1&b=2").post().getString());
        assertEquals("POST a=1 b=2", WS.url(url+"?a=1").setParameter("b", "2").post().getString());

        assertEquals("DELETE", WS.url(url).delete().getString());
        assertEquals("DELETE", WS.url(url).setParameter("name", "value").delete().getString());
        assertEquals("DELETE a=1 b=2", WS.url(url+"?a=1&b=2").delete().getString());
        assertEquals("DELETE a=1 b=2", WS.url(url+"?a=1").setParameter("b", "2").delete().getString());

        assertEquals("PUT", WS.url(url).put().getString());
        assertEquals("PUT", WS.url(url).setParameter("name", "value").put().getString());
        assertEquals("PUT a=1 b=2", WS.url(url+"?a=1&b=2").put().getString());
        assertEquals("PUT a=1 b=2", WS.url(url+"?a=1").setParameter("b", "2").put().getString());

        // Head does not appear to work with UrlFetch
        // assertEquals("HEAD", WS.url(url).head().getString());
    }
    
    @Test
    public void multipleGetContentTest(){
        String url = "http://google.com";
        HttpResponse response = WS.url( url ).post();
        String resp1 = response.getString();        
        String resp2 = response.getString();
        assertEquals(resp1, resp2);  
        assertNotEquals("", resp2);   
    }
}
