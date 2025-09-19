package play.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class FileUploadTest {
  @Test
  public void sizeIsNullForMissingFile() {
    assertNull(new FileUpload().getSize());
  }
}
