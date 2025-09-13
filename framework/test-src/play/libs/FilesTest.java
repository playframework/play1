package play.libs;



import java.io.File;

import org.junit.jupiter.api.Test;

import play.utils.OS;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Marek Piechut
 */
public class FilesTest {

    @Test
    public void testSanitizeFileName() throws Exception {
        // File names to test are on odd indexes and expected results are on even indexes, ex:
        // test_file_name, expected_file_name
        String[] FILE_NAMES = { null, null, "", "", "a", "a", "test.file", "test.file", "validfilename-,^&'@{}[],$=!-#()%.+~_.&&&",
                "validfilename-,^&'@{}[],$=!-#()%.+~_.&&&", "invalid/file", "invalid_file", "invalid\\file", "invalid_file",
                "invalid:*?\\<>|/file", "invalid________file", };

        for (int i = 0; i < FILE_NAMES.length; i += 2) {
            String actual = Files.sanitizeFileName(FILE_NAMES[i]);
            String expected = FILE_NAMES[i + 1];

            assertEquals("String was not sanitized properly", expected, actual);
        }
    }

    @Test
    public void testFileEqualsOnWindows() {
        if (OS.isWindows()) {
            File a = null;
            File b = null;

            a = new File("C:\\temp\\TEST.TXT");
            b = new File("C:\\temp\\TEST.TXT");
            assertTrue( Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));

            a = new File("C:\\temp\\TEST.TXT");
            b = new File("C:\\temp\\TEST.TXT");
            assertTrue(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()) );

            a = new File("C:\\temp\\TEST.TXT");
            b = new File("C:\\temp\\test.txt");
            assertTrue( Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));

            a = new File("C:\\temp\\TEST.TXT");
            b = new File("C:\\temp\\.\\test.txt");
            assertTrue( Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));

            a = new File("C:\\temp\\..\\TEMP\\TEST.TXT");
            b = new File("C:\\temp\\.\\test.txt");
            assertTrue(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));
        }
    }

    @Test
    public void testFileEquals() {
        File a = null;
        File b = null;

        a = new File("temp\\TEST.TXT");
        b = new File("temp\\TEST.TXT");
        assertTrue(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));

        a = new File("\\temp\\TEST.TXT");
        b = new File("\\temp\\TEST.TXT");
        assertTrue(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));

        a = new File("\\temp\\TEST.TXT");
        b = new File("\\temp\\test.txt");
        if (OS.isWindows()) {
            assertTrue(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));
        } else {
            assertFalse(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));
        }

        a = new File("/temp/TEST.TXT");
        b = new File("/temp/TEST.TXT");
        assertTrue(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));

        a = new File("/temp/TEST.TXT");
        b = new File("/temp/test.txt");
        if (OS.isWindows()) {
            assertTrue(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));
        } else {
            assertFalse(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));
        }
    }

    @Test
    public void testFileEqualsWithParentCurrentFolder() {
        File a = null;
        File b = null;

        a = new File("\\temp\\test.txt");
        b = new File("\\temp\\.\\test.txt");
        if (OS.isWindows()) {
            assertTrue( Files.isSameFile(a, b),String.format("Error comparing %s and %s", a.getPath(), b.getPath()));
        } else {
            assertFalse( Files.isSameFile(a, b),String.format("Error comparing %s and %s", a.getPath(), b.getPath()));
        }

        a = new File("/temp/../temp/test.txt");
        b = new File("/temp/test.txt");
        assertTrue(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));

        a = new File("/temp/test.txt");
        b = new File("/temp/./test.txt");
        assertTrue(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));

        a = new File("/temp/../temp/test.txt");
        b = new File("/temp/./test.txt");
        assertTrue(Files.isSameFile(a, b), String.format("Error comparing %s and %s", a.getPath(), b.getPath()));
    }
}
