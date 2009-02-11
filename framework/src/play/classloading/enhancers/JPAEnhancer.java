package play.classloading.enhancers;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * Enhance JPAModel entities classes
 */
public class JPAEnhancer extends Enhancer {

    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get("play.db.jpa.JPAModel"))) {
            return;
        }
        
        // les classes non entity ne doivent pas etre instrumentees
        if (!hasAnnotation(ctClass, "javax.persistence.Entity")) {
            return;
        }
        
        String entityName = ctClass.getSimpleName();
        // Ajoute le constructeur par défaut (obligatoire pour la peristence)
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
        }

        // count
        CtMethod count = CtMethod.make("public static Long count() { return (Long) em().createQuery(\"select count(*) from " + ctClass.getName() + "\").getSingleResult(); }", ctClass);
        ctClass.addMethod(count);
        
        // count2
        CtMethod count2 = CtMethod.make("public static Long count(String query, Object[] params) { return (Long) bindParameters(em().createQuery(createCountQuery(\"" + ctClass.getSimpleName() + "\", \"" + ctClass.getName() + "\", query, params)), params).getSingleResult(); }", ctClass);
        ctClass.addMethod(count2);


        // findAll
        CtMethod findAll = CtMethod.make("public static java.util.List findAll() { return em().createQuery(\"select e from " + entityName + " e\").getResultList();}", ctClass);
        ctClass.addMethod(findAll);

        // findById
        CtMethod findById = CtMethod.make("public static play.db.jpa.JPAModel findById(Long id) { return (" + ctClass.getName() + ") em().find(" + ctClass.getName() + ".class, id); }", ctClass);
        ctClass.addMethod(findById);

        // findBy        
        CtMethod findBy = CtMethod.make("public static java.util.List findBy(String query, Object[] params) { javax.persistence.Query q = em().createQuery(createFindByQuery(\"" + ctClass.getSimpleName() + "\", \"" + ctClass.getName() + "\", query, params)); return bindParameters(q,params).getResultList(); }", ctClass);
        ctClass.addMethod(findBy);
        
        // find        
        CtMethod find = CtMethod.make("public static play.db.jpa.JPAModel.JPAQuery find(String query, Object[] params) { javax.persistence.Query q = em().createQuery(createFindByQuery(\"" + ctClass.getSimpleName() + "\", \"" + ctClass.getName() + "\", query, params)); return new play.db.jpa.JPAModel.JPAQuery(bindParameters(q,params)); }", ctClass);
        ctClass.addMethod(find);
        
        // find        
        CtMethod find2 = CtMethod.make("public static play.db.jpa.JPAModel.JPAQuery find() { javax.persistence.Query q = em().createQuery(createFindByQuery(\"" + ctClass.getSimpleName() + "\", \"" + ctClass.getName() + "\", null, null)); return new play.db.jpa.JPAModel.JPAQuery(bindParameters(q,null)); }", ctClass);
        ctClass.addMethod(find2);
        
        // delete        
        CtMethod delete = CtMethod.make("public static int delete(String query, Object[] params) { javax.persistence.Query q = em().createQuery(createDeleteQuery(\"" + ctClass.getSimpleName() + "\", \"" + ctClass.getName() + "\", query, params)); return bindParameters(q,params).executeUpdate(); }", ctClass);
        ctClass.addMethod(delete);
        
        // deleteAll        
        CtMethod deleteAll = CtMethod.make("public static int deleteAll() { javax.persistence.Query q = em().createQuery(createDeleteQuery(\"" + ctClass.getSimpleName() + "\", \"" + ctClass.getName() + "\", null, null)); return bindParameters(q,null).executeUpdate(); }", ctClass);
        ctClass.addMethod(deleteAll);

        // findOneBy
        CtMethod findOneBy = CtMethod.make("public static play.db.jpa.JPAModel findOneBy(String query, Object[] params) { javax.persistence.Query q = em().createQuery(createFindByQuery(\"" + ctClass.getSimpleName() + "\", \"" + ctClass.getName() + "\", query, params)); java.util.List results = bindParameters(q,params).getResultList(); if(results.size() == 0) return null; return (play.db.jpa.JPAModel)results.get(0); }", ctClass);
        ctClass.addMethod(findOneBy);
        
        // create     
        CtMethod create = CtMethod.make("public static play.db.jpa.JPAModel create(String name, play.mvc.Scope.Params params) { return (play.db.jpa.JPAModel)((play.db.jpa.JPAModel)" + ctClass.getName() + ".class.newInstance()).edit(name, params); }", ctClass);
        ctClass.addMethod(create);

        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

}
