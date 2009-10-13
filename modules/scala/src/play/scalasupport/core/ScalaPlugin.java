package play.scalasupport.core;

import java.util.List;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;

public class ScalaPlugin extends PlayPlugin {

    @Override
    public void compileAll(List<ApplicationClass> classes) {
        ScalaCompiler.compileAll(classes); 
    }

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        new ControllerObjectEnhancer().enhanceThisClass(applicationClass);
    }
    
}
