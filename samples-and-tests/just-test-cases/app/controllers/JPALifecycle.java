package controllers;

import models.ModelWithLifecycleListeners;
import play.mvc.Controller;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class JPALifecycle extends Controller {

    public static void create() {
        ModelWithLifecycleListeners modelWithLifecycleListeners = new ModelWithLifecycleListeners();
        modelWithLifecycleListeners.transientValue = "created";
        modelWithLifecycleListeners.create();
        renderText(modelWithLifecycleListeners.id);
    }

    public static void update(Long id) {
        ModelWithLifecycleListeners modelWithLifecycleListeners = ModelWithLifecycleListeners.findById(id);
        modelWithLifecycleListeners.transientValue = "updated";
        modelWithLifecycleListeners.save();
        renderText(modelWithLifecycleListeners.id);
    }

}
