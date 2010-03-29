# Command related to creation and execution: run, new, clean, test, auto-test

import sys
import os
import subprocess
import shutil
import getopt
import urllib2
import webbrowser
import time

import framework.pym.java as javautils

from framework.pym.utils import *

NAMES = ['run', 'new', 'new,run', 'clean', 'clean,run', 'test', 'auto-test']

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == 'new' or command == 'new,run':
        new(app, args, env)
    if command == 'clean' or command == 'clean,run':
        clean(app)
    if command == 'new,run' or command == 'clean,run' or command == 'run':
        run(app, args)
    if command == 'test':
        test(app, args)
    if command == 'auto-test':
        autotest(app, args)

def new(app, args, env):
    withModules = []
    application_name = None
    try:
        optlist, args = getopt.getopt(args, '', ['with=', 'name='])
        for o, a in optlist:
            if o in ('--with'):
                withModules = a.split(',')
            if o in ('--name'):
                application_name = a
    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ Sorry, unrecognized option"
        print "~ "
        sys.exit(-1)
    if os.path.exists(app.path):
        print "~ Oops. %s already exists" % app.path
        print "~"
        sys.exit(-1)
    
    md = []
    for m in withModules:
        dirname = None
        if os.path.exists(os.path.join(env["basedir"], 'modules/%s' % m)) and os.path.isdir(os.path.join(env["basedir"], 'modules/%s' % m)):
            dirname = m
        else:
            for f in os.listdir(os.path.join(env["basedir"], 'modules')):
                if os.path.isdir(os.path.join(env["basedir"], 'modules/%s' % f)) and f.find('%s-' % m) == 0:
                    dirname = f
                    break
        
        if not dirname:
            print "~ Oops. No module %s found" % m
            print "~ Try to install it using 'play install %s'" % m
            print "~"
            sys.exit(-1)
        
        md.append(dirname)
        
    print "~ The new application will be created in %s" % os.path.normpath(app.path)
    if application_name is None:
        application_name = raw_input("~ What is the application name? ")
    shutil.copytree(os.path.join(env["basedir"], 'resources/application-skel'), app.path)
    app.check()
    replaceAll(os.path.join(app.path, 'conf/application.conf'), r'%APPLICATION_NAME%', application_name)
    replaceAll(os.path.join(app.path, 'conf/application.conf'), r'%SECRET_KEY%', secretKey())
    print "~"
    
    for m in md:
        mn = m
        if mn.find('-') > 0:
            mn = mn[:mn.find('-')]
        replaceAll(os.path.join(app.path, 'conf/application.conf'), r'# ---- MODULES ----', '# ---- MODULES ----\nmodule.%s=${play.path}/modules/%s' % (mn, m) )
    
    # modules
    app.check()
    for module in app.modules():
        commands = os.path.join(module, 'commands.py')
        if os.path.exists(commands):
            execfile(commands)
    
    print "~ OK, the application is created."
    print "~ Start it with : play run %s" % sys.argv[2]
    print "~ Have fun!"
    print "~"

def run(app, args):
    app.check()
    disable_check_jpda = False
    if args.count('-f') == 1:
        disable_check_jpda = True
        args.remove('-f')

    print "~ Ctrl+C to stop"
    print "~ "
    java_cmd = app.java_cmd(args)
    if app.readConf('application.mode') == 'dev':
        if not disable_check_jpda: app.check_jpda()
        java_cmd.insert(2, '-Xdebug')
        java_cmd.insert(2, '-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n' % app.jpda_port)
        java_cmd.insert(2, '-Dplay.debug=yes')
    try:
        subprocess.call(java_cmd, env=os.environ)
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
    print

def clean(app):
    app.check()
    print "~ Deleting %s" % os.path.normpath(os.path.join(app.path, 'tmp'))
    if os.path.exists(os.path.join(app.path, 'tmp')):
        shutil.rmtree(os.path.join(app.path, 'tmp'))
    print "~"

def show_modules(args):
    check_application()
    modules = app.modules()
    if len(modules):
        print "~ Application modules are:"
        print "~ "
        for module in modules:
            print "~ %s" % module
    else:
        print "~ No modules installed in this application"
    print "~ "
    sys.exit(0)

def test(app, args):
    app.check()
    disable_check_jpda = False
    if args.count('-f') == 1:
        disable_check_jpda = True
    java_cmd = app.java_cmd(args)
    print "~ Running in test mode"
    print "~ Ctrl+C to stop"
    print "~ "
    app.check_jpda()
    java_cmd.insert(2, '-Xdebug')
    java_cmd.insert(2, '-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n' % app.jpda_port)
    java_cmd.insert(2, '-Dplay.debug=yes')
    try:
        subprocess.call(java_cmd, env=os.environ)
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
    print


def autotest(app, args):
    app.check()
    print "~ Running in test mode"
    print "~ Ctrl+C to stop"
    print "~ "
    print "~ Deleting %s" % os.path.normpath(os.path.join(app.path, 'tmp'))
    if os.path.exists(os.path.join(app.path, 'tmp')):
        shutil.rmtree(os.path.join(app.path, 'tmp'))
    print "~"
    test_result = os.path.join(app.path, 'test-result')
    if os.path.exists(test_result):
        shutil.rmtree(test_result)
    sout = open(os.path.join(app.log_path(), 'system.out'), 'w')
    java_cmd = app.java_cmd(args)
    try:
        play_process = subprocess.Popen(java_cmd, env=os.environ, stdout=sout)
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
    soutint = open(os.path.join(app.log_path(), 'system.out'), 'r')
    while True:
        if play_process.poll():
            print "~"
            print "~ Oops, application has not started?"
            print "~"
            sys.exit(-1)
        line = soutint.readline().strip()
        if line:
            print line
            if line.find('Listening for HTTP') > -1:
                soutint.close()
                break
    # Launch the browser
    http_port = app.readConf('http_port')
    print "~"
    print "~ Loading the test runner at %s ..." % ('http://localhost:%s/@tests' % http_port)
    try:
        proxy_handler = urllib2.ProxyHandler({})
        opener = urllib2.build_opener(proxy_handler)
        opener.open('http://localhost:%s/@tests' % http_port);
    except urllib2.HTTPError, e:
        print "~"
        print "~ There are compilation errors... (%s)" % (e.code)
        print "~"
        kill(play_process.pid)
        sys.exit(-1)
    print "~ Launching a web browser at http://localhost:%s/@tests?select=all&auto=yes ..." % http_port
    webbrowser.open('http://localhost:%s/@tests?select=all&auto=yes' % http_port)
    while True:
        time.sleep(1)
        if os.path.exists(os.path.join(app.path, 'test-result/result.passed')):
            print "~"
            print "~ All tests passed"
            print "~"
            kill(play_process.pid)
            break
        if os.path.exists(os.path.join(app.path, 'test-result/result.failed')):
            print "~"
            print "~ Some tests have failed. See file://%s for results" % test_result
            print "~"
            kill(play_process.pid)
            break
