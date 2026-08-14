package play.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileUploadTest {
  @Test
  public void sizeIsZeroForMissingFile() {
    assertEquals(0L, new FileUpload().getSize());
  }
}
