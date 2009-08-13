package functional;

import org.junit.*;

import play.test.*;

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