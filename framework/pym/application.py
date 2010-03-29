import os
import os.path
import re
import socket

class ModuleNotFound(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)

class PlayApplication():
    """A Play Application: conf file, java"""

    DEFAULTS = {
        'http_port': '9000',
        'jpda_port': '8000'
    }

    # ~~~~~~~~~~~~~~~~~~~~~~ Constructor

    def __init__(self, application_path, play_base, play_id):
        self.path = application_path
        if application_path is not None:
            self.confpath = os.path.join(application_path, 'conf/application.conf')
        else:
            self.confpath = None
        self.play_id = play_id
        self.play_base = play_base
        self.jpda_port = self.readConf('jpda_port')

    # ~~~~~~~~~~~~~~~~~~~~~~ Configuration File

    def check(self):
        if not os.path.exists(os.path.join(self.path, 'conf/routes')):
            print "~ Oops. %s does not seem to host a valid application" % os.path.normpath(application_path)
            print "~"
            sys.exit(-1)

    def readConf(self, key):
        if (self.confpath is None):
            return ''
        for keyRe in [re.compile('^%' + self.play_id + '.' + key + '\s*='), re.compile('^' + key + '\s*=')]:
            for line in open(self.confpath).readlines():
                if keyRe.match(line):
                    return line[line.find('=')+1:].strip()
        if key in self.DEFAULTS:
            return self.DEFAULTS[key]
        return ''

    def readConfs(self, key):
        result = []
        for line in open(self.confpath).readlines():
            if line.startswith(key):
                result.append(line[line.find('=')+1:].strip())
        for line in open(self.confpath).readlines():
            if line.startswith('%' + self.play_id + '.' + key):
                result.append(line[line.find('=')+1:].strip())
        return result

    def modules(self):
        modules = []
        for m in self.readConfs('module.'):
            om = m
            if '${play.path}' in m:
                m = m.replace('${play.path}', self.play_base)
            if not m[0] == '/':
                m = os.path.normpath(os.path.join(application_path, m))
            if not os.path.exists(m):
                raise ModuleNotFound(m)
            modules.append(m)
        return modules

    # ~~~~~~~~~~~~~~~~~~~~~~ JAVA

    def getClasspath(self):
        classpath = []

        # The default
        classpath.append(os.path.normpath(os.path.join(self.path, 'conf')))
        classpath.append(os.path.normpath(os.path.join(self.play_base, 'framework/play.jar')))

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
        for jar in os.listdir(os.path.join(self.play_base, 'framework/lib')):
            if jar.endswith('.jar'):
                classpath.append(os.path.normpath(os.path.join(self.play_base, 'framework/lib/%s' % jar)))

        return classpath

    def agent_path(self):
        return os.path.join(self.play_base, 'framework/play.jar')

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
            log_path = os.path.join(application_path, 'logs');
        else:
            log_path = os.environ['PLAY_LOG_PATH'];
        if not os.path.exists(log_path):
            os.mkdir(log_path);
        return log_path

    def check_jpda(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.bind(('127.0.0.1', int(self.jpda_port)))
            s.close()
        except socket.error, e:
            print 'JPDA port %s is already used. Will try to use any free port for debugging' % self.jpda_port
            self.jpda_port = 0

    def java_cmd(self, java_args, className='play.server.Server'):
        memory_in_args=False
        for arg in java_args:
            if arg.startswith('-Xm'):
                memory_in_args=True
        if not memory_in_args:
            memory = self.readConf('jvm.memory')
            if memory:
                java_args = java_args + memory.split(' ')

        jpda_port = self.readConf('jpda.port')

        application_mode = self.readConf('application.mode')

        if application_mode == 'prod':
            java_args.append('-server')

        java_policy = self.readConf('java.policy')
        if not java_policy == '':
            policyFile = os.path.join(self.path, 'conf', java_policy)
            if os.path.exists(policyFile):
                print "~ using policy file \"%s\"" % policyFile
                java_args.append('-Djava.security.manager')
                java_args.append('-Djava.security.policy==%s' % policyFile)

        java_cmd = [self.java_path(), '-javaagent:%s' % self.agent_path()] + java_args + ['-classpath', self.cp_args(), '-Dapplication.path=%s' % self.path, '-Dplay.id=%s' % self.play_id, className]
        return java_cmd

