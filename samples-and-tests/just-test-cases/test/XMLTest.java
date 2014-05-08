import org.junit.Test;
import org.w3c.dom.Document;
import play.libs.XML;

import static org.junit.Assert.assertFalse;

public class XMLTest {
  @Test
  public void noXXEVulnerability() {
    Document doc = XML.getDocument("<!DOCTYPE foo [ <!ENTITY xxe SYSTEM \"file:///etc/passwd\" >]><foo>&xxe;</foo>");
    assertFalse(doc.getDocumentElement().getTextContent().contains("root"));
  }
}
