package play.db;

import org.junit.*;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;

public class SQLSplitterTest {

    @Test
    public void verifyConsumeLine() {
        assertEquals(4, SQLSplitter.consumeTillNextLine("abc\ra", 0));
    }

    @Test
    public void verifySkipComments() {
        assertEquals(8, SQLSplitter.consumeComment("--hello\rSELECT * from STUDENTS;", 0));
        assertEquals(8, SQLSplitter.consumeComment("--hello\nSELECT * from STUDENTS;", 0));

        assertEquals(8, SQLSplitter.consumeComment("#hello\r\nSELECT * from STUDENTS;", 0));
        assertEquals(7, SQLSplitter.consumeComment("#hello\rSELECT * from STUDENTS;", 0));
        assertEquals(7, SQLSplitter.consumeComment("#hello\nSELECT * from STUDENTS;", 0));

        assertEquals(8, SQLSplitter.consumeComment("/*h\r\nw*/SELECT * from STUDENTS;", 0));
        assertEquals(9, SQLSplitter.consumeComment("/*hello*/SELECT * from STUDENTS;", 0));
    }

    @Test
    public void verifyDontSkipComments() {
        assertEquals(0, SQLSplitter.consumeComment("SELECT * from STUDENTS;", 0));
        assertEquals(0, SQLSplitter.consumeComment("SELECT * from STUDENTS; #h", 0));
        assertEquals(0, SQLSplitter.consumeComment("-a * from STUDENTS;", 0));
        assertEquals(0, SQLSplitter.consumeComment("a * from STUDENTS; #h", 0));

        assertEquals(9, SQLSplitter.consumeComment("/*hello*/SELECT * from STUDENTS;", 9));
        assertEquals(7, SQLSplitter.consumeComment("#hello\nSELECT * from STUDENTS;", 7));
    }

    @Test
    public void verifySkipStandardQuotes() {
        assertEquals(4, SQLSplitter.consumeQuote("\"12\"w", 0));
        assertEquals(4, SQLSplitter.consumeQuote("'12'w", 0));
        assertEquals(4, SQLSplitter.consumeQuote("`12`w", 0));
        assertEquals(4, SQLSplitter.consumeQuote("[12]w", 0));
    }

    @Test
    public void verifyDontSkipStandardQuotes() {
        assertEquals(4, SQLSplitter.consumeQuote("\"12\"w", 4));
        assertEquals(4, SQLSplitter.consumeQuote("'12'w", 4));
        assertEquals(4, SQLSplitter.consumeQuote("`12`w", 4));
        assertEquals(4, SQLSplitter.consumeQuote("[12]w", 4));

        assertEquals(0, SQLSplitter.consumeQuote("123", 0));
    }

    @Test
    public void verifySkipDollarQuotes() {
        assertEquals(5, SQLSplitter.consumeQuote("$$a$$b", 0));
        assertEquals(6, SQLSplitter.consumeQuote("$$ab$$c", 0));
        assertEquals(7, SQLSplitter.consumeQuote("$1$a$1$b", 0));
        assertEquals(8, SQLSplitter.consumeQuote("$1$a\n$1$b", 0));
        assertEquals(9, SQLSplitter.consumeQuote("$12$a$12$b", 0));
        assertEquals(10, SQLSplitter.consumeQuote("$12$ab$12$c", 0));
        assertEquals(11, SQLSplitter.consumeQuote("$12$a\nb$12$c", 0));

        assertEquals(15, SQLSplitter.consumeQuote("$1$ $f$ $f$ $1$a", 0));
        assertEquals(12, SQLSplitter.consumeQuote("$1$$f$$f$$1$a", 0));
        assertEquals(13, SQLSplitter.consumeQuote("$1$$f$\n$f$$1$a", 0));
    }


    @Test
    public void verifySkipParentheses() {
        assertEquals(4, SQLSplitter.consumeParentheses("(())", 0));
        assertEquals(4, SQLSplitter.consumeParentheses("(())a", 0));
        assertEquals(5, SQLSplitter.consumeParentheses("((b))a", 0));
        assertEquals(7, SQLSplitter.consumeParentheses("(c(b)c)a", 0));
        assertEquals(7, SQLSplitter.consumeParentheses("(c(\n)c)a", 0));
        assertEquals(7, SQLSplitter.consumeParentheses("((')'))a", 0));
        assertEquals(9, SQLSplitter.consumeParentheses("((/*)*/))a", 0));
        assertEquals(14, SQLSplitter.consumeParentheses("(name varchar);", 0));
    }

    @Test
    public void verifyTrailingParenthesis() {
        assertEquals(1, SQLSplitter.consumeParentheses("(", 0));
        assertEquals(3, SQLSplitter.consumeParentheses("(()", 0));
    }

    @Test
    public void verifyDoubleSemicolonHandling() {
        assertEquals(2, SQLSplitter.splitSQL("a;\nb;;\nc;").size());
        assertEquals(3, SQLSplitter.splitSQL("a;\nb;\nc;").size());
    }

    String readFile(String filename) throws Exception {
        File src = new File(getClass().getResource(filename).toURI());
        byte[] srcbytes = new byte[(int) src.length()];

        new FileInputStream(src).read(srcbytes);
        return new String(srcbytes, "UTF-8");
    }

    @Test
    public void verifyTestSplitting() throws Exception {
        java.util.ArrayList<CharSequence> srcArrList = SQLSplitter.splitSQL(readFile("/play/db/test.sql"));
        CharSequence[] srcArr = new CharSequence[(int) srcArrList.size()];
        srcArr = srcArrList.toArray(srcArr);

        assertEquals(readFile("/play/db/test.out.sql").split("==="), srcArr);

        srcArrList = SQLSplitter.splitSQL(readFile("/play/db/test2.sql"));
        srcArr = new CharSequence[(int) srcArrList.size()];
        srcArr = srcArrList.toArray(srcArr);
        assertEquals(readFile("/play/db/test2.out.sql").split("==="), srcArr);
    }
}
