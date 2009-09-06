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

        if (!ctClass.subtypeOf(classPool.get("play.db.jpa.JPASupport"))) {
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
        CtMethod count = CtMethod.make("public static long count() { return Long.parseLong(em().createQuery(\"select count(e) from " + entityName + " e\").getSingleResult().toString()); }", ctClass);
        ctClass.addMethod(count);
        
        // count2
        CtMethod count2 = CtMethod.make("public static long count(String query, Object[] params) { return Long.parseLong(play.db.jpa.JPQLDialect.instance.bindParameters(em().createQuery(play.db.jpa.JPQLDialect.instance.createCountQuery(\"" + entityName + "\", \"" + entityName + "\", query, params)), params).getSingleResult().toString()); }", ctClass);
        ctClass.addMethod(count2);

        // findAll
        CtMethod findAll = CtMethod.make("public static java.util.List findAll() { return em().createQuery(\"select e from " + entityName + " e\").getResultList();}", ctClass);
        ctClass.addMethod(findAll);

        // findById
        CtMethod findById = CtMethod.make("public static Object findById(Object id) { return (" + entityName + ") em().find(" + entityName + ".class, id); }", ctClass);
        ctClass.addMethod(findById);

        // findBy        
        CtMethod findBy = CtMethod.make("public static java.util.List findBy(String query, Object[] params) { javax.persistence.Query q = em().createQuery(play.db.jpa.JPQLDialect.instance.createFindByQuery(\"" + entityName + "\", \"" + entityName + "\", query, params)); return play.db.jpa.JPQLDialect.instance.bindParameters(q,params).getResultList(); }", ctClass);
        ctClass.addMethod(findBy);
        
        // find        
        CtMethod find = CtMethod.make("public static play.db.jpa.JPASupport.JPAQuery find(String query, Object[] params) { javax.persistence.Query q = em().createQuery(play.db.jpa.JPQLDialect.instance.createFindByQuery(\"" + entityName + "\", \"" + entityName + "\", query, params)); return new play.db.jpa.JPASupport.JPAQuery(play.db.jpa.JPQLDialect.instance.createFindByQuery(\"" + entityName + "\", \"" + entityName + "\", query, params), play.db.jpa.JPQLDialect.instance.bindParameters(q,params)); }", ctClass);
        ctClass.addMethod(find);
        
        // find        
        CtMethod find2 = CtMethod.make("public static play.db.jpa.JPASupport.JPAQuery find() { javax.persistence.Query q = em().createQuery(play.db.jpa.JPQLDialect.instance.createFindByQuery(\"" + entityName + "\", \"" + entityName + "\", null, null)); return new play.db.jpa.JPASupport.JPAQuery(play.db.jpa.JPQLDialect.instance.createFindByQuery(\"" + entityName + "\", \"" + entityName + "\", null, null), play.db.jpa.JPQLDialect.instance.bindParameters(q,null)); }", ctClass);
        ctClass.addMethod(find2);
        
        // all        
        CtMethod all = CtMethod.make("public static play.db.jpa.JPASupport.JPAQuery all() { javax.persistence.Query q = em().createQuery(play.db.jpa.JPQLDialect.instance.createFindByQuery(\"" + entityName + "\", \"" + entityName + "\", null, null)); return new play.db.jpa.JPASupport.JPAQuery(play.db.jpa.JPQLDialect.instance.createFindByQuery(\"" + entityName + "\", \"" + entityName + "\", null, null), play.db.jpa.JPQLDialect.instance.bindParameters(q,null)); }", ctClass);
        ctClass.addMethod(all);
        
        // delete        
        CtMethod delete = CtMethod.make("public static int delete(String query, Object[] params) { javax.persistence.Query q = em().createQuery(play.db.jpa.JPQLDialect.instance.createDeleteQuery(\"" + entityName + "\", \"" + entityName + "\", query, params)); return play.db.jpa.JPQLDialect.instance.bindParameters(q,params).executeUpdate(); }", ctClass);
        ctClass.addMethod(delete);
        
        // deleteAll        
        CtMethod deleteAll = CtMethod.make("public static int deleteAll() { javax.persistence.Query q = em().createQuery(play.db.jpa.JPQLDialect.instance.createDeleteQuery(\"" + entityName + "\", \"" + entityName + "\", null, null)); return play.db.jpa.JPQLDialect.instance.bindParameters(q,null).executeUpdate(); }", ctClass);
        ctClass.addMethod(deleteAll);

        // findOneBy
        CtMethod findOneBy = CtMethod.make("public static Object findOneBy(String query, Object[] params) { javax.persistence.Query q = em().createQuery(play.db.jpa.JPQLDialect.instance.createFindByQuery(\"" + entityName + "\", \"" + entityName + "\", query, params)); java.util.List results = play.db.jpa.JPQLDialect.instance.bindParameters(q,params).getResultList(); if(results.size() == 0) return null; return (play.db.jpa.JPASupport)results.get(0); }", ctClass);
        ctClass.addMethod(findOneBy);
        
        // create     
        CtMethod create = CtMethod.make("public static Object create(String name, play.mvc.Scope.Params params) { return (play.db.jpa.JPASupport)((play.db.jpa.JPASupport)" + entityName + ".class.newInstance()).edit(name, params); }", ctClass);
        ctClass.addMethod(create);

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

}
