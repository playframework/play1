import play.*;
import play.mvc.Http.*;
import play.mvc.*;
import play.libs.*;
import play.vfs.*;

import java.io.*;

public class DocViewerPlugin extends PlayPlugin {

    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {
        if ("/@api".equals(request.path) || "/@api/".equals(request.path)) {
            response.status = 302;
            response.setHeader("Location", "/@api/index.html");
            return true;
        }
        if (request.path.startsWith("/@api/")) {
            if(request.path.matches("/@api/-[a-z]+/.*")) {
                String module = request.path.substring(request.path.indexOf("-")+1);
                module = module.substring(0, module.indexOf("/"));
                VirtualFile f = Play.modules.get(module).child("documentation/api/"+request.path.substring(8+module.length()));
                if(f.exists()) {
                    response.contentType = MimeTypes.getMimeType(f.getName());
                    response.out.write(f.content());
                }
                return true;
            }
            File f = new File(Play.frameworkPath, "documentation/api/"+request.path.substring(6));
            if (f.exists()) {
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
        Router.prependRoute("GET", "/@documentation/modules/{module}/{id}", "PlayDocumentation.page");
        Router.prependRoute("GET", "/@documentation/modules/{module}/images/{name}", "PlayDocumentation.image");
        Router.prependRoute("GET", "/@documentation/modules/{module}/files/{name}", "PlayDocumentation.file");
        Router.prependRoute("GET", "/@documentation/cheatsheet/{category}", "PlayDocumentation.cheatSheet");
    }

}
