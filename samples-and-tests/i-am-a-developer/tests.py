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

    def testSimpleProjectCreation(self):
        
        # Well
        step('Hello, I\'m a developer')
        
        self.working_directory = bootstrapWorkingDirectory()
        
        # play new yop
        step('Create a new project')
        
        self.play = callPlay(self, ['new', '%s/yop' % self.working_directory, '--name=YOP'])
        self.assert_(waitFor(self.play, 'The new application will be created'))
        self.assert_(waitFor(self.play, 'OK, the application is created'))
        self.assert_(waitFor(self.play, 'Have fun!'))
        self.play.terminate()
        
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
        
        response = browser.follow_link(text_regex='Documentation')
        self.assert_(browser.viewing_html())
        self.assert_(browser.title() == 'Play manual - Play framework documentation')
        
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
        
        edit(app, 'app/controllers/Application.java', 8, '        render()')        
        try:
            browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines())
            self.assert_(html.count('Compilation error'))
            self.assert_(html.count('insert ";" to complete BlockStatements'))
            self.assert_(html.count('In /app/controllers/Application.java (around line 8)'))
            self.assert_(html.count('       render()'))            
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Compilation error (In /app/controllers/Application.java around line 8)'))
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
            self.assert_(html.count('In /app/controllers/Application.java (around line 8)'))
            self.assert_(html.count('       render()'))            
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Compilation error (In /app/controllers/Application.java around line 8)'))
            self.assert_(waitFor(self.play, 'Syntax error, insert ";" to complete BlockStatements'))
            self.assert_(waitFor(self.play, 'at Invocation.HTTP Request(Play!)'))
        
        # Correct the error
        step('Correct the error')
        
        edit(app, 'app/controllers/Application.java', 8, '        render();')
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
        
        edit(app, 'app/controllers/Application.java', 7, '  public static void index(String name) {')
        edit(app, 'app/controllers/Application.java', 8, '        render(name);')
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
        
        insert(app, 'app/controllers/Application.java', 8, '        int a = 9/0;')     
        try:
            response = browser.reload()
            self.fail()
        except urllib2.HTTPError, error:
            self.assert_(browser.viewing_html())
            self.assert_(browser.title() == 'Application error')
            html = ''.join(error.readlines())
            self.assert_(html.count('Execution exception'))
            self.assert_(html.count('/ by zero'))
            self.assert_(html.count('In /app/controllers/Application.java (around line 8)'))
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Execution exception (In /app/controllers/Application.java around line 8)'))
            self.assert_(waitFor(self.play, 'ArithmeticException occured : / by zero'))
            self.assert_(waitFor(self.play, 'at controllers.Application.index(Application.java:8)'))
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
            self.assert_(html.count('In /app/controllers/Application.java (around line 8)'))
            self.assert_(waitFor(self.play, 'ERROR ~'))
            self.assert_(waitFor(self.play, 'Execution exception (In /app/controllers/Application.java around line 8)'))
            self.assert_(waitFor(self.play, 'ArithmeticException occured : / by zero'))
            self.assert_(waitFor(self.play, 'at controllers.Application.index(Application.java:8)'))
            self.assert_(waitFor(self.play, '...'))

        # Fix it
        step('Fix it')        
        time.sleep(1)
        
        delete(app, 'app/controllers/Application.java', 8)    
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
        
        # Stop the application
        step('Kill play')
        
        killPlay()
        self.play.wait()

    def tearDown(self):
        killPlay()


# --- UTILS

def bootstrapWorkingDirectory():
    test_base = os.path.normpath(os.path.dirname(os.path.realpath(sys.argv[0])))
    working_directory = os.path.join(test_base, 'i-am-working-here')
    if(os.path.exists(working_directory)):
        shutil.rmtree(working_directory)
    os.mkdir(working_directory)
    return working_directory

def callPlay(self, args):
    play_script = os.path.join(self.working_directory, '../../../play')
    process_args = [play_script] + args
    play_process = subprocess.Popen(process_args,stdout=subprocess.PIPE)
    return play_process

def waitFor(process, pattern):
    timer = threading.Timer(5, timeout, [process])
    timer.start()
    while True:
        line = process.stdout.readline().strip()
        if line == '@KILLED':
            return False
        print line
        if line.count(pattern):
            timer.cancel()
            return True

def timeout(process):
    print '@@@@ TIMEOUT !'
    killPlay()

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

if __name__ == '__main__':
    unittest.main()