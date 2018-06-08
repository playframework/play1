package play.deps;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.plugins.repository.TransferListener;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageLogger;

public class HumanReadyLogger implements MessageLogger, TransferListener {

    Set<String> notFound = new HashSet<>();
    Set<String> dynamics = new HashSet<>();
    Set<String> evicteds = new HashSet<>();
    Pattern dep = Pattern.compile("found ([^#]+)#([^;]+);([^\\s]+) in (.*)");
    Pattern depNotFound = Pattern.compile("module not found: ([^#]+)#([^;]+);([^\\s]+)");
    Pattern dynamic = Pattern.compile("\\[(.*)\\] ([^#]+)#([^;]+);([^\\s]+)");
    Pattern evicted = Pattern.compile("([^#]+)#([^;]+);([^\\s]+) by \\[([^#]+)#([^;]+);([^\\s]+)\\].*");
    String downloading = null;
    long length = 0;
    int progress = 0;
    long lastTime = System.currentTimeMillis();
    String[] progressBar = new String[]{".  ", ".. ", "...", "   "};

    public void niceLog(String msg, int level) {
        try {
            if (msg == null) {
                return;
            }

            // Info

            msg = msg.trim();

            if (msg.startsWith("::")) { // Ignore
                return;
            }

            if (msg.startsWith("found ")) { // Dependency found
                if (msg.contains("playCore")) {
                    return;
                }
                Matcher m = dep.matcher(msg);
                if (m.matches()) {
                    System.out.println("~ \t" + m.group(1) + "->" + m.group(2) + " " + m.group(3) + " (from " + m.group(4) + ")");
                    return;
                }
            }

            if (msg.startsWith("module not found")) { // Dependency not found
                Matcher m = depNotFound.matcher(msg);
                if (m.matches()) {
                    notFound.add(m.group(1) + "->" + m.group(2) + " " + m.group(3));
                    return;
                }
            }

            if (msg.startsWith("[")) {
                Matcher m = dynamic.matcher(msg);
                if (m.matches()) {
                    dynamics.add(m.group(2) + "->" + m.group(3) + " " + m.group(4) + " will use version " + m.group(1));
                    return;
                }
            }

            Matcher m = evicted.matcher(msg);
            if (m.matches()) {
                evicteds.add(m.group(2) + " " + m.group(3) + " is overridden by " + m.group(5) + " " + m.group(6));
                return;
            }

            if (msg.startsWith("downloading ")) {
                if (downloading == null) {
                    System.out.println("~");
                    System.out.println("~ Downloading required dependencies,");
                    System.out.println("~");
                }
                downloading = msg;
                progress = 0;
                length = 0;
                lastTime = System.currentTimeMillis();
                System.out.print("~ \t" + downloading);
                return;
            }

            if (msg.startsWith("[SUCCESSFUL")) {
                msg = msg.substring(msg.indexOf("("));
                System.out.println("\r~ \t" + (downloading + "                    ").replace("(jar)", "").replace("downloading", "downloaded").replace("...", "   "));
            }

            //System.out.println(")))) " + msg);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void log(String string, int i) {
        niceLog(string, i);
    }

    @Override
    public void rawlog(String string, int i) {
        niceLog(string, i);
    }

    @Override
    public void debug(String string) {
    }

    @Override
    public void verbose(String string) {
    }

    @Override
    public void deprecated(String string) {
    }

    @Override
    public void info(String string) {
        niceLog(string, Message.MSG_INFO);
    }

    @Override
    public void rawinfo(String string) {
        niceLog(string, Message.MSG_INFO);
    }

    @Override
    public void warn(String string) {
        niceLog(string, Message.MSG_WARN);
    }

    @Override
    public void error(String string) {
        niceLog(string, Message.MSG_ERR);
    }

    @Override
    public List<String> getProblems() {
        return null;
    }

    @Override
    public List<String> getWarns() {
        return null;
    }

    @Override
    public List<String> getErrors() {
        return null;
    }

    @Override
    public void clearProblems() {
    }

    @Override
    public void sumupProblems() {
    }

    @Override
    public void progress() {
    }

    @Override
    public void endProgress() {
    }

    @Override
    public void endProgress(String string) {
    }

    @Override
    public boolean isShowProgress() {
        return false;
    }

    @Override
    public void setShowProgress(boolean bln) {
    }

    @Override
    public void transferProgress(TransferEvent te) {
        //System.out.println(te.getResource().getContentLength());
        if (downloading != null) {
            length += te.getLength();
            System.out.print("\r~ \t" + downloading.replace("...", "") + progressBar[progress] + " " + String.format("%-20s", FileUtils.byteCountToDisplaySize(length) + (te.isTotalLengthSet() ? "/" + FileUtils.byteCountToDisplaySize(te.getTotalLength()): "")) + "\r");
            if (System.currentTimeMillis() - lastTime > 500) {
                lastTime = System.currentTimeMillis();
                progress++;
                if (progress > progressBar.length - 1) {
                    progress = 0;
                }
            }
        }
    }
}
