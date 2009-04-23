package play.modules.gae;

import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import java.io.File;
import java.util.List;
import java.util.ListIterator;
import javax.persistence.Entity;
import javax.persistence.Persistence;
import org.datanucleus.enhancer.DataNucleusEnhancer;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.jobs.JobsPlugin;
import play.mvc.Router;

public class GAEPlugin extends PlayPlugin {
    
    public ApiProxy.Environment devEnvironment = null;
   
    @Override
    public void onLoad() {
        // Remove Jobs from plugin list
        for(ListIterator<PlayPlugin> it = Play.plugins.listIterator(); it.hasNext();) {
            if(it.next() instanceof JobsPlugin) {
                it.remove();
            }
        }
        // Force to PROD mode when hosted on production GAE
        if(ApiProxy.getCurrentEnvironment() != null && ApiProxy.getCurrentEnvironment().getClass().getName().indexOf("development") == -1) {
            Play.mode = Play.Mode.PROD;
        }
        // Create a fake development environment if not run in the Google SDK
        if(ApiProxy.getCurrentEnvironment() == null) {
            Logger.warn("No Google App Engine environment found. Setting up a development environement.");
            devEnvironment = new PlayDevEnvironment();
            ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(Play.applicationPath, "gae-dev")){});
            ApiProxy.setEnvironmentForCurrentThread(new PlayDevEnvironment());
            System.setProperty("appengine.orm.disable.duplicate.emf.exception", "yes");
        }
    }

    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET",  "/_ah/login", "GAEActions.login");
        Router.addRoute("POST", "/_ah/login", "GAEActions.doLogin");
        Router.addRoute("GET",  "/_ah/logout", "GAEActions.logout");
    }

    @Override
    public void onApplicationStart() {
        // It's time to set up JPA
        List<Class> classes = Play.classloader.getAnnotatedClasses(Entity.class);
        if(!classes.isEmpty()) {
            // Hack the JPA plugin
            JPAPlugin.autoTxs = false;
            JPA.entityManagerFactory = Persistence.createEntityManagerFactory("default");
        }        
    }

    @Override
    public void enhance(ApplicationClass applicationClass) {
        if(!applicationClass.javaSource.contains("@Entity")) {
            return;
        }
        DataNucleusEnhancer dataNucleusEnhancer = new DataNucleusEnhancer("JPA", "ASM");
        dataNucleusEnhancer.setVerbose(true);
        ClassLoader tempCl = new ClassLoader() {

            @Override
            /**
             * Temporarely define this class, just for need of enhancing
             */
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                ApplicationClass tempClass = Play.classes.getApplicationClass(name);
                if(tempClass != null) {
                    return defineClass(tempClass.name, tempClass.javaByteCode, 0, tempClass.javaByteCode.length, Play.classloader.protectionDomain);
                }
                return GAEPlugin.class.getClassLoader().loadClass(name);
            }
            
        };
        dataNucleusEnhancer.setClassLoader(tempCl);
        dataNucleusEnhancer.getMetaDataManager().loadClasses(new String[] {applicationClass.name}, tempCl);
        dataNucleusEnhancer.addClass(applicationClass.name, applicationClass.enhancedByteCode);
        dataNucleusEnhancer.enhance();
        applicationClass.enhancedByteCode = dataNucleusEnhancer.getEnhancedBytes(applicationClass.name);
    }
    
    @Override
    public void beforeInvocation() {
        // Set the current developement environment if needed
        if(devEnvironment != null) {
            ApiProxy.setEnvironmentForCurrentThread(new PlayDevEnvironment());
        }
    }        

    @Override
    public void onConfigurationRead() {
        // Disable tmp directory
        Play.configuration.setProperty("play.tmp", "none");
    }

}
