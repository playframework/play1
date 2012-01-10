package play.db.jpa;


import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.util.Set;

import static play.db.jpa.JPQL.extractProp;

import static org.junit.Assert.*;

public class JPQLTest {

	static JPQL jpql;

	@BeforeClass
	public static void setup(){
		jpql = new JPQL(null);
	}

    @Test
    public void testOrder() {

        String query = "ByNameOrderByName";
        String result = jpql.findByToJPQL(query);
        assertTrue(result.endsWith(" ORDER BY name"));

        query = "ByNameOrderByNameAndAge";
        result = jpql.findByToJPQL(query);
        assertTrue(result.endsWith(" ORDER BY name, age"));

        query = "ByNameOrderByNameDesc";
        result = jpql.findByToJPQL(query);
        assertTrue(result.endsWith(" ORDER BY name DESC"));

        query = "ByNameOrderByNameDescAndAge";
        result = jpql.findByToJPQL(query);
        assertTrue(result.endsWith(" ORDER BY name DESC, age"));

        query = "ByNameOrderByNameAndAgeDesc";
        result = jpql.findByToJPQL(query);
        assertTrue(result.endsWith(" ORDER BY name, age DESC"));

        query = "ByNameOrderByNameDescAndAgeDesc";
        result = jpql.findByToJPQL(query);
        assertTrue(result.endsWith(" ORDER BY name DESC, age DESC"));

    }
	
}