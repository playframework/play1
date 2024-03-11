# Command related to execution: junit

import sys
import os, os.path
import shutil
import subprocess
import signal

from play.utils import *

COMMANDS = ['junit']

HELP = {
    'junit': "Automatically run all application tests (via Junit directly)"
}

process = None

def handle_sigterm(signum, frame):
    global process
    if 'process' in globals():
        print("~ Terminating Java proces (SIGTERM)")
        process.terminate()
        sys.exit(0)

first_sigint = True

def handle_sigint(signum, frame):
    global process
    global first_sigint
    if 'process' in globals():
        if first_sigint:
            # Prefix with new line because ^C usually appears on the terminal
            print("\nTerminating Java process")
            process.terminate()
            first_sigint = False
        else:
            print("\nKilling Java process")
            process.kill()

def execute(**kargs):
    app = kargs.get("app")
    args = kargs.get("args")
    junit(app, args)

def junit(app, args):
    app.check()
    print("~ Running junit")
    print("~ Running in test mode")
    print("~ Ctrl+C to stop")
    print("~ ")

    print("~ Deleting %s" % os.path.normpath(os.path.join(app.path, 'tmp')))
    if os.path.exists(os.path.join(app.path, 'tmp')):
        shutil.rmtree(os.path.join(app.path, 'tmp'))
    print("~")

    # read parameters
    add_options = []
    if args.count('--unit'):
        args.remove('--unit')
        add_options.append('-DrunUnitTests')

    if args.count('--functional'):
        args.remove('--functional')
        add_options.append('-DrunFunctionalTests')

    # Run app
    test_result = os.path.join(app.path, 'test-result')
    if os.path.exists(test_result):
        print("~ cleaning test results")
        shutil.rmtree(test_result)
    args.append('-Dplay.autotest')

    classpath = app.getClasspath()
    if os.path.exists(os.path.join(app.path, 'lib-test')):
        app.find_and_add_all_jars(classpath, os.path.join(app.path, 'lib-test'))

    java_cmd = app.java_cmd(args, cp_args=':'.join(classpath), className='play.test.Runner')
    try:
        process = subprocess.Popen(java_cmd, env=os.environ)
        signal.signal(signal.SIGTERM, handle_sigterm)
        signal.signal(signal.SIGINT, handle_sigint)
        return_code = process.wait()
        if 0 != return_code:
            print("~ java process exited with exit code %d" % return_code)
            sys.exit(return_code)
    except OSError:
        print("Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). ")
        sys.exit(-1)
