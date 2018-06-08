import os
import subprocess
import sys
import re
import zipfile
import urllib2
import shutil
import string
import imp
import time
import urllib
import yaml

from play.utils import *

NM = ['new-module', 'nm']
LM = ['list-modules', 'lm']
BM = ['build-module', 'bm']
AM = ['add']
IM = ['install']

COMMANDS = NM + LM + BM + IM + AM

HELP = {
    'new-module': "Create a module",
    'build-module': "Build and package a module",
    'list-modules': "List modules available from the central modules repository",
    'install': "Install a module"
}

DEFAULT_REPO = 'https://www.playframework.com'

DEFAULT_USER_AGENT = 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.7) Gecko/2009021910 Firefox/3.0.7'

def load_module(name):
    base = os.path.normpath(os.path.dirname(os.path.realpath(sys.argv[0])))
    mod_desc = imp.find_module(name, [os.path.join(base, 'framework/pym')])
    return imp.load_module(name, mod_desc[0], mod_desc[1], mod_desc[2])

json = load_module('simplejson')

repositories = []

def execute(**kargs):
    global repositories

    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    repositories = get_repositories(env['basedir'])

    if command in NM:
        new(app, args, env)
    elif command in LM:
        list(app, args)
    elif command in BM:
        build(app, args, env)
    elif command in IM:
        install(app, args, env)
    elif command in AM:
        add(app, args, env)

def get_repositories(play_base):
    repopath = os.path.join(play_base, 'repositories')
    if os.path.exists(repopath):
        repos = []
        f = file(repopath)
        for line in f:
            if not re.match("^\s*#", line) and not line.strip() == "":
                repos.append(line.strip())
        if len(repos) > 0:
            return repos
    return [DEFAULT_REPO]

class Downloader(object):
    before = .0
    history = []
    cycles = 0
    average = lambda self: sum(self.history) / (len(self.history) or 1)

    def __init__(self, width=55):
        self.width = width
        self.kibi = lambda bits: bits / 2 ** 10
        self.proc = lambda a, b: a / (b * 0.01)

    def retrieve(self, url, destination, callback=None):
        self.size = 0
        time.clock()   
        try:
          headers={'User-Agent':DEFAULT_USER_AGENT,
                  'Accept': 'application/json'
          } 
          req = urllib2.Request(url, headers=headers)
          result = urllib2.urlopen(req)
          self.chunk_read(result, destination, report_hook=self.chunk_report)        
        except KeyboardInterrupt:
            print '\n~ Download cancelled'
            print '~'
            for i in range(5):
                try:
                    os.remove(destination)
                    break
                except:
                    time.sleep(.1)
            else: raise
            if callback: callback()
            sys.exit()
        print ''
        return self.size

    def chunk_read(self, response, destination, chunk_size=8192, report_hook=None):
        total_size = response.info().getheader('Content-Length').strip()
        total_size = int(total_size)
        bytes_so_far = 0
        file = open(destination,"wb")

        while 1:
            chunk = response.read(chunk_size)
            file.write(chunk)
            bytes_so_far += len(chunk)

            if not chunk:
                break

            if report_hook:
                #report_hook(bytes_so_far, chunk_size, total_size)
                self.progress(bytes_so_far, chunk_size, total_size)

        return bytes_so_far
        
        
    def chunk_report(self, bytes_so_far, chunk_size, total_size):
      percent = float(bytes_so_far) / total_size
      percent = round(percent*100, 2)
      sys.stdout.write("Downloaded %d of %d bytes (%0.2f%%)\r" % (bytes_so_far, total_size, percent))
      if bytes_so_far >= total_size:
          sys.stdout.write('\n')

        
    def progress(self, bytes_so_far, blocksize, filesize):
        self.cycles += 1
        bits = min(bytes_so_far, filesize)
        if bits != filesize:
            done = self.proc(bits, filesize)
        else:
            done = 100
        bar = self.bar(bytes_so_far, filesize, done)
        if not self.cycles % 3 and bits != filesize:
            now = time.clock()
            elapsed = now-self.before
            if elapsed:
                speed = self.kibi(blocksize * 3 / elapsed)
                self.history.append(speed)
                self.history = self.history[-4:]
            self.before = now
        average = round(sum(self.history[-4:]) / 4, 1)
        self.size = self.kibi(bits)
        print '\r~ [%s] %s KiB/s  ' % (bar, str(average)),

    def bar(self, bytes_so_far, filesize, done):
        span = self.width * done * 0.01
        offset = len(str(int(done))) - .99
        result = ('%s of %s KiB (%d%%)' % (self.kibi(bytes_so_far), self.kibi(filesize), done,)).center(self.width)
        return result.replace(' ', '-', int(span - offset))

class Unzip:
    def __init__(self, verbose = False, percent = 10):
        self.verbose = verbose
        self.percent = percent

    def extract(self, file, dir):
        if not dir.endswith(':') and not os.path.exists(dir):
            os.mkdir(dir)
        zf = zipfile.ZipFile(file)
        # create directory structure to house files
        self._createstructure(file, dir)
        num_files = len(zf.namelist())
        percent = self.percent
        divisions = 100 / percent
        perc = int(num_files / divisions)
        # extract files to directory structure
        for i, name in enumerate(zf.namelist()):
            if self.verbose == True:
                print "Extracting %s" % name
            elif perc > 0 and (i % perc) == 0 and i > 0:
                complete = int (i / perc) * percent
            if not name.endswith('/'):
                outfile = open(os.path.join(dir, name), 'wb')
                try:
                    outfile.write(zf.read(name))
                    outfile.flush()
                finally:
                    outfile.close()

    def _createstructure(self, file, dir):
        self._makedirs(self._listdirs(file), dir)

    def _makedirs(self, directories, basedir):
        """ Create any directories that don't currently exist """
        for dir in directories:
            curdir = os.path.join(basedir, dir)
            if not os.path.exists(curdir):
                os.makedirs(curdir)

    def _listdirs(self, file):
            """ Grabs all the directories in the zip structure
            This is necessary to create the structure before trying
            to extract the file to it. """
            zf = zipfile.ZipFile(file)
            dirs = []
            for name in zf.namelist():
                    dn = os.path.dirname(name)
                    dirs.append(dn)
            dirs.sort()
            return dirs


def new(app, args, play_env):
    if os.path.exists(app.path):
        print "~ Oops. %s already exists" % app.path
        print "~"
        sys.exit(-1)

    print "~ The new module will be created in %s" % os.path.normpath(app.path)
    print "~"
    application_name = os.path.basename(app.path)
    copy_directory(os.path.join(play_env["basedir"], 'resources/module-skel'), app.path)
    # check_application()
    replaceAll(os.path.join(app.path, 'build.xml'), r'%MODULE%', application_name)
    replaceAll(os.path.join(app.path, 'commands.py'), r'%MODULE%', application_name)
    replaceAll(os.path.join(app.path, 'conf/messages'), r'%MODULE%', application_name)
    replaceAll(os.path.join(app.path, 'conf/dependencies.yml'), r'%MODULE%', application_name)
    replaceAll(os.path.join(app.path, 'conf/routes'), r'%MODULE%', application_name)
    replaceAll(os.path.join(app.path, 'conf/routes'), r'%MODULE_LOWERCASE%', string.lower(application_name))
    os.mkdir(os.path.join(app.path, 'app'))
    os.mkdir(os.path.join(app.path, 'app/controllers'))
    os.mkdir(os.path.join(app.path, 'app/controllers/%s' % application_name))
    os.mkdir(os.path.join(app.path, 'app/models'))
    os.mkdir(os.path.join(app.path, 'app/models/%s' % application_name))
    os.mkdir(os.path.join(app.path, 'app/views'))
    os.mkdir(os.path.join(app.path, 'app/views/%s' % application_name))
    os.mkdir(os.path.join(app.path, 'app/views/tags'))
    os.mkdir(os.path.join(app.path, 'app/views/tags/%s' % application_name))
    os.mkdir(os.path.join(app.path, 'src/play'))
    os.mkdir(os.path.join(app.path, 'src/play/modules'))
    os.mkdir(os.path.join(app.path, 'src/play/modules/%s' % application_name))

    print "~ OK, the module is created."
    print "~ Start using it by adding it to the dependencies.yml of your project, as decribed in the documentation."
    print "~"
    print "~ Have fun!"
    print "~"


def list(app, args):
    print "~ You can also browse this list online at:"
    for repo in repositories:
        print "~    %s/modules" % repo
    print "~"

    modules_list = load_module_list()

    for mod in modules_list:
        print "~ [%s]" % mod['name']
        print "~   %s" % mod['fullname']
        print "~   %s/modules/%s" % (mod['server'], mod['name'])

        vl = ''
        i = 0
        for v in mod['versions']:
            vl += v["version"]
            i = i+1
            if i < len(mod['versions']):
                vl += ', '

        if vl:
            print "~   Versions: %s" % vl
        else:
            print "~   (No versions released yet)"
        print "~"

    print "~ To install one of these modules use:"
    print "~ play install module-version (eg: play install scala-1.0)"
    print "~"
    print "~ Or you can just install the default release of a module using:"
    print "~ play install module (eg: play install scala)"
    print "~"


def build(app, args, env):
    ftb = env["basedir"]
    version = None
    name = None
    fwkMatch = None
    origModuleDefinition = None

    try:
        optlist, args = getopt.getopt(args, '', ['framework=', 'version=', 'require='])
        for o, a in optlist:
            if o in ('--framework'):
                ftb = a
            if o in ('--version'):
                version = a
            if o in ('--require'):
                fwkMatch = a
    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ "
        sys.exit(-1)

    deps_file = os.path.join(app.path, 'conf', 'dependencies.yml')
    if os.path.exists(deps_file):
        f = open(deps_file)
        try:
            deps = yaml.load(f.read())
            if 'self' in deps:
               splitted = deps["self"].split(" -> ")
               if len(splitted) == 2:
                    nameAndVersion = splitted.pop().strip()
                    splitted = nameAndVersion.split(" ")
                    if len(splitted) == 2:
                       version = splitted.pop()
                       name = splitted.pop()
            for dep in deps["require"]:
                if isinstance(dep, basestring):
                    splitted = dep.split(" ")
                    if len(splitted) == 2 and splitted[0] == "play":
                        fwkMatch = splitted[1]
        finally:
            f.close()

    if name is None:
        name = os.path.basename(app.path)
    if version is None:
        version = raw_input("~ What is the module version number? ")
    if fwkMatch is None:
        fwkMatch = raw_input("~ What are the playframework versions required? ")

    if os.path.exists(deps_file):
        f = open(deps_file)
        deps = yaml.load(f.read())
        if 'self' in deps:
           splitted = deps["self"].split(" -> ")
           f.close()
           if len(splitted) == 2:
               nameAndVersion = splitted.pop().strip()
               splitted = nameAndVersion.split(" ")
               if len(splitted) == 1:
                  try:
                    deps = open(deps_file).read()
                    origModuleDefinition = re.search(r'self:\s*(.*)\s*', deps).group(1)
                    modifiedModuleDefinition = '%s %s' % (origModuleDefinition, version)
                    replaceAll(deps_file, origModuleDefinition, modifiedModuleDefinition)
                  except:
                    pass

    build_file = os.path.join(app.path, 'build.xml')
    if os.path.exists(build_file):
        print "~"
        print "~ Building..."
        print "~"
        status = subprocess.call('ant -f %s -Dplay.path=%s' % (build_file, ftb), shell=True)
        print "~"
        if status:
            sys.exit(status)

    mv = '%s-%s' % (name, version)
    print("~ Packaging %s ... " % mv)

    dist_dir = os.path.join(app.path, 'dist')
    if os.path.exists(dist_dir):
        shutil.rmtree(dist_dir)
    os.mkdir(dist_dir)

    manifest = os.path.join(app.path, 'manifest')
    manifestF = open(manifest, 'w')
    try:
        manifestF.write('version=%s\nframeworkVersions=%s\n' % (version, fwkMatch))
    finally:
        manifestF.close()

    zip = zipfile.ZipFile(os.path.join(dist_dir, '%s.zip' % mv), 'w', zipfile.ZIP_STORED)
    try:
        for (dirpath, dirnames, filenames) in os.walk(app.path):
            if dirpath == dist_dir:
                continue
            if dirpath.find(os.sep + '.') > -1 or dirpath.find('/tmp/') > -1  or dirpath.find('/test-result/') > -1 or dirpath.find('/logs/') > -1 or dirpath.find('/eclipse/') > -1 or dirpath.endswith('/test-result') or dirpath.endswith('/logs')  or dirpath.endswith('/eclipse') or dirpath.endswith('/nbproject'):
                continue
            for file in filenames:
                if file.find('~') > -1 or file.endswith('.iml') or file.startswith('.'):
                    continue
                zip.write(os.path.join(dirpath, file), os.path.join(dirpath[len(app.path):], file))
    finally:
        zip.close()

    os.remove(manifest)
    
    # Reset the module definition
    if origModuleDefinition:
        try:
            replaceAll(deps_file, '%s %s' % (origModuleDefinition, version), origModuleDefinition)
        except:
            pass

    print "~"
    print "~ Done!"
    print "~ Package is available at %s" % os.path.join(dist_dir, '%s.zip' % mv)
    print "~"


def install(app, args, env):
    if len(sys.argv) < 3:
        help_file = os.path.join(env["basedir"], 'documentation/commands/cmd-install.txt')
        print open(help_file, 'r').read()
        sys.exit(0)

    name = cmd = sys.argv[2]
    groups = re.match(r'^([a-zA-Z0-9_]+)([-](.*))?$', name)
    module = groups.group(1)
    version = groups.group(3)

    server = None
    if args is not None:
        for param in args:
            if param.startswith("--force-server="):
                server = param[15:]
    modules_list = load_module_list(server)
    fetch = None

    for mod in modules_list:
        if mod['name'] == module:
            for v in mod['versions']:
                if version is None and v['isDefault']:
                    print '~ Will install %s-%s' % (module, v['version'])
                    print '~ This module is compatible with: %s' % v['matches']
                    ok = raw_input('~ Do you want to install this version (y/n)? ')
                    if not ok == 'y':
                        print '~'
                        sys.exit(-1)
                    print '~ Installing module %s-%s...' % (module, v['version'])
                    fetch = '%s/modules/%s-%s.zip' % (mod['server'], module, v['version'])
                    break
                if version  == v['version']:
                    print '~ Will install %s-%s' % (module, v['version'])
                    print '~ This module is compatible with: %s' % v['matches']
                    ok = raw_input('~ Do you want to install this version (y/n)? ')
                    if not ok == 'y':
                        print '~'
                        sys.exit(-1)

                    print '~ Installing module %s-%s...' % (module, v['version'])
                    fetch = '%s/modules/%s-%s.zip' % (mod['server'], module, v['version'])
                    break

    if fetch is None:
        print '~ No module found \'%s\'' % name
        print '~ Try play list-modules to get the modules list'
        print '~'
        sys.exit(-1)

    archive = os.path.join(env["basedir"], 'modules/%s-%s.zip' % (module, v['version']))
    if os.path.exists(archive):
        os.remove(archive)

    print '~'
    print '~ Fetching %s' % fetch

    Downloader().retrieve(fetch, archive)

    if not os.path.exists(archive):
        print '~ Oops, file does not exist'
        print '~'
        sys.exist(-1)

    print '~ Unzipping...'

    if os.path.exists(os.path.join(env["basedir"], 'modules/%s-%s' % (module, v['version']))):
        shutil.rmtree(os.path.join(env["basedir"], 'modules/%s-%s' % (module, v['version'])))
    os.mkdir(os.path.join(env["basedir"], 'modules/%s-%s' % (module, v['version'])))

    Unzip().extract(archive, os.path.join(env["basedir"], 'modules/%s-%s' % (module, v['version'])))
    os.remove(archive)
    print '~'
    print '~ Module %s-%s is installed!' % (module, v['version'])
    print '~ You can now use it by adding it to the dependencies.yml file:'
    print '~'
    print '~ require:'
    print '~     play -> %s %s' % (module, v['version'])
    print '~'
    sys.exit(0)


def add(app, args, env):
    app.check()

    m = None
    try:
        optlist, args = getopt.getopt(args, '', ['module='])
        for o, a in optlist:
            if o in ('--module'):
                m = a
    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ "
        sys.exit(-1)

    if m is None:
        print "~ Usage: play add --module=<modulename>"
        print "~ "
        sys.exit(-1)

    appConf = os.path.join(app.path, 'conf/application.conf')
    if not fileHas(appConf, '# ---- MODULES ----'):
        print "~ Line '---- MODULES ----' missing in your application.conf. Add it to use this command."
        print "~ "
        sys.exit(-1)

    mn = m
    if mn.find('-') > 0:
        mn = mn[:mn.find('-')]

    if mn in app.module_names():
        print "~ Module %s already declared in application.conf, not doing anything." % mn
        print "~ "
        sys.exit(-1)

    replaceAll(appConf, r'# ---- MODULES ----', '# ---- MODULES ----\nmodule.%s=${play.path}/modules/%s' % (mn, m) )
    print "~ Module %s add to application %s." % (mn, app.name())
    print "~ "


def load_module_list(custom_server=None):

    def addServer(module, server):
        module['server'] = server
        return module

    def any(arr, func):
        for x in arr:
            if func(x): return True
        return False

    modules = None
    if custom_server is not None:
        rev = [custom_server]
    else:
        rev = repositories[:] # clone
        rev.reverse()

    for repo in rev:
        result = load_modules_from(repo)
        if modules is None:
            modules = map(lambda m: addServer(m, repo), result['modules'])
        else:
            for module in result['modules']:
                if not any(modules, lambda m: m['name'] == module['name']):
                    modules.append(addServer(module, repo))
    return modules


def load_modules_from(modules_server):
    try:
        url = '%s/modules' % modules_server
        headers={'User-Agent':DEFAULT_USER_AGENT,
                'Accept': 'application/json'
        } 
        req = urllib2.Request(url, headers=headers)
        result = urllib2.urlopen(req)
        return json.loads(result.read())
    except urllib2.HTTPError, e:
        print "~ Oops,"
        print "~ Cannot fetch the modules list from %s (%s)..." % (url, e.code)
        print e.reason
        print "~"
        sys.exit(-1)
    except urllib2.URLError, e:
        print "~ Oops,"
        print "~ Cannot fetch the modules list from %s ..." % (url)
        print "~"
        sys.exit(-1)
