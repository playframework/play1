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

public class GAEPlugin extends PlayPlugin {
    
    public ApiProxy.Environment devEnvironment = null;
   

    @Override
    public void onLoad() {
        for(ListIterator<PlayPlugin> it = Play.plugins.listIterator(); it.hasNext();) {
            if(it.next() instanceof JobsPlugin) {
                it.remove();
            }
        }
        if(ApiProxy.getCurrentEnvironment() != null && ApiProxy.getCurrentEnvironment().getClass().getName().indexOf("development") == -1) {
            Play.mode = Play.Mode.PROD;
        }
    }

    @Override
    public void onApplicationStart() {

        if(ApiProxy.getCurrentEnvironment() == null) {
            Logger.warn("No Google App Engine environment found. Setting up a development environement.");
            devEnvironment = new PlayDevEnvironment();
            ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(Play.applicationPath, "gae-dev")){});
            ApiProxy.setEnvironmentForCurrentThread(new PlayDevEnvironment());
            System.setProperty("appengine.orm.disable.duplicate.emf.exception", "yes");
        } 
        
        // it's time to set up JPA
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
        if(devEnvironment != null) {
            ApiProxy.setEnvironmentForCurrentThread(new PlayDevEnvironment());
        }
    }    
    

    @Override
    public void onConfigurationRead() {
        Play.configuration.setProperty("play.tmp", "none");
    }

}
