package play.modules.testrunner;

import org.htmlunit.AlertHandler;

import org.htmlunit.ConfirmHandler;
import org.htmlunit.DefaultCssErrorHandler;
import org.htmlunit.DefaultPageCreator;
import org.htmlunit.Page;
import org.htmlunit.PromptHandler;
import org.htmlunit.WebClient;
import org.htmlunit.WebResponse;
import org.htmlunit.WebWindow;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


import org.htmlunit.corejs.javascript.Context;
import org.htmlunit.corejs.javascript.ScriptRuntime;
import org.htmlunit.corejs.javascript.ScriptableObject;
import org.htmlunit.BrowserVersion;

import static org.apache.commons.io.IOUtils.closeQuietly;

public class FirePhoque {

    public static void main(String[] args) throws Exception {

        String app = System.getProperty("application.url", "http://localhost:9000");

        // Tests description
        File root = null;
        String selenium = null;
        List<String> tests = null;
        BufferedReader in = null;
        StringBuilder urlStringBuilder = new StringBuilder(app).append("/@tests.list");
            
        String runUnitTests = System.getProperty("runUnitTests");
        String runFunctionalTests = System.getProperty("runFunctionalTests");
        String runSeleniumTests = System.getProperty("runSeleniumTests");
        
        if(runUnitTests != null || runFunctionalTests != null || runSeleniumTests != null){
            urlStringBuilder.append("?");
            urlStringBuilder.append("runUnitTests=").append(runUnitTests != null);
            System.out.println("~ Run unit tests:" + (runUnitTests != null));

            urlStringBuilder.append("&runFunctionalTests=").append(runFunctionalTests != null);
            System.out.println("~ Run functional tests:" + (runFunctionalTests != null));

            urlStringBuilder.append("&runSeleniumTests=").append(runSeleniumTests != null);
            System.out.println("~ Run selenium tests:" + (runSeleniumTests != null));
        }
        
        try {
            in = new BufferedReader(new InputStreamReader(new URL(urlStringBuilder.toString()).openStream(), StandardCharsets.UTF_8));
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
        } catch(Exception e) {
            System.out.println("~ The application does not start. There are errors: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        } finally {
            closeQuietly(in);
        }

        // Let's tweak WebClient

        String headlessBrowser = System.getProperty("headlessBrowser", "FIREFOX");
        BrowserVersion browserVersion;
        if ("CHROME".equals(headlessBrowser)) {
            browserVersion = BrowserVersion.CHROME;
        } else if ("FIREFOX".equals(headlessBrowser)) {
            browserVersion = BrowserVersion.FIREFOX;
        } else if ("INTERNET_EXPLORER".equals(headlessBrowser)) {
            browserVersion = BrowserVersion.EDGE;
        } else if ("EDGE".equals(headlessBrowser)) {
            browserVersion = BrowserVersion.EDGE;
        } else {
            browserVersion = BrowserVersion.FIREFOX;
        }

        try (WebClient firephoque = new WebClient(browserVersion)) {
            firephoque.setPageCreator(new DefaultPageCreator() {
                /**
                 * Generated Serial version UID
                 */
                private static final long serialVersionUID = 6690993309672446834L;

                @Override
                public Page createPage(WebResponse wr, WebWindow ww) throws IOException {
                    return createHtmlPage(wr, ww);
                }
            });

            firephoque.getOptions().setThrowExceptionOnFailingStatusCode(false);

            int timeout = Integer.parseInt(System.getProperty("webclientTimeout", "-1"));
            if (timeout >= 0) {
                firephoque.getOptions().setTimeout(timeout);
            }

            firephoque.setAlertHandler((page, message) -> {
                try {
                    ScriptableObject window = page.getEnclosingWindow().getScriptableObject();
                    String script = "parent.selenium.browserbot.recordedAlerts.push('" + message.replace("'", "\\'") + "');";
                    ScriptRuntime.evalSpecial(Context.getCurrentContext(), window, window, new Object[]{script}, null, 0);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            firephoque.setConfirmHandler((page, message) -> {
                try {
                    ScriptableObject window = page.getEnclosingWindow().getScriptableObject();
                    String script = "parent.selenium.browserbot.recordedConfirmations.push('" + message.replace("'", "\\'") + "');" +
                            "var result = parent.selenium.browserbot.nextConfirmResult;" +
                            "parent.selenium.browserbot.nextConfirmResult = true;" +
                            "result";

                    Object result = ScriptRuntime.evalSpecial(Context.getCurrentContext(), window, window, new Object[]{script}, null, 0);
                    // window.execScript(script,  "JavaScript");

                    return (Boolean) result;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });
            firephoque.setPromptHandler((page, message, defaultValue) -> {
                try {
                    ScriptableObject window = page.getEnclosingWindow().getScriptableObject();
                    String script = "parent.selenium.browserbot.recordedPrompts.push('" + message.replace("'", "\\'") + "');" +
                            "var result = !parent.selenium.browserbot.nextConfirmResult ? null : parent.selenium.browserbot.nextPromptResult;" +
                            "parent.selenium.browserbot.nextConfirmResult = true;" +
                            "parent.selenium.browserbot.nextPromptResult = '';" +
                            "result";
                    Object result = ScriptRuntime.evalSpecial(Context.getCurrentContext(), window, window, new Object[]{script}, null, 0);
                    //window.execScript(script,  "JavaScript");
                    return result != null ? (String)result : defaultValue;
                } catch(Exception e) {
                    e.printStackTrace();
                    return "";
                }
            });
            firephoque.getOptions().setThrowExceptionOnScriptError(false);
            firephoque.getOptions().setPrintContentOnFailingStatusCode(false);

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
                    url = new URL(app + selenium + "?baseUrl=" + app + "&test=/@tests/" + test + ".suite&auto=true&resultsUrl=/@tests/" + test);
                }
                firephoque.openWindow(url, "headless");
                firephoque.waitForBackgroundJavaScript(5 * 60 * 1000);
                int retry = 0;
                while (retry < 5) {
                    if (new File(root, test.replace('/', '.') + ".passed.html").exists()) {
                        System.out.print("PASSED     ");
                        break;
                    } else if (new File(root, test.replace('/', '.') + ".failed.html").exists()) {
                        System.out.print("FAILED  !  ");
                        ok = false;
                        break;
                    } else {
                        if (retry++ == 4) {
                            System.out.print("ERROR   ?  ");
                            ok = false;
                            break;
                        } else {
                            Thread.sleep(1000);
                        }
                    }
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
}
