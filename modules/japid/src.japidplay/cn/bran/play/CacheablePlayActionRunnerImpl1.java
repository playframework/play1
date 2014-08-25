package cn.bran.play;

public class CacheablePlayActionRunnerImpl1 extends CacheablePlayActionRunner{
	RunAction runAction;
	
	public RunAction getRunAction() {
		return runAction;
	}

	public void setRunAction(RunAction runAction) {
		this.runAction = runAction;
	}

	public CacheablePlayActionRunnerImpl1(String ttl) {
		super(ttl);
	}
	
//	public CacheablePlayActionRunnerImpl1(String ttl, Object... args) {
//		super(ttl, args);
//	}

	@Override
	protected void runPlayAction() throws JapidResult {
		runAction.runPlayAction();
	}

}
