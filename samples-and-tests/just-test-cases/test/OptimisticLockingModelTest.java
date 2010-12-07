import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import models.OptimisticLockingModel.OptimisticLockingCheck;

import org.junit.Test;

import play.data.validation.CheckWithCheck;

/**
 * @author niels
 *
 */
public class OptimisticLockingModelTest {

    /**
     * Test method for {@link models.OptimisticLockingModel.VersionedModel#setVersion(java.lang.Long)}.
     */
    @Test
    public void testSetVersion() {
        final TestModel testModel = new TestModel();
        final OptimisticLockingCheck check = new OptimisticLockingCheckWithoutMessage();
        
        testModel.setVersion(Long.valueOf(2));        
        assertTrue(check.isSatisfied(testModel, ""));
        testModel.setVersion(Long.valueOf(2));
        assertTrue(check.isSatisfied(testModel, ""));
        testModel.setVersion(Long.valueOf(3));
        assertTrue(check.isSatisfied(testModel, ""));
        testModel.setVersion(Long.valueOf(1));
        assertFalse(check.isSatisfied(testModel, ""));
    }
    
    public static class TestModel extends models.OptimisticLockingModel {
        public String text;
    }

    private static final class OptimisticLockingCheckWithoutMessage extends models.OptimisticLockingModel.OptimisticLockingCheck {
        
        {
            checkWithCheck= new CheckWithCheck();
            checkWithCheck.setMessage("optimisticLocking.modelHasChanged");
        }

        @Override
        public void setMessage(String message, String... vars) {
        }
        
        
    }
}
