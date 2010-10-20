package play.test;

import java.io.*;
import org.apache.commons.lang.exception.*;
import org.junit.rules.*;
import org.junit.runners.model.*;
import play.*;
import play.Invoker.DirectInvocation;

public enum StartPlay implements MethodRule {
	INVOKE_THE_TEST_IN_PLAY_CONTEXT {
		public Statement apply(final Statement base, FrameworkMethod method, Object target) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					if (!Play.started) {
						Play.forceProd = true;
						Play.init(new File("."), "test");
					}

					try {
						Invoker.invokeInThread(new DirectInvocation() {
							@Override
							public void execute() throws Exception {
								try {
									base.evaluate();
								} catch (Throwable e) {
									throw new RuntimeException(e);
								}
							}
						});
					} catch (Throwable e) {
						throw ExceptionUtils.getRootCause(e);
					}
				}
			};
		}
	},
	JUST_RUN_THE_TEST {
		public Statement apply(final Statement base, FrameworkMethod method, Object target) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					base.evaluate();
				}
			};
		}
	};

	private static boolean runningInsidePlay = false;

	public static StartPlay rule() {
		if (!Play.started) {
			runningInsidePlay = true;
		}
		return runningInsidePlay ? INVOKE_THE_TEST_IN_PLAY_CONTEXT : JUST_RUN_THE_TEST;
	}
}