import os, os.path
import shutil
import time

from play.utils import *

COMMANDS = ['netbeansify', 'nb']

HELP = {
    'netbeansify': 'Create all NetBeans configuration files'
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    app.check()
    classpath = app.getClasspath()
    modules = app.modules()
    application_name = app.readConf('application.name')
    if not application_name:
        application_name = os.path.dirname(app.path)
    nbproject = os.path.join(app.path, 'nbproject')
    if os.path.exists(nbproject):
        shutil.rmtree(nbproject)
        if os.name == 'nt':
            time.sleep(1)
    shutil.copytree(os.path.join(play_env["basedir"], 'resources/_nbproject'), nbproject)
    replaceAll(os.path.join(nbproject, 'project.xml'), r'%APPLICATION_NAME%', application_name)
    replaceAll(os.path.join(nbproject, 'project.xml'), r'%ANT_SCRIPT%', os.path.normpath(os.path.join(play_env["basedir"], 'framework/build.xml')))
    replaceAll(os.path.join(nbproject, 'project.xml'), r'%APPLICATION_PATH%', os.path.normpath(app.path))
    replaceAll(os.path.join(nbproject, 'project.xml'), r'%PLAY_CLASSPATH%', ';'.join(classpath + ['nbproject%sclasses'%os.sep, '%s%sframework%ssrc'%(play_env["basedir"], os.sep, os.sep)]))
    replaceAll(os.path.join(nbproject, 'project.xml'), r'%PLAY_ID%', play_env["id"])
    mr = ""
    for module in modules:
        mr += "<package-root>%s</package-root>" % os.path.normpath(os.path.join(module, 'app'))
    replaceAll(os.path.join(nbproject, 'project.xml'), r'%MODULES%', mr)
    mr = ""
    for dir in os.listdir(app.path):
        if os.path.isdir(os.path.join(app.path, dir)) and dir not in ['app', 'conf', 'test', 'test-result', 'public', 'tmp', 'logs', 'nbproject', 'lib']:
            if not re.search("\.[svn|git|hg|scc|vssscc]", dir):
                mr = '<source-folder style="tree"><label>%s</label><location>%s</location></source-folder>' % (dir, dir)
    replaceAll(os.path.join(nbproject, 'project.xml'), r'%MORE%', mr)
    print "~ OK, the application is ready for netbeans"
    print "~ Just open %s as a netbeans project" % os.path.normpath(app.path)
    print "~"
    print "~ Use netbeansify again when you want to update netbeans configuration files, then close and open you project again."
    print "~"
