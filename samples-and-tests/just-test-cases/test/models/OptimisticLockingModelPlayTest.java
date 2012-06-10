/**
 * 
 */
package models;

import java.util.List;
import java.util.Map;

import javax.persistence.Entity;

import models.OptimisticLockingModel.OptimisticLockingCheck;

import org.junit.Test;

import play.data.validation.Error;
import play.data.validation.Validation;
import play.data.validation.Validation.ValidationResult;
import play.test.UnitTest;

/**
 * @author niels
 *
 */
public class OptimisticLockingModelPlayTest extends UnitTest {

    @Test    
    public void testOptimisticLockingCheck() {
        final TestModel testModel = new TestModel();
        
        Validation.clear();
        
        ValidationResult result = Validation.current().valid(testModel);
        assertTrue(result.ok);
        
        //You must disable setMessage in the check for this test:-/
        testModel.setVersion(Long.valueOf(2));        
        result = Validation.current().valid(testModel);
        assertTrue(result.ok);
        testModel.setVersion(Long.valueOf(2));
        result = Validation.current().valid(testModel);
        assertTrue(result.ok);
        testModel.setVersion(Long.valueOf(3));
        result = Validation.current().valid(testModel);
        assertTrue(result.ok);
        testModel.setVersion(Long.valueOf(1));
        Validation.clear();
        result = Validation.current().valid(testModel);
        assertFalse(result.ok);
        assertNotNull(Validation.errors(".version"));
        Error error = Validation.errors(".version").get(0);
        System.out.println(error.getKey());
        assertEquals("The object was changed. Your version is 1 the database version is 2. " +
                "Reload and do your changes again.", error.message());       
    }
    
    
    @Entity
    public static class TestModel extends OptimisticLockingModel {
        public String text;
    }

}
