package play.templates;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import play.Logger;
import play.Play;
import play.classloading.BytecodeCache;
import play.exceptions.JavaExecutionException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.TagInternalException;
import play.exceptions.TemplateExecutionException;
import play.exceptions.TemplateExecutionException.DoBodyException;
import play.libs.Codec;
import play.libs.IO;

public abstract class BaseTemplate extends Template {

    public String compiledSource;
    public Map<Integer, Integer> linesMatrix = new HashMap<>();
    public Set<Integer> doBodyLines = new HashSet<>();
    public Class compiledTemplate;
    public String compiledTemplateName;
    public Long timestamp = System.currentTimeMillis();

    public BaseTemplate(String name, String source) {
        this.name = name;
        this.source = source;
    }

    public BaseTemplate(String source) {
        this.name = Codec.UUID();
        this.source = source;
    }

    public void loadPrecompiled() {
        try {
            File file = Play.getFile("precompiled/templates/" + name);
            byte[] code = IO.readContent(file);
            directLoad(code);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load precompiled template " + name, e);
        }
    }

    public boolean loadFromCache() {
        try {
            long start = System.currentTimeMillis();
            byte[] bc = BytecodeCache.getBytecode(name, source);
            if (bc != null) {
                directLoad(bc);
                if (Logger.isTraceEnabled()) {
                    Logger.trace("%sms to load template %s from cache", System.currentTimeMillis() - start, name);
                }
                return true;
            }
        } catch (Exception e) {
            Logger.warn(e, "Cannot load %s from cache", name);
        }
        return false;
    }

    abstract void directLoad(byte[] code) throws Exception;

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
            if (stackTraceElement.getLineNumber() > 0 && Play.classes.hasClass(stackTraceElement.getClassName())) {
                throw new JavaExecutionException(Play.classes.getApplicationClass(stackTraceElement.getClassName()), stackTraceElement.getLineNumber(), cleanStackTrace(e));
            }
        }
        throw new RuntimeException(e);
    }

    protected abstract Throwable cleanStackTrace(Throwable e);
    public static final ThreadLocal<BaseTemplate> layout = new ThreadLocal<>();
    public static final ThreadLocal<Map<Object, Object>> layoutData = new ThreadLocal<>();
    public static final ThreadLocal<BaseTemplate> currentTemplate = new ThreadLocal<>();

    public static final class RawData {

        public String data;

        public RawData(Object val) {
            if (val == null) {
                data = "";
            } else {
                data = val.toString();
            }
        }

        @Override
        public String toString() {
            return data;
        }
    }
}
