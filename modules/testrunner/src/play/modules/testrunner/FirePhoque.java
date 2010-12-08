package play.modules.testrunner;

import com.gargoylesoftware.htmlunit.AlertHandler;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ConfirmHandler;
import com.gargoylesoftware.htmlunit.DefaultPageCreator;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.PromptHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.javascript.host.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;

public class FirePhoque {

    public static void main(String[] args) throws Exception {

        Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        String app = System.getProperty("application.url", "http://localhost:9000");

        // Tests description
        File root = null;
        String selenium = null;
        List<String> tests = null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new URL(app + "/@tests.list").openStream(), "utf-8"));
            String marker = in.readLine();
            if (!marker.equals("---")) {
                throw new RuntimeException("Oops");
            }
            root = new File(in.readLine());
            selenium = in.readLine();
            tests = new ArrayList<String>();
            String line;
            while ((line = in.readLine()) != null) {
                tests.add(line);
            }
            in.close();
        } catch(Exception e) {
            System.out.println("~ The application does not start. There are compilation errors.");
            System.exit(-1);
        }

        // Let's tweak WebClient
        WebClient firephoque = new WebClient(BrowserVersion.INTERNET_EXPLORER_8);
        firephoque.setPageCreator(new DefaultPageCreator() {

            @Override
            public Page createPage(WebResponse wr, WebWindow ww) throws IOException {
                Page page = createHtmlPage(wr, ww);
                return page;
            }
        });
        firephoque.setThrowExceptionOnFailingStatusCode(false);
        firephoque.setAlertHandler(new AlertHandler() {
            public void handleAlert(Page page, String string) {
                try {
                    Window window = (Window)page.getEnclosingWindow().getScriptObject();
                    window.custom_eval(
                        "parent.selenium.browserbot.recordedAlerts.push('" + string.replace("'", "\\'")+ "');"
                    );
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
        firephoque.setConfirmHandler(new ConfirmHandler() {
            public boolean handleConfirm(Page page, String string) {
                try {
                    Window window = (Window)page.getEnclosingWindow().getScriptObject();
                    Object result = window.custom_eval(
                        "parent.selenium.browserbot.recordedConfirmations.push('" + string.replace("'", "\\'")+ "');" +
                        "var result = parent.selenium.browserbot.nextConfirmResult;" +
                        "parent.selenium.browserbot.nextConfirmResult = true;" +
                        "result"
                    );
                    return (Boolean)result;
                } catch(Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
        firephoque.setPromptHandler(new PromptHandler() {
            public String handlePrompt(Page page, String string) {
                try {
                    Window window = (Window)page.getEnclosingWindow().getScriptObject();
                    Object result = window.custom_eval(
                        "parent.selenium.browserbot.recordedPrompts.push('" + string.replace("'", "\\'")+ "');" +
                        "var result = !parent.selenium.browserbot.nextConfirmResult ? null : parent.selenium.browserbot.nextPromptResult;" +
                        "parent.selenium.browserbot.nextConfirmResult = true;" +
                        "parent.selenium.browserbot.nextPromptResult = '';" +
                        "result"
                    );
                    return (String)result;
                } catch(Exception e) {
                    e.printStackTrace();
                    return "";
                }
            }
        });
        firephoque.setThrowExceptionOnScriptError(false);


        // Go!
        int maxLength = 0;
        for (String test : tests) {
            String testName = test.replace(".class", "").replace(".test.html", "").replace(".", "/").replace("$", "/");
            if (testName.length() > maxLength) {
                maxLength = testName.length();
            }
        }
        System.out.println("~ " + tests.size() + " test" + (tests.size() != 1 ? "s" : "") + " to run:");
        System.out.println("~");
        firephoque.openWindow(new URL(app + "/@tests/init"), "headless");
        boolean ok = true;
        for (String test : tests) {
            long start = System.currentTimeMillis();
            String testName = test.replace(".class", "").replace(".test.html", "").replace(".", "/").replace("$", "/");
            System.out.print("~ " + testName + "... ");
            for (int i = 0; i < maxLength - testName.length(); i++) {
                System.out.print(" ");
            }
            System.out.print("    ");
            URL url;
            if (test.endsWith(".class")) {
                url = new URL(app + "/@tests/" + test);
            } else {
                url = new URL(app + "" + selenium + "?baseUrl=" + app + "&test=/@tests/" + test + ".suite&auto=true&resultsUrl=/@tests/" + test);
            }
            firephoque.openWindow(url, "headless");
            firephoque.waitForBackgroundJavaScript(5 * 60 * 1000);
            if (new File(root, test.replace("/", ".") + ".passed.html").exists()) {
                System.out.print("PASSED     ");
            } else if (new File(root, test.replace("/", ".") + ".failed.html").exists()) {
                System.out.print("FAILED  !  ");
                ok = false;
            } else {
                System.out.print("ERROR   ?  ");
                ok = false;
            }

            //
            int duration = (int) (System.currentTimeMillis() - start);
            int seconds = (duration / 1000) % 60;
            int minutes = (duration / (1000 * 60)) % 60;

            if (minutes > 0) {
                System.out.println(minutes + " min " + seconds + "s");
            } else {
                System.out.println(seconds + "s");
            }
        }
        firephoque.openWindow(new URL(app + "/@tests/end?result=" + (ok ? "passed" : "failed")), "headless");

    }
}
