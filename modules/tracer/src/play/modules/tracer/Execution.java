/**
 * 
 */
package play.modules.tracer;

import java.util.ArrayList;
import java.util.List;

public abstract class Execution {
	public long start = System.nanoTime();
	public long end = 0L;
	public int line;
	
	public Execution parent;
	public List<Execution> children = new ArrayList<Execution>();
	
	public Execution(Execution parent, int line) {
		this.parent = parent;
		this.line = line;
	}
	
	public long duration() {
		return end - start;
	}
	
	public boolean isFinished() {
		return end > 0;
	}
	
	public void end() {
		end = System.nanoTime();
	}
	
	protected String partialToJSON() {
		return null;
	}
	
	public String toJSON() {
		StringBuffer sb = new StringBuffer("{");
		sb.append("\"start\":").append(start).append(",");
		sb.append("\"end\":").append(end).append(",");
		sb.append("\"line\":").append(line).append(",");
		sb.append("\"type\": \"").append(this.getClass().getSimpleName()).append("\",");
		String partial = partialToJSON();
		if(partial != null && partial.length() > 0) {
			sb.append(partial).append(",");
		}
		sb.append("\"children\": [");
		if(children.size() > 0) {
			sb.append(children.get(0).toJSON());
			for(int i = 1; i < children.size(); i++) {
				sb.append(",").append(children.get(i).toJSON());
			}
		}
		sb.append("]");
		sb.append("}");
		return sb.toString();
	}
	
	public boolean appendExecution(Execution e) {
		return children.add(e);
	}
}