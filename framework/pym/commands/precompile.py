import os, os.path
import shutil
import subprocess

from framework.pym.utils import *

COMMANDS = ['precompile']

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    app.check()
    java_cmd = app.java_cmd(args)
    if os.path.exists(os.path.join(app.path, 'tmp')):
        shutil.rmtree(os.path.join(app.path, 'tmp'))
    if os.path.exists(os.path.join(app.path, 'precompiled')):
        shutil.rmtree(os.path.join(app.path, 'precompiled'))
    java_cmd.insert(2, '-Dprecompile=yes')
    try:
        subprocess.call(java_cmd, env=os.environ)
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
    print
