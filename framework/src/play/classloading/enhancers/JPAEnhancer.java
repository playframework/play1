package play.classloading.enhancers;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * Enhance JPAModel classes.
 */
public class JPAEnhancer extends Enhancer {

    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
    	CtClass ctClass = makeClass(applicationClass);
    	
    	if(!ctClass.subtypeOf(classPool.get("play.db.jpa.JPAModel"))) {
            return;
        }
    	
        String entityName = ctClass.getSimpleName();
        Logger.trace("Enhacing "+entityName);
        
        // Ajoute le constructeur par défaut (obligatoire pour la peristence)
        try {
            boolean hasDefaultConstructor = false;
            for (CtConstructor constructor : ctClass.getConstructors()) {
                if (constructor.getParameterTypes().length == 0) {
                    hasDefaultConstructor = true;
                    break;
                }
            }
            if (!hasDefaultConstructor) {
                CtConstructor defaultConstructor = CtNewConstructor.make("private " + ctClass.getSimpleName() + "() {}", ctClass);
                ctClass.addConstructor(defaultConstructor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Implémenter les méthodes statiques
        
        // count
        CtMethod count = CtMethod.make("public static Long count() { return (Long) getEntityManager().createQuery(\"select count(*) from "+ctClass.getName()+"\").getSingleResult(); }", ctClass);
        ctClass.addMethod(count);        
        
        // findAll
        CtMethod findAll = CtMethod.make("public static java.util.List findAll() { return getEntityManager().createQuery(\"select e from "+entityName+" e\").getResultList();}", ctClass);
        ctClass.addMethod(findAll);

        // findById
        CtMethod findById = CtMethod.make("public static play.db.jpa.JPAModel findById(Long id) { return ("+ctClass.getName()+") getEntityManager().find("+ctClass.getName()+".class, id); }", ctClass);
        ctClass.addMethod(findById);
        
        // findBy        
        CtMethod findBy = CtMethod.make("public static java.util.List findBy(String query, Object[] params) { javax.persistence.Query q = getEntityManager().createQuery(createFindByQuery(\"" + ctClass.getSimpleName() + "\", \"" + ctClass.getName() + "\", query, params)); return bindParameters(q,params).getResultList(); }", ctClass);
        ctClass.addMethod(findBy);
        
        // findOneBy
        CtMethod findOneBy = CtMethod.make("public static play.db.jpa.JPAModel findOneBy(String query, Object[] params) { javax.persistence.Query q = getEntityManager().createQuery(createFindByQuery(\"" + ctClass.getSimpleName() + "\", \"" + ctClass.getName() + "\", query, params)); return ("+ctClass.getName()+") bindParameters(q,params).getSingleResult(); }", ctClass);
        ctClass.addMethod(findOneBy);
        
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

}
