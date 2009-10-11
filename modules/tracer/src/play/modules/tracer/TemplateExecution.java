package play.modules.tracer;


public class TemplateExecution extends BodyExecution {
	public String name;
	
	public TemplateExecution(Execution parent, int line, String name) {
		super(parent, line);
		this.name = name;
	}

	protected String partialToJSON() {
		StringBuffer sb = new StringBuffer("\"name\":\"").append(name).append("\"");
		String superPartialToJSON = super.partialToJSON();
		if(superPartialToJSON != null)
			sb.append(",").append(superPartialToJSON);
		return sb.toString();
	}

}
