package play.data.binding;

import org.junit.Test;
import play.data.parsing.ApacheMultipartParser;
import play.data.parsing.DataParsers;
import play.data.parsing.TextParser;
import play.data.parsing.UrlEncodedParser;

import static org.junit.Assert.assertEquals;

public class DataParsersTest {
    @Test
    public void getDataParserDependingOnContentType() {
        assertEquals(UrlEncodedParser.class, DataParsers.forContentType("application/x-www-form-urlencoded").getClass());
        assertEquals(ApacheMultipartParser.class, DataParsers.forContentType("multipart/form-data").getClass());
        assertEquals(ApacheMultipartParser.class, DataParsers.forContentType("multipart/mixed").getClass());
        assertEquals(TextParser.class, DataParsers.forContentType("application/xml").getClass());
        assertEquals(TextParser.class, DataParsers.forContentType("application/json").getClass());
    }
}
