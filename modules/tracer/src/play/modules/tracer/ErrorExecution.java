package play.modules.tracer;

public class ErrorExecution extends Execution {
	public Throwable throwable;
	
	public ErrorExecution(Execution parent, int line, Throwable throwable) {
		super(parent, line);
		this.throwable = throwable;
	}

	public void assign(String variable, Object value, Class klass, int line,
			boolean isLocal) {
		// nothing
	}


	public void assignIfModified(String variable, Object value, Class klass,
			int line, boolean isLocal) {
		// nothing
	}

}
