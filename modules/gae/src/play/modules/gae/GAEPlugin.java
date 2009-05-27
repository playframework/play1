package play.modules.gae;

import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import java.io.File;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import javax.mail.Session;
import javax.persistence.Entity;
import javax.persistence.Persistence;
import org.datanucleus.enhancer.DataNucleusEnhancer;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.cache.Cache;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.db.jpa.JPQLDialect;
import play.exceptions.UnexpectedException;
import play.jobs.JobsPlugin;
import play.libs.IO;
import play.libs.Mail;
import play.mvc.Router;

public class GAEPlugin extends PlayPlugin {
    
    public ApiProxy.Environment devEnvironment = null;
    public boolean prodGAE;
   
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
            prodGAE = true;
        }
        // Create a fake development environment if not run in the Google SDK
        if(ApiProxy.getCurrentEnvironment() == null) {
            Logger.warn("");
            Logger.warn("Google App Engine module");
            Logger.warn("~~~~~~~~~~~~~~~~~~~~~~~");
            Logger.warn("No Google App Engine environment found. Setting up a development environement");
            devEnvironment = new PlayDevEnvironment();
            ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(Play.applicationPath, "war")){});
            ApiProxy.setEnvironmentForCurrentThread(new PlayDevEnvironment());
            System.setProperty("appengine.orm.disable.duplicate.emf.exception", "yes");
            File warExt = Play.getFile("war");
            if(!warExt.exists()) {
                warExt.mkdir();
            }
            File webInf = Play.getFile("war/WEB-INF");
            if(!webInf.exists()) {
                webInf.mkdir();
            }
            File xml = Play.getFile("war/WEB-INF/appengine-web.xml");
            try {
                if(!xml.exists()) {
                    IO.writeContent("<appengine-web-app xmlns=\"http://appengine.google.com/ns/1.0\">\n" +                    
                    "\t<application><!-- Replace this with your application id from http://appengine.google.com --></application>\n" +
                    "\t<version>1</version>\n" +
                    "</appengine-web-app>\n", xml);
                }
                if(IO.readContentAsString(xml).contains("<!-- Replace this with your application id from http://appengine.google.com -->")) {
                    Logger.warn("Don't forget to define your GAE application id in the 'war/WEB-INF/appengine-web.xml' file");
                }
            } catch(Exception e) {
                Logger.error(e, "Cannot init GAE files");
            }
            Logger.warn("");
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
            JPQLDialect.instance = new DataNucleusDialect ();
        } 
        
        // Wrap the GAE cache
        if(devEnvironment == null) {
            Cache.forcedCacheImpl = new GAECache();
        }

        // Provide the correct JavaMail session
        Mail.session = Session.getDefaultInstance(new Properties(), null);
        Mail.asynchronousSend = false;
    }

    @Override
    public void enhance(ApplicationClass applicationClass) {
        ClassLoader tempCl = new ClassLoader() {

            @Override
            /**
             * Temporarely define this class, just for need of enhancing
             */
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                Class c = findLoadedClass(name);
                if (c != null) {
                    return c;
                }
                ApplicationClass tempClass = Play.classes.getApplicationClass(name);
                if(tempClass != null) {
                    return defineClass(tempClass.name, tempClass.javaByteCode, 0, tempClass.javaByteCode.length, Play.classloader.protectionDomain);
                }
                return GAEPlugin.class.getClassLoader().loadClass(name);
            }
            
        };
        try {
            if(!tempCl.loadClass(applicationClass.name).isAnnotationPresent(Entity.class)) {
                return;
            }
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
                
        DataNucleusEnhancer dataNucleusEnhancer = new DataNucleusEnhancer("JDO", "ASM");
        dataNucleusEnhancer.setVerbose(true);
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
        if(devEnvironment == null) {
            Play.configuration.setProperty("application.log", "DEBUG");
        }
    }

}
