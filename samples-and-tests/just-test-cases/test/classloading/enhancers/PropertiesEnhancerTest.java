package classloading.enhancers;

import static org.junit.Assert.*;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import models.Project;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import play.test.FunctionalTest;
import play.test.UnitTest;


public class PropertiesEnhancerTest extends FunctionalTest {

    @Test
    public void testEnhancerMethods() throws Exception {
	Project obj = new Project();
	// get all of the properties for a POJO
	PropertyDescriptor[] descriptors = PropertyUtils
		.getPropertyDescriptors(obj);
      
	assertEquals(10, descriptors.length);

	assertTrue(validAccessorFor(obj, descriptors, "class", true, false));
	assertTrue(validAccessorFor(obj, descriptors, "companies", true, true));
	assertTrue(validAccessorFor(obj, descriptors, "company", true, true));
	assertTrue(validAccessorFor(obj, descriptors, "endDate", true, true));
	assertTrue(validAccessorFor(obj, descriptors, "entityId", true, false));
	assertTrue(validAccessorFor(obj, descriptors, "id", true, false));
	assertTrue(validAccessorFor(obj, descriptors, "name", true, true));
	assertTrue(validAccessorFor(obj, descriptors, "observation", true, true));
	assertTrue(validAccessorFor(obj, descriptors, "startDate", true, true));
    }

    private boolean validAccessorFor(Object obj,
	    PropertyDescriptor[] descriptors, String name, boolean readMethod,
	    boolean writeMethod) {
	// go through all values
	for (int i = 0; i < descriptors.length; i++) {
	    PropertyDescriptor descriptor = descriptors[i];
	    if (descriptor.getName().equals(name)) {
		if (readMethod) {
		    Method method = descriptor.getReadMethod();
		    if (method == null) {
			return false;
		    }
		}

		if (writeMethod) {
		    Method method = descriptor.getWriteMethod();
		    if (method == null) {
			return false;
		    }
		}
		return true;
	    }
	}
	return false;
    }

    @Test
    public void testEnhancerProperty() throws Exception {
	Project obj = new Project();
	obj.name = "toto";
	Object value = PropertyUtils.getProperty(obj, "name");

	assertNotNull(value);
	assertEquals("toto", value);
    }
}
