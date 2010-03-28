import play.*;
import play.mvc.Http.*;
import play.mvc.*;
import play.libs.*;

import java.io.*;

public class DocViewerPlugin extends PlayPlugin { 
    
    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {
        if(request.path.startsWith("/@api/")) {
            File f = new File(Play.frameworkPath, "documentation/api/"+request.path.substring(6));
            if(f.exists()) {
                response.contentType = MimeTypes.getMimeType(f.getName());
                response.out.write(IO.readContent(f));
            }
            return true;
        }
        return false;
    }
    
    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/@documentation/?", "PlayDocumentation.index");
        Router.addRoute("GET", "/@documentation/{id}", "PlayDocumentation.page");
        Router.addRoute("GET", "/@documentation/images/{name}", "PlayDocumentation.image");
        Router.addRoute("GET", "/@documentation/files/{name}", "PlayDocumentation.file");
        Router.addRoute("GET", "/@documentation/modules/{module}/{id}", "PlayDocumentation.page");
        Router.addRoute("GET", "/@documentation/modules/{module}/images/{name}", "PlayDocumentation.image");
    }

}
