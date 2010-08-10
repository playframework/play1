package controllers;

import models.ErrorModel;
import play.data.validation.Valid;
import play.mvc.Controller;

public class Errors extends Controller {

    public static void index() {
        ErrorModel model = new ErrorModel();
        model.getMember().setName(flash.get("model.member.name"));
        render("Application/errors.html", model);
    }

    public static void submit(@Valid ErrorModel model) {
        if (validation.hasErrors()) {
            params.flash();
            index();
        } else {
            renderText("it works now: " + model.getMember().getName());
        }
    }   
}