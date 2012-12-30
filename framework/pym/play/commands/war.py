import sys
import os
import getopt
import shutil

import play.commands.precompile
from play.utils import *

COMMANDS = ["war"]

HELP = {
    'war': 'Export the application as a standalone WAR archive'
}

def execute(**kargs):

    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    war_path = None
    war_zip_path = None
    war_exclusion_list = []
    try:
        optlist, args = getopt.getopt(args, 'o:', ['output=', 'zip','exclude='])
        for o, a in optlist:
            if o in ('-o', '--output'):
                war_path = os.path.normpath(os.path.abspath(a))
        for o, a in optlist:
            if o in ('--zip'):
                war_zip_path = war_path + '.war'
            if o in ('--exclude'):
                war_exclusion_list = a.split(':')
                print "~ Excluding these directories :"
                for excluded in war_exclusion_list:
                    print "~  %s" %excluded
    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ Please specify a path where to generate the WAR, using the -o or --output option."
        print "~ To exclude some directories, use the --exclude option and ':'-separator (eg: --exclude .svn:target:logs:tmp)."
        print "~ "
        sys.exit(-1)

    if not war_path:
        print "~ Oops. Please specify a path where to generate the WAR, using the -o or --output option"
        print "~ To exclude some directories, use the --exclude option and ':'-separator (eg: --exclude .svn:target:logs:tmp)."
        print "~"
        sys.exit(-1)

    if os.path.exists(war_path) and not os.path.exists(os.path.join(war_path, 'WEB-INF')):
        print "~ Oops. The destination path already exists but does not seem to host a valid WAR structure"
        print "~"
        sys.exit(-1)

    if isParentOf(app.path, war_path):
        print "~ Oops. Please specify a destination directory outside of the application"
        print "~"
        sys.exit(-1)

    # Precompile first
    precompilation_result = play.commands.precompile.execute(command=command, app=app, args=args, env=env)

    if precompilation_result != 0:
        print "~ Please fix compilation errors before packaging WAR"
        print "~"
        sys.exit(precompilation_result)

    # Package
    package_as_war(app, env, war_path, war_zip_path, war_exclusion_list)

    print "~ Done !"
    print "~"
    print "~ You can now load %s as a standard WAR into your servlet container" % (os.path.normpath(war_path))
    print "~ You can't use play standard commands to run/stop/debug the WAR application..."
    print "~ ... just use your servlet container commands instead"
    print "~"
    print "~ Have fun!"
    print "~"
