package play.db;

import org.h2.tools.Server;
import play.Play;
import play.PlayPlugin;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class DBBrowserPlugin extends PlayPlugin {

    private Server h2Server;

    @Override
    public void onConfigurationRead() {
        if (!Play.mode.isDev()) {
            Play.pluginCollection.disablePlugin(this);
        }
    }

    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {
        if (request.path.equals("/@db")) {
            response.status = Http.StatusCode.FOUND;
            String serverOptions[] = new String[] { };

            // For H2 embedded database, we'll also start the Web console
            if (h2Server != null) {
                h2Server.stop();
            }

            String domain = request.domain;
            if (domain.equals("")) {
                domain = "localhost";
            }

            if (!domain.equals("localhost")) {
                serverOptions = new String[] {"-webAllowOthers"};
            }
            
            h2Server = Server.createWebServer(serverOptions);
            h2Server.start();

            response.setHeader("Location", "http://" + domain + ":8082/");
            return true;
        }
        return false;
    }
}
