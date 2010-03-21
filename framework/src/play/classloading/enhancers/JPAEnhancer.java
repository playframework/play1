package play.classloading.enhancers;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;

/**
 * Enhance JPASupport entities classes
 */
public class JPAEnhancer extends Enhancer {

    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get("play.db.jpa.JPABase"))) {
            return;
        }
        
        // Enhance only JPA entities
        if (!hasAnnotation(ctClass, "javax.persistence.Entity")) {
            return;
        }
        
        String entityName = ctClass.getName();
        
        // Add a default constructor if needed
        try {
            boolean hasDefaultConstructor = false;
            for (CtConstructor constructor : ctClass.getConstructors()) {
                if (constructor.getParameterTypes().length == 0) {
                    hasDefaultConstructor = true;
                    break;
                }
            }
            if (!hasDefaultConstructor && !ctClass.isInterface()) {
                CtConstructor defaultConstructor = CtNewConstructor.make("private " + ctClass.getSimpleName() + "() {}", ctClass);
                ctClass.addConstructor(defaultConstructor);
            }
        } catch (Exception e) {
            Logger.error(e, "Error in JPAEnhancer");
            throw new UnexpectedException("Error in JPAEnhancer", e);
        }

        // count
        CtMethod count = CtMethod.make("public static long count() { return play.db.jpa.JPQL.instance.count(\"" + entityName + "\"); }", ctClass);
        ctClass.addMethod(count);
        
        // count2
        CtMethod count2 = CtMethod.make("public static long count(String query, Object[] params) { return play.db.jpa.JPQL.instance.count(\"" + entityName + "\", query, params); }", ctClass);
        ctClass.addMethod(count2);

        // findAll
        CtMethod findAll = CtMethod.make("public static java.util.List findAll() { return play.db.jpa.JPQL.instance.findAll(\"" + entityName + "\"); }", ctClass);
        ctClass.addMethod(findAll);

        // findById
        CtMethod findById = CtMethod.make("public static play.db.jpa.JPABase findById(Object id) { return play.db.jpa.JPQL.instance.findById(\"" + entityName + "\", id); }", ctClass);
        ctClass.addMethod(findById);

        // findBy        
        CtMethod findBy = CtMethod.make("public static java.util.List findBy(String query, Object[] params) { return play.db.jpa.JPQL.instance.findBy(\"" + entityName + "\", query, params); }", ctClass);
        ctClass.addMethod(findBy);
        
        // find        
        CtMethod find = CtMethod.make("public static play.db.jpa.JPASupport.JPAQuery find(String query, Object[] params) { return play.db.jpa.JPQL.instance.find(\"" + entityName + "\", query, params); }", ctClass);
        ctClass.addMethod(find);
        
        // find        
        CtMethod find2 = CtMethod.make("public static play.db.jpa.JPASupport.JPAQuery find() { return play.db.jpa.JPQL.instance.find(\"" + entityName + "\"); }", ctClass);
        ctClass.addMethod(find2);
        
        // all        
        CtMethod all = CtMethod.make("public static play.db.jpa.JPASupport.JPAQuery all() { return play.db.jpa.JPQL.instance.all(\"" + entityName + "\"); }", ctClass);
        ctClass.addMethod(all);
        
        // delete        
        CtMethod delete = CtMethod.make("public static int delete(String query, Object[] params) { return play.db.jpa.JPQL.instance.delete(\"" + entityName + "\", query, params); }", ctClass);
        ctClass.addMethod(delete);
        
        // deleteAll        
        CtMethod deleteAll = CtMethod.make("public static int deleteAll() { return play.db.jpa.JPQL.instance.deleteAll(\"" + entityName + "\"); }", ctClass);
        ctClass.addMethod(deleteAll);

        // findOneBy
        CtMethod findOneBy = CtMethod.make("public static play.db.jpa.JPABase findOneBy(String query, Object[] params) { return play.db.jpa.JPQL.instance.findOneBy(\"" + entityName + "\", query, params); }", ctClass);
        ctClass.addMethod(findOneBy);
        
        // create     
        CtMethod create = CtMethod.make("public static play.db.jpa.JPABase create(String name, play.mvc.Scope.Params params) { return play.db.jpa.JPQL.instance.create(\"" + entityName + "\", name, params); }", ctClass);
        ctClass.addMethod(create);

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

}
