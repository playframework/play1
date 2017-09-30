# Command related to execution: auto-test

import sys
import os, os.path
import shutil
import urllib, urllib2
import subprocess
import webbrowser
import time
import signal

from play.utils import *

COMMANDS = ['autotest','auto-test']

HELP = {
    'autotest': "Automatically run all application tests"
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")
    cmdloader = kargs.get("cmdloader")
    
    autotest(app, args)
        
def autotest(app, args):
    app.check()
    print "~ Running in test mode"
    print "~ Ctrl+C to stop"
    print "~ "

    print "~ Deleting %s" % os.path.normpath(os.path.join(app.path, 'tmp'))
    if os.path.exists(os.path.join(app.path, 'tmp')):
        shutil.rmtree(os.path.join(app.path, 'tmp'))
    print "~"

    # Kill if exists
    http_port = 9000
    protocol = 'http'
    if app.readConf('https.port'):
        http_port = app.readConf('https.port')
        protocol = 'https'
    else:
        http_port = app.readConf('http.port')
    try:
        proxy_handler = urllib2.ProxyHandler({})
        opener = urllib2.build_opener(proxy_handler)
        opener.open('http://localhost:%s/@kill' % http_port)
    except Exception, e:
        pass

    # Do not run the app if SSL is configured and no cert store is configured
    keystore = app.readConf('keystore.file')
    if protocol == 'https' and not keystore:
      print "https without keystore configured. play auto-test will fail. Exiting now."
      sys.exit(-1)
      
    # read parameters
    add_options = []        
    if args.count('--unit'):
        args.remove('--unit')
        add_options.append('-DrunUnitTests')
            
    if args.count('--functional'):
        args.remove('--functional')
        add_options.append('-DrunFunctionalTests')
            
    if args.count('--selenium'):
        args.remove('--selenium')
        add_options.append('-DrunSeleniumTests')
      
    # Handle timeout parameter
    weblcient_timeout = -1
    if app.readConf('webclient.timeout'):
        weblcient_timeout = app.readConf('webclient.timeout')
    
    for arg in args:
        if arg.startswith('--timeout='):
            args.remove(arg)
            weblcient_timeout = arg[10:]
          
    if weblcient_timeout >= 0:  
        add_options.append('-DwebclientTimeout=' + weblcient_timeout)
            
    # Run app
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
            if line.find('Server is up and running') > -1: # This line is written out by Server.java to system.out and is not log file dependent
                soutint.close()
                break

    # Run FirePhoque
    print "~"
    print "~ Starting FirePhoque..."

    headless_browser = ''
    if app.readConf('headlessBrowser'):
        headless_browser = app.readConf('headlessBrowser')

    fpcp = []
    fpcp.append(os.path.normpath(os.path.join(app.play_env["basedir"], 'modules/testrunner/conf')))
    fpcp.append(os.path.join(app.play_env["basedir"], 'modules/testrunner/lib/play-testrunner.jar'))
    fpcp_libs = os.path.join(app.play_env["basedir"], 'modules/testrunner/firephoque')
    for jar in os.listdir(fpcp_libs):
        if jar.endswith('.jar'):
           fpcp.append(os.path.normpath(os.path.join(fpcp_libs, jar)))
    cp_args = ':'.join(fpcp)
    if os.name == 'nt':
        cp_args = ';'.join(fpcp)
    java_cmd = [java_path()] + add_options + ['-Djava.util.logging.config.file=logging.properties', '-classpath', cp_args, '-Dapplication.url=%s://localhost:%s' % (protocol, http_port), '-DheadlessBrowser=%s' % (headless_browser), 'play.modules.testrunner.FirePhoque']
    if protocol == 'https':
        java_cmd.insert(-1, '-Djavax.net.ssl.trustStore=' + app.readConf('keystore.file'))
    try:
        subprocess.call(java_cmd, env=os.environ)
    except OSError:
        print "Could not execute the headless browser. "
        sys.exit(-1)

    print "~"
    time.sleep(1)
    
    # Kill if exists
    try:
        proxy_handler = urllib2.ProxyHandler({})
        opener = urllib2.build_opener(proxy_handler)
        opener.open('%s://localhost:%s/@kill' % (protocol, http_port))
    except Exception, e:
        pass
 
    if os.path.exists(os.path.join(app.path, 'test-result/result.passed')):
        print "~ All tests passed"
        print "~"
        testspassed = True
    if os.path.exists(os.path.join(app.path, 'test-result/result.failed')):
        print "~ Some tests have failed. See file://%s for results" % test_result
        print "~"
        sys.exit(1)
