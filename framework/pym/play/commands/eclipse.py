import os, os.path
import shutil
import time

from play.utils import *

COMMANDS = ['eclipsify', 'ec']

HELP = {
    'eclipsify': 'Create all Eclipse configuration files'
}

def execute(**kargs):
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    is_application = os.path.exists(os.path.join(app.path, 'conf', 'application.conf'))
    if is_application:
        app.check()
        app.check_jpda()
    modules = app.modules()
    classpath = app.getClasspath()

    # determine the name of the project
    # if this is an application, the name of the project is in the application.conf file
    # if this is a module, we infer the name from the path
    application_name = app.readConf('application.name')
    vm_arguments = app.readConf('jvm.memory')
    
    javaVersion = getJavaVersion()
    
    print "~ using java version \"%s\"" % javaVersion
    if javaVersion.startswith("1.5") or javaVersion.startswith("1.6") or javaVersion.startswith("1.7"):
        print "~ ERROR: java version prior to 1.8 are no longer supported: current version \"%s\" : please update" % javaVersion
            
    vm_arguments = vm_arguments +' -noverify'

    if application_name:
        application_name = application_name.replace("/", " ")
    else:
        application_name = os.path.basename(app.path)

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
    if is_application:
        shutil.copytree(os.path.join(play_env["basedir"], 'resources/eclipse'), eclipse)
    shutil.copytree(os.path.join(play_env["basedir"], 'resources/eclipse/.settings'), dotSettings)
    replaceAll(dotProject, r'%PROJECT_NAME%', application_name)

    playJarPath = os.path.join(play_env["basedir"], 'framework', 'play-%s.jar' % play_env['version'])
    playSourcePath = os.path.join(os.path.dirname(playJarPath), 'src')
    if os.name == 'nt':
        playSourcePath=playSourcePath.replace('\\','/').capitalize()

    cpJarToSource = {}
    lib_src = os.path.join(app.path, 'tmp/lib-src')
    for el in classpath:
        # library sources jars in the lib directory
        if os.path.basename(el) != "conf" and el.endswith('-sources.jar'):
            cpJarToSource[el.replace('-sources', '')] = el

        # pointers to source jars produced by 'play deps'
        src_file = os.path.join(lib_src, os.path.basename(el) + '.src')
        if os.path.exists(src_file):
            f = file(src_file)
            cpJarToSource[el] = f.readline().rstrip()
            f.close()

    javadocLocation = {}
    for el in classpath:
        urlFile = el.replace(r'.jar','.docurl')
        if os.path.basename(el) != "conf" and os.path.exists(urlFile):
            javadocLocation[el] = urlFile

    cpXML = ""
    for el in classpath:
        if os.path.basename(el) != "conf":
            if el == playJarPath:
                cpXML += '<classpathentry kind="lib" path="%s" sourcepath="%s" />\n\t' % (os.path.normpath(el) , playSourcePath)
            else:
                if cpJarToSource.has_key(el):
                    cpXML += '<classpathentry kind="lib" path="%s" sourcepath="%s"/>\n\t' % (os.path.normpath(el), cpJarToSource[el])
                else:
                    if javadocLocation.has_key(el):
                        cpXML += '<classpathentry kind="lib" path="%s">\n\t\t' % os.path.normpath(el)
                        cpXML += '<attributes>\n\t\t\t'
                        f = file(javadocLocation[el])
                        url = f.readline()
                        f.close()
                        cpXML += '<attribute name="javadoc_location" value="%s"/>\n\t\t' % (url.strip())
                        cpXML += '</attributes>\n\t'
                        cpXML += '</classpathentry>\n\t'
                    else:
                        cpXML += '<classpathentry kind="lib" path="%s"/>\n\t' % os.path.normpath(el)
    if not is_application:
        cpXML += '<classpathentry kind="src" path="src"/>'
    replaceAll(dotClasspath, r'%PROJECTCLASSPATH%', cpXML)

    # generate source path for test folder if one exists
    cpTEST = ""
    if os.path.exists(os.path.join(app.path, 'test')):
        cpTEST += '<classpathentry kind="src" path="test"/>'
    replaceAll(dotClasspath, r'%TESTCLASSPATH%', cpTEST)

    if len(modules):
        lXML = ""
        cXML = ""
        for module in modules:
            lXML += '<link><name>%s</name><type>2</type><location>%s</location></link>\n' % (os.path.basename(module), os.path.join(module, 'app').replace('\\', '/'))
            if os.path.exists(os.path.join(module, "conf")):
                lXML += '<link><name>conf/%s</name><type>2</type><location>%s/conf</location></link>\n' % (os.path.basename(module), module.replace('\\', '/'))
            if os.path.exists(os.path.join(module, "public")):
                lXML += '<link><name>public/%s</name><type>2</type><location>%s/public</location></link>\n' % (os.path.basename(module), module.replace('\\', '/'))
            cXML += '<classpathentry kind="src" path="%s"/>\n\t' % (os.path.basename(module))
        replaceAll(dotProject, r'%LINKS%', '<linkedResources>%s</linkedResources>' % lXML)
        replaceAll(dotClasspath, r'%MODULES%', cXML)
    else:
        replaceAll(dotProject, r'%LINKS%', '')
        replaceAll(dotClasspath, r'%MODULES%', '')

    if is_application:
        replaceAll(os.path.join(app.path, 'eclipse/debug.launch'), r'%PROJECT_NAME%', application_name)
        replaceAll(os.path.join(app.path, 'eclipse/debug.launch'), r'%PLAY_BASE%', play_env["basedir"])
        replaceAll(os.path.join(app.path, 'eclipse/debug.launch'), r'%PLAY_ID%', play_env["id"])
        replaceAll(os.path.join(app.path, 'eclipse/debug.launch'), r'%JPDA_PORT%', str(app.jpda_port))
        replaceAll(os.path.join(app.path, 'eclipse/debug.launch'), r'%PLAY_VERSION%', play_env["version"])
        replaceAll(os.path.join(app.path, 'eclipse/debug.launch'), r'%VM_ARGUMENTS%', vm_arguments)

        replaceAll(os.path.join(app.path, 'eclipse/test.launch'), r'%PROJECT_NAME%', application_name)
        replaceAll(os.path.join(app.path, 'eclipse/test.launch'), r'%PLAY_BASE%', play_env["basedir"])
        replaceAll(os.path.join(app.path, 'eclipse/test.launch'), r'%PLAY_ID%', play_env["id"])
        replaceAll(os.path.join(app.path, 'eclipse/test.launch'), r'%JPDA_PORT%', str(app.jpda_port))
        replaceAll(os.path.join(app.path, 'eclipse/test.launch'), r'%PLAY_VERSION%', play_env["version"])
        replaceAll(os.path.join(app.path, 'eclipse/test.launch'), r'%VM_ARGUMENTS%', vm_arguments)

        replaceAll(os.path.join(app.path, 'eclipse/connect.launch'), r'%PROJECT_NAME%', application_name)
        replaceAll(os.path.join(app.path, 'eclipse/connect.launch'), r'%JPDA_PORT%', str(app.jpda_port))

        os.rename(os.path.join(app.path, 'eclipse/connect.launch'), os.path.join(app.path, 'eclipse/Connect JPDA to %s.launch' % application_name))
        os.rename(os.path.join(app.path, 'eclipse/test.launch'), os.path.join(app.path, 'eclipse/Test %s.launch' % application_name))
        os.rename(os.path.join(app.path, 'eclipse/debug.launch'), os.path.join(app.path, 'eclipse/%s.launch' % application_name))
   
    if is_application:
        print "~ OK, the application \"%s\" is ready for eclipse" % application_name
    else:
        print "~ OK, the module \"%s\" is ready for eclipse" % application_name
    print "~ Use File/Import/General/Existing project to import %s into eclipse" % os.path.normpath(app.path)
    print "~"
    print "~ Use eclipsify again when you want to update eclipse configuration files."
    print "~ However, it's often better to delete and re-import the project into your workspace since eclipse keeps dirty caches..."
    print "~"
