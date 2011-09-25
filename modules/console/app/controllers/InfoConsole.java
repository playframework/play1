package controllers;

import play.mvc.*;
import play.jobs.JobsPlugin;
import play.*;

@With(Secure.class)
@Check("isAdmin")
public class InfoConsole extends Controller {
  public static void index() {
     renderArgs.put("freemem", Runtime.getRuntime().freeMemory());
     renderArgs.put("processors", Runtime.getRuntime().availableProcessors());
     renderArgs.put("totalmem", Runtime.getRuntime().totalMemory());
     renderArgs.put("threads", Thread.getAllStackTraces().size());
     renderArgs.put("jobs", JobsPlugin.scheduledJobs);
     renderArgs.put("executor",JobsPlugin.executor);
     render("/console/index.html");
  }
}
