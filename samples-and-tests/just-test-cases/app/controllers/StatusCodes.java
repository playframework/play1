package controllers;

import play.jobs.Job;
import play.mvc.Controller;
import play.mvc.Http.Response;

public class StatusCodes extends Controller {

  public static void justOkay() {
    renderText("Okay");
  }

  public static void rendersNotFound() {
    notFound();
  }

  public static void rendersUnauthorized() {
    unauthorized();
  }

  public static void usesContinuation() {
    final String text = await(new Job<String>() {
      @Override
      public String doJobWithResult() throws Exception {
        return "Job completed successfully";
      }
    }.now());
    Response.current().status = Integer.valueOf(201);
    renderText(text);
  }

  public static void throwsException() throws Exception {
    throw new UnsupportedOperationException("Whoops");
  }

}
