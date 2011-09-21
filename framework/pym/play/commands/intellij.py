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

    replaceAll(imlFile, r'%PLAYHOME%', play_env["basedir"].replace('\\', '/'))
    replaceAll(imlFile, r'%PLAYVERSION%', play_env["version"].replace('\\', '/'))

    lXML = ""
    mlXML = ""
    msXML = ""
    jdXML = ""
    depXML = ""
    depJavadocXML = ""
    lib_path = os.path.join(app.path, 'lib')
    if os.path.exists(lib_path):
        msXML += '<root url="file://$MODULE_DIR$/lib" />'
        for jar_file in os.listdir(lib_path):
            if jar_file.endswith('-sources.jar'):
                depXML += '<root url="jar://$MODULE_DIR$/lib/%s!/" />\n' % jar_file
            elif jar_file.endswith('-javadoc.jar'):
                depJavadocXML += '<root url="jar://$MODULE_DIR$/lib/%s!/" />\n' % jar_file
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
    replaceAll(imlFile, r'%MODULE_LIB_SOURCES%', depXML)
    replaceAll(imlFile, r'%MODULE_LIB_JAVADOC%', depJavadocXML)

    print "~ OK, the application is ready for Intellij Idea"
    print "~ Use File/New Module/Import Existing module"
    print "~"