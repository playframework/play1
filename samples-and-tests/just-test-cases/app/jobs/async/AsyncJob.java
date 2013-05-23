package jobs.async;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import controllers.async.AsyncApplication;

import models.async.AsyncTrace;

import play.Logger;
import play.jobs.Job;
import play.mvc.Http.Response;
import play.mvc.Scope.RenderArgs;

public class AsyncJob extends Job {

  @Override
  public void doJob() throws Exception {    
    AsyncApplication.createTrace("~~~~ Begining job execution.");
    Logger.debug("~~~~ Begining job execution.");
    for (Integer i = 1; i < 6; i++) {
      AsyncApplication.createTrace(String.format("~~~~ In job, task %d...", i));
      Logger.debug("~~~~ In job, task %d...", i);
      Thread.sleep(1000);
    }
    AsyncApplication.createTrace("~~~~ Ending job execution.");
    Logger.debug("~~~~ Ending job execution.");
  }
  

}
