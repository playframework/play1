package play.test;

import static play.test.FunctionalTest.*;

import org.junit.jupiter.api.Test;

import play.mvc.Http.Response;

public class ContentTypeAssertionTest {

    @Test
    public void givenContentTypeIsMissing_shouldThrowAssertionError() {
        assertThrows(AssertionError.class, () -> {
            Response responseWithoutContentType = new Response();
            assertContentType("text/html", responseWithoutContentType);
        });
    }
}
