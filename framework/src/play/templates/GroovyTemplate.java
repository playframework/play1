package play.templates;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import java.io.File;
import java.io.FileOutputStream;
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
import play.data.binding.Unbinder;
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
import play.utils.Java;
import play.mvc.ActionInvoker;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.utils.HTML;

/**
 * A template
 */
public class GroovyTemplate extends BaseTemplate {

    public GroovyTemplate(String name, String source) {
        super(name, source);
    }

    public GroovyTemplate(String source) {
        super(source);
    }

    public static class TClassLoader extends GroovyClassLoader {

        public TClassLoader() {
            super(Play.classloader);
        }

        public Class defineTemplate(String name, byte[] byteCode) {
            return defineClass(name, byteCode, 0, byteCode.length, Play.classloader.protectionDomain);
        }
    }

    @SuppressWarnings("unchecked")
    void directLoad(byte[] code) throws Exception {
        TClassLoader tClassLoader = new TClassLoader();
        String[] lines = new String(code, "utf-8").split("\n");
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
                compilationUnit.addSource(new SourceUnit(name, compiledSource, compilerConfiguration, tClassLoader, compilationUnit.getErrorCollector()));
                Field phasesF = compilationUnit.getClass().getDeclaredField("phaseOperations");
                phasesF.setAccessible(true);
                LinkedList[] phases = (LinkedList[]) phasesF.get(compilationUnit);
                LinkedList<GroovyClassOperation> output = new LinkedList<GroovyClassOperation>();
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
                if (System.getProperty("precompile") != null) {
                    try {
                        // emit bytecode to standard class layout as well
                        File f = Play.getFile("precompiled/templates/" + name.replaceAll("\\{(.*)\\}", "from_$1").replace(":", "_").replace("..", "parent"));
                        f.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(sb.toString().getBytes("utf-8"));
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Logger.trace("%sms to compile template %s to %d classes", System.currentTimeMillis() - start, name, groovyClassesForThisTemplate.size());

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
            layoutData.set(new HashMap<Object, Object>());
            TagContext.init();
        }
        ExecutableTemplate t = (ExecutableTemplate) InvokerHelper.createScript(compiledTemplate, binding);
        t.template = this;
        Monitor monitor = null;
        try {
            monitor = MonitorFactory.start(name);
            long start = System.currentTimeMillis();
            t.run();
            monitor.stop();
            monitor = null;
            Logger.trace("%sms to render template %s", System.currentTimeMillis() - start, name);
        } catch (NoRouteFoundException e) {
            if (e.isSourceAvailable()) {
                throw e;
            }
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
            if (monitor != null) {
                monitor.stop();
            }
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

    Throwable cleanStackTrace(Throwable e) {
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

    /**
     * Groovy template
     */
    public static abstract class ExecutableTemplate extends Script {

        // Leave this field public to allow custom creation of TemplateExecutionException from different pkg
        public GroovyTemplate template;

        @Override
        public Object getProperty(String property) {
            try {
                if (property.equals("actionBridge")) {
                    return new ActionBridge(this);
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
            BaseTemplate tagTemplate = null;
            try {
                tagTemplate = (BaseTemplate)TemplateLoader.load("tags/" + templateName + "." + callerExtension);
            } catch (TemplateNotFoundException e) {
                try {
                    tagTemplate = (BaseTemplate)TemplateLoader.load("tags/" + templateName + ".tag");
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
            args.put("_attrs", attrs);
            // all other vars are template-specific
            args.put("_caller", getBinding().getVariables());
            if (attrs != null) {
                for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                    args.put("_" + entry.getKey(), entry.getValue());
                }
            }
            args.put("_body", body);
            try {
                tagTemplate.render(args);
            } catch (TagInternalException e) {
                throw new TemplateExecutionException(template, fromLine, e.getMessage(), template.cleanStackTrace(e));
            } catch (TemplateNotFoundException e) {
                throw new TemplateNotFoundException(e.getPath(), template, fromLine);
            }
            TagContext.exitTag();
        }

        public Class _(String className) throws Exception {
            try {
                return Play.classloader.loadClass(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        public String __safe(Object val) {
            if (val instanceof RawData) {
                return ((RawData) val).data;
            }
            if (!template.name.endsWith(".html") || TagContext.hasParentTag("verbatim")) {
                return val.toString();
            }
            return HTML.htmlEscape(val.toString());
        }

        public Object get(String key) {
            return GroovyTemplate.layoutData.get().get(key);
        }

        static class ActionBridge extends GroovyObjectSupport {

            ExecutableTemplate template = null;
            String controller = null;
            boolean absolute = false;

            public ActionBridge(ExecutableTemplate template, String controllerPart, boolean absolute) {
                this.template = template;
                this.controller = controllerPart;
                this.absolute = absolute;
            }

            public ActionBridge(ExecutableTemplate template) {
                this.template = template;
            }

            @Override
            public Object getProperty(String property) {
                return new ActionBridge(template, controller == null ? property : controller + "." + property, absolute);
            }

            public Object _abs() {
                this.absolute = true;
                return this;
            }

            @Override
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
                            // too many parameters versus action, possibly a developer error. we must warn him.
                            if (names.length < ((Object[]) param).length) {
                                throw new NoRouteFoundException(action, null);
                            }
                            for (int i = 0; i < ((Object[]) param).length; i++) {
                                if (((Object[]) param)[i] instanceof Router.ActionDefinition && ((Object[]) param)[i] != null) {
                                    Unbinder.unBind(r, ((Object[]) param)[i].toString(), i < names.length ? names[i] : "");
                                } else if (isSimpleParam(actionMethod.getParameterTypes()[i])) {
                                    if (((Object[]) param)[i] != null) {
                                        Unbinder.unBind(r, ((Object[]) param)[i].toString(), i < names.length ? names[i] : "");
                                    }
                                } else {
                                    Unbinder.unBind(r, ((Object[]) param)[i], i < names.length ? names[i] : "");
                                }
                            }
                        }
                        Router.ActionDefinition def = Router.reverse(action, r);
                        if (absolute) {
                            def.absolute();
                        }
                        if (template.template.name.endsWith(".html") || template.template.name.endsWith(".xml")) {
                            def.url = def.url.replace("&", "&amp;");
                        }
                        return def;
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

    static boolean isSimpleParam(Class type) {
        return Number.class.isAssignableFrom(type) || type.equals(String.class) || type.isPrimitive();
    }
}
