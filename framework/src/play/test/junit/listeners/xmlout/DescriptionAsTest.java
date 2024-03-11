package play.test.junit.listeners.xmlout;

import org.junit.runner.Description;

import junit.framework.Test;
import junit.framework.TestResult;

/**
 * Wraps {@link Description} into {@link Test} enough to fake {@link org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter}.
 */
public class DescriptionAsTest implements Test {
    private final Description description;

    public DescriptionAsTest(Description description) {
        this.description = description;
    }

    public int countTestCases() {
        return 1;
    }

    public void run(TestResult result) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@link org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter} determines the test name by reflection.
     */
    public String getName() {
        return description.getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DescriptionAsTest that = (DescriptionAsTest) o;

        return description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return description.hashCode();
    }
}
