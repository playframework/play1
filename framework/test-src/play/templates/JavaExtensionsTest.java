package play.templates;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class JavaExtensionsTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    //TODO @Test public void testEnumValues()  {}

    //TODO @Test public void testContains()  {}

    //TODO @Test public void testAsXml()  {}

    //TODO @Test public void testAdd()  {}

    //TODO @Test public void testRemove()  {}

    //TODO @Test public void testToStringClosure()  {}

    //TODO @Test public void testCapitalizeWords()  {}

    //TODO @Test public void testPad()  {}

    //TODO @Test public void testEscapeHtml()  {}

    //TODO @Test public void testEscapeJavaScript()  {}

    //TODO @Test public void testRawObject()  {}

    //TODO @Test public void testRawObjectObject()  {}

    //TODO @Test public void testAsAttrMapObject()  {}

    //TODO @Test public void testAsAttrMap()  {}

    //TODO @Test public void testEval()  {}

    //TODO @Test public void testEscapeXml()  {}

    //TODO @Test public void testFormatNumberString()  {}

    //TODO @Test public void testFormatDate()  {}

    //TODO @Test public void testFormatDateString()  {}

    //TODO @Test public void testFormatDateStringString()  {}

    //TODO @Test public void testFormatDateStringStringString()  {}

    //TODO @Test public void testPage()  {}

    //TODO @Test public void testSinceDate()  {}

    //TODO @Test public void testSinceDateBoolean()  {}

    //TODO @Test public void testAsdateLongString()  {}

    //TODO @Test public void testAsdateLongStringString()  {}

    //TODO @Test public void testAsdateLongStringStringString()  {}

    //TODO @Test public void testNl2br()  {}

    //TODO @Test public void testUrlEncode()  {}

    //TODO @Test public void testFormatSize()  {}

    //TODO @Test public void testFormatCurrencyNumberString()  {}

    //TODO @Test public void testFormatCurrencyNumberLocale()  {}

    //TODO @Test public void testAddSlashes()  {}

    //TODO @Test public void testCapFirst()  {}

    //TODO @Test public void testCapAll()  {}

    //TODO @Test public void testCut()  {}

    //TODO @Test public void testDivisibleBy()  {}

    //TODO @Test public void testEscape()  {}

    @Test
    public void testPluralizeNumber() {
        assertEquals("s", JavaExtensions.pluralize(0));
        assertEquals("", JavaExtensions.pluralize(1));
        assertEquals("s", JavaExtensions.pluralize(2));
    }

    @Test
    public void testPluralizeCollection() {
        List <String> testCollection = new ArrayList <String>();
        assertEquals("s", JavaExtensions.pluralize(testCollection));
        testCollection.add("1");
        assertEquals("", JavaExtensions.pluralize(testCollection));
        testCollection.add("2");
        assertEquals("s", JavaExtensions.pluralize(testCollection));
    }

    @Test
    public void testPluralizeNumberString() {
        String plural = "n";
        assertEquals(plural, JavaExtensions.pluralize(0, plural));
        assertEquals("", JavaExtensions.pluralize(1, plural));
        assertEquals(plural, JavaExtensions.pluralize(2, plural));
    }

    @Test
    public void testPluralizeCollectionString() {
        String plural = "n";
        List <String> testCollection = new ArrayList <String>();
        assertEquals(plural, JavaExtensions.pluralize(testCollection, plural));
        testCollection.add("1");
        assertEquals("", JavaExtensions.pluralize(testCollection, plural));
        testCollection.add("2");
        assertEquals(plural, JavaExtensions.pluralize(testCollection, plural));
    }

    @Test
    public void testPluralizeNumberStringArray() {
        String[] forms = {"Test", "Tests"};
        assertEquals(forms[1], JavaExtensions.pluralize(0, forms));
        assertEquals(forms[0], JavaExtensions.pluralize(1, forms));
        assertEquals(forms[1], JavaExtensions.pluralize(2, forms));

    }

    @Test
    public void testPluralizeCollectionStringArray() {
        String[] forms = {"Test", "Tests"};
        List <String> testCollection = new ArrayList <String>();
        assertEquals(forms[1], JavaExtensions.pluralize(testCollection, forms));
        testCollection.add("1");
        assertEquals(forms[0], JavaExtensions.pluralize(testCollection, forms));
        testCollection.add("2");
        assertEquals(forms[1], JavaExtensions.pluralize(testCollection, forms));
    }

    //TODO @Test public void testNoAccents()  {}

    //TODO @Test public void testSlugifyString()  {}

    //TODO @Test public void testSlugifyStringBoolean()  {}

    //TODO @Test public void testCamelCase()  {}

    //TODO @Test public void testYesno()  {}

    //TODO @Test public void testLast()  {}

    //TODO @Test public void testJoin()  {}

}
