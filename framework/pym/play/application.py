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

class PlayApplication:
    """A Play Application: conf file, java"""

    # ~~~~~~~~~~~~~~~~~~~~~~ Constructor

    def __init__(self, application_path, env, ignoreMissingModules = False):
        self.path = application_path
        if application_path is not None:
            confpath = os.path.join(application_path, 'conf/application.conf')
            try:
                self.conf = PlayConfParser(confpath, env["id"], application_path)
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
            om = m
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

    def load_modules(self):
        if os.environ.has_key('MODULES'):
            if os.name == 'nt':
                modules = os.environ['MODULES'].split(';')
            else:
                modules = os.environ['MODULES'].split(':')
        else:
            modules = []

        if play_app is not None:
            try:
                modules = play_app.modules()
            except ModuleNotFound, e:
                print 'Module not found %s' % e
                sys.exit(-1)

            if isTestFrameworkId(play_env["id"]):
                modules.append(os.path.normpath(os.path.join(play_env["basedir"], 'modules/testrunner')))

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

    def getClasspath(self):
        classpath = []

        # The default
        classpath.append(os.path.normpath(os.path.join(self.path, 'conf')))
        classpath.append(os.path.normpath(os.path.join(self.play_env["basedir"], 'framework/play-%s.jar' % self.play_env['version'])))

        # The application
        if os.path.exists(os.path.join(self.path, 'lib')):
            for jar in os.listdir(os.path.join(self.path, 'lib')):
                if jar.endswith('.jar'):
                    classpath.append(os.path.normpath(os.path.join(self.path, 'lib/%s' % jar)))

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

    def agent_path(self):
        return os.path.join(self.play_env["basedir"], 'framework/play-%s.jar' % self.play_env['version'])

    def cp_args(self):
        classpath = self.getClasspath()
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
            return os.path.join(self.path, self.play_env['pid_file']);
        elif os.environ.has_key('PLAY_PID_PATH'):
            return os.environ['PLAY_PID_PATH'];
        else:
            return os.path.join(self.path, 'server.pid');

    def log_path(self):
        if not os.environ.has_key('PLAY_LOG_PATH'):
            log_path = os.path.join(self.path, 'logs');
        else:
            log_path = os.environ['PLAY_LOG_PATH'];
        if not os.path.exists(log_path):
            os.mkdir(log_path);
        return log_path

    def check_jpda(self):
        self.jpda_port = self.readConf('jpda.port')
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.bind(('127.0.0.1', int(self.jpda_port)))
            s.close()
        except socket.error, e:
            print 'JPDA port %s is already used. Will try to use any free port for debugging' % self.jpda_port
            self.jpda_port = 0

    def java_cmd(self, java_args, cp_args=None, className='play.server.Server', args=['']):
        memory_in_args=False
        for arg in java_args:
            if arg.startswith('-Xm'):
                memory_in_args=True
        if not memory_in_args:
            memory = self.readConf('jvm.memory')
            if memory:
                java_args = java_args + memory.split(' ')
        if cp_args is None:
            cp_args = self.cp_args()

        self.jpda_port = self.readConf('jpda.port')

        application_mode = self.readConf('application.mode')

        if application_mode == 'prod':
            java_args.append('-server')

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

        if self.readConf('application.mode') == 'dev':
            if not self.play_env["disable_check_jpda"]: self.check_jpda()
            java_args.append('-Xdebug')
            java_args.append('-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n' % self.jpda_port)
            java_args.append('-Dplay.debug=yes')
        
        java_cmd = [self.java_path(), '-javaagent:%s' % self.agent_path()] + java_args + ['-classpath', cp_args, '-Dapplication.path=%s' % self.path, '-Dplay.id=%s' % self.play_env["id"], className] + args
        return java_cmd

class PlayConfParser:

    DEFAULTS = {
        'http.port': '9000',
        'jpda.port': '8000'
    }

    def __init__(self, filepath, frameworkId, application_path):
        self.id = frameworkId
        self.entries = dict()
        self.readFile(filepath, application_path)

    def readFile(self, filepath, application_path):
	try:
            f = file(filepath)
            for line in f:
                linedef = line.strip()
                if len(linedef) == 0:
                    continue
                if linedef[0] in ('!', '#'):
                    continue
                sep = linedef.find('=');
                if sep == -1:
                    continue
                key = linedef.split('=')[0].strip()
                val = linedef[(sep+1):].lstrip();
                if linedef.find('@include.') == 0:
                    val = os.path.join(application_path, 'conf', val)
                    self.readFile(val, application_path)
                else:
                    self.entries[key] = val
            f.close()
        except IOError, x:
            print "WARNING: Failed to read configuration from %s %s" % (filepath, x)

    def get(self, key):
        idkey = '%' + self.id + "." + key
        if idkey in self.entries:
            return self.entries[idkey]
        if key in self.entries:
            return self.entries[key]
        if key in self.DEFAULTS:
            return self.DEFAULTS[key]
        return ''

    def getAllKeys(self, query):
        # We need to take both naked and with id,
        # BUT an entry with id should override the naked one
        # Ex:
        #   module.foo = "foo"
        #   module.bar = "bar"
        #   %dev.module.bar = "bar2"
        #     => ["module.foo", "%dev.module.bar"]
        result = []
        for (key, value) in self.entries.items():
            if key.startswith('%' + self.id + '.' + query):
                result.append(key)
        for (key, value) in self.entries.items():
            if key.startswith(query) and not hasKey(result, '%' + self.id + "." + key):
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
        return True;
    except:
        return False;
