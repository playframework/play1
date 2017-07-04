package play.libs;

import org.junit.Test;
import play.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class IOTest {
    @Test
    public void copyDirectory_copiesFilesWithSpecialCharacterInName() throws IOException {
        Logger.info("user.dir: %s", System.getProperty("user.dir"));
        Path currentPath = Paths.get(System.getProperty("user.dir"));
        Logger.info("currentPath: %s", currentPath);
        Logger.info("file.encoding: %s", System.getProperty("file.encoding"));
        String fileName = "Anhang Performance-Bericht_ Übersicht §18 Bundesbank gesetz Meldepflichten.PDF";
        Logger.info("fileName: %s", fileName);
        Path filePath = Paths.get(fileName);
        Path sourceDirPath = Paths.get("test-src/play/libs/Unterlagen BBank-Reporting");
        Path sourceFilePath = sourceDirPath.resolve(filePath);
        Logger.info("absoluteSourceFilePath: %s, exists: %b", sourceFilePath, Files.exists(sourceFilePath));
        Path targetDirPath = Paths.get("test-classes/play/libs");

        IO.copyDirectory(sourceDirPath.toFile(), targetDirPath.toFile());

        Path targetFilePath = targetDirPath.resolve(filePath);
        assertTrue(Files.exists(targetFilePath));
        assertTrue(Files.deleteIfExists(targetFilePath));
    }
}