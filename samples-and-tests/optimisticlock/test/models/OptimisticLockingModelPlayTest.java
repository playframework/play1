/**
 * 
 */
package models;

import models.OptimisticLockingModel.OptimisticLockingCheck;

import org.junit.Test;

import play.data.validation.Validation;
import play.data.validation.Validation.ValidationResult;
import play.test.UnitTest;

/**
 * @author niels
 *
 */
public class OptimisticLockingModelPlayTest extends UnitTest {

    /**
     * Test method for {@link models.optimisticlock.VersionedModel#setVersion(java.lang.Long)}.
     */
    @Test
    public void testSetVersion() {
        final TestModel testModel = new TestModel();
        final OptimisticLockingCheck check = new OptimisticLockingCheck();
        //TODO niels Das funktiniert nicht. Es gibt eine Exception.
        //ValidationResult result = Validation.current().valid(testModel);
        //assertTrue(result.ok);
        
        //You must disable setMessage in the check for this test:-/
        testModel.setVersion(Long.valueOf(2));        
        assertTrue(check.isSatisfied(testModel, ""));
        testModel.setVersion(Long.valueOf(2));
        assertTrue(check.isSatisfied(testModel, ""));
        testModel.setVersion(Long.valueOf(3));
        assertTrue(check.isSatisfied(testModel, ""));
        testModel.setVersion(Long.valueOf(1));
        assertFalse(check.isSatisfied(testModel, ""));
    }
    
    public static class TestModel extends OptimisticLockingModel {
        public String text;
    }

}
