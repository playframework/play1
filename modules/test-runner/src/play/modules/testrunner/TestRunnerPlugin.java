package play.modules.testrunner;

import play.Logger;
import play.PlayPlugin;
import play.mvc.Router;
import play.server.Server;

public class TestRunnerPlugin extends PlayPlugin {

    @Override
    public void onLoad() {
        Logger.info("");
        Logger.info("Go to http://localhost:" + Server.port + "/@tests to run the tests");
        Logger.info("");
    }    

    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/@tests", "TestRunner.index");
        Router.addRoute("GET", "/@tests/{test}", "TestRunner.run");
    }

}
