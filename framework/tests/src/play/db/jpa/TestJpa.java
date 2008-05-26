package play.db.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import junit.framework.TestCase;
import play.Play;
import play.db.Db;

public class TestJpa extends TestCase{
    
    public void testJpa () {
        List<Class> classes = new ArrayList<Class>();
        classes.add(Dummy.class);
        Jpa.init(classes, new Properties());
        EntityManager em = Jpa.getEntityManager();
        em.getTransaction().begin();
        Dummy dum = new Dummy();
        dum.ppt="test";
        em.persist(dum);

        Query q = em.createQuery("select d from Dummy d");
        List res = q.getResultList();
        assertEquals(1, res.size());
        
        Dummy dum2= (Dummy) res.get(0);
        assertEquals(dum.id, dum2.id);
        assertEquals(dum.ppt, dum2.ppt);
        em.getTransaction().commit();
    }
    
    protected void setUp() throws Exception {
        Properties p = new Properties ();
        p.setProperty("db.driver", "org.hsqldb.jdbcDriver");
        p.setProperty("db.url", "jdbc:hsqldb:mem:aname");
        p.setProperty("db.user", "sa");
        p.setProperty("db.pass", "");
        Play.configuration=p;
        Db.init();
    }
}
