package controllers;

import org.junit.Assert;
import play.Logger;
import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 9/6/11
 * Time: 10:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class SlowResponseTestController extends Controller {


    public static void slowResponse() throws Exception {
        int seconds = 2;
        Logger.info("Sleeping " + seconds + " before sending response");
        Thread.sleep(seconds*1000);
        renderText("Response after sleep");
    }

    public static void testWSAsyncWithException() {
        String url = "http://localhost:9003/SlowResponseTestController/slowResponse";
        // first make sure SlowResponse works
        String res = await(WS.url(url).getAsync()).getString();
        Assert.assertEquals("Response after sleep", res);

        // assert that we get exception if we have too short timeout
        boolean gotException = false;
        try {
            await(WS.url(url).timeout("1s").getAsync());
        } catch (Exception e) {
            gotException = true;
        }
        Assert.assertTrue("Did not get exception!", gotException);

        // assert that we get exception if we connect to invalid port
        gotException = false;
        try {
            await(WS.url("http://localhost:55651").getAsync());
        } catch (Exception e) {
            gotException = true;
        }
        Assert.assertTrue("Did not get exception!", gotException);

        renderText("ok");
    }

    // check fix for http://play.lighthouseapp.com/projects/57987/tickets/1656-awaitPromise-still-suspends-indefinitely-while-timeout-occurs-if-waiting-for-multiple-promises-with-a-promise-returned-by-waitAll
    public static void testWSAsyncAwaitAllWithException() {
        String url = "http://localhost:9003/SlowResponseTestController/slowResponse";

        // first will timeout...
        F.Promise remoteCall1 = WS.url(url).timeout("1s").getAsync();
        F.Promise remoteCall2 = WS.url(url).timeout("4s").getAsync();

        F.Promise promises = F.Promise.waitAll(remoteCall1, remoteCall2);

        // assert that we get exception if we have too short timeout
        boolean gotException = false;
        try {
          await(promises);
        } catch (Exception e) {
           Logger.info(e, "got xpected timeout ");
           gotException = true;
        }
        Assert.assertTrue("Did not get exception!", gotException);

        renderText("ok");
    }

}
