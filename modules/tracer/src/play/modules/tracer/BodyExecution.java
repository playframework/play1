/**
 * 
 */
package play.modules.tracer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import play.Logger;

public abstract class BodyExecution extends Execution {
	public List<Variable> variables = new ArrayList<Variable>();
	
	public boolean loopDetected = false;
	
	@Override
	public boolean appendExecution(Execution e) {
		if(e instanceof LineExecution) {
			LineExecution lexec = (LineExecution) e;
			LineExecution last = null;
			if(children.size() > 0) {
				last = (LineExecution) children.get(children.size() - 1);
				Logger.debug("accepted start line %s in %s", lexec.line, this);
			}
			if(last == null || last.line < lexec.line) {
				lexec.afterLoop = loopDetected;
				super.appendExecution(e);
				loopDetected = false;
				Logger.debug("accepted start line %s in %s", lexec.line, this);
				return true;
			} else {
				loopDetected = true;
			}
		}
		return false;
	}

	protected Variable findVariable(String name, boolean isLocal, int line) {
		Variable result = null;
		int distance = -1;
		for(Variable lv : variables) {
			if(lv.name.equals(name) && lv.isLocal == isLocal && lv.startLine <= line && lv.endLine >= line) {
				int d = line - lv.startLine;
				if(d > distance) {
					result = lv;
					distance = d;
				}
			}
		}
		return result;
	}
	
	protected List<Variable> getActiveVariable(int line) {
		List<Variable> result = new ArrayList<Variable>();
		for(Variable variable : variables) {
			if(variable.startLine <= line && variable.endLine >= line)
				result.add(variable);
		}
		return result;
	}
	
	public BodyExecution(Execution parent, int line) {
		super(parent, line);
	}
	
	@Override
	protected String partialToJSON() {
		StringBuffer sb = new StringBuffer("\"variables\":[");
		if(variables.size() > 0) {
			Iterator<Variable> iterator = variables.iterator();
			sb.append(iterator.next().toJSON());
			while(iterator.hasNext()) {
				sb.append(",").append(iterator.next().toJSON());
			}
		}
		return sb.append("]").toString();
	}
	
	@Override
	public void end() {
		for(Execution e : children)
			e.end();
		super.end();
	}
}