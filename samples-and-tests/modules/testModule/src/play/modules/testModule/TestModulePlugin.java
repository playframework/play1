package play.modules.testModule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.Play;
import play.Play.Mode;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.mvc.Router.Route;
import play.mvc.Scope;
import play.mvc.Scope.Params;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.RouteArgs;
import play.mvc.Scope.Session;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

public class TestModulePlugin extends PlayPlugin {

  @Override
  public void onLoad() {
  }

  @Override
  public boolean compileSources() {
    return super.compileSources();
  }

 
  @Override
  public void afterApplicationStart() {
  }

    @Override
    public boolean serveStatic(VirtualFile file, Request request,
	    Response response) {
	boolean ret = false;

	if (file.getName().startsWith("session.test")) {
	    Logger.error("##### %s", file.getName());
	    if (request != null && request.params != null) {
		Logger.error(request.params.toString());
	    }

	    if (request != null && request.args != null) {
		Logger.error(request.args.toString());
	    }

	    RouteArgs routeArgs = RouteArgs.current();
	    if (routeArgs != null && routeArgs.data != null) {
		Logger.error(routeArgs.data.toString());
	    }

	    RenderArgs renderArg = RenderArgs.current();
	    if (renderArg != null && renderArg.data != null) {
		Logger.error(renderArg.data.toString());
	    }

	    Params params = Params.current();
	    if (params != null && params.data != null) {
		Logger.error(params.toString());
	    }

	    Session session = Session.current();
	    if (session != null) {
		Logger.error(session.all().toString());
	    }
	}

	return ret;
    }

}
