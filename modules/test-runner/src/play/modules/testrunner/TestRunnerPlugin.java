package play.modules.testrunner;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.mvc.Router;

public class TestRunnerPlugin extends PlayPlugin {

    @Override
    public void onLoad() {
        Logger.info("");
        Logger.info("Go to http://localhost:" + Play.configuration.getProperty("http.port", "9000") + "/@tests to run the tests");
        Logger.info("");
    }    

    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/@tests", "TestRunner.index");
        Router.addRoute("GET", "/@tests/{<.*>test}", "TestRunner.run");
        Router.addRoute("POST", "/@tests/{<.*>test}", "TestRunner.saveResult");
    }

}
