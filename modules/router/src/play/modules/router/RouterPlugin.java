package play.modules.router;

import play.PlayPlugin;
import play.Play;
import play.classloading.ApplicationClasses;
import play.mvc.Router;
import play.utils.Java;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RouterPlugin extends PlayPlugin {


    @Override
    public void onClassesChange(List<ApplicationClasses.ApplicationClass> modified) {
        computeRoutes();
    }

    @Override
    public void onApplicationStart() {
        computeRoutes();
    }

    protected void computeRoutes() {
        List<Class> controllerClasses = getControllerClasses();
        List<Method> gets = Java.findAllAnnotatedMethods(controllerClasses, Get.class);
        for (Method get : gets) {
            Get annotation = get.getAnnotation(Get.class);
            if (annotation != null) {
                if (annotation.priority() != -1) {
                    Router.addRoute(annotation.priority(), "GET", annotation.value(), get.getDeclaringClass().getSimpleName() + "." + get.getName(), annotation.params());
                } else {
                    Router.addRoute("GET", annotation.value(), get.getDeclaringClass().getSimpleName() + "." + get.getName(), annotation.params());
                }
            }
        }
        List<Method> posts = Java.findAllAnnotatedMethods(controllerClasses, Post.class);
        for (Method post : posts) {
            Post annotation = post.getAnnotation(Post.class);
            if (annotation != null) {
                if (annotation.priority() != -1) {
                    Router.addRoute(annotation.priority(), "POST", annotation.value(), post.getDeclaringClass().getSimpleName() + "." + post.getName(), annotation.params());
                } else {
                    Router.addRoute("POST", annotation.value(), post.getDeclaringClass().getSimpleName() + "." + post.getName(), annotation.params());
                }

            }
        }
        List<Method> puts = Java.findAllAnnotatedMethods(controllerClasses, Put.class);
        for (Method put : puts) {
            Put annotation = put.getAnnotation(Put.class);
            if (annotation != null) {
                if (annotation.priority() != -1) {
                    Router.addRoute(annotation.priority(), "PUT", annotation.value(), put.getDeclaringClass().getSimpleName() + "." + put.getName(), annotation.params());
                } else {
                    Router.addRoute("PUT", annotation.value(), put.getDeclaringClass().getSimpleName() + "." + put.getName(), annotation.params());
                }
            }
        }

        List<Method> deletes = Java.findAllAnnotatedMethods(controllerClasses, Delete.class);
        for (Method delete : deletes) {
            Delete annotation = delete.getAnnotation(Delete.class);
            if (annotation != null) {
                if (annotation.priority() != -1) {
                    Router.addRoute(annotation.priority(), "DELETE", annotation.value(), delete.getDeclaringClass().getSimpleName() + "." + delete.getName(), annotation.params());
                } else {
                    Router.addRoute("DELETE", annotation.value(), delete.getDeclaringClass().getSimpleName() + "." + delete.getName(), annotation.params());
                }
            }
        }

        List<Method> serveStatics = Java.findAllAnnotatedMethods(controllerClasses, ServeStatic.class);
        for (Method serveStatic : serveStatics) {
            ServeStatic annotation = serveStatic.getAnnotation(ServeStatic.class);
            if (annotation != null) {
                if (annotation.priority() != -1) {
                    Router.addRoute(annotation.priority(), "GET", annotation.value(), "staticDir:" + annotation.directory());
                } else {
                    Router.addRoute("GET", annotation.value(), "staticDir:" + annotation.directory());
                }
            }
        }
    }

    public List<Class> getControllerClasses() {
        List<Class> returnValues = new ArrayList<Class>();
        List<ApplicationClasses.ApplicationClass> classes = Play.classes.all();
        for (ApplicationClasses.ApplicationClass clazz : classes) {
            if (clazz.name.startsWith("controllers.")) {
                returnValues.add(clazz.javaClass);
            }
        }
        return returnValues;
    }

}