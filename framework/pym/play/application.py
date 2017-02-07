import sys
import os
import os.path
import re
import shutil
import socket

from play.utils import *


class ModuleNotFound(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)

class PlayApplication(object):
    """A Play Application: conf file, java"""

    # ~~~~~~~~~~~~~~~~~~~~~~ Constructor

    def __init__(self, application_path, env, ignoreMissingModules = False):
        self.path = application_path
        # only parse conf it is exists - if it should be there, it will be caught later 
        # (depends on command)
        confExists = os.path.exists(os.path.join(self.path, 'conf', 'application.conf')); 
        if application_path is not None and confExists:
            confFolder = os.path.join(application_path, 'conf/')
            try:
                self.conf = PlayConfParser(confFolder, env)
            except Exception as err:
                print "~ Failed to parse application configuration", err
                self.conf = None # No app / Invalid app
        else:
            self.conf = None
        self.play_env = env

        if env.has_key('jpda.port'):
            self.jpda_port = env['jpda.port']
        else:
            self.jpda_port = self.readConf('jpda.port')

        self.ignoreMissingModules = ignoreMissingModules

    # ~~~~~~~~~~~~~~~~~~~~~~ Configuration File

    def check(self):
        try:
            assert os.path.exists(os.path.join(self.path, 'conf', 'routes'))
            assert os.path.exists(os.path.join(self.path, 'conf', 'application.conf'))
        except AssertionError:
            print "~ Oops. conf/routes or conf/application.conf missing."
            print "~ %s does not seem to host a valid application." % os.path.normpath(self.path)
            print "~"
            sys.exit(-1)

    def readConf(self, key):
        if (self.conf is None):
            return ''
        return self.conf.get(key)

    def readConfs(self, key):
        if (self.conf is None):
            return []
        return self.conf.getAll(key)

    # ~~~~~~~~~~~~~~~~~~~~~~ Modules

    def modules(self):
        modules = []
        application_mode = self.readConf('application.mode').lower()
        if not application_mode:
            application_mode = "dev"
        if application_mode == 'dev':
            #Load docviewer module
			modules.append(os.path.normpath(os.path.join(self.play_env["basedir"], 'modules/docviewer')))
			
        for m in self.readConfs('module.'):
            if '${play.path}' in m:
                m = m.replace('${play.path}', self.play_env["basedir"])
            if m[0] is not '/':
                m = os.path.normpath(os.path.join(self.path, m))
            if not os.path.exists(m) and not self.ignoreMissingModules:
                print "~ Oops,"
                print "~ Module not found: %s" % (m)
                print "~"
                if m.startswith('${play.path}/modules'):
                    print "~ You can try to install the missing module using 'play install %s'" % (m[21:])
                    print "~"
                sys.exit(-1)
            modules.append(m)
        if self.path and os.path.exists(os.path.join(self.path, 'modules')):
            for m in os.listdir(os.path.join(self.path, 'modules')):
                mf = os.path.join(os.path.join(self.path, 'modules'), m)
                if os.path.basename(mf)[0] == '.':
                    continue
                if os.path.isdir(mf):
                    modules.append(mf)
                else:
                    modules.append(open(mf, 'r').read().strip())
        if isTestFrameworkId( self.play_env["id"] ):
            modules.append(os.path.normpath(os.path.join(self.play_env["basedir"], 'modules/testrunner')))
        return set(modules) # Ensure we don't have duplicates

    def module_names(self):
        return map(lambda x: x[7:],self.conf.getAllKeys("module."))

    def override(self, f, t):
        fromFile = None
        for module in self.modules():
            pc = os.path.join(module, f)
            if os.path.exists(pc): fromFile = pc
        if not fromFile:
            print "~ %s not found in any module" % f
            print "~ "
            sys.exit(-1)
        toFile = os.path.join(self.path, t)
        if os.path.exists(toFile):
            response = raw_input("~ Warning! %s already exists and will be overridden (y/n)? " % toFile)
            if not response == 'y':
                return
        if not os.path.exists(os.path.dirname(toFile)):
            os.makedirs(os.path.dirname(toFile))
        shutil.copyfile(fromFile, toFile)
        print "~ Copied %s to %s " % (fromFile, toFile)

    def name(self):
        return self.readConf("application.name")

    # ~~~~~~~~~~~~~~~~~~~~~~ JAVA

    def find_and_add_all_jars(self, classpath, dir):

        # ignore dirs that start with ".", example: .svn
        if dir.find(".") == 0:
            return

        for file in os.listdir(dir):
            fullPath = os.path.normpath(os.path.join(dir,file))
            if os.path.isdir(fullPath):
                self.find_and_add_all_jars(classpath, fullPath)
            else:
                if fullPath.endswith('.jar'):
                    classpath.append(fullPath)

    def getClasspath(self):
        classpath = []

        # The default
        classpath.append(os.path.normpath(os.path.join(self.path, 'conf')))
        classpath.append(os.path.normpath(os.path.join(self.play_env["basedir"], 'framework/play-%s.jar' % self.play_env['version'])))

        # The application - recursively add jars to the classpath inside the lib folder to allow for subdirectories
        if os.path.exists(os.path.join(self.path, 'lib')):
            self.find_and_add_all_jars(classpath, os.path.join(self.path, 'lib'))

        # The modules
        for module in self.modules():
            if os.path.exists(os.path.join(module, 'lib')):
                libs = os.path.join(module, 'lib')
                if os.path.exists(libs):
                    for jar in os.listdir(libs):
                        if jar.endswith('.jar'):
                            classpath.append(os.path.normpath(os.path.join(libs, '%s' % jar)))

        # The framework
        for jar in os.listdir(os.path.join(self.play_env["basedir"], 'framework/lib')):
            if jar.endswith('.jar'):
                classpath.append(os.path.normpath(os.path.join(self.play_env["basedir"], 'framework/lib/%s' % jar)))

        return classpath

    def getFrameworkClasspath(self):
        classpath = []

        # The default
        classpath.append(os.path.normpath(os.path.join(self.path, 'conf')))
        classpath.append(os.path.normpath(os.path.join(self.play_env["basedir"], 'framework/play-%s.jar' % self.play_env['version'])))

        # The framework
        for jar in os.listdir(os.path.join(self.play_env["basedir"], 'framework/lib')):
            if jar.endswith('.jar'):
                classpath.append(os.path.normpath(os.path.join(self.play_env["basedir"], 'framework/lib/%s' % jar)))

        return classpath

    def agent_path(self):
        return os.path.join(self.play_env["basedir"], 'framework/play-%s.jar' % self.play_env['version'])

    def cp_args(self):
        classpath = self.getClasspath()
        cp_args = ':'.join(classpath)
        if os.name == 'nt':
            cp_args = ';'.join(classpath)
        return cp_args

    def fw_cp_args(self):
        classpath = self.getFrameworkClasspath()
        cp_args = ':'.join(classpath)
        if os.name == 'nt':
            cp_args = ';'.join(classpath)
        return cp_args

    def pid_path(self):
        if self.play_env.has_key('pid_file'):
            return os.path.join(self.path, self.play_env['pid_file'])
        elif os.environ.has_key('PLAY_PID_PATH'):
            return os.environ['PLAY_PID_PATH']
        else:
            return os.path.join(self.path, 'server.pid')

    def log_path(self):
        if not os.environ.has_key('PLAY_LOG_PATH'):
            log_path = os.path.join(self.path, 'logs')
        else:
            log_path = os.environ['PLAY_LOG_PATH']
        if not os.path.exists(log_path):
            os.mkdir(log_path)
        return log_path

    def check_jpda(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.bind(('', int(self.jpda_port)))
            s.close()
        except socket.error, e:
            if "disable_random_jpda" in self.play_env and self.play_env["disable_random_jpda"]:
                print 'JPDA port %s is already used, and command line option "-f" was specified. Cannot start server\n' % self.jpda_port
                sys.exit(-1)
            else:
                print 'JPDA port %s is already used. Will try to use any free port for debugging' % self.jpda_port
                self.jpda_port = 0

    def java_args_memory(self, java_args):
        args_memory = []
        memory_in_args=False    
        for arg in java_args:
            if arg.startswith('-Xm'):
                memory_in_args=True
                args_memory.append(arg)
            
        if not memory_in_args:
            memory = self.readConf('jvm.memory')
            if memory:
                args_memory = args_memory + memory.split(' ')
            elif 'JAVA_OPTS' in os.environ:
                args_memory = args_memory + os.environ['JAVA_OPTS'].split(' ')
                
        return args_memory        
    
    def java_cmd(self, java_args, cp_args=None, className='play.server.Server', args = None):
        if args is None:
            args = ['']
        memory_in_args=False
        for arg in java_args:
            if arg.startswith('-Xm'):
                memory_in_args=True
        if not memory_in_args:
            memory = self.readConf('jvm.memory')
            if memory:
                java_args = java_args + memory.split(' ')
            elif 'JAVA_OPTS' in os.environ:
                java_args = java_args + os.environ['JAVA_OPTS'].split(' ')
        if cp_args is None:
            cp_args = self.cp_args()

        if self.play_env.has_key('jpda.port'):
            self.jpda_port = self.play_env['jpda.port']

        application_mode = self.readConf('application.mode').lower()
        if not application_mode:
            print "~ Warning: no application.mode defined in you conf/application.conf. Using DEV mode."
            application_mode = "dev"


        if application_mode == 'prod':
            java_args.append('-server')

        if self.play_env.has_key('jvm_version'):
            javaVersion = self.play_env['jvm_version']
        else:
            javaVersion = getJavaVersion() 
        print "~ using java version \"%s\"" % javaVersion
        
        if javaVersion.startswith("1.5") or javaVersion.startswith("1.6") or javaVersion.startswith("1.7"):
            print "~ ERROR: java version prior to 1.8 are no longer supported: current version \"%s\" : please update" % javaVersion
            
        java_args.append('-noverify')

        java_policy = self.readConf('java.policy')
        if java_policy != '':
            policyFile = os.path.join(self.path, 'conf', java_policy)
            if os.path.exists(policyFile):
                print "~ using policy file \"%s\"" % policyFile
                java_args.append('-Djava.security.manager')
                java_args.append('-Djava.security.policy==%s' % policyFile)

        if self.play_env.has_key('http.port'):
            args += ["--http.port=%s" % self.play_env['http.port']]
        if self.play_env.has_key('https.port'):
            args += ["--https.port=%s" % self.play_env['https.port']]
            
        java_args.append('-Dfile.encoding=utf-8')

        if application_mode == 'dev':
            self.check_jpda()
            java_args.append('-Xdebug')
            java_args.append('-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n' % self.jpda_port)
            java_args.append('-Dplay.debug=yes')
        
        java_cmd = [java_path(), '-javaagent:%s' % self.agent_path()] + java_args + ['-classpath', cp_args, '-Dapplication.path=%s' % self.path, '-Dplay.id=%s' % self.play_env["id"], className] + args
        return java_cmd

    # ~~~~~~~~~~~~~~~~~~~~~~ MISC

    def toRelative(self, path):
        return _absoluteToRelative(path, self.path).replace("//", "/")

def _absoluteToRelative(path, start):
    """Return a relative version of a path"""
    # Credit - http://pypi.python.org/pypi/BareNecessities/0.2.8
    if not path:
        raise ValueError("no path specified")
    start_list = os.path.abspath(start).split(os.path.sep)
    path_list = os.path.abspath(path).split(os.path.sep)
    # Work out how much of the filepath is shared by start and path.
    i = len(os.path.commonprefix([start_list, path_list]))
    rel_list = [os.path.pardir] * (len(start_list)-i) + path_list[i:]
    if not rel_list:
        return os.path.curdir
    return os.path.join(*rel_list)

class PlayConfParser:

    DEFAULTS = {
        'http.port': '9000',
        'jpda.port': '8000'
    }

    def __init__(self, confFolder, env):
        self.id = env["id"]
        self.entries = self.readFile(confFolder, "application.conf")
        if env.has_key('jpda.port'):
            self.entries['jpda.port'] = env['jpda.port']
        if env.has_key('http.port'):
            self.entries['http.port'] = env['http.port']
        if env.has_key('jvm_version'):
            self.entries['jvm_version'] = env['jvm_version']

    def readFile(self, confFolder, filename):
        f = file(confFolder + filename)
        result = dict()
        for line in f:
            linedef = line.strip()
            if len(linedef) == 0:
                continue
            if linedef[0] in ('!', '#'):
                continue
            if linedef.find('=') == -1:
                continue
            key = linedef.split('=')[0].strip()
            value = linedef[(linedef.find('=')+1):].strip()
            result[key] = value
        f.close()
        
        # minimize the result based on frameworkId
        washedResult = dict()
        
        # first get all keys with correct framework id
        for (key, value) in result.items():
            if key.startswith('%' + self.id + '.'):
                stripedKey = key[(len(self.id)+2):]
                washedResult[stripedKey]=value
        # now get all without framework id if we don't already have it
        for (key, value) in result.items():
            if not key.startswith('%'):
                # check if we already have it
                if not (key in washedResult):
                    # add it
                    washedResult[key]=value
                    
        # find all @include
        includeFiles = []
        for (key, value) in washedResult.items():
            if key.startswith('@include.'):
                includeFiles.append(value)
                
        # process all include files
        for includeFile in includeFiles:
            # read include file
            try:
                fromIncludeFile = self.readFile(confFolder, self._expandValue(includeFile))

                # add everything from include file 
                for (key, value) in fromIncludeFile.items():
                    washedResult[key]=value
            except Exception as err:
                print "~ Failed to load included configuration %s: %s" % (includeFile, err)
        
        return washedResult

    def get(self, key):
        if key in self.entries:
            return self.entries[key]
        if key in self.DEFAULTS:
            return self.DEFAULTS[key]
        return ''

    def getAllKeys(self, query):
        result = []
        for (key, value) in self.entries.items():
            if key.startswith(query):
                result.append(key)
        return result

    def getAll(self, query):
        result = []
        for key in self.getAllKeys(query):
            result.append(self.entries.get(key))
        return result

    def _expandValue(self, value):
        def expandvar(match):
            key = match.group(1)
            if key == 'play.id':
                return self.id
            else: # unkonwn
                return '${%s}' % key

        return re.sub('\${([a-z.]+)}', expandvar, value)
        
def hasKey(arr, elt):
    try:
        i = arr.index(elt)
        return True
    except:
        return False
