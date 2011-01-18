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
    args = kargs.get("args")
    play_env = kargs.get("env")

    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    app.check()
    classpath = app.getClasspath()
    
    debug = ['-Xdebug', '-Xrunjdwp:transport=dt_socket,address=8888,server=y,suspend=n']
    
    add_options = ['-Dapplication.path=%s' % (app.path), '-Dframework.path=%s' % (play_env['basedir']), '-Dplay.id=%s' % play_env['id']]
    if args.count('--verbose'):
        add_options.append('-Dverbose')
    if args.count('--sync'):
        add_options.append('-Dsync')
    if args.count('--debug'):
        add_options.append('-Ddebug')
                
    java_cmd = [app.java_path()] + add_options + ['-classpath', app.cp_args(), 'play.deps.DependenciesManager']
    
    subprocess.call(java_cmd, env=os.environ)


