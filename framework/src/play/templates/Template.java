package play.templates;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.syntax.SyntaxException;
import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.SignaturesNamesRepository;
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
import play.mvc.ActionInvoker;
import play.mvc.Router;

public class Template {
    
    public String name;
    public String source;
    public String groovySource;
    public Map<Integer,Integer> linesMatrix = new HashMap<Integer, Integer>();
    public Set<Integer> doBodyLines = new HashSet<Integer>();
    public Class compiledTemplate;
    public Long timestamp = System.currentTimeMillis();
    boolean needJavaRecompilation;
    
    
    public Template(String name, String source) {
        this.name = name;
        this.source = source;
    }
    
    public void compile() {
        if (compiledTemplate == null) {
            CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
            compilerConfiguration.setSourceEncoding("utf-8"); // ouf
            GroovyClassLoader classLoader = new GroovyClassLoader(Play.classloader, compilerConfiguration);
            try {
                long start = System.currentTimeMillis();
                Play.classloader.loadingTracer.set(new ArrayList<ApplicationClass>());
                compiledTemplate = classLoader.parseClass(new ByteArrayInputStream(groovySource.getBytes("utf-8")));
                if(!Play.classloader.loadingTracer.get().isEmpty()) {
                    needJavaRecompilation = true;
                } else {
                    needJavaRecompilation = false;
                }
                Play.classloader.loadingTracer.set(null);
                Logger.trace("%sms to compile template %s", System.currentTimeMillis()-start, name);
            } catch (UnsupportedEncodingException e) {
                throw new UnexpectedException(e);
            } catch (MultipleCompilationErrorsException e) {
                if (e.getErrorCollector().getLastError() != null) {
                    SyntaxErrorMessage errorMessage = (SyntaxErrorMessage) e.getErrorCollector().getLastError();
                    SyntaxException syntaxException = errorMessage.getCause();
                    Integer line = this.linesMatrix.get(syntaxException.getLine());
                    if (line == null) {
                        line = 0;
                    }
                    String message = syntaxException.getMessage();
                    if(message.indexOf("@")>0) {
                        message = message.substring(0, message.lastIndexOf("@"));
                    }
                    throw new TemplateCompilationException(this, line, message);
                }
                throw new UnexpectedException(e);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
        }
    }

    public String render(Map<String, Object> args) {
        compile();
        Binding binding = new Binding(args);
        binding.setVariable("messages", new Messages());
        binding.setVariable("lang", Lang.get());
        StringWriter writer = null;
        Boolean applyLayouts = false;
        if(!args.containsKey("out")) { 
            applyLayouts = true;
            layout.set(null);
            writer = new StringWriter();
            binding.setProperty("out", new PrintWriter(writer));
            currentTemplate.set(this);
        }
        if(!args.containsKey("_body") && !args.containsKey("_isLayout") && !args.containsKey("_isInclude")) {
            layoutData.set(new HashMap());
        }
        ExecutableTemplate t = (ExecutableTemplate) InvokerHelper.createScript(compiledTemplate, binding);        
        t.template = this;
        try {
            long start = System.currentTimeMillis();
            t.run();
            Logger.trace("%sms to render template %s", System.currentTimeMillis()-start, name);
        } catch(NoRouteFoundException e ) {
            throwException(e);
        } catch(PlayException e) {
            throw e;
        } catch(DoBodyException e) {
            Exception ex = (Exception) e.getCause();
            throwException(ex);
        } catch(Exception e) {
            throwException(e);
        }
        if(applyLayouts && layout.get() != null) {
            Map<String,Object> layoutArgs = new HashMap<String,Object>(args);
            layoutArgs.remove("out");
            layoutArgs.put("_isLayout", true);
            String layoutR = layout.get().render(layoutArgs);
            return layoutR.replace("____%LAYOUT%____", writer.toString().trim());
        }
        if(writer != null) {
            return writer.toString();
        }
        return null;
    }
    
    void throwException(Exception e) {
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            if (stackTraceElement.getClassName().equals(compiledTemplate.getName()) || stackTraceElement.getClassName().startsWith(compiledTemplate.getName() + "$_run_closure")) {
                if (doBodyLines.contains(stackTraceElement.getLineNumber())) {
                    throw new DoBodyException(e);
                } else if (e instanceof TagInternalException) {
                    throw (TagInternalException) e;
                } else if (e instanceof NoRouteFoundException) {
                   NoRouteFoundException ex = (NoRouteFoundException)e;
                   throw new NoRouteFoundException(ex.getAction(), ex.getArgs(), this, this.linesMatrix.get(stackTraceElement.getLineNumber()));
                } else if (e instanceof TemplateExecutionException ) {
                   throw (TemplateExecutionException)e;
                } else {
                    throw new TemplateExecutionException(this, this.linesMatrix.get(stackTraceElement.getLineNumber()), e.getMessage(), e);
                }
            }
        }
        throw new RuntimeException(e);
    }
    
    public static ThreadLocal<Template> layout = new ThreadLocal<Template>();
    public static ThreadLocal<Map> layoutData = new ThreadLocal<Map>();
    public static ThreadLocal<Template> currentTemplate = new ThreadLocal<Template>();

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
            if(template.name.indexOf(".")>0) {
                callerExtension = template.name.substring(template.name.lastIndexOf(".")+1);
            }
            Template tagTemplate = null;
            try {
                tagTemplate = TemplateLoader.load("tags/"+templateName+"."+callerExtension);
            } catch(TemplateNotFoundException e) {
                try {
                    tagTemplate = TemplateLoader.load("tags/"+templateName+".tag");
                } catch(TemplateNotFoundException ex) {
                    throw new TemplateNotFoundException("tags/"+templateName+"."+callerExtension+" or tags/"+templateName+".tag", template, fromLine);
                }
            }
            Map<String, Object> args = new HashMap<String, Object>();
            args.putAll(getBinding().getVariables());
            if(attrs != null) {
                for(String key:attrs.keySet()) {
                    args.put("_"+key, attrs.get(key));
                }
            }
            PrintWriter callerOut = (PrintWriter)args.get("out");
            StringWriter writer = new StringWriter();
            args.put("_body", body);
            args.put("out", new PrintWriter(writer));
            try {
                tagTemplate.render(args);  
                String tagResult = writer.toString().trim();
                if(callerOut != null) {
                    callerOut.print(tagResult);
                    args.put("out", callerOut);
                }
            } catch(TagInternalException e) {
                throw new TemplateExecutionException(template, fromLine, e.getMessage(), e);
            } catch(TemplateNotFoundException e) {
                throw new TemplateNotFoundException(e.getPath(), template, fromLine);
            }
                    
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
                String action = controller+"."+name;
                try {
                    Map<String, String> r = new HashMap<String, String>();
                    String[] names = SignaturesNamesRepository.get(ActionInvoker.getActionMethod(action));
                    if(param instanceof Object[]) {
                        for(int i=0; i<((Object[])param).length; i++) {
                            r.put(names[i], ((Object[])param)[i] == null ? null : ((Object[])param)[i].toString());
                        }
                    }
                    return Router.reverse(action, r);
                } catch(ActionNotFoundException e) {
                    throw new NoRouteFoundException(action, null);
                }
            }
        }
    }
    
}
