package play.plugins;

import java.util.ArrayList;
import java.util.List;

import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.ContinuationEnhancer;
import play.classloading.enhancers.ControllersEnhancer;
import play.classloading.enhancers.Enhancer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer;
import play.classloading.enhancers.MailerEnhancer;
import play.classloading.enhancers.SigEnhancer;
import play.exceptions.UnexpectedException;

/**
 * Plugin used for core tasks
 */
public class EnhancerPlugin extends PlayPlugin {

    @Override
    public void enhance(ApplicationClass applicationClass) {
        List<Enhancer> enhancers = new ArrayList<Enhancer>(4);
        if (applicationClass.name.startsWith("controllers.")) {
            enhancers.add(new SigEnhancer());
            enhancers.add(new ContinuationEnhancer());
            enhancers.add(new ControllersEnhancer());
            if (applicationClass.name.endsWith("Mailer")) {
                enhancers.add(new MailerEnhancer());
            }
            enhancers.add(new LocalvariablesNamesEnhancer());
        } else if (applicationClass.name.endsWith("Mailer")) {
            enhancers.add(new MailerEnhancer());
            enhancers.add(new LocalvariablesNamesEnhancer());
        }
        for (Enhancer enhancer : enhancers) {
            try {
                enhancer.enhanceThisClass(applicationClass);
            } catch (Exception e) {
                throw new UnexpectedException("While applying " + enhancer + " on " + applicationClass.name, e);
            }
        }
    }
}
