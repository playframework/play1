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
        Router.prependRoute("GET", "/@documentation/?", "PlayDocumentation.index");
        Router.prependRoute("GET", "/@documentation/{id}", "PlayDocumentation.page");
        Router.prependRoute("GET", "/@documentation/images/{name}", "PlayDocumentation.image");
        Router.prependRoute("GET", "/@documentation/files/{name}", "PlayDocumentation.file");
    }

}
