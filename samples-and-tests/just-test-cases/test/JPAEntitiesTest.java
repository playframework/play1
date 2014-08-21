import org.junit.*;
import org.playframework.models.Cat;

import play.db.jpa.JPA;
import play.db.jpa.GenericModel.JPAQuery;
import play.test.*;

import java.util.*;

import javax.persistence.Query;

import models.*;

public class JPAEntitiesTest extends UnitTest {

    @Test
    public void testJpaEntities() {
	Cat cat = new Cat();
	cat.name = "test";
	JPA.em().persist(cat);

	String jpql = "Select e From Cat e";
	Query query = JPA.em().createQuery(jpql);
	JPAQuery q = new JPAQuery(jpql, query);
	Cat cat2 = q.first();
	assertEquals(cat.name, cat2.name);
    }
    
}

