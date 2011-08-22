package play.templates;

import static org.junit.Assert.*;
import static org.fest.assertions.Assertions.assertThat;

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

    //Trivial no test @Test public void testEnumValues()  {}

    @Test 
    public void testContains()  {
        String[] testArray = {"a", "b", "c"};
        assertTrue(JavaExtensions.contains(testArray, "a"));
        assertFalse(JavaExtensions.contains(testArray, "1"));
    }

    //TODO @Test public void testAsXml()  {}

    @Test 
    public void testAdd()  {
        String[] testArray = {"a", "b", "c"};
        assertThat(JavaExtensions.add(new String[]{"a", "b"}, "c")).hasSize(3).contains(testArray);
        
    }

    @Test 
    public void testRemove()  {
        String[] testArray = {"a", "b", "c"};
        assertThat(JavaExtensions.remove(testArray, "c")).hasSize(2).contains("a", "b");
    }

    //TODO @Test public void testToStringClosure()  {}

    @Test 
    public void testCapitalizeWords()  {
        assertThat(JavaExtensions.capitalizeWords("This is a small   test!")).as("This Is A Small  Test!");
    }

    @Test 
    public void testPad()  {
        assertThat(JavaExtensions.pad("12345", 4)).as("12345");
        assertThat(JavaExtensions.pad("12345", 5)).as("12345");
        assertThat(JavaExtensions.pad("12345", 6)).as("12345&nbsp;");
        assertThat(JavaExtensions.pad("12345", 8)).as("12345&nbsp;&nbsp;&nbsp;");
    }

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

    @Test 
    public void testYesno()  {
        String yes = "Y";
        String no = "N";
        String[] yesNo = {yes, no};
        assertEquals(no, JavaExtensions.yesno(null, yesNo));
        assertEquals(no, JavaExtensions.yesno(Boolean.FALSE, yesNo));
        assertEquals(yes, JavaExtensions.yesno(Boolean.TRUE, yesNo));
        //String
        assertEquals(no, JavaExtensions.yesno("", yesNo));
        assertEquals(yes, JavaExtensions.yesno("Test", yesNo));
        //Number
        assertEquals(no, JavaExtensions.yesno(Long.valueOf(0), yesNo));
        assertEquals(yes, JavaExtensions.yesno(Long.valueOf(1), yesNo));
        assertEquals(yes, JavaExtensions.yesno(Long.valueOf(-1), yesNo));
        //Collection
        List <String> testCollection = new ArrayList <String>();
        assertEquals(no, JavaExtensions.yesno(testCollection, yesNo));
        testCollection.add("1");
        assertEquals(yes, JavaExtensions.yesno(testCollection, yesNo));
        
    }

    @Test 
    public void testLast()  {
        List <String> testCollection = new ArrayList <String>();
        testCollection.add("1");
        testCollection.add("2");
        assertEquals("2", JavaExtensions.last(testCollection));
    }

    @Test 
    public void testJoin()  {
        List <String> testCollection = new ArrayList <String>();
        testCollection.add("1");
        testCollection.add("2");
        
        assertEquals("1, 2", JavaExtensions.join(testCollection, ", "));
    }

}
