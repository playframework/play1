package play.data;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class FileUploadTest {
  @Test
  public void sizeIsNullForMissingFile() {
    assertNull(new FileUpload().getSize());
  }
}
