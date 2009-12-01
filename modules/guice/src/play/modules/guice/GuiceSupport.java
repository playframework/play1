package play.modules.guice;

import com.google.inject.Injector;
 /**
  * should be implemented if a custom injector is desired
  */

public abstract class GuiceSupport {
  protected abstract Injector configure();
}
