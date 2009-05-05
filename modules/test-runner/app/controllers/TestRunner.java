package controllers;

import java.io.File;
import java.util.*;

import play.Play;
import play.libs.Files;
import play.libs.IO;
import play.mvc.*;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.test.*;
import play.vfs.FileSystemFile;

public class TestRunner extends Controller {

    public static void index() {
        List<Class> unitTests = TestEngine.allUnitTests();
        List<Class> functionalTests = TestEngine.allFunctionalTests();
        List<String> seleniumTests = TestEngine.allSeleniumTests();
        render(unitTests, functionalTests, seleniumTests);
    }

    public static void run(String test) throws Exception {
        if(test.equals("init")) {
            File testResults = Play.getFile("test-result");
            if(testResults.exists()) {
                if(!Files.delete(testResults)) {
                    throw new RuntimeException("Cannot delete " + testResults);
                }
            }
            testResults.mkdir();
            renderText("done");
        }
	if(test.endsWith(".class")) {
            Thread.sleep(250);
            TestEngine.TestResults results = TestEngine.run(test.substring(0, test.length() - 6));
            response.status = results.passed ? 200 : 500;
            Template resultTemplate = TemplateLoader.load("TestRunner/results.html");
            Map<String,Object> options = new HashMap<String,Object>();
            options.put("test", test);
            options.put("results", results);
            String result = resultTemplate.render(options);
            File testResults = Play.getFile("test-result/"+test.replace(".class", ".java")+(results.passed ? ".passed" : ".failed" )+".html");
            IO.writeContent(result, testResults);
            response.contentType = "text/html";
            renderText(result);
        } 
        if(test.endsWith(".test.html.suite")) {
            test = test.substring(0, test.length()-6);
            render("TestRunner/selenium-suite.html", test);           
        }
        if(test.endsWith(".test.html")) {
            File testFile = Play.getFile("test/"+test);
            if(testFile.exists()) {
                Template testTemplate = TemplateLoader.load(new FileSystemFile((testFile)));
                Map<String,Object> options = new HashMap<String,Object>();
                response.contentType = "text/html";
                renderText(testTemplate.render(options));
            } else {
                renderText("Test not found, %s", testFile);
            }
        }
        if(test.endsWith(".test.html.result")) {
            test = test.substring(0, test.length()-7);
            File testResults = Play.getFile("test-result/"+test.replace("/", ".")+".passed.html");
            if(testResults.exists()) {
                response.contentType = "text/html";
                response.status = 200;
                renderText(IO.readContentAsString(testResults));
            }
            testResults = Play.getFile("test-result/"+test.replace("/", ".")+".failed.html");
            if(testResults.exists()) {
                response.contentType = "text/html";
                response.status = 500;
                renderText(IO.readContentAsString(testResults));
            }
            notFound("No test result");         
        }
    }
    
    public static void saveResult(String test, String result) throws Exception {
        String table = params.get("testTable.1");
        File testResults = Play.getFile("test-result/"+test.replace("/", ".")+"."+result+".html");
        IO.writeContent(table, testResults);
        renderText("done");
    }

}

