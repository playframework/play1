package play.modules.testrunner;

import java.io.File;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.mvc.Router;
import play.vfs.VirtualFile;

public class TestRunnerPlugin extends PlayPlugin {

    @Override
    public void onLoad() {
        VirtualFile appRoot = VirtualFile.open(Play.applicationPath);
        Play.javaPath.add(appRoot.child("test"));
        for (VirtualFile module : Play.modules.values()) {
            File modulePath = module.getRealFile();
            if (!modulePath.getAbsolutePath().startsWith(Play.frameworkPath.getAbsolutePath()) && !Play.javaPath.contains(module.child("test"))) {
                Play.javaPath.add(module.child("test"));
            }
        }
        Logger.info("");
        Logger.info("Go to http://localhost:" + Play.configuration.getProperty("http.port", "9000") + "/@tests to run the tests");
        Logger.info("");
    }

    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/@tests", "TestRunner.index");
        Router.addRoute("GET", "/@tests/{<.*>test}", "TestRunner.run");
        Router.addRoute("POST", "/@tests/{<.*>test}", "TestRunner.saveResult");
        Router.addRoute("GET", "/@tests/emails", "TestRunner.mockEmail");
    }
}
