package play.test;

import static org.junit.Assert.*;
import static play.test.FunctionalTest.*;

import org.junit.Test;

import play.mvc.Http.Response;

public class ContentTypeAssertionTest {

	@Test(expected=AssertionError.class)
	public void givenContentTypeIsMissing_shouldThrowAssertionError() {
		Response responseWithoutContentType = new Response();

		assertContentType("text/html", responseWithoutContentType);
	}

}
