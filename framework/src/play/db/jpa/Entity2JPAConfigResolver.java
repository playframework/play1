package play.db.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.PersistenceUnit;

import play.Play;
import play.classloading.ApplicationClassloaderState;
import play.db.DBConfig;

public class Entity2JPAConfigResolver {

    private static ApplicationClassloaderState ourClassloaderState = null;
    private static Map<Class, String> class2ConfigNameMapping = new HashMap<Class, String>();

    protected synchronized static String getJPAConfigNameForEntityClass(Class clazz) {

        // if there is changes to application classes we must invalidate the cache
        ApplicationClassloaderState currentState = Play.classloader.currentState;
        if (!currentState.equals(ourClassloaderState)) {
            // must invalidate cache
            ourClassloaderState = currentState;
            class2ConfigNameMapping.clear();
        }

        String configName = class2ConfigNameMapping.get(clazz);
        if (configName!=null) {
            return configName;
        }
        configName = resolveJPAConfigNameForEntityClass(clazz);
        class2ConfigNameMapping.put(clazz, configName);
        return configName;

    }

    private static String resolveJPAConfigNameForEntityClass(Class clazz) {
        @SuppressWarnings("unchecked")
        PersistenceUnit persistenceUnitAnnotation = (PersistenceUnit)clazz.getAnnotation(PersistenceUnit.class);
        if (persistenceUnitAnnotation!=null) {
            return persistenceUnitAnnotation.name();
        } else {

            // look for @PersistenceUnit on package and sub packages
            Package pg = clazz.getPackage();
            if (pg!= null) {
                persistenceUnitAnnotation = pg.getAnnotation(PersistenceUnit.class);
                if (persistenceUnitAnnotation!=null) {
                    return persistenceUnitAnnotation.name();
                }
            }

            // annotation missing. Assuming default
            return DBConfig.defaultDbConfigName;
        }
    }

}
