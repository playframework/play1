import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import play.Play;
import play.Logger;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.test.FunctionalTest;


public class XForwardedSupportTest extends FunctionalTest {
    private static final String xForwardedFor = "10.10.10.10";
    private static final String PAGE_URL = "/users/list";
    private static final String HEADER_XFORWARDED_FOR = "x-forwarded-for";
    private static final String CONFIG_XFORWARD_SUPPORT = "XForwardedSupport"; 

    @Test
    public void testValidXForwards() throws Exception {
    	//Values from application.conf (with commas and comma-spaces for delimiters)
    	//XForwardedSupport=127.0.0.1,1.2.3.4, 5.5.5.5
    	//These are valid remoteAddresses

  		String remoteAddress = "1.2.3.4";
		assertValidTest(remoteAddress, xForwardedFor);

  		remoteAddress = "127.0.0.1";
		assertValidTest(remoteAddress, xForwardedFor);

  		remoteAddress = "5.5.5.5";
		assertValidTest(remoteAddress, xForwardedFor);
	}
	
	@Test
    public void testInvalidXForwards() throws Exception {
  		String remoteAddress = "1.2.3.5";
		assertInvalidTest(remoteAddress, xForwardedFor);

  		remoteAddress = "6.6.6.6";
		assertInvalidTest(remoteAddress, xForwardedFor);
    }

    @Test
    public void testAllForwardSupoprt(){
	// modify play configuration, to test ALL
	String prev = null;
	try{
	    prev = Play.configuration.getProperty(CONFIG_XFORWARD_SUPPORT);
	    Play.configuration.setProperty(CONFIG_XFORWARD_SUPPORT, "ALL");

	    // These are valid if you set XFowardedSupport to ALL 
	    String remoteAddress = "1.2.3.5";
	    assertValidTest(remoteAddress, xForwardedFor);

	    remoteAddress = "6.6.6.6";
	    assertValidTest(remoteAddress, xForwardedFor);

	    remoteAddress = "16.16.16.16";
	    assertValidTest(remoteAddress, xForwardedFor);

	}finally{
	    //restore
	    Play.configuration.setProperty(CONFIG_XFORWARD_SUPPORT, prev);
	}
    }

	private void assertValidTest(String remoteAddress, String xForwardedFor) {
		Request request = getRequest(remoteAddress, xForwardedFor);
		Response response = GET(request, PAGE_URL); 
		assertIsOk(response);
		
		//remoteAddress should be changed to xForwardedFor address
		assertEquals(xForwardedFor, request.remoteAddress);
	}

	private void assertInvalidTest(String remoteAddress, String xForwardedFor) {
		try {
			Request request = getRequest(remoteAddress, xForwardedFor);
			Response response = GET(request, PAGE_URL);
			fail("XForwarded request should have thrown a runtime exception.");
		} catch (RuntimeException re) {
			assertTrue(re.getMessage().contains(remoteAddress));
		}
	}

	private Request getRequest(String remoteAddress, String xForwardedFor) {
        Map<String, Header> headers = new HashMap<String, Header>();

        Header header = new Header();
        header.name = HEADER_XFORWARDED_FOR;
        header.values = Arrays.asList(new String[]{xForwardedFor});

        headers.put(HEADER_XFORWARDED_FOR, header);

		Request request = Request.createRequest(
                remoteAddress,
                "GET",
                "/",
                "",
                null,
                null,
                null,
                null,
                false,
                80,
                "localhost",
                false,
                headers,
                null
        );
 
 		return request;
	}
}
