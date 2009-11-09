import org.junit._
import org.junit.Assert._
import play.test._
import play.db.jpa.QueryFunctions._
import models._

class JPQLTest extends UnitTest {
    @Test
    def findJavaModel() {
    	new Juser("bob@gmail.com", "secret", "Bob").save()
        val bob: Juser = find[Juser]("byEmail", "bob@gmail.com").first
        
        assertNotNull(bob)
        assertEquals("Bob", bob.fullname)
    }

    @Test
    def FindScalaModel() {
    	new User("harry@gmail.com", "secret", "Harry").save()
        val harry = findOneBy[User]("byEmail", "harry@gmail.com")
        
        assertNotNull(harry)
        assertEquals("Harry", harry.fullname)
    }

    @Test
    def deleteAndcount() {
    	deleteAll[User]
    	deleteAll[Juser]
    	new User("tom@gmail.com", "secret", "Tom").save()
    	new User("harry@gmail.com", "secret", "Harry").save()
    	new Juser("bob@gmail.com", "secret", "Bob").save()

    	assertEquals(2L, count[User])
    	assertEquals(1L, count[Juser])
    	assertEquals(1L, count[User]("byEmail", "tom@gmail.com"))
    }
    
    @Test
    def findAllUsers() {
    	deleteAll[User]
    	val tom: User = new User("tom@gmail.com", "secret", "Tom").save()
    	val harry: User = new User("harry@gmail.com", "secret", "Harry").save()
    	val users = findAll[User]

    	assertNotNull(users)
    	assertEquals(2, users.size())
    	assertTrue(users.contains(tom))
    	assertTrue(users.contains(harry))
    }
}
