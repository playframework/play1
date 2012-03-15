import static org.junit.Assert.*;
import models.Author;
import models.Book;

import org.junit.Before;
import org.junit.Test;

import play.data.validation.Error;
import play.data.validation.Validation;
import play.data.validation.Validation.ValidationResult;
import play.test.Fixtures;
import play.test.UnitTest;


/**
 * Test the @Unique-annotation
 *
 */
public class UniqueTest extends UnitTest {

    @Before
    public void setUp() {
        Fixtures.deleteDatabase();
        Fixtures.loadModels("uniqueTestdata.yml");
    }
    @Test
    public void testSingleColumn() {
        Book firstBook = Book.find("byIsbn", "1").first();
        firstBook.isbn = "2";
        ValidationResult res = Validation.current().valid(firstBook);
        assertFalse(res.ok);
        assertNotNull(Validation.errors(".isbn"));
        Error error = Validation.errors(".isbn").get(0);
        assertEquals("Must be unique", error.message());
    }

    @Test
    public void testMultiColumn() {
        Author bob = Author.find("byName", "Bob").first();
        Book firstBook = Book.find("byIsbn", "1").first();
        firstBook.author = bob;
        ValidationResult res = Validation.current().valid(firstBook);
        assertFalse(res.ok);
        assertNotNull(Validation.errors(".title"));
        Error error = Validation.errors(".title").get(0);
        assertEquals("Must be unique", error.message());

        Validation.clear();
        firstBook.title = "Bobs Book";
        res = Validation.current().valid(firstBook);
        assertTrue(res.ok);
    }

}
