import org.junit.Test;

import play.libs.URLs;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class RedirectTest extends FunctionalTest {

	@Test
	public void redirectHttp_expectOk() {

		final Response response = GET("/redirector/index?target=http://google.com");
		assertStatus(302, response);
		final String location = response.headers.get("Location").value();

		assertEquals("http://google.com", location);

	}

	// prove fix of [#1557] - also support non-http redirects
	@Test
	public void redirectFtp_expectOk() {

		final Response response = GET("/redirector/index?target=ftp://google.com");
		assertStatus(302, response);
		final String location = response.headers.get("Location").value();

		assertEquals("ftp://google.com", location);

	}

	@Test
	public void redirectrelative_expectOk() {
		final Request req = newRequest();
		req.port = 2003;
		final Response response = GET(req, "/redirector/index?target=/someurl");
		assertStatus(302, response);
		final String location = response.headers.get("Location").value();

		// note: only works if port is not 80
		// in that case relative url is returned for ease of testing
		assertEquals("http://" + req.domain + ":" + req.port + "/someurl", location);

	}
	
	//[#1675] exponential redirect regression
	@Test
	public void timeSpendInRederect_expectContstant() {

		long shortUrlmSecs = timeRedirectRequest("result-a/");
		long longUrlmSecs =  timeRedirectRequest("result-abcdefghijklmnopq/");
		// on core2 duo timing used to be 30ms vs 2000ms 
		// should be constant now
		String msg = String.format("long redirect takes exponentially longer %s vs %s", shortUrlmSecs, longUrlmSecs);
		assertTrue(msg, longUrlmSecs < 400 );
		assertTrue(msg, shortUrlmSecs < 400 );
		
	}
	//[#1675] make sure Action redirects still work
	@Test
	public void actionRedirect_expectOK() {

		final Request req = newRequest();
		req.port = 2003;
		final Response response = GET(req, "/redirector/index?target=Application.hello");
		assertStatus(302, response);
		final String location = response.headers.get("Location").value();

		// note: only works if port is not 80
		// in that case relative url is returned for ease of testing
		assertEquals("http://" + req.domain + ":" + req.port + "/sayHello", location);
		
	}


	
	

	private long timeRedirectRequest(String target){
		long start = System.currentTimeMillis();
		final Request req = newRequest();
		final Response response = GET(req, "/redirector/index?target=" + URLs.encodePart(target));
		assertStatus(302, response);		
		return( System.currentTimeMillis() - start);
	}

}
