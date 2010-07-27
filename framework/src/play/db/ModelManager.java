package play.db;

import play.Play;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;

public class ModelManager {

    public static ModelLoader loaderFor(Class<Model> clazz) {
        if(Model.class.isAssignableFrom(clazz)) {
            for(PlayPlugin plugin : Play.plugins) {
                ModelLoader loader = plugin.modelLoader(clazz);
                if(loader != null) {
                    return loader;
                }
            }
        }
        throw new UnexpectedException("Model " + clazz.getName() + " is not managed by any plugin");
    }

}
