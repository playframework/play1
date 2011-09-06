package controllers;

import org.junit.Assert;
import play.Logger;
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

}
