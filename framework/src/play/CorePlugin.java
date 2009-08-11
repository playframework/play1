package play;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import com.jamonapi.utils.Misc;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import play.libs.Crypto;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class CorePlugin extends PlayPlugin {

    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {
        if (request.path.equals("/@status")) {
            if(!Play.started) {
                response.status = 503;
                response.print("Not started");
                return true;
            }
            response.contentType = "text/plain";
            Header authorization = request.headers.get("authorization");
            if(authorization != null && Crypto.sign("@status").equals(authorization.value())) {
                StringBuffer dump = new StringBuffer();
                for (PlayPlugin plugin : Play.plugins) {
                    try {
                        String status = plugin.getStatus();
                        if (status != null) {
                            dump.append(status);
                            dump.append("\n");
                        }
                    } catch (Throwable e) {
                        dump.append(plugin.getClass().getName() + ".getStatus() has failed (" + e.getMessage() + ")");
                    }
                }                
                response.status = 200;
                response.print(dump);
                return true;
            } else {
                response.status = 401;
                response.print("Not authorized");
                return true;
            }            
        }
        return super.rawInvocation(request, response);
    }

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
        out.println("ID: " + (Play.id == null || Play.id.equals("") ? "(not set)" : Play.id));
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
        for (PlayPlugin plugin : Play.plugins) {
            out.println(plugin.index + ":" + plugin.getClass().getName());
        }
        out.println();
        out.println("Configuration:");
        out.println("~~~~~~~~~~~~");
        for(Object key : Play.configuration.keySet()) {
            out.println(key+"="+Play.configuration.getProperty(key.toString()));
        }
        out.println();
        out.println("Threads:");
        out.println("~~~~~~~~");
        visit(out, getRootThread(), 0);
        out.println();
        out.println("Requests execution pool:");
        out.println("~~~~~~~~~~~~~~~~~~~~~~~~");
        out.println("Pool size: " + Invoker.executor.getPoolSize());
        out.println("Active count: " + Invoker.executor.getActiveCount());
        out.println("Scheduled task count: " + Invoker.executor.getTaskCount());
        out.println("Queue size: " + Invoker.executor.getQueue().size());
        out.println();
        out.println("Monitors:");
        out.println("~~~~~~~~");
        Object[][] data = Misc.sort(MonitorFactory.getRootMonitor().getBasicData(), 3, "desc");
        int lm = 10;
        for(Object[] row : data) {
            if(row[0].toString().length() > lm) {
                lm = row[0].toString().length();
            }
        }
        for(Object[] row : data) {
            out.println(String.format("%-"+(lm)+"s -> %8.0f hits; %8.1f avg; %8.1f min; %8.1f max;", row[0], row[1], row[2], row[6], row[7]));
        }
        return sw.toString();
    }

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

    static ThreadGroup getRootThread() {
        ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }
}
