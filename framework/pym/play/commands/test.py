import sys
import subprocess

from play.utils import *

COMMANDS = ['test']

HELP = {
    'test': "Run the application in test mode in the current shell",
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")
    cmdloader = kargs.get("cmdloader")
    
    test(app, args)
    
def test(app, args):
    app.check()
    java_cmd = app.java_cmd(args)
    print "~ Running in test mode"
    print "~ Ctrl+C to stop"
    print "~ "

    try:
        return_code = subprocess.call(java_cmd, env=os.environ)
        if 0 != return_code:
            sys.exit(return_code)
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
    print "~ "


