import os, os.path
import shutil
import urllib, urllib2
import subprocess
import simplejson as json

from play.utils import *

COMMANDS = ['evolutions','ev', 'evolutions:apply', 'ev:apply', 'evolutions:markApplied', 'ev:markApplied', 'evolutions:resolve', 'ev:resolve']

HELP = {
    'evolutions': 'Run the evolution check',
    'evolutions:apply': 'Automatically apply pending evolutions',
    'evolutions:markApplied': 'Mark pending evolutions as manually applied',
    'evolutions:resolve': 'Resolve partially applied evolution'
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    if command.find(':resolve') > 0:
        args.append('-Dmode=resolve')

    if command.find(':apply') > 0:
        args.append('-Dmode=apply')

    if command.find(':markApplied') > 0:
        args.append('-Dmode=markApplied')

    classpath = app.getClasspath()
    args_memory = app.java_args_memory(args) 
    app.jpda_port = app.readConf('jpda.port')

    add_options = ['-Dapplication.path=%s' % (app.path), '-Dframework.path=%s' % (play_env['basedir']), '-Dplay.id=%s' % play_env['id'], '-Dplay.version=%s' % play_env['version']]
    if args.count('--jpda'):
        print "~ Waiting for JPDA client to continue"
        args.remove('--jpda')
        add_options.append('-Xdebug')
        add_options.append('-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=y' % app.jpda_port)
    add_options.extend(args)
    # Remove duplicate memory arg
    for arg in args_memory: 
        if arg in add_options:
            add_options.remove(arg)

    java_cmd = [java_path()] + add_options + args_memory + ['-classpath', app.cp_args(), 'play.db.Evolutions']
    try:
        return_code = subprocess.call(java_cmd, env=os.environ)
        if 0 != return_code:
            sys.exit(return_code);
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
