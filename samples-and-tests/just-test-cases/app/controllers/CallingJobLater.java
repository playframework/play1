package controllers;

import models.City;
import play.jobs.Job;
import play.libs.F.*;
import play.mvc.Controller;

/**
 * Test controller to check Job.afterRequest() behavior.
 *
 * In one request we save a fresh entity and schedule a job to do something with this entity, identified by id.
 *
 * Job.now() won't see this enetity, because transaction is not ended yet, so we schedule a job to run after the request is complete.
 * Then the job should be able to retrieve the entity. Which is exactly what we want to test.
 */
public class CallingJobLater extends Controller {

  /*
   * Static variables in controllers are not usually considered a good practice.
   * However this controller is for testing purposes only and I need to store
   * information between requests and don't want to use DB.
   */
  private static Promise p;
  private static String resultName;


  public static void saveEntityTriggerJob() {
    final City city = new City();
    city.name = "cityNameJobLater";
    city.save();

    p = new Job() {
      public void doJob() throws Exception {
        City mycity = City.findById(city.id);
        resultName = mycity.name;
      }
    }.afterRequest();
    renderText("saved");
  }

  public static void renderSaveEntityTriggerJobResult() {
    if(p == null)
      renderText("Promise is not set yet");

    await(p);
    String resultNameToRender = resultName;

    nullifyStaticVariables();

    renderText(resultNameToRender);
  }

  private static void nullifyStaticVariables() {
    p = null;
    resultName = null;
  }

}
