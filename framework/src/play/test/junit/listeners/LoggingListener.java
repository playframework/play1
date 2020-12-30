/*******************************************************************************
 * Copyright (c) 2018 Nosto Solutions Ltd All Rights Reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Nosto Solutions Ltd ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the agreement you entered into with
 * Nosto Solutions Ltd.
 ******************************************************************************/
package play.test.junit.listeners;

import java.text.NumberFormat;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import play.Logger;

public class LoggingListener extends RunListener {

    public static final LoggingListener INSTANCE = new LoggingListener();

    private LoggingListener() {
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
        Logger.info("Time: %d" + elapsedTimeAsString(runTime));
    }

    protected void printFailures(Result result) {
        List<Failure> failures = result.getFailures();
        if (failures.size() == 0) {
            return;
        }
        if (failures.size() == 1) {
            Logger.info("There was " + failures.size() + " failure:");
        } else {
            Logger.info("There were " + failures.size() + " failures:");
        }
    }

    protected void printFooter(Result result) {
        if (result.wasSuccessful()) {
            Logger.info("");
            Logger.info("OK");
            Logger.info(" (" + result.getRunCount() + " test" + (result.getRunCount() == 1 ? "" : "s") + ")");

        } else {
            Logger.error("");
            Logger.error("FAILURES!!!");
            Logger.error("Tests run: " + result.getRunCount() + ",  Failures: " + result.getFailureCount());
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
