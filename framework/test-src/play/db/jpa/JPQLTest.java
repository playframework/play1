package play.db.jpa;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JPQLTest {

    static JPQL jpql;

    @BeforeAll
    public static void setup(){
        jpql = new JPQL();
    }

    @Test
    public void testFindBy() {
        String query = "ByName";
        String result = jpql.findByToJPQL(query);
        assertTrue(result.equals("name = ?1"));
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
