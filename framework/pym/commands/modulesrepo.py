import os
import sys
import zipfile
import urllib2
import shutil
import string

from framework.pym.utils import *
import framework.pym.simplejson as json

NM = ['new-module']
LM = ['list-modules', 'lm']
BM = ['build-modules', 'bm']
IM = ['install']

NAMES = NM + LM + BM + IM

# TODO: Make that configurable
modules_server = 'http://www.playframework.org'

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command in NM:
        new(app, args, env)
    elif command in LM:
        list(app, args)
    elif command in BM:
        build(app, args)
    elif command in IM:
        install(app, args)

def new(app, args, play_env):
    if os.path.exists(app.path):
        print "~ Oops. %s already exists" % app.path
        print "~"
        sys.exit(-1)

    print "~ The new module will be created in %s" % os.path.normpath(app.path)
    print "~"
    application_name = os.path.basename(app.path)
    shutil.copytree(os.path.join(play_env["basedir"], 'resources/module-skel'), app.path)
    # check_application()
    replaceAll(os.path.join(app.path, 'build.xml'), r'%MODULE%', application_name)
    replaceAll(os.path.join(app.path, 'conf/messages'), r'%MODULE%', application_name)
    replaceAll(os.path.join(app.path, 'conf/routes'), r'%MODULE%', application_name)
    replaceAll(os.path.join(app.path, 'conf/routes'), r'%MODULE_LOWERCASE%', string.lower(application_name))
    os.mkdir(os.path.join(app.path, 'app/controllers/%s' % application_name))
    os.mkdir(os.path.join(app.path, 'app/models/%s' % application_name))
    os.mkdir(os.path.join(app.path, 'app/views/%s' % application_name))
    os.mkdir(os.path.join(app.path, 'app/views/tags/%s' % application_name))
    os.mkdir(os.path.join(app.path, 'src/play/modules/%s' % application_name))

    print "~ OK, the module is created."
    print "~ Start using it by adding this line in the application.conf modules list: "
    print "~ module.%s=%s" % (application_name, os.path.normpath(app.path))
    print "~"
    print "~ Have fun!"
    print "~"

def list(app, args):
    print "~ You can also browse this list online at %s/modules" % modules_server
    print "~"

    modules_list = load_module_list()

    for mod in modules_list['modules']:
        print "~ [%s]" % mod['name']
        print "~   %s" % mod['fullname']
        print "~   %s/modules/%s" % (modules_server, mod['name'])
        
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

def build(app, args):
    ftb = play_base

    try:
        optlist, args = getopt.getopt(remaining_args, '', ['framework='])
        for o, a in optlist:
            if o in ('--framework'):
                ftb = a
    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ "
        sys.exit(-1)

    version = raw_input("~ What is the module version number? ")
    fwkMatch = raw_input("~ What are the playframework versions required? ")

    build_file = os.path.join(app.path, 'build.xml')
    if os.path.exists(build_file):
        print "~"
        print "~ Building..."
        print "~"
        os.system('ant -f %s -Dplay.path=%s' % (build_file, ftb) )
        print "~"

    mv = '%s-%s' % (os.path.basename(app.path), version)
    print("~ Packaging %s ... " % mv)

    dist_dir = os.path.join(app.path, 'dist')
    if os.path.exists(dist_dir):
        shutil.rmtree(dist_dir)
    os.mkdir(dist_dir)

    manifest = os.path.join(app.path, 'manifest')
    manifestF = open(manifest, 'w')
    manifestF.write('version=%s\nframeworkVersions=%s\n' % (version, fwkMatch))
    manifestF.close()

    zip = zipfile.ZipFile(os.path.join(dist_dir, '%s.zip' % mv), 'w', zipfile.ZIP_STORED)
    for (dirpath, dirnames, filenames) in os.walk(app.path):
        if dirpath == dist_dir:
            continue
        if dirpath.find('/.') > -1 or dirpath.find('/tmp/') > -1  or dirpath.find('/test-result/') > -1 or dirpath.find('/logs/') > -1 or dirpath.find('/eclipse/') > -1 or dirpath.endswith('/test-result') or dirpath.endswith('/logs')  or dirpath.endswith('/eclipse') or dirpath.endswith('/nbproject'):
            continue
        for file in filenames:
            if file.find('~') > -1 or file.endswith('.iml') or file.startswith('.'):
                continue
            zip.write(os.path.join(dirpath, file), os.path.join(dirpath[len(app.path):], file))
    zip.close()

    os.remove(manifest)

    print "~"
    print "~ Done!"
    print "~ Package is available at %s" % os.path.join(dist_dir, '%s.zip' % mv)
    print "~"

def install(app, args):
    if len(sys.argv) < 3:
        help_file = os.path.join(play_base, 'documentation/commands/cmd-install.txt')
        print open(help_file, 'r').read()
        sys.exit(0)

    name = cmd = sys.argv[2]
    groups = re.match(r'^([a-zA-Z0-9]+)([-](.*))?$', name)
    module = groups.group(1)
    version = groups.group(3)
    
    modules_list = load_module_list()
    fetch = None
    
    for mod in modules_list['modules']:
        if mod['name'] == module:
            for v in mod['versions']:
                if version == None and v['isDefault']:
                    print '~ Will install %s-%s' % (module, v['version'])
                    print '~ This module is compatible with: %s' % v['matches']
                    ok = raw_input('~ Do you want to install this version (y/n)? ')
                    if not ok == 'y':
                        print '~'
                        sys.exit(-1)
                    print '~ Installing module %s-%s...' % (module, v['version'])
                    fetch = '%s/modules/%s-%s.zip' % (modules_server, module, v['version'])
                    break
                if version  == v['version']:
                    print '~ Will install %s-%s' % (module, v['version'])
                    print '~ This module is compatible with: %s' % v['matches']
                    ok = raw_input('~ Do you want to install this version (y/n)? ')
                    if not ok == 'y':
                        print '~'
                        sys.exit(-1)

                    print '~ Installing module %s-%s...' % (module, v['version'])
                    fetch = '%s/modules/%s-%s.zip' % (modules_server, module, v['version'])
                    break
                    
    if fetch == None:
        print '~ No module found \'%s\'' % name
        print '~ Try play list-modules to get the modules list'
        print '~'
        sys.exit(-1)
    
    archive = os.path.join(play_base, 'modules/%s-%s.zip' % (module, v['version']))
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
    
    if os.path.exists(os.path.join(play_base, 'modules/%s-%s' % (module, v['version']))):
        shutil.rmtree(os.path.join(play_base, 'modules/%s-%s' % (module, v['version'])))
    os.mkdir(os.path.join(play_base, 'modules/%s-%s' % (module, v['version'])))
    
    Unzip().extract(archive, os.path.join(play_base, 'modules/%s-%s' % (module, v['version'])))
    os.remove(archive)
    print '~'
    print '~ Module %s-%s is installed!' % (module, v['version'])
    print '~ You can now use it by add adding this line to application.conf file:'
    print '~'
    print '~ module.%s=${play.path}/modules/%s-%s' % (module, module, v['version'])
    print '~'
    sys.exit(0)

def load_module_list():
    try:
        url = '%s/modules' % modules_server
        proxy_handler = urllib2.ProxyHandler({})
        req = urllib2.Request(url)
        req.add_header('Accept', 'application/json')
        opener = urllib2.build_opener(proxy_handler)
        result = opener.open(req)
        return json.loads(result.read())
    except urllib2.HTTPError, e:
        print "~ Oops,"
        print "~ Cannot fetch the modules list from %s (%s)..." % (url, e.code)
        print "~"
        sys.exit(-1)
    except urllib2.URLError, e:
        print "~ Oops,"
        print "~ Cannot fetch the modules list from %s ..." % (url)
        print "~"
        sys.exit(-1)

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
