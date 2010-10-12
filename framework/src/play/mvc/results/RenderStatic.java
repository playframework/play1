package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.vfs.VirtualFile;

public class RenderStatic extends Result {

    public String file;
    public VirtualFile resolvedFile;

    public RenderStatic(String file) {
        this.file = file;
    }

    @Override
    public void apply(Request request, Response response) {
    }

}
