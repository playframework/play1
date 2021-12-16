package play.test.junit.listeners;

import java.text.NumberFormat;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import play.Logger;

public class LoggingListener extends RunListener {

    public LoggingListener() {
        //
    }

    @Override
    public void testRunFinished(Result result) {
        printHeader(result.getRunTime());
        printFailures(result);
        printFooter(result);
    }

    @Override
    public void testStarted(Description description) {
        Logger.info("🚀 Test started %s", description.getDisplayName());
    }

    @Override
    public void testFailure(Failure failure) {
        Logger.error(failure.getException(),"💥 Test started %s", failure.getDescription());
    }

    @Override
    public void testIgnored(Description description) {
        Logger.info("Test %s was ignored", description.getDisplayName());
    }

    protected void printHeader(long runTime) {
        Logger.info("");
        Logger.debug("Time: %d" + elapsedTimeAsString(runTime));
    }

    protected void printFailures(Result result) {
        List<Failure> failures = result.getFailures();
        if (failures.size() == 0) {
            return;
        }
        if (failures.size() == 1) {
            Logger.info("There was %d failure:", failures.size());
        } else {
            Logger.info("There were %d failures:", failures.size());
        }
    }

    protected void printFooter(Result result) {
        if (result.wasSuccessful()) {
            Logger.info("All tests passed");
            Logger.info(" (%d test%s)", result.getRunCount(), result.getRunCount() == 1 ? "" : "s");

        } else {
            Logger.error("One or more tests failed");
            Logger.error("Tests run: %d, Failures: %d", result.getRunCount(), result.getFailureCount());
        }
    }

    /**
     * Returns the formatted string of the elapsed time. Duplicated from
     * BaseTestRunner. Fix it.
     */
    protected String elapsedTimeAsString(long runTime) {
        return NumberFormat.getInstance().format((double) runTime / 1000);
    }
}
