import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;
import models.otherdb.*;
import play.test.Fixtures;

public class TransactionalJPATest extends FunctionalTest {

    @Test
    public void testImport() {
        Response response = GET("/Transactional/readOnlyTest");
        assertIsOk(response);
        response = GET("/Transactional/echoHowManyPosts");
        assertIsOk(response);
        assertEquals("There are 0 posts", getContent(response));
    }

    @Test
    public void testDisableTransaction() {

        //make sure isInsideTransaction returns true when actually inside a transaction
        Response response = GET("/Transactional/verifyIsInsideTransaction");
        assertIsOk(response);
        assertEquals("isInsideTransaction: true", getContent(response));

        //verify that we can make the controller run without any transaction at all
        response = GET("/Transactional/disabledTransactionTest");
        assertIsOk(response);
        assertEquals("isInsideTransaction: false", getContent(response));

        //verify that a method cannot use a tranaction if the controller-class
        //is annotated with @NoTransaction
        response = GET("/Transactional2/disabledTransactionTest");
        assertIsOk(response);
        assertEquals("isInsideTransaction: false", getContent(response));

    }

    @Test
    public void testMultipleJPASupport() {
        Fixtures.delete(EntityInOtherDb.class);
        Response response = GET("/Transactional/useMultipleJPAConfigs");
        assertIsOk(response);
        assertEquals("ok 1", getContent(response));

        // check it again to make sure we don't hang the other db lock
        response = GET("/Transactional/useMultipleJPAConfigs");
        assertIsOk(response);
        assertEquals("ok 2", getContent(response));
    }

}

