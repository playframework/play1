package controllers;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.IO;
import play.libs.Mail;
import play.mvc.*;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.test.*;
import play.vfs.*;

public class TestRunner extends Controller {

    public static void index() {
        List<Class> unitTests = TestEngine.allUnitTests();
        List<Class> functionalTests = TestEngine.allFunctionalTests();
        List<String> seleniumTests = TestEngine.allSeleniumTests();
        render(unitTests, functionalTests, seleniumTests);
    }

    public static void list() {
        StringWriter list = new StringWriter();
        PrintWriter p = new PrintWriter(list);
        p.println("---");
        p.println(Play.getFile("test-result").getAbsolutePath());
        p.println(Router.reverse(Play.modules.get("_testrunner").child("/public/test-runner/selenium/TestRunner.html")));
        for(Class c : TestEngine.allUnitTests()) {
            p.println(c.getName() + ".class");
        }
        for(Class c : TestEngine.allFunctionalTests()) {
            p.println(c.getName() + ".class");
        }
        for(String c : TestEngine.allSeleniumTests()) {
            p.println(c);
        }
        renderText(list);
    }

    public static void run(String test) throws Exception {
        if (test.equals("init")) {
            File testResults = Play.getFile("test-result");
            if (!testResults.exists()) {
                testResults.mkdir();
            }
            for(File tr : testResults.listFiles()) {
                if ((tr.getName().endsWith(".html") || tr.getName().startsWith("result.")) && !tr.delete()) {
                    Logger.warn("Cannot delete %s ...", tr.getAbsolutePath());
                }
            }
            renderText("done");
        }
        if (test.equals("end")) {
            File testResults = Play.getFile("test-result/result." + params.get("result"));
            IO.writeContent(params.get("result"), testResults);
            renderText("done");
        }
        if (test.endsWith(".class")) {
            Play.getFile("test-result").mkdir();
            TestEngine.TestResults results = TestEngine.run(test.substring(0, test.length() - 6));
            response.status = results.passed ? 200 : 500;
            Template resultTemplate = TemplateLoader.load("TestRunner/results.html");
            Map<String, Object> options = new HashMap<String, Object>();
            options.put("test", test);
            options.put("results", results);
            String result = resultTemplate.render(options);
            File testResults = Play.getFile("test-result/" + test + (results.passed ? ".passed" : ".failed") + ".html");
            IO.writeContent(result, testResults);
            try {
                // Write xml output
                options.remove("out");
                resultTemplate = TemplateLoader.load("TestRunner/results-xunit.xml");
                String resultXunit = resultTemplate.render(options);
                File testXunitResults = Play.getFile("test-result/TEST-" + test.substring(0, test.length()-6) + ".xml");
                IO.writeContent(resultXunit, testXunitResults);
            } catch(Exception e) {
                Logger.error(e, "Cannot ouput XML unit output");
            }            
            response.contentType = "text/html";
            renderText(result);
        }
        if (test.endsWith(".test.html.suite")) {
            test = test.substring(0, test.length() - 6);
            render("TestRunner/selenium-suite.html", test);
        }
        if (test.endsWith(".test.html")) {
            File testFile = Play.getFile("test/" + test);
            if (!testFile.exists()) {
                for(VirtualFile root : Play.roots) {
                    File moduleTestFile = Play.getFile(root.relativePath()+"/test/" + test);
                    if(moduleTestFile.exists()) {
                        testFile = moduleTestFile;
                    }
                }
            }
            if (testFile.exists()) {
                Template testTemplate = TemplateLoader.load(VirtualFile.open(testFile));
                Map<String, Object> options = new HashMap<String, Object>();
                response.contentType = "text/html";
                renderText(testTemplate.render(options));
            } else {
                renderText("Test not found, %s", testFile);
            }
        }
        if (test.endsWith(".test.html.result")) {
            flash.keep();
            test = test.substring(0, test.length() - 7);
            File testResults = Play.getFile("test-result/" + test.replace("/", ".") + ".passed.html");
            if (testResults.exists()) {
                response.contentType = "text/html";
                response.status = 200;
                renderText(IO.readContentAsString(testResults));
            }
            testResults = Play.getFile("test-result/" + test.replace("/", ".") + ".failed.html");
            if (testResults.exists()) {
                response.contentType = "text/html";
                response.status = 500;
                renderText(IO.readContentAsString(testResults));
            }
            response.status = 404;
            renderText("No test result");
        }
    }

    public static void saveResult(String test, String result) throws Exception {
        String table = params.get("testTable.1");
        File testResults = Play.getFile("test-result/" + test.replace("/", ".") + "." + result + ".html");
        Template resultTemplate = TemplateLoader.load("TestRunner/selenium-results.html");
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("test", test);
        options.put("table", table);
        options.put("result", result);
        String rf = resultTemplate.render(options);
        IO.writeContent(rf, testResults);
        renderText("done");
    }

    public static void mockEmail(String by) {
        String email = Mail.Mock.getLastMessageReceivedBy(by);
        if(email == null) {
            notFound();
        }
        renderText(email);
    }

	public static void cacheEntry(String key){
    	String value = Cache.get(key,String.class);
    	if(value == null){
    		notFound();
    	}
    	renderText(value);
    }
	
}

