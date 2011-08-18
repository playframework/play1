import os, os.path
import shutil
import time

from play.utils import *

COMMANDS = ['antify']

HELP = {
    'antify': 'Create a build.xml file for this project'
}

def execute(**kargs):
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    is_application = os.path.exists(os.path.join(app.path, 'conf', 'application.conf'))
    app.check()
    
    shutil.copyfile(os.path.join(play_env["basedir"], 'resources/build.xml'), os.path.join(app.path, 'build.xml'))
    
    print "~ OK, a build.xml file has been created"
    print "~ Define the PLAY_PATH env property, and use it with ant run|start|stop"
    print "~"
