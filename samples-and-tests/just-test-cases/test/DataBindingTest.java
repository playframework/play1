import java.util.HashMap;
import java.util.Map;

import models.Child;
import models.Parent;
import models.Person;

import org.junit.Test;

import com.google.gson.Gson;

import play.data.binding.NoBinding;
import play.libs.WS;
import play.mvc.Http;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.Fixtures;
import play.test.FunctionalTest;
import play.test.UnitTest;

public class DataBindingTest extends FunctionalTest {

    @Test
    public void testThatBindingWithQueryStringAndBodyWorks() {
        Http.Response response = POST("/DataBinding/myInputStream?productCode=XXX", "text/plain", "A_body");

        assertIsOk(response);
        assertContentEquals("XXX - A_body", response);
    }


    @Test
    public void testBindingList() {
        Http.Response response = POST("/DataBinding/myList?items[0].id=23&items[10].id=1&items[1].id=12&items[2].id=43&items[6].id=35&items[8].id=32", "text/plain", "A_body");

        assertIsOk(response);
        assertContentEquals("MyBook[23],MyBook[12],MyBook[43],null,null,null,MyBook[35],null,MyBook[32],null,MyBook[1]", response);
    }
    
    @Test
    public void testEditAnEntity() {
	Map<String, String> params = new HashMap<String, String>();
	params.put("entity.date", "2013-10-03 11:33:05:125 AM");
	params.put("entity.yop", "yop");
        Response response = POST(Router.reverse("DataBinding.editAnEntity").url, params);
        assertIsOk(response);
        assertContentMatch("2013-10-03 11:33:05:125 AM", response);
        assertContentMatch("--yop--", response);
    }

    @Test
    public void testDispatchAnEntity() {
	Map<String, String> params = new HashMap<String, String>();
	params.put("entity.date", "2013-10-03 11:33:05:125 AM");
	params.put("entity.yop", "yop");
        Response response = POST(Router.reverse("DataBinding.dispatchAnEntity").url, params);
        assertStatus(302, response);
    }
    
    @Test
    public void testEdit() {
	Map<String, String> params = new HashMap<String, String>();
	//params.put("child.father.code", mother.code);

        Response response = POST(Router.reverse("DataBinding.editAnEntity").url, params);
	
    }
    
    /** 
     * This test will check that entity Key are required in order ot mBind JPA  entities
     * 
     */
    @Test
    public void testJPABinding() {
	Fixtures.idCache.clear();
	Fixtures.deleteAllModels();
	Fixtures.loadModels("pc.yml");
	
	Parent father = Parent.find("byName", "parent").first();
	Parent mother = Parent.find("byName", "mother").first();
	Person tutor = Person.find("byUserName", "tutor").first();
	Person newTutor = Person.find("byUserName", "new_tutor").first();
	Child child = Child.find("byName", "child_2").first();
	
	assertNotNull(father);
	assertNotNull(mother);
	assertNotNull(child);
	assertNotNull(tutor);
	
	assertEquals(father, child.father);
	assertEquals(mother, child.mother);
	assertEquals(tutor, child.tutor);
	
	
	Map<String, String> params = new HashMap<String, String>();
	params.put("child.id", child.id.toString());
	params.put("child.father.code", mother.code);
	params.put("child.mother.code", father.code);
	params.put("child.tutor.id", newTutor.getId().toString());
        Response response = POST(Router.reverse("DataBinding.saveChild").url, params);
        assertContentEquals("{\"name\":\"child_2\", \"father\":\"mother\", \"mother\":\"parent\", \"tutor\":\"new_tutor\"}", response);
        
        params.clear();
	params.put("child.id", child.id.toString());
	params.put("child.father", mother.code);
	params.put("child.mother", father.code);
	params.put("child.tutor", newTutor.getId().toString());
        response = POST(Router.reverse("DataBinding.saveChild").url, params);
        assertContentEquals("{\"name\":\"child_2\", \"father\":\"null\", \"mother\":\"null\", \"tutor\":\"null\"}", response);
    }
    
    /** 
     * This test will check that the no binding work will not work with fixtures
     * 
     */
    @Test
    public void testNoBindingAndFixtures() {
	Fixtures.idCache.clear();
	Fixtures.deleteAllModels();
	Fixtures.loadModels("pc.yml");
	
	Child child = Child.find("byName", "child_2").first();
	assertNotNull(child.father);
	assertNotNull(child.mother);
	assertNotNull(child.tutor);
	
	// Should be null as test has the annotation @NoBinding("Fixtures")
	assertNotEquals("testFixtureNoBinding", child.test);
	assertNull(child.test);
    }
	
    
    /** 
     * This test will check that NoBinding work on entities
     * 
     */
    @Test
    public void testSaveEntityWithNoBindingAnnotation() {
	Fixtures.idCache.clear();
	Fixtures.deleteAllModels();
	Fixtures.loadModels("pc.yml");
	
	Parent father = Parent.find("byName", "parent").first();
	Parent mother = Parent.find("byName", "mother").first();
	Person tutor = Person.find("byUserName", "tutor").first();
	Person newTutor = Person.find("byUserName", "new_tutor").first();
	Child child = Child.find("byName", "child_2").first();
	
	assertNotNull(father);
	assertNotNull(mother);
	assertNotNull(child);
	assertNotNull(tutor);
	
	assertEquals(father, child.father);
	assertEquals(mother, child.mother);
	assertEquals(tutor, child.tutor);
	
	
	Map<String, String> params = new HashMap<String, String>();
	params.put("child.id", child.id.toString());
	params.put("child.father.code", mother.code);
	params.put("child.mother.code", father.code);	
	params.put("child.tutor.id", newTutor.getId().toString());
        Response response = POST(Router.reverse("DataBinding.saveChild").url, params);
    	
        assertContentEquals("{\"name\":\"child_2\", \"father\":\"mother\", \"mother\":\"parent\", \"tutor\":\"new_tutor\"}", response);
        
        response = POST(Router.reverse("DataBinding.saveChildAsSecure").url, params);
        assertContentEquals("{\"name\":\"child_2\", \"father\":\"parent\", \"mother\":\"mother\", \"tutor\":\"tutor\"}", response);
    }
}

