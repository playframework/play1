package controllers.async;

import play.*;
import play.mvc.*;
import play.mvc.Http.Response;

import java.io.ByteArrayOutputStream;
import java.util.*;

import jobs.async.AsyncJob;

import models.*;
import models.async.AsyncTrace;

public class AsyncApplication extends Controller {

    public static void index() {
        createTrace("~~~~ Begining controller execution.");
        Logger.debug("~~~~ Begining controller execution.");
        await(new AsyncJob().now());

        createTrace("~~~~ Ending controller execution => Redirecting.");
        Logger.debug("~~~~ Ending controller execution => Redirecting.");
        redirection();
    }

    public static void redirection() {
        render();
    }
    
    @Util
    public static void createTrace(String content){
        AsyncTrace trace = new AsyncTrace();
        trace.position = AsyncTrace.count();
        trace.content = content;
        trace.save();
    }

}