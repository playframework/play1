import os, os.path
import shutil

from framework.pym.utils import *

COMMANDS = ['idealize', 'idea']

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

    if len(modules):
        lXML = ""
        cXML = ""
        for module in modules:
            lXML += '    <content url="file://%s">\n      <sourceFolder url="file://%s" isTestSource="false" />\n    </content>\n' % (module, os.path.join(module, 'app').replace('\\', '/'))
        replaceAll(imlFile, r'%LINKS%', lXML)
    else:
        replaceAll(imlFile, r'%LINKS%', '')

    print "~ OK, the application is ready for Intellij Idea"
    print "~ Use File/New Module/Import Existing module"
    print "~"
