package play.libs;

import org.junit.Test;
import play.Play;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExpressionTest {
  @Test
  public void evaluate() {
    assertEquals("some.property", Expression.evaluate("some.property", "defaultValue"));
  }

  @Test
  public void evaluateInjectsPropertyValue() {
    Play.configuration = new Properties();
    Play.configuration.setProperty("some.property", "awSomeValue");
    assertEquals("awSomeValue", Expression.evaluate("${some.property}", "defaultValue"));
  }

  @Test
  public void evaluateInjectsDefaultValueIfPropertyIsMissing() {
    Play.configuration = new Properties();
    assertEquals("defaultValue", Expression.evaluate("${some.property}", "defaultValue"));
  }

  @Test
  public void evaluateNullReturnsNull() {
    assertNull(Expression.evaluate(null, "defaultValueIsIgnored"));
  }
}
