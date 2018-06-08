import os, os.path
import shutil
import urllib, urllib2
import subprocess
import simplejson as json

from play.utils import *

COMMANDS = ['dependencies','deps']

HELP = {
    'dependencies': 'Resolve and retrieve project dependencies'
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    force = "false"
    trim = "false"
    shortModuleNames = "false"
    
    if args.count('--forceCopy') == 1:
        args.remove('--forceCopy')
        force = "true"
        
    if args.count('--forProd') == 1:
        args.remove('--forProd')
        force = "true"
        trim = "true"

    if args.count('--shortModuleNames') == 1:
        args.remove('--shortModuleNames')
        shortModuleNames = "true"

    classpath = app.getClasspath()
    args_memory = app.java_args_memory(args) 
    app.jpda_port = app.readConf('jpda.port')

    add_options = ['-Dapplication.path=%s' % (app.path), '-Dframework.path=%s' % (play_env['basedir']), '-Dplay.id=%s' % play_env['id'], '-Dplay.version=%s' % play_env['version'], '-Dplay.forcedeps=%s' % (force), '-Dplay.trimdeps=%s' % (trim), '-Dplay.shortModuleNames=%s' % (shortModuleNames)]
    if args.count('--verbose'):
        args.remove('--verbose')
        add_options.append('-Dverbose')
    if args.count('--sync'):
        args.remove('--sync')
        add_options.append('-Dsync')
    if args.count('--nosync'):
        args.remove('--nosync')
        add_options.append('-Dnosync')
    if args.count('--debug'):
        args.remove('--debug')
        add_options.append('-Ddebug')
    if args.count('--clearcache'):
        args.remove('--clearcache')
        add_options.append('-Dclearcache')
    if args.count('--jpda'):
        args.remove('--jpda')
        print "~ Waiting for JPDA client to continue"
        add_options.append('-Xdebug')
        add_options.append('-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=y' % app.jpda_port)
    for arg in args:
        if arg.startswith("-D"):
            add_options.append(arg)
        elif not arg.startswith('-Xm'):
            print "~ WARNING: " + arg + " argument will be skipped"    

    java_cmd = [java_path()] + add_options + args_memory + ['-classpath', app.fw_cp_args(), 'play.deps.DependenciesManager']
    try:
        return_code = subprocess.call(java_cmd, env=os.environ)
        if 0 != return_code:
            sys.exit(return_code);
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
