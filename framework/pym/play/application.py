import sys
import os, os.path
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
        if application_path is not None:
            confFolder = os.path.join(application_path, 'conf/')
            try:
                self.conf = PlayConfParser(confFolder, env)
            except:
                self.conf = None # No app / Invalid app
        else:
            self.conf = None
        self.play_env = env
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
            response = raw_input("~ Warning! %s already exists and will be overriden (y/n)? " % toFile)
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


    def java_path(self):
        if not os.environ.has_key('JAVA_HOME'):
            return "java"
        else:
            return os.path.normpath("%s/bin/java" % os.environ['JAVA_HOME'])

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
        self.jpda_port = self.readConf('jpda.port')
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.bind(('', int(self.jpda_port)))
            s.close()
        except socket.error, e:
            print 'JPDA port %s is already used. Will try to use any free port for debugging' % self.jpda_port
            self.jpda_port = 0

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

        self.jpda_port = self.readConf('jpda.port')

        application_mode = self.readConf('application.mode').lower()

        if application_mode == 'prod':
            java_args.append('-server')
	# JDK 7 compat
	java_args.append('-XX:-UseSplitVerifier')
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
        java_args.append('-XX:CompileCommand=exclude,jregex/Pretokenizer,next')

        if self.readConf('application.mode').lower() == 'dev':
            if not self.play_env["disable_check_jpda"]: self.check_jpda()
            java_args.append('-Xdebug')
            java_args.append('-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n' % self.jpda_port)
            java_args.append('-Dplay.debug=yes')
        
        java_cmd = [self.java_path(), '-javaagent:%s' % self.agent_path()] + java_args + ['-classpath', cp_args, '-Dapplication.path=%s' % self.path, '-Dplay.id=%s' % self.play_env["id"], className] + args
        return java_cmd

    # ~~~~~~~~~~~~~~~~~~~~~~ MISC

    def toRelative(self, path):
        return _absoluteToRelative(path, self.path, "").replace("//", "/")

def _absoluteToRelative(path, reference, dots):
    if path.find(reference) > -1:
        ending = path.find(reference) + len(reference)
        return dots + path[ending:]
    else:
        return _absoluteToRelative(path, os.path.dirname(reference), "/.." + dots)

class PlayConfParser:

    DEFAULTS = {
        'http.port': '9000',
        'jpda.port': '8000'
    }

    def __init__(self, confFolder, env):
        self.id = env["id"]
        self.entries = self.readFile(confFolder, "application.conf")
        if env.has_key('http.port'):
            self.entries['http.port'] = env['http.port']

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
            fromIncludeFile = self.readFile(confFolder, includeFile)

            # add everything from include file 
            for (key, value) in fromIncludeFile.items():
                washedResult[key]=value
        
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

def hasKey(arr, elt):
    try:
        i = arr.index(elt)
        return True
    except:
        return False
