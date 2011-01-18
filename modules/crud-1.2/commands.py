# CRUD

import getopt
from play.utils import *

COMMANDS = ['crud:ov', 'crud:override']

HELP = {
    'crud:override': 'Override a view'
}

# ~~~~~~~~~~~~~~~~~~~~~~ [crud:ov] Override a view
def execute(**kargs):
    app = kargs.get("app")
    remaining_args = kargs.get("args")
    play_env = kargs.get("env")

    try:
        optlist, args = getopt.getopt(remaining_args, 't:', ['css','layout','template='])
        for o, a in optlist:
            if o in ('-t', '--template'):
                c = a.split('/')[0]
                t = a.split('/')[1]
                app.override('app/views/CRUD/%s.html' % t, 'app/views/%s/%s.html' % (c, t))
                print "~ "
                return

            if o == '--layout':
                app.override('app/views/CRUD/layout.html', 'app/views/CRUD/layout.html')
                print "~ "
                return

            if o == '--css':
                app.override('public/stylesheets/crud.css', 'public/stylesheets/crud.css')
                print "~ "
                return

    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ "
        sys.exit(-1)

    print "~ Specify the template to override, ex : -t Users/list" 
    print "~ "
    print "~ Use --css to override the CRUD css" 
    print "~ Use --layout to override the CRUD layout" 
    print "~ "
