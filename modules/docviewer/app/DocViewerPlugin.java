import play.PlayPlugin;
import play.mvc.Router;

public class DocViewerPlugin extends PlayPlugin { 
    
    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/@documentation/?", "PlayDocumentation.index");
        Router.addRoute("GET", "/@documentation/{id}", "PlayDocumentation.page");
        Router.addRoute("GET", "/@documentation/images/{name}", "PlayDocumentation.image");
    }

}
