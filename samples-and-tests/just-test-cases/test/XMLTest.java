import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import play.libs.XML;

import static org.junit.jupiter.api.Assertions.assertNull;


public class XMLTest {
  @Test
  public void noXXEVulnerability() {
    Document doc = XML.getDocument("<!DOCTYPE foo [ <!ENTITY xxe SYSTEM \"file:///etc/passwd\" >]><foo>&xxe;</foo>");
    // DOCTYPE declarations are rejected outright (disallow-doctype-decl), so the
    // document is never built and the external entity cannot be expanded.
    assertNull(doc);
  }
}
