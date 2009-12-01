package play.modules.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.ArrayList;
import java.util.List;
import play.Play;
import play.PlayPlugin;
import play.inject.BeanSource;
import play.Logger;
/**
 *  Enable <a href="http://google-guice.googlecode.com">Guice</a> integration
 *  in Playframework.
 *  This plugin first scans for a custom Guice Injector if it's not found, then
 *  it tries to create an injector from all the guice modules available on the classpath.
 *  The Plugin is then passed to Play injector for Controller IoC.
 *
 *  @author <a href="mailto:info@lucianofiandesio.com">Luciano Fiandesio</a>
 *  @author <a href="mailto:info@hausel@freemail.hu">Peter Hausel</a>
 */
public class GuicePlugin extends PlayPlugin implements BeanSource {

	
    Injector injector;

    @Override
    public void onApplicationStart() {
        
        final List<Module> modules = new ArrayList<Module>();
        final List<Class> ll = Play.classloader.getAllClasses();
        Logger.debug("Starting Guice modules scanning");
		Boolean newInjectorNeeded = true;
		StringBuffer moduleList = new StringBuffer();
        for (final Class clz : ll) {
        	//first check if there is a custom Injector on the classpath, if so, stop scanning 
			//and ignore modules altogether
			if (clz.getSuperclass() != null && GuiceSupport.class.isAssignableFrom(clz)) {
               try {
			       GuiceSupport gs = (GuiceSupport) clz.newInstance();
			       this.injector = gs.configure();
			       newInjectorNeeded = false;
               	   Logger.info("Guice injector was found: " + clz.getName());
			   	   break;
			   } catch (Exception e) {
				    e.printStackTrace();
					throw new IllegalStateException("Unable to create Guice Injector for " + clz.getName());
			   }
			}
            if (clz.getSuperclass() != null && AbstractModule.class.isAssignableFrom(clz)) {
                try {
                    modules.add((Module) clz.newInstance());
                	moduleList.append(clz.getName()+" ");
                } catch (Exception e) {
					e.printStackTrace();
					throw new IllegalStateException("Unable to create Guice module for " + clz.getName());
                }
            }


        }
		if (newInjectorNeeded && modules.isEmpty()) {
			 throw new IllegalStateException("could not find any custom guice injector or abstract modules. Are you sure you have at least one on the classpath?");
		}
        if (!modules.isEmpty() && newInjectorNeeded) {
			Logger.info("Guice modules were found: "+moduleList);
            this.injector = Guice.createInjector(modules);
        } 
        play.inject.Injector.inject(this);
    }

    public <T> T getBeanOfType(Class<T> clazz) {
        if (this.injector==null)return null;
        return this.injector.getInstance(clazz);
    }
}
