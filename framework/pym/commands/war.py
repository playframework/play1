
# NAMES = ["war"]

def package_as_war(war_path, war_zip_path):
    check_application()
    load_modules()
    do_classpath()
    do_java()
    
    if not war_path:
        print "~ Oops. Please specify a path where to generate the WAR, using the -o or --output option"
        print "~"
        sys.exit(-1)
    
    if os.path.exists(war_path) and not os.path.exists(os.path.join(war_path, 'WEB-INF')):
        print "~ Oops. The destination path already exists but does not seem to host a valid WAR structure"
        print "~"
        sys.exit(-1)
        
    if isParentOf(application_path, war_path):
        print "~ Oops. Please specify a destination directory outside of the application"
        print "~"
        sys.exit(-1)
        
    print "~ Packaging current version of the framework and the application to %s ..." % (os.path.normpath(war_path))
    if os.path.exists(war_path): shutil.rmtree(war_path)
    if os.path.exists(os.path.join(application_path, 'war')):
        shutil.copytree(os.path.join(application_path, 'war'), war_path)
    else:
        os.mkdir(war_path)
    if not os.path.exists(os.path.join(war_path, 'WEB-INF')): os.mkdir(os.path.join(war_path, 'WEB-INF'))
    if not os.path.exists(os.path.join(war_path, 'WEB-INF/web.xml')):
        shutil.copyfile(os.path.join(play_base, 'resources/war/web.xml'), os.path.join(war_path, 'WEB-INF/web.xml'))
    application_name = play_app.readConf('application.name')
    replaceAll(os.path.join(war_path, 'WEB-INF/web.xml'), r'%APPLICATION_NAME%', application_name)
    if play_id:
        replaceAll(os.path.join(war_path, 'WEB-INF/web.xml'), r'%PLAY_ID%', play_id)
    else:
        replaceAll(os.path.join(war_path, 'WEB-INF/web.xml'), r'%PLAY_ID%', 'war')
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application')): shutil.rmtree(os.path.join(war_path, 'WEB-INF/application'))
    shutil.copytree(application_path, os.path.join(war_path, 'WEB-INF/application'))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application/war')):
        shutil.rmtree(os.path.join(war_path, 'WEB-INF/application/war'))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application/logs')):
        shutil.rmtree(os.path.join(war_path, 'WEB-INF/application/logs'))
    shutil.copytree(os.path.join(application_path, 'conf'), os.path.join(war_path, 'WEB-INF/classes'))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/lib')): shutil.rmtree(os.path.join(war_path, 'WEB-INF/lib'))
    os.mkdir(os.path.join(war_path, 'WEB-INF/lib'))
    for jar in classpath:
        if jar.endswith('.jar') and jar.find('provided-') == -1:
            shutil.copyfile(jar, os.path.join(war_path, 'WEB-INF/lib/%s' % os.path.split(jar)[1]))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/framework')): shutil.rmtree(os.path.join(war_path, 'WEB-INF/framework'))
    os.mkdir(os.path.join(war_path, 'WEB-INF/framework'))
    shutil.copytree(os.path.join(play_base, 'framework/templates'), os.path.join(war_path, 'WEB-INF/framework/templates'))
    
    # modules
    for module in modules:
        to = os.path.join(war_path, 'WEB-INF/modules/%s' % os.path.basename(module))
        shutil.copytree(module, to)
        if os.path.exists(os.path.join(to, 'src')):
            shutil.rmtree(os.path.join(to, 'src'))
        if os.path.exists(os.path.join(to, 'dist')):
            shutil.rmtree(os.path.join(to, 'dist'))
        if os.path.exists(os.path.join(to, 'samples-and-tests')):
            shutil.rmtree(os.path.join(to, 'samples-and-tests'))
        if os.path.exists(os.path.join(to, 'build.xml')):
            os.remove(os.path.join(to, 'build.xml'))
        if os.path.exists(os.path.join(to, 'commands.py')):
            os.remove(os.path.join(to, 'commands.py'))
        if os.path.exists(os.path.join(to, 'lib')):
            shutil.rmtree(os.path.join(to, 'lib'))
        if os.path.exists(os.path.join(to, 'nbproject')):
            shutil.rmtree(os.path.join(to, 'nbproject'))
        if os.path.exists(os.path.join(to, 'documentation')):
            shutil.rmtree(os.path.join(to, 'documentation'))
    pm = play_app.readConfs('module.')
    for m in pm:
        nm = os.path.basename(m)
        replaceAll(os.path.join(war_path, 'WEB-INF/application/conf/application.conf'), m, '../modules/%s' % nm)
    
    if not os.path.exists(os.path.join(war_path, 'WEB-INF/resources')): os.mkdir(os.path.join(war_path, 'WEB-INF/resources'))
    shutil.copyfile(os.path.join(play_base, 'resources/messages'), os.path.join(war_path, 'WEB-INF/resources/messages'))
    
    if war_zip_path:
        print "~ Creating zipped archive to %s ..." % (os.path.normpath(war_zip_path))
        if os.path.exists(war_zip_path):
            os.remove(war_zip_path)
        zip = zipfile.ZipFile(war_zip_path, 'w', zipfile.ZIP_STORED)
        dist_dir = os.path.join(application_path, 'dist')
        for (dirpath, dirnames, filenames) in os.walk(war_path):
            if dirpath == dist_dir:
                continue
            if dirpath.find('/.') > -1:
                continue
            for file in filenames:
                if file.find('~') > -1 or file.startswith('.'):
                    continue
                zip.write(os.path.join(dirpath, file), os.path.join(dirpath[len(war_path):], file))

        zip.close()

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")

    war_path = None
    war_zip_path = None
    try:
        optlist, args = getopt.getopt(remaining_args, 'o:', ['output=', 'zip'])
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
        
    package_as_war(war_path, war_zip_path)
    
    print "~ Done !"
    print "~"
    print "~ You can now load %s as a standard WAR into your servlet container" % (os.path.normpath(war_path))
    print "~ You can't use play standard commands to run/stop/debug the WAR application..."
    print "~ ... just use your servlet container commands instead"
    print "~"
    print "~ Have fun!"
    print "~"
    sys.exit(0)