package play.libs;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.io.CleanupMode.ALWAYS;

class IOTest {

    @Test void copyDirectoryShouldCopyDirectoryContentRecursively(@TempDir(cleanup = ALWAYS) File target) throws Exception {
        File source = new File(getClass().getResource(getClass().getSimpleName() + "/copyDirectory").toURI());
        if (target.exists()) {
            FileUtils.forceDelete(target);
        }

        assertThat(source).exists();
        assertThat(target).doesNotExist();

        IO.copyDirectory(source, target);

        assertThat(target).exists();
        try (var files = Files.walk(source.toPath())) {
            files.forEach(file -> {
                var original = source.toPath().relativize(file);
                var copy = target.toPath().resolve(original);
                assertThat(copy).exists();
                if (Files.isRegularFile(copy)) {
                    assertThat(copy).hasSameBinaryContentAs(file);
                }
            });
        }
    }
}
