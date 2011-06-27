#!/usr/bin/python

import unittest
import os
import shutil
import sys
import subprocess
import re
import time
import urllib2
import mechanize
import threading

# --- TESTS

class IamADeveloper(unittest.TestCase):
    
    def testLogLevelsAndLog4jConfig(self):

        # Testing job developing
        step('Hello, I am testing loglevels')

        self.working_directory = bootstrapWorkingDirectory('i-am-testing-log-levels-here')
    
        # play new job-app
        step('Create a new project')
    
        self.play = callPlay(self, ['new', '%s/loglevelsapp' % self.working_directory, '--name=LOGLEVELSAPP'])
        self.assert_(waitFor(self.play, 'The new application will be created'))
        self.assert_(waitFor(self.play, 'OK, the application is created'))
        self.assert_(waitFor(self.play, 'Have fun!'))
        
        self.play.wait()
    
        app = '%s/loglevelsapp' % self.working_directory
            
        #inserting some log-statements in our controller
        insert(app, "app/controllers/Application.java", 13, '        Logger.debug("I am a debug message");')
        insert(app, "app/controllers/Application.java", 14, '        Logger.info("I am an info message");')            
    
        # Run the newly created application
        step('Run our logger-application')
    
        self.play = callPlay(self, ['run', app])
        #wait for play to be ready
        self.assert_(waitFor(self.play, 'Listening for HTTP on port 9000'))
    
        step("Send request to trigger some logging")

        browser = mechanize.Browser()
        response = browser.open('http://localhost:9000/')

    
        step("check that only info log message is logged")
        self.assert_(waitForWithFail(self.play, 'I am an info message', 'I am a debug message'))

        step("stop play")
        killPlay()
        self.play.wait()

        #now we're going to manually configure log4j to log debug messages
        step('Writing log4j config file')
        
        create(app, 'conf/log4j.xml')
        
        insert(app, "conf/log4j.xml", 1, '<?xml version="1.0" encoding="UTF-8" ?>')
        insert(app, "conf/log4j.xml", 2, '<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">')
        insert(app, "conf/log4j.xml", 3, '<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">')
        insert(app, "conf/log4j.xml", 4, '  <appender name="console" class="org.apache.log4j.ConsoleAppender">')
        insert(app, "conf/log4j.xml", 5, '      <param name="Target" value="System.out"/>')
        insert(app, "conf/log4j.xml", 6, '      <layout class="org.apache.log4j.PatternLayout">')
        insert(app, "conf/log4j.xml", 7, '          <param name="ConversionPattern" value="%m%n"/>')
        insert(app, "conf/log4j.xml", 8, '      </layout>')
        insert(app, "conf/log4j.xml", 9, '  </appender>')
        insert(app, "conf/log4j.xml", 10, ' <logger name="play">')
        insert(app, "conf/log4j.xml", 11, '     <level value="debug"/>')
        insert(app, "conf/log4j.xml", 12, ' </logger>')
        insert(app, "conf/log4j.xml", 13, ' <root>')
        insert(app, "conf/log4j.xml", 14, '     <priority value="info"/>')
        insert(app, "conf/log4j.xml", 15, '     <appender-ref ref="console"/>')
        insert(app, "conf/log4j.xml", 16, ' </root>')
        insert(app, "conf/log4j.xml", 17, '</log4j:configuration>')
        
            
        # Run the newly created application
        step('re-run our logger-application')
    
        self.play = callPlay(self, ['run', app])
        #wait for play to be ready
        self.assert_(waitFor(self.play, 'Listening for HTTP on port 9000'))
    
        step("Send request to trigger some logging")

        browser = mechanize.Browser()
        response = browser.open('http://localhost:9000/')

    
        step("check that both debug and info message is logged")
        self.assert_(waitFor(self.play, 'I am a debug message'))        
        self.assert_(waitFor(self.play, 'I am an info message'))

        step("stop play")
        killPlay()
        self.play.wait()
    
        step("done testing logging")


    def testCreateAndRunForJobProject(self):

        # Testing job developing
        step('Hello, I am a job-developer')

        self.working_directory = bootstrapWorkingDirectory('i-am-creating-jobs-here')
    
        # play new job-app
        step('Create a new project')
    
        self.play = callPlay(self, ['new', '%s/jobapp' % self.working_directory, '--name=JOBAPP'])
        self.assert_(waitFor(self.play, 'The new application will be created'))
        self.assert_(waitFor(self.play, 'OK, the application is created'))
        self.assert_(waitFor(self.play, 'Have fun!'))
        self.play.wait()
    
        app = '%s/jobapp' % self.working_directory
            
        #create our first job - which is executed sync on startup with @OnApplicationStart
    
        createDir( app, 'app/jobs')
        create(app, 'app/jobs/Job1.java')
        insert(app, 'app/jobs/Job1.java', 1, "package jobs;")
        insert(app, 'app/jobs/Job1.java', 2, "import play.jobs.*;")
        insert(app, 'app/jobs/Job1.java', 3, "import play.*;")
        insert(app, 'app/jobs/Job1.java', 4, "@OnApplicationStart")
        insert(app, 'app/jobs/Job1.java', 5, "public class Job1 extends Job {")
        insert(app, 'app/jobs/Job1.java', 6, "  public void doJob() throws Exception{")
        insert(app, 'app/jobs/Job1.java', 7, '      Logger.info("Job starting");')
        insert(app, 'app/jobs/Job1.java', 8, '      Thread.sleep(2000);')
        insert(app, 'app/jobs/Job1.java', 9, '      Logger.info("Job done");')
        insert(app, 'app/jobs/Job1.java', 10, '  }')
        insert(app, 'app/jobs/Job1.java', 11, '}')
    
        #modify our controller to log when exeuted
        insert(app, "app/controllers/Application.java", 13, '        Logger.info("Processing request");')
    
    
        # Run the newly created application
        step('Run the newly created job-application')
    
        self.play = callPlay(self, ['run', app])
        #wait for play to be ready
        self.assert_(waitFor(self.play, 'Listening for HTTP on port 9000'))
    
        step("Send request to start app")

        browser = mechanize.Browser()
        response = browser.open('http://localhost:9000/')

    
        step("check that job completed before processing request")
        self.assert_(waitFor(self.play, 'Job done'))
        self.assert_(waitFor(self.play, 'Processing request'))

        step("stop play")
        killPlay()
        self.play.wait()
            
        #now we change the job to be async
        step("Change job to async")
    
        edit(app, 'app/jobs/Job1.java', 4, "@OnApplicationStart(async=true)")        

        # start play again
        step('Run the job-application again')
    
        self.play = callPlay(self, ['run', app])
        #wait for play to be ready
        self.assert_(waitFor(self.play, 'Listening for HTTP on port 9000'))
    
        step("Send request to start app")

        browser = mechanize.Browser()
        response = browser.open('http://localhost:9000/')

    
        step("check that the request is processed before the job finishes")
        self.assert_(waitFor(self.play, 'Processing request'))
        self.assert_(waitFor(self.play, 'Job done'))

        step("stop play")
        killPlay()
        self.play.wait()
    
        step("done testing jobapp")
    

    def testSimpleProjectCreation(self):

        # Well
        step('Hello, I\'m a developer')
        
        self.working_directory = bootstrapWorkingDirectory('i-am-working-here')
        
        # play new yop
        step('Create a new project')
        
        self.play = callPlay(self, ['new', '%s/yop' % self.working_directory, '--name=YOP'])
        self.assert_(waitFor(self.play, 'The new application will be created'))
        self.assert_(waitFor(self.play, 'OK, the application is created'))
        self.assert_(waitFor(self.play, 'Have fun!'))
        self.play.wait()
        
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/app')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/app/controllers')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/app/controllers/Application.java')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/app/models')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/app/views')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/app/views/Application')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/app/views/Application/index.html')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/app/views/main.html')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/app/views/errors/404.html')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/app/views/errors/500.html')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/conf')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/conf/routes')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/conf/messages')))
        self.assert_(os.path.exists(os.path.join(self.working_directory, 'yop/conf/application.conf')))

        app = '%s/yop' % self.working_directory

        # Run the newly created application
        step('Run the newly created application')
        
        self.play = callPlay(self, ['run', app])
        self.assert_(waitFor(self.play, 'Listening for HTTP on port 9000'))
        
        # Start a browser
        step('Start a browser')
        
        browser = mechanize.Browser()
        
        # Open the home page
        step('Open the home page')
        
        response = browser.open('http://localhost:9000/')
        self.assert_(waitFor(self.play, "Application 'YOP' is now started !"))
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Your application is ready !')
        
        html = response.get_data()
        self.assert_(html.count('Your application is ready !'))
        
        # Open the documentation
        step('Open the documentation')
        
        response = browser.open('http://localhost:9000/@documentation')
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Play manual - Documentation')
        
        html = response.get_data()
        self.assert_(html.count('Getting started'))
        
        # Go back to home
        step('Go back to home')
        
        response = browser.back()
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Your application is ready !')
        
        # Refresh
        step('Refresh home')
        
        response = browser.reload()
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Your application is ready !')        
        html = response.get_data()
        self.assert_(html.count('Your application is ready !'))
        
        # Make a mistake in Application.java and refresh
        step('Make a mistake in Application.java')
        
        edit(app, 'app/controllers/Application.java', 13, '        render()')        
        try:
            browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines())
            self.assert_(html.count('Compilation error'))
            self.assert_(html.count('insert ";" to complete BlockStatements'))
            self.assert_(html.count('In /app/controllers/Application.java (around line 13)'))
            self.assert_(html.count('       render()'))            
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Compilation error (In /app/controllers/Application.java around line 13)'))
            self.assert_(waitFor(self.play, 'Syntax error, insert ";" to complete BlockStatements'))
            self.assert_(waitFor(self.play, 'at Invocation.HTTP Request(Play!)'))

        # Refresh again
        step('Refresh again')

        try:
            browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines())
            self.assert_(html.count('Compilation error'))
            self.assert_(html.count('insert ";" to complete BlockStatements'))
            self.assert_(html.count('In /app/controllers/Application.java (around line 13)'))
            self.assert_(html.count('       render()'))            
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Compilation error (In /app/controllers/Application.java around line 13)'))
            self.assert_(waitFor(self.play, 'Syntax error, insert ";" to complete BlockStatements'))
            self.assert_(waitFor(self.play, 'at Invocation.HTTP Request(Play!)'))
        
        # Correct the error
        step('Correct the error')
        
        edit(app, 'app/controllers/Application.java', 13, '        render();')
        response = browser.reload()
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Your application is ready !')        
        html = response.get_data()
        self.assert_(html.count('Your application is ready !'))

        # Refresh again
        step('Refresh again')
        
        response = browser.reload()
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Your application is ready !')        
        html = response.get_data()
        self.assert_(html.count('Your application is ready !'))
        
        # Let's code hello world
        step('Let\'s code hello world')
        time.sleep(1)
        
        edit(app, 'app/controllers/Application.java', 12, '  public static void index(String name) {')
        edit(app, 'app/controllers/Application.java', 13, '        render(name);')
        edit(app, 'app/views/Application/index.html', 2, "#{set title:'Hello world app' /}")
        edit(app, 'app/views/Application/index.html', 4, "Hello ${name} !!")
        response = browser.reload()
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Hello world app')        
        html = response.get_data()
        self.assert_(html.count('Hello  !!'))
        
        response = browser.open('http://localhost:9000/?name=Guillaume')
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Hello world app')        
        html = response.get_data()
        self.assert_(html.count('Hello Guillaume !!'))
        
        # Make a mistake in the template
        step('Make a mistake in the template')
        time.sleep(1)
        
        edit(app, 'app/views/Application/index.html', 4, "Hello ${name !!")
        try:
            response = browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines()) 
            self.assert_(html.count('Template compilation error'))
            self.assert_(html.count('In /app/views/Application/index.html (around line 4)'))
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Template compilation error (In /app/views/Application/index.html around line 4)'))
            self.assert_(waitFor(self.play, 'at Invocation.HTTP Request(Play!)'))
        
        # Refresh again
        step('Refresh again')
        
        try:
            response = browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines()) 
            self.assert_(html.count('Template compilation error'))
            self.assert_(html.count('In /app/views/Application/index.html (around line 4)'))
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Template compilation error (In /app/views/Application/index.html around line 4)'))
            self.assert_(waitFor(self.play, 'at Invocation.HTTP Request(Play!)'))
            
        # Try a template runtime exception  
        step('Try a template runtime exception ')  
        time.sleep(1)
        
        edit(app, 'app/views/Application/index.html', 4, "Hello ${user.name}")
        try:
            response = browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines()) 
            self.assert_(html.count('Template execution error '))
            self.assert_(html.count('In /app/views/Application/index.html (around line 4)'))
            self.assert_(html.count('Cannot get property \'name\' on null object'))
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Template execution error (In /app/views/Application/index.html around line 4)'))
            self.assert_(waitFor(self.play, 'Execution error occured in template /app/views/Application/index.html.'))
            self.assert_(waitFor(self.play, 'at Invocation.HTTP Request(Play!)'))
            self.assert_(waitFor(self.play, 'at /app/views/Application/index.html.(line:4)'))
            self.assert_(waitFor(self.play, '...'))

        # Refresh again
        step('Refresh again')
        
        try:
            response = browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines()) 
            self.assert_(html.count('Template execution error '))
            self.assert_(html.count('In /app/views/Application/index.html (around line 4)'))
            self.assert_(html.count('Cannot get property \'name\' on null object'))
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Template execution error (In /app/views/Application/index.html around line 4)'))
            self.assert_(waitFor(self.play, 'Execution error occured in template /app/views/Application/index.html.'))
            self.assert_(waitFor(self.play, 'at Invocation.HTTP Request(Play!)'))
            self.assert_(waitFor(self.play, 'at /app/views/Application/index.html.(line:4)'))
            self.assert_(waitFor(self.play, '...'))

        # Fix it
        step('Fix it')        
        time.sleep(1)
        
        edit(app, 'app/views/Application/index.html', 4, "Hello ${name} !!")
        response = browser.reload()
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Hello world app')        
        html = response.get_data()
        self.assert_(html.count('Hello Guillaume !!'))

        # Make a Java runtime exception
        step('Make a Java runtime exception')  
        
        insert(app, 'app/controllers/Application.java', 13, '        int a = 9/0;')     
        try:
            response = browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines())
            self.assert_(html.count('Execution exception'))
            self.assert_(html.count('/ by zero'))
            self.assert_(html.count('In /app/controllers/Application.java (around line 13)'))
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Execution exception (In /app/controllers/Application.java around line 13)'))
            self.assert_(waitFor(self.play, 'ArithmeticException occured : / by zero'))
            self.assert_(waitFor(self.play, 'at controllers.Application.index(Application.java:13)'))
            self.assert_(waitFor(self.play, '...'))

        # Refresh again
        step('Refresh again')
        
        try:
            response = browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines())
            self.assert_(html.count('Execution exception'))
            self.assert_(html.count('/ by zero'))
            self.assert_(html.count('In /app/controllers/Application.java (around line 13)'))
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Execution exception (In /app/controllers/Application.java around line 13)'))
            self.assert_(waitFor(self.play, 'ArithmeticException occured : / by zero'))
            self.assert_(waitFor(self.play, 'at controllers.Application.index(Application.java:13)'))
            self.assert_(waitFor(self.play, '...'))

        # Fix it
        step('Fix it')        
        time.sleep(1)
        
        delete(app, 'app/controllers/Application.java', 13)    
        response = browser.reload()
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Hello world app')        
        html = response.get_data()
        self.assert_(html.count('Hello Guillaume !!'))

        # Refresh again
        step('Refresh again')
        
        response = browser.reload()
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Hello world app')        
        html = response.get_data()
        self.assert_(html.count('Hello Guillaume !!'))

        # Create a new route
        step('Create a new route')
        
        insert(app, 'conf/routes', 7, "GET      /hello          Hello.hello")
        try:
            response = browser.open('http://localhost:9000/hello')
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Not found')
        
        # Create the new controller
        step('Create the new controller')
        time.sleep(1)
        
        create(app, 'app/controllers/Hello.java')
        insert(app, 'app/controllers/Hello.java', 1, "package controllers;")
        insert(app, 'app/controllers/Hello.java', 2, "import play.mvc.*;")
        insert(app, 'app/controllers/Hello.java', 3, "public class Hello extends Application {")
        insert(app, 'app/controllers/Hello.java', 4, "  public static void hello() {")
        insert(app, 'app/controllers/Hello.java', 5, '      renderText("Hello");')
        insert(app, 'app/controllers/Hello.java', 6, '  }')
        insert(app, 'app/controllers/Hello.java', 7, '}')
        
        # Retry
        step('Retry')
        
        browser.reload()
        self.assert_(not browser.viewing_html())   
        html = response.get_data()
        self.assert_(html.count('Hello'))
        
        # Rename the Hello controller
        step('Rename the Hello controller')
        time.sleep(1)
        
        rename(app, 'app/controllers/Hello.java', 'app/controllers/Hello2.java')
        edit(app, 'app/controllers/Hello2.java', 3, "public class Hello2 extends Application {")
        
        try:
            browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Not found')

        # Refresh again
        step('Refresh again')
            
        try:
            browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Not found')            

        # Correct the routes file
        step('Correct the routes file')
        time.sleep(1)

        edit(app, 'conf/routes', 7, "GET      /hello          Hello2.hello")

        browser.reload()
        self.assert_(not browser.viewing_html())   
        html = response.get_data()
        self.assert_(html.count('Hello'))        

        # Retry
        step('Retry')
        
        browser.reload()
        self.assert_(not browser.viewing_html())   
        html = response.get_data()
        self.assert_(html.count('Hello'))
        
        # Rename again
        step('Rename again')
        time.sleep(1)
        
        rename(app, 'app/controllers/Hello2.java', 'app/controllers/Hello3.java')
        edit(app, 'conf/routes', 7, "GET      /hello          Hello3.hello")
        
        try:
            browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines())
            self.assert_(html.count('Compilation error'))
            self.assert_(html.count('/app/controllers/Hello3.java</strong> could not be compiled'))
            self.assert_(html.count('The public type Hello2 must be defined in its own file'))
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Compilation error (In /app/controllers/Hello3.java around line 3)'))
            self.assert_(waitFor(self.play, 'at Invocation.HTTP Request(Play!)'))
            
        # Refresh again
        step('Refresh again')

        try:
            browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines())
            self.assert_(html.count('Compilation error'))
            self.assert_(html.count('/app/controllers/Hello3.java</strong> could not be compiled'))
            self.assert_(html.count('The public type Hello2 must be defined in its own file'))
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Compilation error (In /app/controllers/Hello3.java around line 3)'))
            self.assert_(waitFor(self.play, 'at Invocation.HTTP Request(Play!)'))
            
        # Fix it
        step('Fix it')
        
        edit(app, 'app/controllers/Hello3.java', 3, "public class Hello3 extends Application {")
        browser.reload()
        self.assert_(not browser.viewing_html())   
        html = response.get_data()
        self.assert_(html.count('Hello'))

        # Stop the application
        step('Kill play')
        
        killPlay()
        self.play.wait()

    def tearDown(self):
        killPlay()





# --- UTILS

def bootstrapWorkingDirectory( folder ):
    test_base = os.path.normpath(os.path.dirname(os.path.realpath(sys.argv[0])))
    working_directory = os.path.join(test_base, folder )
    if(os.path.exists(working_directory)):
        shutil.rmtree(working_directory)
    os.mkdir(working_directory)
    return working_directory

def callPlay(self, args):
    play_script = os.path.join(self.working_directory, '../../../play')
    process_args = [play_script] + args
    play_process = subprocess.Popen(process_args,stdout=subprocess.PIPE)
    return play_process

#returns true when pattern is seen
def waitFor(process, pattern):
    return waitForWithFail(process, pattern, "")
    

#returns true when pattern is seen, but false if failPattern is seen
def waitForWithFail(process, pattern, failPattern):
    timer = threading.Timer(5, timeout, [process])
    timer.start()
    while True:
        line = process.stdout.readline().strip()
        #print timeoutOccured
        if timeoutOccured:
            return False
        if line == '@KILLED':
            return False
        if line: print line
        if failPattern != "" and line.count(failPattern):
            timer.cancel()
            return False
        if line.count(pattern):
            timer.cancel()
            return True

timeoutOccured = False

def timeout(process):
    global timeoutOccured 
    print '@@@@ TIMEOUT !'
    killPlay()
    timeoutOccured = True

def killPlay():
    try:
        urllib2.urlopen('http://localhost:9000/@kill')
    except:
        pass

def step(msg):
    print
    print '# --- %s' % msg
    print

def edit(app, file, line, text):
    fname = os.path.join(app, file)
    source = open(fname, 'r')
    lines = source.readlines()
    lines[line-1] = '%s\n' % text
    source.close()
    source = open(fname, 'w')
    source.write(''.join(lines))
    source.close()
    os.utime(fname, None)

def insert(app, file, line, text):
    fname = os.path.join(app, file)
    source = open(fname, 'r')
    lines = source.readlines()
    lines[line-1:line-1] = '%s\n' % text
    source.close()
    source = open(fname, 'w')
    source.write(''.join(lines))
    source.close()
    os.utime(fname, None)

def create(app, file):
    fname = os.path.join(app, file)
    source = open(fname, 'w')
    source.close()
    os.utime(fname, None)

def createDir(app, file):
    fname = os.path.join(app, file)
    os.mkdir( fname )


def delete(app, file, line):
    fname = os.path.join(app, file)
    source = open(fname, 'r')
    lines = source.readlines()
    del lines[line-1]
    source.close()
    source = open(fname, 'w')
    source.write(''.join(lines))
    source.close()
    os.utime(fname, None)    

def rename(app, fro, to):
    os.rename(os.path.join(app, fro), os.path.join(app, to))

if __name__ == '__main__':
    unittest.main()