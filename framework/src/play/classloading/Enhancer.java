package play.classloading;

import play.classloading.ApplicationClasses.ApplicationClass;

public interface Enhancer {
    
    void enhanceThisClass(ApplicationClass applicationClass);

}
