package application;

import org.junit.Test;
import play.test.FunctionalTest;
import play.mvc.Http.Response;

public class SomeTests extends FunctionalTest {

  @Test
  public void fakeTest() {
    assertEquals(2, 1 + 1); // A really important thing to test
  }

  @Test
  public void indexTest() {
    // make a request on the application (embedded)
    Response response = GET("/");
    // check the HTTP response status is OK
    assertStatus(200, response);
    // check the response is HTML
    assertContentType("text/html", response);
    // check the declared charset encoding
    assertCharset(play.Play.defaultWebEncoding, response);
    // check some content in the page. may also test a regexp
    assertContentMatch("<h1>Validation samples</h1>", response);
  }
}

