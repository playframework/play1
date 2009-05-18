package functional;

import org.junit.*;
import play.test.*;
import play.libs.*;
import play.mvc.*;

import models.*;

public class SecurityTest extends FunctionalTest {
	
	@Test
	public void indexIsFree() {
		assertStatus(200, GET("/"));
	}
	
	@Test
	public void forumCreationIsRestricted() {
		assertStatus(403, POST("/forums"));
	}
	
	@Test
	public void forumDeletionIsRestricted() {
		assertStatus(403, POST("/forums/1/delete"));
	}
	
}