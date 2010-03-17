
def execute(app):
    app.check()
    load_modules()
    do_classpath()
    disable_check_jpda = False
    if remaining_args.count('-f') == 1:
        disable_check_jpda = True
        remaining_args.remove('-f')
    do_java()
    print "~ Ctrl+C to stop"
    print "~ "
    if application_mode == 'dev':
        if not disable_check_jpda: check_jpda()
        java_cmd.insert(2, '-Xdebug')
        java_cmd.insert(2, '-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n' % jpda_port)
        java_cmd.insert(2, '-Dplay.debug=yes')
    try:
        subprocess.call(java_cmd, env=os.environ)
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
    print
    sys.exit(0)
