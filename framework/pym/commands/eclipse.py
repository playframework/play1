import os, os.path
import shutil

from framework.pym.utils import *

COMMANDS = ['eclipsify', 'ec']

def execute(**kargs):
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    app.check()
    app.check_jpda()
    modules = app.modules()
    classpath = app.getClasspath()

    application_name = app.readConf('application.name')
    if not application_name:
        application_name = os.path.dirname(app.path)
    dotProject = os.path.join(app.path, '.project')
    dotClasspath = os.path.join(app.path, '.classpath')
    dotSettings = os.path.join(app.path, '.settings')
    eclipse = os.path.join(app.path, 'eclipse')
    if os.path.exists(eclipse):
        shutil.rmtree(eclipse)
        if os.name == 'nt':
            time.sleep(1)

    if os.path.exists(dotSettings):
        shutil.rmtree(dotSettings)
        if os.name == 'nt':
            time.sleep(1)

    shutil.copyfile(os.path.join(play_env["basedir"], 'resources/eclipse/.project'), dotProject)
    shutil.copyfile(os.path.join(play_env["basedir"], 'resources/eclipse/.classpath'), dotClasspath)
    shutil.copytree(os.path.join(play_env["basedir"], 'resources/eclipse'), eclipse)
    shutil.copytree(os.path.join(play_env["basedir"], 'resources/eclipse/.settings'), dotSettings)
    replaceAll(dotProject, r'%PROJECT_NAME%', application_name)

    playJarPath = os.path.join(play_env["basedir"], 'framework','play.jar')
    playSourcePath = os.path.dirname(playJarPath)
    if os.name == 'nt':
        playSourcePath=playSourcePath.replace('\\','/').capitalize()

    cpXML = ""
    for el in classpath:
        if not os.path.basename(el) == "conf":
            if el == playJarPath:
                cpXML += '<classpathentry kind="lib" path="%s" sourcepath="%s" />\n\t' % (os.path.normpath(el) , playSourcePath)
            else:
                cpXML += '<classpathentry kind="lib" path="%s" />\n\t' % os.path.normpath(el)
    replaceAll(dotClasspath, r'%PROJECTCLASSPATH%', cpXML)

    if len(modules):
        lXML = ""
        cXML = ""
        for module in modules:
            lXML += '<link><name>%s</name><type>2</type><location>%s</location></link>\n' % (os.path.basename(module), os.path.join(module, 'app').replace('\\', '/'))
            if os.path.exists(os.path.join(module, "conf")):
                lXML += '<link><name>conf/%s</name><type>2</type><location>%s/conf</location></link>\n' % (os.path.basename(module), module.replace('\\', '/'))
            if os.path.exists(os.path.join(module, "public")):
                lXML += '<link><name>public/%s</name><type>2</type><location>%s/public</location></link>\n' % (os.path.basename(module), module.replace('\\', '/'))
            cXML += '<classpathentry kind="src" path="%s"/>' % (os.path.basename(module))
        replaceAll(dotProject, r'%LINKS%', '<linkedResources>%s</linkedResources>' % lXML)
        replaceAll(dotClasspath, r'%MODULES%', cXML)
    else:
        replaceAll(dotProject, r'%LINKS%', '')
        replaceAll(dotClasspath, r'%MODULES%', '')

    replaceAll(os.path.join(app.path, 'eclipse/debug.launch'), r'%PROJECT_NAME%', application_name)
    replaceAll(os.path.join(app.path, 'eclipse/debug.launch'), r'%PLAY_BASE%', play_env["basedir"])
    replaceAll(os.path.join(app.path, 'eclipse/debug.launch'), r'%PLAY_ID%', play_env["id"])
    replaceAll(os.path.join(app.path, 'eclipse/debug.launch'), r'%JPDA_PORT%', app.jpda_port)

    replaceAll(os.path.join(app.path, 'eclipse/test.launch'), r'%PROJECT_NAME%', application_name)
    replaceAll(os.path.join(app.path, 'eclipse/test.launch'), r'%PLAY_BASE%', play_env["basedir"])
    replaceAll(os.path.join(app.path, 'eclipse/test.launch'), r'%PLAY_ID%', play_env["id"])
    replaceAll(os.path.join(app.path, 'eclipse/test.launch'), r'%JPDA_PORT%', app.jpda_port)

    replaceAll(os.path.join(app.path, 'eclipse/connect.launch'), r'%PROJECT_NAME%', application_name)
    replaceAll(os.path.join(app.path, 'eclipse/connect.launch'), r'%JPDA_PORT%', app.jpda_port)

    os.rename(os.path.join(app.path, 'eclipse/connect.launch'), os.path.join(app.path, 'eclipse/Connect JPDA to %s.launch' % application_name))
    os.rename(os.path.join(app.path, 'eclipse/test.launch'), os.path.join(app.path, 'eclipse/Test %s.launch' % application_name))
    os.rename(os.path.join(app.path, 'eclipse/debug.launch'), os.path.join(app.path, 'eclipse/%s.launch' % application_name))

    # Module-specific modifications
    for module in modules:
        commands = os.path.join(module, 'commands.py')
        if os.path.exists(commands):
            execfile(commands)

    print "~ OK, the application is ready for eclipse"
    print "~ Use File/Import/General/Existing project to import %s into eclipse" % os.path.normpath(app.path)
    print "~"
    print "~ Use eclipsify again when you want to update eclipse configuration files."
    print "~ However, it's often better to delete and re-import the project into your workspace since eclipse keeps dirty caches..."
    print "~"
