import sys
import os
import getopt
import shutil
import zipfile

from play.utils import *

COMMANDS = ["war"]

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    war_path = None
    war_zip_path = None
    try:
        optlist, args = getopt.getopt(args, 'o:', ['output=', 'zip'])
        for o, a in optlist:
            if o in ('-o', '--output'):
                war_path = os.path.normpath(os.path.abspath(a))
        for o, a in optlist:
            if o in ('--zip'):
                war_zip_path = war_path + '.war'
    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ Please specify a path where to generate the WAR, using the -o or --output option"
        print "~ "
        sys.exit(-1)
        
    package_as_war(app, env, war_path, war_zip_path)
    
    print "~ Done !"
    print "~"
    print "~ You can now load %s as a standard WAR into your servlet container" % (os.path.normpath(war_path))
    print "~ You can't use play standard commands to run/stop/debug the WAR application..."
    print "~ ... just use your servlet container commands instead"
    print "~"
    print "~ Have fun!"
    print "~"
