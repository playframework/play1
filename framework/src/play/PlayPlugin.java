package play;

import play.mvc.results.Result;

public abstract class PlayPlugin {
    
    public void onLoad() {
    }
    
    public void detectChange() {
    }

    public void onApplicationStart() {
    }

    public void onApplicationStop() {
    }

    public void beforeInvocation() {
    }

    public void afterInvocation() {
    }

    public void onInvocationException(Throwable e) {
    }

    public void invocationFinally() {
    } 
    
    public void beforeActionInvocation() {        
    }
    
    public void onActionInvocationResult(Result result) {
    }
    
}
