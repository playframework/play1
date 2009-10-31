/**
 * 
 */
package play.modules.tracer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import play.modules.tracer.Execution;
import play.mvc.Http.Request;
import play.mvc.Router.Route;

public class Trace {
	public static class ActionContext {
		public String action;
		public String routeFile;
		public int routeLine;
		public SerializedObject request;
		
		public String toJSON() {
			StringBuffer sb = new StringBuffer("{\"action\":\"").append(action).append("\",");
			sb.append("\"routeFile\":\"").append(routeFile).append("\",");
			sb.append("\"routeLine\":").append(routeLine).append(",");
			sb.append("\"request\":").append(request.toJSON(true));
			sb.append("}");
			return sb.toString();
		}
	}

	public String id;
	public ActionContext actionContext;
	public List<Execution> executions = new ArrayList<Execution>();
	public Execution current;
	public long start = System.currentTimeMillis();
	public long realStart = 0L;
	public long end = 0L;
	
	public Trace(Route route) {
		id = UUID.randomUUID().toString();
		actionContext = new ActionContext();
		actionContext.action  = Request.current().action;
		actionContext.routeLine = route.routesFileLine;
		actionContext.routeFile = route.routesFile;
		actionContext.request = SerializedObject.serialize("request", Request.current(), Request.class, false);
	}
	
	public void end() {
		Execution e = current;
		while(e != null) {
			e.end();
			e = e.parent;
		}
		end = System.currentTimeMillis();
	}
	
	public void appendExecution(Execution e) {
		if(realStart == 0)
			realStart = System.currentTimeMillis();
		if(current == null) {
			current = e;
			executions.add(e);
		} else {
			if(current.appendExecution(e))
				current = e;
		}
	}
	
	public String toJSON() {
		StringBuffer sb = new StringBuffer("{");
		sb.append("\"id\":\"").append(id).append("\",");
		sb.append("\"start\":").append(start).append(",");
		sb.append("\"realStart\":").append(realStart).append(",");
		sb.append("\"end\":").append(end).append(',');
		sb.append("\"actionContext\":").append(actionContext.toJSON()).append(",");
		sb.append("\"executions\":[");
		if(executions.size() > 0) {
			sb.append(executions.get(0).toJSON());
			for(int i = 1; i < executions.size(); i++) {
				sb.append(",").append(executions.get(i).toJSON());
			}
		}
		sb.append("]");
		sb.append("}");
		return sb.toString();
	}
}