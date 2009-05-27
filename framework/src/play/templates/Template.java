package play.templates;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilationUnit.GroovyClassOperation;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.tools.GroovyClass;
import play.Logger;
import play.Play;
import play.Play.Mode;
import play.classloading.BytecodeCache;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.exceptions.ActionNotFoundException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.TagInternalException;
import play.exceptions.TemplateCompilationException;
import play.exceptions.TemplateExecutionException;
import play.exceptions.TemplateExecutionException.DoBodyException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.Codec;
import play.libs.Java;
import play.mvc.ActionInvoker;
import play.mvc.Http.Request;
import play.mvc.Router;

/**
 * A template
 */
public class Template {

    public String name;
    public String source;
    public String groovySource;
    public Map<Integer, Integer> linesMatrix = new HashMap<Integer, Integer>();
    public Set<Integer> doBodyLines = new HashSet<Integer>();
    public Class compiledTemplate;
    public String compiledTemplateName;
    public Long timestamp = System.currentTimeMillis();

    public Template(String name, String source) {
        this.name = name;
        this.source = source;
    }

    public static class TClassLoader extends GroovyClassLoader {

        public TClassLoader() {
            super(Play.classloader);
        }

        public Class defineTemplate(String name, byte[] byteCode) {
            return defineClass(name, byteCode, 0, byteCode.length, Play.classloader.protectionDomain);
        }
    }

    public boolean loadFromCache() {
        try {
            long start = System.currentTimeMillis();
            TClassLoader tClassLoader = new TClassLoader();
            byte[] bc = BytecodeCache.getBytecode(name, source);
            if (bc != null) {

                String[] lines = new String(bc, "utf-8").split("\n");
                this.linesMatrix = (HashMap<Integer, Integer>) Java.deserialize(Codec.decodeBASE64(lines[1]));
                this.doBodyLines = (HashSet<Integer>) Java.deserialize(Codec.decodeBASE64(lines[3]));
                for (int i = 4; i < lines.length; i = i + 2) {
                    String className = lines[i];
                    byte[] byteCode = Codec.decodeBASE64(lines[i + 1]);
                    Class c = tClassLoader.defineTemplate(className, byteCode);
                    if (compiledTemplate == null) {
                        compiledTemplate = c;
                    }
                }
                Logger.trace("%sms to load template %s from cache", System.currentTimeMillis() - start, name);
                return true;
            }
        } catch(Exception e) {
            Logger.warn(e, "Cannot load %s from cache", name);
        }
        return false;
    }

    public void compile() {
        if (compiledTemplate == null) {
            try {
                long start = System.currentTimeMillis();

                TClassLoader tClassLoader = new TClassLoader();

                // Let's compile the groovy source
                final List<GroovyClass> groovyClassesForThisTemplate = new ArrayList<GroovyClass>();
                // ~~~ Please !
                CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
                compilerConfiguration.setSourceEncoding("utf-8"); // ouf                        
                CompilationUnit compilationUnit = new CompilationUnit(compilerConfiguration);
                compilationUnit.addSource(new SourceUnit(name, groovySource, compilerConfiguration, tClassLoader, compilationUnit.getErrorCollector()));
                Field phasesF = compilationUnit.getClass().getDeclaredField("phaseOperations");
                phasesF.setAccessible(true);
                LinkedList[] phases = (LinkedList[]) phasesF.get(compilationUnit);
                LinkedList output = new LinkedList();
                phases[Phases.OUTPUT] = output;
                output.add(new GroovyClassOperation() {

                    public void call(GroovyClass gclass) {
                        groovyClassesForThisTemplate.add(gclass);
                    }
                });
                compilationUnit.compile();
                // ouf 

                // Define script classes
                StringBuilder sb = new StringBuilder();
                sb.append("LINESMATRIX" + "\n");
                sb.append(Codec.encodeBASE64(Java.serialize(linesMatrix)).replaceAll("\\s", ""));
                sb.append("\n");
                sb.append("DOBODYLINES" + "\n");
                sb.append(Codec.encodeBASE64(Java.serialize(doBodyLines)).replaceAll("\\s", ""));
                sb.append("\n");
                for (GroovyClass gclass : groovyClassesForThisTemplate) {
                    tClassLoader.defineTemplate(gclass.getName(), gclass.getBytes());
                    sb.append(gclass.getName() + "\n");
                    sb.append(Codec.encodeBASE64(gclass.getBytes()).replaceAll("\\s", ""));
                    sb.append("\n");
                }
                // Cache
                BytecodeCache.cacheBytecode(sb.toString().getBytes("utf-8"), name, source);
                compiledTemplate = tClassLoader.loadClass(groovyClassesForThisTemplate.get(0).getName());

                Logger.trace("%sms to compile template %s", System.currentTimeMillis() - start, name);

            } catch (MultipleCompilationErrorsException e) {
                if (e.getErrorCollector().getLastError() != null) {
                    SyntaxErrorMessage errorMessage = (SyntaxErrorMessage) e.getErrorCollector().getLastError();
                    SyntaxException syntaxException = errorMessage.getCause();
                    Integer line = this.linesMatrix.get(syntaxException.getLine());
                    if (line == null) {
                        line = 0;
                    }
                    String message = syntaxException.getMessage();
                    if (message.indexOf("@") > 0) {
                        message = message.substring(0, message.lastIndexOf("@"));
                    }
                    throw new TemplateCompilationException(this, line, message);
                }
                throw new UnexpectedException(e);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
        }
        compiledTemplateName = compiledTemplate.getName();
    }

    public String render(Map<String, Object> args) {
        compile();
        Binding binding = new Binding(args);
        binding.setVariable("play", new Play());
        binding.setVariable("messages", new Messages());
        binding.setVariable("lang", Lang.get());
        StringWriter writer = null;
        Boolean applyLayouts = false;
        if (!args.containsKey("out")) {
            applyLayouts = true;
            layout.set(null);
            writer = new StringWriter();
            binding.setProperty("out", new PrintWriter(writer));
            currentTemplate.set(this);
        }
        if (!args.containsKey("_body") && !args.containsKey("_isLayout") && !args.containsKey("_isInclude")) {
            layoutData.set(new HashMap());
            TagContext.init();
        }
        ExecutableTemplate t = (ExecutableTemplate) InvokerHelper.createScript(compiledTemplate, binding);
        t.template = this;
        try {
            long start = System.currentTimeMillis();
            t.run();
            Logger.trace("%sms to render template %s", System.currentTimeMillis() - start, name);
        } catch (NoRouteFoundException e) {
            throwException(e);
        } catch (PlayException e) {
            throw (PlayException) cleanStackTrace(e);
        } catch (DoBodyException e) {
            if (Play.mode == Mode.DEV) {
                compiledTemplate = null;
                BytecodeCache.deleteBytecode(name);
            }
            Exception ex = (Exception) e.getCause();
            throwException(ex);
        } catch (Throwable e) {
            if (Play.mode == Mode.DEV) {
                compiledTemplate = null;
                BytecodeCache.deleteBytecode(name);
            }
            throwException(e);
        } finally {
        }
        if (applyLayouts && layout.get() != null) {
            Map<String, Object> layoutArgs = new HashMap<String, Object>(args);
            layoutArgs.remove("out");
            layoutArgs.put("_isLayout", true);
            String layoutR = layout.get().render(layoutArgs);
            return layoutR.replace("____%LAYOUT%____", writer.toString().trim());
        }
        if (writer != null) {
            return writer.toString();
        }
        return null;
    }

    void throwException(Throwable e) {
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            if (stackTraceElement.getClassName().equals(compiledTemplateName) || stackTraceElement.getClassName().startsWith(compiledTemplateName + "$_run_closure")) {
                if (doBodyLines.contains(stackTraceElement.getLineNumber())) {
                    throw new DoBodyException(e);
                } else if (e instanceof TagInternalException) {
                    throw (TagInternalException) cleanStackTrace(e);
                } else if (e instanceof NoRouteFoundException) {
                    NoRouteFoundException ex = (NoRouteFoundException) cleanStackTrace(e);
                    if (ex.getFile() != null) {
                        throw new NoRouteFoundException(ex.getFile(), this, this.linesMatrix.get(stackTraceElement.getLineNumber()));
                    }
                    throw new NoRouteFoundException(ex.getAction(), ex.getArgs(), this, this.linesMatrix.get(stackTraceElement.getLineNumber()));
                } else if (e instanceof TemplateExecutionException) {
                    throw (TemplateExecutionException) cleanStackTrace(e);
                } else {
                    throw new TemplateExecutionException(this, this.linesMatrix.get(stackTraceElement.getLineNumber()), e.getMessage(), cleanStackTrace(e));
                }
            }
        }
        throw new RuntimeException(e);
    }

    static Throwable cleanStackTrace(Throwable e) {
        List<StackTraceElement> cleanTrace = new ArrayList<StackTraceElement>();
        for (StackTraceElement se : e.getStackTrace()) {
            if (se.getClassName().startsWith("Template_")) {
                String tn = se.getClassName().substring(9);
                if (tn.indexOf("$") > -1) {
                    tn = tn.substring(0, tn.indexOf("$"));
                }
                Integer line = TemplateLoader.templates.get(tn).linesMatrix.get(se.getLineNumber());
                if (line != null) {
                    String ext = "";
                    if (tn.indexOf(".") > -1) {
                        ext = tn.substring(tn.indexOf(".") + 1);
                        tn = tn.substring(0, tn.indexOf("."));
                    }
                    StackTraceElement nse = new StackTraceElement(TemplateLoader.templates.get(tn).name, ext, "line", line);
                    cleanTrace.add(nse);
                }
            }
            if (!se.getClassName().startsWith("org.codehaus.groovy.") && !se.getClassName().startsWith("groovy.") && !se.getClassName().startsWith("sun.reflect.") && !se.getClassName().startsWith("java.lang.reflect.") && !se.getClassName().startsWith("Template_")) {
                cleanTrace.add(se);
            }
        }
        e.setStackTrace(cleanTrace.toArray(new StackTraceElement[cleanTrace.size()]));
        return e;
    }
    public static ThreadLocal<Template> layout = new ThreadLocal<Template>();
    public static ThreadLocal<Map> layoutData = new ThreadLocal<Map>();
    public static ThreadLocal<Template> currentTemplate = new ThreadLocal<Template>();

    /**
     * Groovy template
     */
    public static abstract class ExecutableTemplate extends Script {

        Template template;

        @Override
        public Object getProperty(String property) {
            try {
                if (property.equals("actionBridge")) {
                    return new ActionBridge();
                }
                return super.getProperty(property);
            } catch (MissingPropertyException mpe) {
                return null;
            }
        }

        public void invokeTag(Integer fromLine, String tag, Map<String, Object> attrs, Closure body) {
            String templateName = tag.replace(".", "/");
            String callerExtension = "tag";
            if (template.name.indexOf(".") > 0) {
                callerExtension = template.name.substring(template.name.lastIndexOf(".") + 1);
            }
            Template tagTemplate = null;
            try {
                tagTemplate = TemplateLoader.load("tags/" + templateName + "." + callerExtension);
            } catch (TemplateNotFoundException e) {
                try {
                    tagTemplate = TemplateLoader.load("tags/" + templateName + ".tag");
                } catch (TemplateNotFoundException ex) {
                    if (callerExtension.equals("tag")) {
                        throw new TemplateNotFoundException("tags/" + templateName + ".tag", template, fromLine);
                    }
                    throw new TemplateNotFoundException("tags/" + templateName + "." + callerExtension + " or tags/" + templateName + ".tag", template, fromLine);
                }
            }
            TagContext.enterTag(tag);
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("session", getBinding().getVariables().get("session"));
            args.put("flash", getBinding().getVariables().get("flash"));
            args.put("request", getBinding().getVariables().get("request"));
            args.put("params", getBinding().getVariables().get("params"));
            args.put("play", getBinding().getVariables().get("play"));
            args.put("lang", getBinding().getVariables().get("lang"));
            args.put("messages", getBinding().getVariables().get("messages"));
            args.put("out", getBinding().getVariable("out"));
            // all other vars are template-specific
            args.put("_caller", getBinding().getVariables());
            if (attrs != null) {
                for (String key : attrs.keySet()) {
                    args.put("_" + key, attrs.get(key));
                }
            }
            args.put("_body", body);
            try {
                tagTemplate.render(args);
            } catch (TagInternalException e) {
                throw new TemplateExecutionException(template, fromLine, e.getMessage(), cleanStackTrace(e));
            } catch (TemplateNotFoundException e) {
                throw new TemplateNotFoundException(e.getPath(), template, fromLine);
            }
            TagContext.exitTag();
        }

        public Class _(String className) throws Exception {
            return Play.classloader.loadClass(className);
        }

        static class ActionBridge extends GroovyObjectSupport {

            String controller = null;

            public ActionBridge(String controllerPart) {
                this.controller = controllerPart;
            }

            public ActionBridge() {
            }

            @Override
            public Object getProperty(String property) {
                return new ActionBridge(controller == null ? property : controller + "." + property);
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object invokeMethod(String name, Object param) {
                try {
                    if (controller == null) {
                        controller = Request.current().controller;
                    }
                    String action = controller + "." + name;
                    if (action.endsWith(".call")) {
                        action = action.substring(0, action.length() - 5);
                    }
                    try {
                        Map<String, Object> r = new HashMap<String, Object>();
                        Method actionMethod = (Method) ActionInvoker.getActionMethod(action)[1];
                        String[] names = (String[]) actionMethod.getDeclaringClass().getDeclaredField("$" + actionMethod.getName() + LocalVariablesNamesTracer.computeMethodHash(actionMethod.getParameterTypes())).get(null);
                        if (param instanceof Object[]) {
                            for (int i = 0; i < ((Object[]) param).length; i++) {
                                r.put(names[i], ((Object[]) param)[i] == null ? null : ((Object[]) param)[i].toString());
                            }
                        }
                        return Router.reverse(action, r);
                    } catch (ActionNotFoundException e) {
                        throw new NoRouteFoundException(action, null);
                    }
                } catch (Exception e) {
                    if (e instanceof PlayException) {
                        throw (PlayException) e;
                    }
                    throw new UnexpectedException(e);
                }
            }
        }
    }
}
