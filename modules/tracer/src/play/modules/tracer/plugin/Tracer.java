package play.modules.tracer.plugin;

import groovy.lang.Binding;

import java.lang.reflect.Method;
import java.util.Map;

import play.modules.tracer.service.TraceService;
import play.modules.tracer.ErrorExecution;
import play.modules.tracer.Execution;
import play.modules.tracer.LineExecution;
import play.modules.tracer.MethodExecution;
import play.modules.tracer.TemplateExecution;
import play.modules.tracer.Trace;
import play.modules.tracer.Variable;

import play.Logger;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.mvc.Router;
import play.mvc.Http.Request;
import play.mvc.Router.Route;
import play.mvc.Scope.RenderArgs;
import play.templates.Template;

@SuppressWarnings("unchecked")
public class Tracer extends PlayPlugin {
	public static ThreadLocal<Trace> current = new ThreadLocal<Trace>();
	public static ThreadLocal<Route> currentRoute = new ThreadLocal<Route>();
	
	public static Trace current() {
		return current.get();
	}
	
	@Override
	public void enhance(ApplicationClass applicationClass) throws Exception {
		TracerEnhancer enhancer = new TracerEnhancer();
		enhancer.enhanceThisClass(applicationClass);
	}
	
	@Override
	public void onTemplateCompilation(Template template) {
		TemplateEnhancer.enhance(template);
	}
	
	@Override
	public void onApplicationStart() {
		Logger.info("Tracer plugin starts");
		Router.addRoute("get", "/modules/tracer/traces", "Traces.list");
		Router.addRoute("get", "/modules/tracer/traces/{id}", "Traces.show");
		Router.addRoute("get", "/modules/tracer/traces/{id}/json", "Traces.jsonTrace");
		Router.addRoute("get", "/public", "staticDir:public");
		Router.addRoute("get", "/modules/tracer/source/core", "Traces.getClassSource");
		Router.addRoute("get", "/modules/tracer/source/template", "Traces.getTemplateSource");
		Router.addRoute("get", "/modules/tracer/files", "Traces.getFile");
	}
	
	@Override
	public void beforeInvocation() {
		// nothing to do right now
	}
	
	@Override
	public void onRequestRouting(Route route) {
		currentRoute.set(route);
	}
	
	@Override
	public void beforeActionInvocation(Method actionMethod) {
		if(actionMethod.getDeclaringClass().getCanonicalName().equals("controllers.Traces")) return; // prevent tracing ourselves
		Trace trace = new Trace(currentRoute.get());
		Logger.debug("TRACER beforeActionInvocation (" + Request.current().action + " [" + Request.current().toString() + "]");
		current.set(trace);
		RenderArgs.current().put("__trace_id", trace.id);
		Logger.info("Tracer starts tracking execution of %s.%s", actionMethod.getDeclaringClass().getCanonicalName(), actionMethod.getName());
	}
	
	@Override
	public void invocationFinally() {
		Trace trace = current();
		if(trace != null) {
			trace.end();
			TraceService.save(trace);
			current.remove();
		}
		currentRoute.remove();
	}
	
	@Override
	public void onInvocationException(Throwable e) {
		Logger.debug("Tracer.onInvocationException(): ", e);
		Trace trace = current();
		if(trace != null) {
			Execution error = new ErrorExecution(trace.current, trace.current.line, e);
			trace.appendExecution(error);
		}
	}
	
	/*
	 * Methods to be called by the code inserted by TracerEnhancer.
	 */
	
	public static void enterMethod(Class klass, String method, Object object, Variable[] variables, Map<Integer, Variable[]> readAccessesByLine, Map<Integer, Variable[]> writeAccessesByLine) {
		Trace trace = current();
		if(trace != null) {
			Logger.debug("TRACER enterMethod klass: " + klass.getCanonicalName() + ", method: "+ method + ", object: " + (object != null ? "{}" : "null") + ", current: "+trace.current);
			MethodExecution mexec = new MethodExecution(trace.current, 0, klass, method, object, variables, readAccessesByLine, writeAccessesByLine);
			trace.appendExecution(mexec);
		}
	}
	
	public static void exitMethod() {
		Logger.debug("Tracer.exitMethod()");
		Trace trace = current();
		if(trace != null) {
			trace.current.end();
			trace.current = trace.current.parent;
		}
	}
	
	public static void enterTemplate(String template) {
		Logger.debug("TRACER calling enterTemplate '%s'", template);
		Trace trace = current();
		if(trace != null) {
			TemplateExecution texec = new TemplateExecution(trace.current, 0, template);
			trace.appendExecution(texec);
		}
	}
	
	public static void exitTemplate() {
		Logger.debug("TRACER calling exitTemplate");
		Trace trace = current();
		if(trace != null) {
			trace.current.end();
			trace.current = trace.current.parent;
		}
	}
	
	public static void startLine(int line) {
		Trace trace = current();
		if(trace != null) {
			LineExecution lineExecution = new LineExecution(trace.current, line);
			trace.appendExecution(lineExecution);
		}
	}
	
	public static void endLine(Variable[] readVars, Variable[] writtenVars) {
		Trace trace = current();
		if(trace != null) {
			if(trace.current instanceof LineExecution) {
				((LineExecution)trace.current).end(readVars, writtenVars);
				trace.current = trace.current.parent;
			}
		}
	}
	
	public static void endLine(Binding binding) {
		Trace trace = current();
		if(trace != null) {
			if(trace.current instanceof LineExecution) {
				((LineExecution)trace.current).end(binding);
				trace.current = trace.current.parent;
			}
		}
	}
}
