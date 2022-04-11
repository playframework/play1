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
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")
    cmdloader = kargs.get("cmdloader")

    junit(app, args)

def junit(app, args):
    app.check()
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
        shutil.rmtree(test_result)
    args.append('-Dplay.autotest')
    java_cmd = app.java_cmd(args, className='play.test.Runner')
    try:
        process = subprocess.Popen (java_cmd, env=os.environ)
        signal.signal(signal.SIGTERM, handle_sigterm)
        signal.signal(signal.SIGINT, handle_sigint)
        return_code = process.wait()
        if 0 != return_code:
            sys.exit(return_code)
    except OSError:
        print("Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). ")
        sys.exit(-1)

    if os.path.exists(os.path.join(app.path, 'test-result/result.passed')):
        print("~ All tests passed")
        print("~")
        testspassed = True
    if os.path.exists(os.path.join(app.path, 'test-result/result.failed')):
        print("~ Some tests have failed. See file://%s for results" % test_result)
        print("~")
        sys.exit(1)
