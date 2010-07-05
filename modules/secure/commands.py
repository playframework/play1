# Secure

import getopt
from play.utils import *

MODULE = "secure"

COMMANDS = ["secure:", "secure:ov", "secure:override"]

HELP = {
    "secure:": "Show help for the secure module",
    "secure:override": "Override the CSS, login or layout"
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == 'secure:':
        print "~ Use: --css to override the Secure css" 
        print "~      --login to override the login page" 
        print "~      --layout to override the login layout page" 
        print "~ "
        return

    try:
        optlist, args2 = getopt.getopt(args, '', ['css', 'login', 'layout'])
        for o, a in optlist:
            if o == '--css':
                app.override('public/stylesheets/secure.css', 'public/stylesheets/secure.css')
                print "~ "
                return
            if o == '--login':
                app.override('app/views/Secure/login.html', 'app/views/Secure/login.html')
                print "~ "
                return
            if o == '--layout':
                app.override('app/views/Secure/layout.html', 'app/views/Secure/layout.html')
                print "~ "
                return

    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ "
        sys.exit(-1)
