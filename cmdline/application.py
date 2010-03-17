import os
import os.path
import re

class ModuleNotFound(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)

class PlayApplication():
    """Parse and access Play configuration file"""

    defaults = {
        'http_port': '9000'
    }

    def __init__(self, application_path, play_base, play_id):
        self.confpath = os.path.join(application_path, 'conf/application.conf')
        self.play_id = play_id
        self.play_base = play_base

    def check():
        if not os.path.exists(os.path.join(self.application_path, 'conf/routes')):
            print "~ Oops. %s does not seem to host a valid application" % os.path.normpath(application_path)
            print "~"
            sys.exit(-1)

    def readConf(self, key):
        for keyRe in [re.compile('^%' + self.play_id + '.' + key + '\s*='), re.compile('^' + key + '\s*=')]:
            for line in open(self.confpath).readlines():
                if keyRe.match(line):
                    return line[line.find('=')+1:].strip()
        if key in defaults:
            return defaults[key]
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
