package play.data.binding;

import org.junit.Test;
import play.data.parsing.ApacheMultipartParser;
import play.data.parsing.DataParsers;
import play.data.parsing.TextParser;
import play.data.parsing.UrlEncodedParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataParsersTest {
    @Test
    public void getDataParserDependingOnContentType() {
        assertEquals(UrlEncodedParser.class, DataParsers.forContentType("application/x-www-form-urlencoded").getClass());
        assertEquals(ApacheMultipartParser.class, DataParsers.forContentType("multipart/form-data").getClass());
        assertEquals(ApacheMultipartParser.class, DataParsers.forContentType("multipart/mixed").getClass());
        assertEquals(TextParser.class, DataParsers.forContentType("application/xml").getClass());
        assertEquals(TextParser.class, DataParsers.forContentType("application/json").getClass());
    }

    @Test
    public void usesTextDataProviderForAnyContentTypeStartingWithText() {
        assertEquals(TextParser.class, DataParsers.forContentType("text/").getClass());
        assertEquals(TextParser.class, DataParsers.forContentType("text/plain").getClass());
        assertEquals(TextParser.class, DataParsers.forContentType("text/anything else").getClass());
    }

    @Test
    public void returnsNullForUnsupportedContentTypes() {
        assertNull(DataParsers.forContentType("unknown"));
        assertNull(DataParsers.forContentType(""));
        assertNull(DataParsers.forContentType("text"));
    }
}
