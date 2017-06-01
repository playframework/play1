import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import play.jobs.Every;
import play.jobs.Job;
import play.test.UnitTest;

/**
 * Unit tests for the {@link Job} class.
 */
public class JobTest extends UnitTest {

    /**
     * A Job class that is annotated with {@link Every} to run every second.
     */
    public static class TestJob extends Job {
        private final AtomicLong totalRuns = new AtomicLong(0);
        private volatile boolean throwException = false;

        @Override
        public void doJob() throws Exception {
            totalRuns.getAndIncrement();

            // To avoid logging an error every second, we only throw an
            // exception when the relevant test case is running.
            if (throwException) {
                throwException = false;
                throw new Exception("Throwing an exception");
            }
        }
    }

    /**
     * A Job class that is annotated with {@link Every} to run every second.
     */
    @Every("1s")
    public static class RunEverySecond extends Job {
        private static final AtomicLong totalRuns = new AtomicLong(0);
        private static volatile boolean throwException = false;

        @Override
        public void doJob() throws Exception {
            totalRuns.getAndIncrement();

            // To avoid logging an error every second, we only throw an
            // exception when the relevant test case is running.
            if (throwException) {
                throwException = false;
                throw new Exception("Throwing an exception");
            }
        }
    }

    /**
     * A Job class that is annotated with {@link Every} to never run.
     */
    @Every("never")
    public static class RunNever extends Job {
        private static final AtomicLong totalRuns = new AtomicLong(0);

        @Override
        public void doJob() throws Exception {
            totalRuns.getAndIncrement();
        }
    }

    /**
     * A Job class that is annotated with {@link Every} to never run (using mixed case)
     */
    @Every("NeveR")
    public static class RunNeverMixedCase extends Job {
        private static final AtomicLong totalRuns = new AtomicLong(0);

        @Override
        public void doJob() throws Exception {
            totalRuns.getAndIncrement();
        }
    }

    /**
     * Tests that a job which is annotated to run every second does, indeed, run every second.
     */
    @Test
    public void testEverySecond() {
        long beforeRuns = RunEverySecond.totalRuns.get();
        pause(1500); // wait long enough for the job to have run at least once
        long afterRuns = RunEverySecond.totalRuns.get();
        assertTrue("RunEverySecond job was never run", beforeRuns < afterRuns);
        assertTrue("RunEverySecond job was run too many times", afterRuns - beforeRuns <= 2);
    }

    /**
     * Tests that jobs which are annotated to never run have never been run.
     */
    @Test
    public void testEveryNever() {
        assertEquals(0, RunNever.totalRuns.get());
        assertEquals(0, RunNeverMixedCase.totalRuns.get());
    }

    /**
     * Tests that throwing an exception does not halt the periodic scheduling of an {@code @Every} annotation.
     * This is a regression test for Lighthouse [#2060]
     */
    @Test
    public void testExceptionDoesNotHaltReschedulingWithEveryAnnotation() {

        // Configure RunEverySecond to throw an exception.
        RunEverySecond.throwException = true;
        long beforeRuns = RunEverySecond.totalRuns.get();

        pause(2500); // wait long enough for the job to have run at least twice

        // Make sure it threw an exception and ran multiple times.
        long afterRuns = RunEverySecond.totalRuns.get();
        assertFalse("RunEverySecond job never ran", RunEverySecond.throwException);
        assertTrue("RunEverySecond job was not run after throwing an exception", 2 <= afterRuns - beforeRuns);
    }

    /**
     * Tests that throwing an exception does not halt the periodic scheduling of the {@link Job#every(int)} method.
     * This is a regression test for Lighthouse [#2060]
     */
    @Test
    public void testExceptionDoesNotHaltReschedulingEveryMethod() {

        // Schedule a job to run every second.
        TestJob job = new TestJob();
        job.throwException = true;
        job.every("1s");

        pause(2500); // wait long enough for the job to have run at least twice

        // Make sure it threw an exception and ran multiple times.
        assertFalse("TestJob job never ran", job.throwException);
        assertTrue("TestJob job was not run after throwing an exception", 2 <= job.totalRuns.get());
    }
}
