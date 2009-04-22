package play.modules.gae;

import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import java.io.File;
import java.util.List;
import javax.jdo.JDOHelper;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.datanucleus.JDOClassLoaderResolver;
import org.datanucleus.enhancer.DataNucleusEnhancer;
import org.datanucleus.enhancer.asm.ASMClassEnhancer;
import org.datanucleus.metadata.FileMetaData;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;
import play.db.jpa.JPA;

public class GAEPlugin extends PlayPlugin {
    
    public ApiProxy.Environment devEnvironment = null;

    @Override
    public void onApplicationStart() {

        if(ApiProxy.getCurrentEnvironment() == null) {
            Logger.warn("No Google App Engine environment found. Setting up a development environement.");
            devEnvironment = new PlayDevEnvironment();
            ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(Play.applicationPath, "gae-dev")){});
            ApiProxy.setEnvironmentForCurrentThread(new PlayDevEnvironment());
        }
        
        // it's time to set up JPA
        List<Class> classes = Play.classloader.getAnnotatedClasses(Entity.class);
        if(!classes.isEmpty()) {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
        
            // Hack the JPA plugin
            JPA.entityManagerFactory = emf;
            
            // Hack Datanucleus
        }
        
    }

    @Override
    public void enhance(ApplicationClass applicationClass) {
        if(!applicationClass.javaSource.contains("@Entity")) {
            return;
        }
        System.out.println("ENHANCING "+applicationClass.name);
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
        new JDOClassLoaderResolver().setPrimary(Play.classloader);
    }    
    

    @Override
    public void onConfigurationRead() {
        Play.configuration.setProperty("play.tmp", "none");
    }

}
