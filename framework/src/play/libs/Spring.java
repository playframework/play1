package play.libs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.xml.sax.InputSource;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.Play.Mode;
import play.exceptions.PlayException;
import play.vfs.VirtualFile;

/**
 * SpringApplication context support
 * 
 * To enable it, edit or create a new play.plugins file, and add 
 * something like 5:play.libs.Spring
 *
 */
public class Spring extends PlayPlugin {

    public static GenericApplicationContext applicationContext;
    private long startDate = 0;

    @Override
    public void detectChange() {
        if (Play.mode == Mode.DEV) {
            VirtualFile appRoot = VirtualFile.open(Play.applicationPath);
            long mod = appRoot.child("conf/application-context.xml").lastModified();
            if (mod > startDate) {
                throw new RuntimeException("conf/application-context.xml has changed");
            }
        }
    }

    @Override
    public void onApplicationStop() {
        if (applicationContext != null) {
            Logger.debug("Closing Spring application context");
            applicationContext.close();
        }
    }

    @Override
    public void onApplicationStart() {
        URL url = this.getClass().getClassLoader().getResource("application-context.xml");
        if (url != null) {
            InputStream is = null;
            try {
                Logger.debug("Starting Spring application context");
                applicationContext = new GenericApplicationContext();
                applicationContext.setClassLoader(Play.classloader);
                XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(applicationContext);
                xmlReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
                PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
                configurer.setProperties(Play.configuration);
                applicationContext.addBeanFactoryPostProcessor(configurer);

                is = url.openStream();
                xmlReader.loadBeanDefinitions(new InputSource(is));
                ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(Play.classloader);
                try {
                    applicationContext.refresh();
                    startDate = System.currentTimeMillis();
                } catch (BeanCreationException e) {
                    Throwable ex = e.getCause();
                    if (ex instanceof PlayException) {
                        throw (PlayException) ex;
                    } else {
                        throw e;
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }
            } catch (IOException e) {
                Logger.error(e, "Can't load spring config file");
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Logger.error(e, "Can't close spring config file stream");
                    }
                }
            }
        }
    }

    public static Object getBean(String name) {
        if (applicationContext == null) {
            throw new SpringError();
        }
        return applicationContext.getBean(name);
    }

    public static class SpringError extends PlayException {

        private static final long serialVersionUID = 1L;

        public String getErrorDescription() {
            return "The Spring application context is not started.";
        }

        public String getErrorTitle() {
            return "Spring context is not started !";
        }
    }
}
