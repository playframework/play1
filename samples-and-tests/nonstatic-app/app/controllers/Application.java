package controllers;

import play.mvc.PlayController;
import play.mvc.results.RenderTemplate;
import play.mvc.results.RenderText;
import play.mvc.results.Result;
import play.templates.Template;
import play.templates.TemplateLoader;

import static java.util.Collections.emptyMap;

public class Application implements PlayController {

    public Result index() {
        Template template = TemplateLoader.load("Application/index.html");
        return new RenderTemplate(template, emptyMap());
    }

    public Result hello() {
        return new RenderText("Hello world!");
    }

}