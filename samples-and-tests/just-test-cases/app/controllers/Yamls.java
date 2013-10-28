package controllers;

import models.YamlModel;

import org.apache.commons.mail.EmailException;

import play.mvc.*;

public class Yamls extends Controller {
    
    public static void viewYamlModel(Long id) {
	YamlModel model = YamlModel.findById(id);
	renderText(model.name);
    }

}
