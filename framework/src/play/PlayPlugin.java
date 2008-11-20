package play;

import play.mvc.results.Result;

public abstract class PlayPlugin implements Comparable<PlayPlugin> {

    public int index;

    public void onLoad() {
    }

    public void detectChange() {
    }

    public void onApplicationStart() {
    }

    public void afterApplicationStart() {
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
    
    public void afterActionInvocation() {
    }

    public void onConfigurationRead() {
    }

    public int compareTo(PlayPlugin o) {
        return (index < o.index ? -1 : (index == o.index ? 0 : 1));
    }
}
