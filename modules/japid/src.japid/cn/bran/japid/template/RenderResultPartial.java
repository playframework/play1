package cn.bran.japid.template;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;


/**
 * Represented a partially rendered result at the top level. Pre-rendered
 * content is in the content in the super class, which is to be interpolated
 * with the result from each of the ActionRunners that represent dynamic content
 * that lazily generated at the moment of getContent().
 * 
 * Objects of this class can be cached and reevalued.
 * 
 * TODO: make partial render result take arguments to fine control action runners's
 * behavior, such as use different argument to the nested action.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class RenderResultPartial extends RenderResult {
	Map<Integer, ActionRunner> actionRunners;
	private String viewName;

	public Map<Integer, ActionRunner> getActionRunners() {
		return actionRunners;
	}

	public RenderResultPartial(Map<String, String> headers, StringBuilder content, long renderTime, Map<Integer, ActionRunner> actions) {
		super(headers, content, renderTime);
		this.actionRunners = actions;
	}
	
	public RenderResultPartial(Map<String, String> headers, StringBuilder content, long renderTime, Map<Integer, ActionRunner> actions, String viewName) {
		super(headers, content, renderTime);
		this.actionRunners = actions;
		this.viewName = viewName;
	}
	
	public RenderResultPartial() {
	}

	@Override
	public StringBuilder getContent() {
		StringBuilder superContent = super.getContent();
			
		StringBuilder sb = new StringBuilder();
		if (actionRunners != null && actionRunners.size() > 0) {
			int segStart = 0;
			for (Map.Entry<Integer, ActionRunner> arEntry : actionRunners.entrySet()) {
				int pos = arEntry.getKey();
				sb.append(superContent.substring(segStart, pos));
				segStart = pos;
				ActionRunner a = arEntry.getValue();
				sb.append(a.run().getContent().toString());
			}
			sb.append(superContent.substring(segStart));
			return sb;
		} else {
			sb.append(superContent.toString());
			return sb;
		}
	}
	

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(actionRunners);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		actionRunners = (Map<Integer, ActionRunner>) in.readObject();
	}

	@Override
	public String toString() {
		return getContent().toString();
	}
	
}
