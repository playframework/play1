package plugins;

import play.PlayPlugin;
import play.mvc.Http;
import play.mvc.Scope;
import services.Mathematics;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.math.BigDecimal;

public class DebugPlugin extends PlayPlugin {
  @Inject private Mathematics mathematics;

  @Override
  public void beforeActionInvocation(Method actionMethod) {
    Scope.RenderArgs renderArgs = Scope.RenderArgs.current();
    Http.Request request = Http.Request.current();
    renderArgs.put("actionName", request.action);
    renderArgs.put("actionMethod", actionMethod.getName());
    renderArgs.put("math", ": SQRT(2)=" + mathematics.sqrt(new BigDecimal(2)));
  }
}
