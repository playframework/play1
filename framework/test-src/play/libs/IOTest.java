package play.libs;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class IOTest {
    @Test
    public void copyDirectory_copiesFilesWithSpecialCharacterInName() {
        String fileName = "Anhang Performance-Bericht_ Übersicht §18 Bundesbank gesetz Meldepflichten.PDF";
        File source = new File("test-src/play/libs/Unterlagen BBank-Reporting");
        File target = new File("test-classes/play/libs");
        File targetFile = new File(target, fileName);
        if (targetFile.exists()) {
            assertTrue(targetFile.delete());
        }
        assertTrue(source.exists());
        assertTrue(target.exists());
        
        IO.copyDirectory(source, target);

        assertTrue(targetFile.exists());
    }
}