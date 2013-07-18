package controllers;

import models.City;
import play.jobs.Job;
import play.libs.F.*;
import play.mvc.Controller;


public class CallingJobLater extends Controller {

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
    renderText(resultName);
  }

}
