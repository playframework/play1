package play.libs;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marek Piechut
 */
public class FilesTest {

    @Test
    public void testSanitizeFileName() throws Exception {
        //File names to test are on odd indexes and expected results are on even indexes, ex:
        //test_file_name, expected_file_name
        String[] FILE_NAMES = {
                null, null,
                "", "",
                "a", "a",
                "test.file", "test.file",
                "validfilename-,^&'@{}[],$=!-#()%.+~_.&&&", "validfilename-,^&'@{}[],$=!-#()%.+~_.&&&",
                "invalid/file", "invalid_file",
                "invalid\\file", "invalid_file",
                "invalid:*?\\<>|/file", "invalid________file",
        };

        for (int i = 0; i < FILE_NAMES.length; i += 2) {
            String actual = Files.sanitizeFileName(FILE_NAMES[i]);
            String expected = FILE_NAMES[i + 1];

            assertEquals("String was not sanitized properly", expected, actual);
        }
    }
}
