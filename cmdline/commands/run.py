import os
import subprocess
import cmdline.java as javautils

NAMES = ['run']

def execute(app, args=[]):
    app.check()
    disable_check_jpda = False
    if args.count('-f') == 1:
        disable_check_jpda = True
        args.remove('-f')

    print "~ Ctrl+C to stop"
    print "~ "
    java_cmd = app.java_cmd(args)
    if app.readConf('application.mode') == 'dev':
        if not disable_check_jpda: app.check_jpda()
        java_cmd.insert(2, '-Xdebug')
        java_cmd.insert(2, '-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n' % app.jpda_port)
        java_cmd.insert(2, '-Dplay.debug=yes')
    try:
        subprocess.call(java_cmd, env=os.environ)
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
    print
    sys.exit(0)
