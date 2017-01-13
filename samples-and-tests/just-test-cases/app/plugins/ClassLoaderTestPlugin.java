package plugins;

import play.PlayPlugin;

public class ClassLoaderTestPlugin extends PlayPlugin {

    public ClassLoader contextClassLoader;

    @Override
    public void onApplicationStart() {
        contextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void onApplicationStop() {
        contextClassLoader = null;
    }
}
