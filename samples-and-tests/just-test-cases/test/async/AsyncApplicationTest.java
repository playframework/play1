package async;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.*;
import org.junit.Before;

import controllers.async.AsyncApplication;

import play.Logger;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;
import models.*;
import models.async.AsyncTrace;

public class AsyncApplicationTest extends FunctionalTest {
    public static Map<Long, String> goodTraceOrders = new HashMap<Long, String>();

    @Before
    public void intit() {
        AsyncTrace.deleteAll();
        goodTraceOrders.put(0L, "~~~~ Beginning functional test execution.");
        goodTraceOrders.put(1L, "~~~~ Begining controller execution.");
        goodTraceOrders.put(2L, "~~~~ Begining job execution.");
        goodTraceOrders.put(3L, "~~~~ In job, task 1...");
        goodTraceOrders.put(4L, "~~~~ In job, task 2...");
        goodTraceOrders.put(5L, "~~~~ In job, task 3...");
        goodTraceOrders.put(6L, "~~~~ In job, task 4...");
        goodTraceOrders.put(7L, "~~~~ In job, task 5...");
        goodTraceOrders.put(8L, "~~~~ Ending job execution.");
        goodTraceOrders.put(9L,
                "~~~~ Ending controller execution => Redirecting.");
        goodTraceOrders.put(10L, "~~~~ Ending functional test execution.");
    }

    @Test
    public void testThatIndexPageWorks() {

        AsyncApplication
                .createTrace("~~~~ Beginning functional test execution.");
        Logger.debug("~~~~ Beginning functional test execution.");

        Response response = GET("/async/AsyncApplication/index");
        assertStatus(302, response);
        assertTrue(response.getHeader("Location").contains("/redirection"));

        AsyncApplication.createTrace("~~~~ Ending functional test execution.");
        Logger.debug("~~~~ Ending functional test execution.");

        assertEquals(goodTraceOrders.size(), AsyncTrace.count());

        Iterator it = goodTraceOrders.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Long position = (Long) pairs.getKey();
            AsyncTrace trace = AsyncTrace.find("byPosition", position).first();
            assertNotNull(trace);
            assertEquals(pairs.getValue(), trace.content);
        }
    }
  
  
}