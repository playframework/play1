package controllers;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import play.Logger;
import play.Play;
import play.libs.IO;
import play.libs.Mail;
import play.mvc.*;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.test.*;
import play.test.TestEngine.TestResult;
import play.test.TestEngine.TestResults;
import play.vfs.*;

public class TestRunner extends Controller {

    public static void index(String format) {
        List<Class> unitTests = TestEngine.allUnitTests();
        List<Class> functionalTests = TestEngine.allFunctionalTests();
        List<String> seleniumTests = TestEngine.allSeleniumTests();
        if (format != null && format.equals("json")) {
        	Map<String, List<String>> result = new HashMap<String, List<String>>();
        	List<String> unitTestsNames = new ArrayList<String>();
        	for (int i = 0; i < unitTests.size(); i++) {
        		unitTestsNames.add(unitTests.get(i).getName());
        	}
        	List<String> functionalTestsNames = new ArrayList<String>();
        	for (int i = 0; i < functionalTests.size(); i++) {
        		functionalTestsNames.add(functionalTests.get(i).getName());
        	}
        	result.put("unitTests", unitTestsNames);
        	result.put("functionalTests", functionalTestsNames);
        	result.put("seleniumTests", seleniumTests);
        	renderJSON(result);
        } else {
        	render(unitTests, functionalTests, seleniumTests);
        }
    }

    public static void run(String test, String format) throws Exception {
    	if (format == null || format.isEmpty()) format = "html";
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
            java.lang.Thread.sleep(250);
            TestEngine.TestResults results = TestEngine.run(test.substring(0, test.length() - 6));
            if (format.equals("json")) {
                response.status = 200; // 500 prevents some clients to get the body
                String json = new Gson().toJson(results);
                response.contentType = "application/json";
                renderText(json);
            } else {
                response.status = results.passed ? 200 : 500;
                Map<String, Object> options = new HashMap<String, Object>();
                options.put("test", test);
                options.put("results", results);
                Template resultTemplate = TemplateLoader.load("TestRunner/results.html");
                String result = resultTemplate.render(options);
                File testResults = Play.getFile("test-result/" + test.replace(".class", ".java") + (results.passed ? ".passed" : ".failed") + ".html");
                IO.writeContent(result, testResults);
                response.contentType = "text/html";
                renderText(result);
            }
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

}

