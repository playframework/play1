package play;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.jamonapi.MonitorFactory;
import com.jamonapi.utils.Misc;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import play.Play.Mode;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.ContinuationEnhancer;
import play.classloading.enhancers.ControllersEnhancer;
import play.classloading.enhancers.Enhancer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer;
import play.classloading.enhancers.MailerEnhancer;
import play.classloading.enhancers.PropertiesEnhancer;
import play.classloading.enhancers.SigEnhancer;
import play.exceptions.UnexpectedException;
import play.libs.Crypto;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * Plugin used for core tasks
 */
public class CorePlugin extends PlayPlugin {

    /**
     * Get the application status
     */
    public static String computeApplicationStatus(boolean json) {
        if (json) {
            JsonObject o = new JsonObject();
            for (PlayPlugin plugin : Play.pluginCollection.getEnabledPlugins()) {
                try {
                    JsonObject status = plugin.getJsonStatus();
                    if (status != null) {
                        o.add(plugin.getClass().getName(), status);
                    }
                } catch (Throwable e) {
                    JsonObject error = new JsonObject();
                    error.add("error", new JsonPrimitive(e.getMessage()));
                    o.add(plugin.getClass().getName(), error);
                }
            }
            return o.toString();
        }
        StringBuilder dump = new StringBuilder(16);
        for (PlayPlugin plugin : Play.pluginCollection.getEnabledPlugins()) {
            try {
                String status = plugin.getStatus();
                if (status != null) {
                    dump.append(status);
                    dump.append("\n");
                }
            } catch (Throwable e) {
                dump.append(plugin.getClass().getName()).append(".getStatus() has failed (").append(e.getMessage()).append(")");
            }
        }
        return dump.toString();
    }

    /**
     * Intercept /@status and check that the Authorization header is valid. 
     * Then ask each plugin for a status dump and send it over the HTTP response.
     *
     * You can ask the /@status using the authorization header and putting your status secret key in it.
     * Prior to that you would be required to start play with  a -DstatusKey=yourkey
     */
    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {
        if (Play.mode == Mode.DEV && request.path.equals("/@kill")) {
            System.out.println("@KILLED");
            if (Play.standalonePlayServer) {
                System.exit(0);
            } else {
                Logger.error("Cannot execute @kill since Play is not running as standalone server");
            }
        }
        if (request.path.equals("/@status") || request.path.equals("/@status.json")) {
            if(!Play.started) {
                response.print("Application is not started");
                response.status = 503;
                return true;
            }
            response.contentType = request.path.contains(".json") ? "application/json" : "text/plain";
            Header authorization = request.headers.get("authorization");
            if (authorization != null && (Crypto.sign("@status").equals(authorization.value()) || System.getProperty("statusKey", Play.secretKey).equals(authorization.value()))) {
                response.print(computeApplicationStatus(request.path.contains(".json")));
                response.status = 200;
                return true;
            }
            response.status = 401;
            if (response.contentType.equals("application/json")) {
                response.print("{\"error\": \"Not authorized\"}");
            } else {
                response.print("Not authorized");
            }
            return true;
        }
        return super.rawInvocation(request, response);
    }

    /**
     * Retrieve status about play core.
     */
    @Override
    public String getStatus() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println("Java:");
        out.println("~~~~~");
        out.println("Version: " + System.getProperty("java.version"));
        out.println("Home: " + System.getProperty("java.home"));
        out.println("Max memory: " + Runtime.getRuntime().maxMemory());
        out.println("Free memory: " + Runtime.getRuntime().freeMemory());
        out.println("Total memory: " + Runtime.getRuntime().totalMemory());
        out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        out.println();
        out.println("Play framework:");
        out.println("~~~~~~~~~~~~~~~");
        out.println("Version: " + Play.version);
        out.println("Path: " + Play.frameworkPath);
        out.println("ID: " + (StringUtils.isEmpty(Play.id) ? "(not set)" : Play.id));
        out.println("Mode: " + Play.mode);
        out.println("Tmp dir: " + (Play.tmpDir == null ? "(no tmp dir)" : Play.tmpDir));
        out.println();
        out.println("Application:");
        out.println("~~~~~~~~~~~~");
        out.println("Path: " + Play.applicationPath);
        out.println("Name: " + Play.configuration.getProperty("application.name", "(not set)"));
        out.println("Started at: " + (Play.started ? new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date(Play.startedAt)) : "Not yet started"));
        out.println();
        out.println("Loaded modules:");
        out.println("~~~~~~~~~~~~~~");
        for (String module : Play.modules.keySet()) {
            out.println(module + " at " + Play.modules.get(module).getRealFile());
        }
        out.println();
        out.println("Loaded plugins:");
        out.println("~~~~~~~~~~~~~~");
        for (PlayPlugin plugin : Play.pluginCollection.getAllPlugins()) {
            out.println(plugin.index + ":" + plugin.getClass().getName() + " [" + (Play.pluginCollection.isEnabled(plugin) ? "enabled" : "disabled") + "]");
        }
        out.println();
        out.println("Threads:");
        out.println("~~~~~~~~");
        try {
            visit(out, getRootThread(), 0);
        } catch (Throwable e) {
            out.println("Oops; " + e.getMessage());
        }
        out.println();
        out.println("Requests execution pool:");
        out.println("~~~~~~~~~~~~~~~~~~~~~~~~");
        out.println("Pool size: " + Invoker.executor.getPoolSize());
        out.println("Active count: " + Invoker.executor.getActiveCount());
        out.println("Scheduled task count: " + Invoker.executor.getTaskCount());
        out.println("Queue size: " + Invoker.executor.getQueue().size());
        out.println();
        try {
            out.println("Monitors:");
            out.println("~~~~~~~~");
            Object[][] data = Misc.sort(MonitorFactory.getRootMonitor().getBasicData(), 3, "desc");
            int lm = 10;
            for (Object[] row : data) {
                if (row[0].toString().length() > lm) {
                    lm = row[0].toString().length();
                }
            }
            for (Object[] row : data) {
                if (((Double) row[1]) > 0) {
                    out.println(String.format("%-" + (lm) + "s -> %8.0f hits; %8.1f avg; %8.1f min; %8.1f max;", row[0], row[1], row[2], row[6], row[7]));
                }
            }
        } catch (Exception e) {
            out.println("No monitors found");
        }
        return sw.toString();
    }

    @Override
    public JsonObject getJsonStatus() {
        JsonObject status = new JsonObject();

        {
            JsonObject java = new JsonObject();
            java.addProperty("version", System.getProperty("java.version"));
            status.add("java", java);
        }

        {
            JsonObject memory = new JsonObject();
            memory.addProperty("max", Runtime.getRuntime().maxMemory());
            memory.addProperty("free", Runtime.getRuntime().freeMemory());
            memory.addProperty("total", Runtime.getRuntime().totalMemory());
            status.add("memory", memory);
        }

        {
            JsonObject application = new JsonObject();
            application.addProperty("uptime", Play.started ? System.currentTimeMillis() - Play.startedAt : -1);
            application.addProperty("path", Play.applicationPath.getAbsolutePath());
            status.add("application", application);
        }

        {
            JsonObject pool = new JsonObject();
            pool.addProperty("size", Invoker.executor.getPoolSize());
            pool.addProperty("active", Invoker.executor.getActiveCount());
            pool.addProperty("scheduled", Invoker.executor.getTaskCount());
            pool.addProperty("queue", Invoker.executor.getQueue().size());
            status.add("pool", pool);
        }

        {
            JsonArray monitors = new JsonArray();
            try {
                Object[][] data = Misc.sort(MonitorFactory.getRootMonitor().getBasicData(), 3, "desc");
                for (Object[] row : data) {
                    if (((Double) row[1]) > 0) {
                        JsonObject o = new JsonObject();
                        o.addProperty("name", row[0].toString());
                        o.addProperty("hits", (Double) row[1]);
                        o.addProperty("avg", (Double) row[2]);
                        o.addProperty("min", (Double) row[6]);
                        o.addProperty("max", (Double) row[7]);
                        monitors.add(o);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            status.add("monitors", monitors);
        }

        return status;
    }

    /**
     * Recursively visit all JVM threads
     */
    static void visit(PrintWriter out, ThreadGroup group, int level) {
        // Get threads in `group'
        int numThreads = group.activeCount();
        Thread[] threads = new Thread[numThreads * 2];
        numThreads = group.enumerate(threads, false);

        // Enumerate each thread in `group'
        for (int i = 0; i < numThreads; i++) {
            // Get thread
            Thread thread = threads[i];
            out.println(thread + " " + thread.getState());
        }

        // Get thread subgroups of `group'
        int numGroups = group.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
        numGroups = group.enumerate(groups, false);

        // Recursively visit each subgroup
        for (int i = 0; i < numGroups; i++) {
            visit(out, groups[i], level + 1);
        }
    }

    /**
     * Retrieve the JVM root thread group.
     */
    static ThreadGroup getRootThread() {
        ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        Class<?>[] enhancers = new Class[]{
            ContinuationEnhancer.class,
            SigEnhancer.class,
            ControllersEnhancer.class,
            MailerEnhancer.class,
            PropertiesEnhancer.class,
            LocalvariablesNamesEnhancer.class
        };
        for (Class<?> enhancer : enhancers) {
            try {
                long start = System.currentTimeMillis();
                ((Enhancer) enhancer.newInstance()).enhanceThisClass(applicationClass);
                if (Logger.isTraceEnabled()) {
                    Logger.trace("%sms to apply %s to %s", System.currentTimeMillis() - start, enhancer.getSimpleName(), applicationClass.name);
                }
            } catch (Exception e) {
                throw new UnexpectedException("While applying " + enhancer + " on " + applicationClass.name, e);
            }
        }
    }
}
