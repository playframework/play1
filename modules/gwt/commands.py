# GWT

# ~~~~~~~~~~~~~~~~~~~~~~ Check paths
if play_command.startswith('gwt:'):
    gwt_path = None
    try:
        optlist, args = getopt.getopt(remaining_args, '', ['gwt='])
        for o, a in optlist:
            if o == '--gwt':
                gwt_path = os.path.normpath(os.path.abspath(a))

    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ "
        sys.exit(-1)

    if not gwt_path and os.environ.has_key('GWT_PATH'):
        gwt_path = os.path.normpath(os.path.abspath(os.environ['GWT_PATH']))

    if not gwt_path:
        print "~ You need to specify the path of you GWT installation, "
        print "~ either using the $GWT_PATH environment variable or with the --gwt option" 
        print "~ "
        sys.exit(-1)
        
    # check
    if not os.path.exists(os.path.join(gwt_path, 'gwt-user.jar')):
        print "~ %s seems not to be a valid GWT installation (checked for gwt-user.jar)" % gwt_path
        print "~ This module has been tested with GWT 1.6"
        print "~ "
        sys.exit(-1)


# ~~~~~~~~~~~~~~~~~~~~~~ [gwt:init] Init GWT project
if play_command == 'gwt:init':
        
    # Create gwt-public
    if not os.path.exists(os.path.join(application_path, 'gwt-public')):
        os.mkdir(os.path.join(application_path, 'gwt-public'))
    if not os.path.exists(os.path.join(application_path, 'gwt-public/index.html')):
        shutil.copyfile(os.path.join(play_base, 'modules/gwt/resources/index.html'), os.path.join(application_path, 'gwt-public/index.html'))
        
    # Create packages
    if not os.path.exists(os.path.join(application_path, 'app/client')):
        os.mkdir(os.path.join(application_path, 'app/client'))
    if not os.path.exists(os.path.join(application_path, 'app/Main.gwt.xml')):
        shutil.copyfile(os.path.join(play_base, 'modules/gwt/resources/Main.gwt.xml'), os.path.join(application_path, 'app/Main.gwt.xml'))
    if not os.path.exists(os.path.join(application_path, 'app/client/Main.java')):
        shutil.copyfile(os.path.join(play_base, 'modules/gwt/resources/Main.java'), os.path.join(application_path, 'app/client/Main.java'))
    
    print "~ Ok. A Main GWT module has been created in app/gwt and GWT static resources come to gwt-public"
    print "~ Run play gwt:browser to run the hosted mode browser"
    print "~"
    print "~ Have fun !"
    print "~"
    sys.exit(0)


# ~~~~~~~~~~~~~~~~~~~~~~ [gwt:browser] Run the hosted mode browser
if play_command == 'gwt:browser':
    
    # Run
    print "~ Running com.google.gwt.dev.HostedMode ..."
    print "~"
    do_classpath()
    do_java()
    cp = []
    cp.append(os.path.normpath(os.path.join(application_path, 'app')))
    cp.append(os.path.normpath(os.path.join(play_base, 'modules/gwt/lib/gwt-user.jar')))
    cp.append(os.path.normpath(os.path.join(gwt_path, 'gwt-dev-windows.jar')))
    cp.append(os.path.normpath(os.path.join(gwt_path, 'gwt-dev-mac.jar')))
    cp.append(os.path.normpath(os.path.join(gwt_path, 'gwt-dev-linux.jar')))
    for jar in os.listdir(os.path.join(application_path, 'lib')):
        if jar.endswith('.jar'):
            cp.append(os.path.normpath(os.path.join(application_path, 'lib/%s' % jar)))
    cps = ':'.join(cp)
    if os.name == 'nt':
        cps = ';'.join(cp)
    # '-logLevel', 'DEBUG',
    gwt_cmd = [java_path, '-Xmx256M', '-classpath', cps, 'com.google.gwt.dev.HostedMode', '-noserver', '-startupUrl', 'http://localhost:9000/@gwt', '-war', os.path.normpath(os.path.join(application_path, 'gwt-public')), 'Main']
    if os.path.exists(os.path.normpath(os.path.join(gwt_path, 'gwt-dev-mac.jar'))):
        gwt_cmd.insert(2, '-XstartOnFirstThread')
    gwt_cmd.insert(2, '-Xdebug')
    gwt_cmd.insert(2, '-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n' % '3408')
    subprocess.call(gwt_cmd, env=os.environ)
    print "~"
    sys.exit(0)
    
