import os, os.path
import shutil

from play.utils import *

COMMANDS = ['idealize', 'idea']

HELP = {
    'idealize': 'Create all IntelliJ Idea configuration files'
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    app.check()
    modules = app.modules()
    classpath = app.getClasspath()

    application_name = app.readConf('application.name')
    imlFile = os.path.join(app.path, application_name + '.iml')
    shutil.copyfile(os.path.join(play_env["basedir"], 'resources/idea/imlTemplate.xml'), imlFile)
    cpXML = ""

    playHome = play_env["basedir"].replace('\\', '/')

    if os.name == 'nt':
        # On Windows, IntelliJ needs uppercase driveletter
        if playHome[1:2] == ':':
            playHome = playHome[0:1].upper() + playHome[1:]

    replaceAll(imlFile, r'%PLAYHOME%', playHome)
    replaceAll(imlFile, r'%PLAYVERSION%', play_env["version"].replace('\\', '/'))

    lXML = ""
    mlXML = ""
    msXML = ""
    jdXML = ""
    if os.path.exists(os.path.join(app.path, 'lib')):
        msXML += '                  <root url="file://$MODULE_DIR$/lib" />'
    if len(modules):
        for i, module in enumerate(modules):
            libpath = os.path.join(module, 'lib')
            srcpath = os.path.join(module, 'src')
            lXML += '        <content url="file://%s">\n            <sourceFolder url="file://%s" isTestSource="false" />\n        </content>\n' % (module, os.path.join(module, 'app').replace('\\', '/'))
            if os.path.exists(srcpath):
                msXML += '                    <root url="file://$MODULE_DIR$%s"/>\n' % (app.toRelative(srcpath).replace('\\', '/'))
            if os.path.exists(libpath):
                mlXML += '                    <root url="file://$MODULE_DIR$%s"/>\n' % (app.toRelative(libpath).replace('\\', '/'))
                jdXML += '                <jarDirectory url="file://$MODULE_DIR$%s" recursive="false"/>\n' % (app.toRelative(libpath).replace('\\', '/'))
    replaceAll(imlFile, r'%LINKS%', lXML)
    replaceAll(imlFile, r'%MODULE_LINKS%', mlXML)
    replaceAll(imlFile, r'%MODULE_LIB_CLASSES%', msXML)
    replaceAll(imlFile, r'%MODULE_LIBRARIES%', jdXML)
    
    iprFile = os.path.join(app.path, application_name + '.ipr')
    shutil.copyfile(os.path.join(play_env["basedir"], 'resources/idea/iprTemplate.xml'), iprFile)
    replaceAll(iprFile, r'%PROJECT_NAME%', application_name)
    

    print "~ OK, the application is ready for Intellij Idea"
    print "~ Use File, Open Project... to open \"" + application_name + ".ipr\""
    print "~"

