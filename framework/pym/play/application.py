import sys
import os, os.path
import re
import shutil
import socket

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
                self.conf = PlayConfParser(confpath, env["id"])
            except:
                self.conf = None # No app / Invalid app
        else:
            self.conf = None
        self.play_env = env
        self.jpda_port = self.readConf('jpda_port')
        self.ignoreMissingModules = ignoreMissingModules

    # ~~~~~~~~~~~~~~~~~~~~~~ Configuration File

    def check(self):
        if not os.path.exists(os.path.join(self.path, 'conf', 'routes')):
            print "~ Oops. %s does not seem to host a valid application" % os.path.normpath(self.path)
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
        if self.play_env["id"] == 'test':
            modules.append(os.path.normpath(os.path.join(self.play_env["basedir"], 'modules/testrunner')))
        return modules

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

            if play_env["id"] == 'test':
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
        classpath.append(os.path.normpath(os.path.join(self.play_env["basedir"], 'framework/play.jar')))

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
        return os.path.join(self.play_env["basedir"], 'framework/play.jar')

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
        if os.environ.has_key('PLAY_PID_PATH'):
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

        java_cmd = [self.java_path(), '-javaagent:%s' % self.agent_path()] + java_args + ['-classpath', cp_args, '-Dapplication.path=%s' % self.path, '-Dplay.id=%s' % self.play_env["id"], className] + args
        return java_cmd

class PlayConfParser:

    DEFAULTS = {
        'http.port': '9000',
        'jpda.port': '8000'
    }

    def __init__(self, filepath, frameworkId):
        self.id = frameworkId
        f = file(filepath)
        self.entries = dict()
        for line in f:
            linedef = line.strip()
            if len(linedef) == 0:
                continue
            if linedef[0] in ('!', '#'):
                continue
            if linedef.find('=') == -1:
                continue
            self.entries[linedef.split('=')[0].rstrip()] = linedef.split('=')[1].lstrip()
        f.close()

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
