import sys
import os, os.path
import re
import random
import fileinput
import getopt
import shutil


def replaceAll(file, searchExp, replaceExp):
    replaceExp = replaceExp.replace('\\', '\\\\')
    searchExp = searchExp.replace('$', '\\$')
    searchExp = searchExp.replace('{', '\\{')
    searchExp = searchExp.replace('}', '\\}')
    searchExp = searchExp.replace('.', '\\.')
    for line in fileinput.input(file, inplace=1):
        line = re.sub(searchExp, replaceExp, line)
        sys.stdout.write(line)

def fileHas(file, searchExp):
    # The file doesn't get closed if we don't iterate to the end, so
    # we must continue even after we found the match.
    found = False
    for line in fileinput.input(file):
        if line.find(searchExp) > -1:
            found = True
    return found

def secretKey():
    return ''.join([random.choice('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789') for i in range(64)])

def isParentOf(path1, path2):
    if len(path2) < len(path1) or len(path2) < 2:
        return False
    if (path1 == path2):
        return True
    return isParentOf(path1, os.path.dirname(path2))

def isParentOf(path1, path2):
    if len(path2) < len(path1) or len(path2) < 2:
        return False
    if (path1 == path2):
        return True

def getWithModules(args, env):
    withModules = []
    try:
        optlist, newargs = getopt.getopt(args, '', ['with='])
        for o, a in optlist:
            if o in ('--with='):
                withModules = a.split(',')
    except getopt.GetoptError:
        pass # Other argument that --with= has been passed (which is OK)
    md = []
    for m in withModules:
        dirname = None
        candidate = os.path.join(env["basedir"], 'modules/%s' % m)
        if os.path.exists(candidate) and os.path.isdir(candidate):
            dirname = candidate
        else:
            for f in os.listdir(os.path.join(env["basedir"], 'modules')):
                if os.path.isdir(os.path.join(env["basedir"], 'modules/%s' % f)) and f.find('%s-' % m) == 0:
                    dirname = os.path.join(env["basedir"], 'modules/%s' % f)
                    break
        md.append(dirname)
    return md

def package_as_war(app, env, war_path, war_zip_path):
    app.check()
    modules = app.modules()
    classpath = app.getClasspath()

    if not war_path:
        print "~ Oops. Please specify a path where to generate the WAR, using the -o or --output option"
        print "~"
        sys.exit(-1)

    if os.path.exists(war_path) and not os.path.exists(os.path.join(war_path, 'WEB-INF')):
        print "~ Oops. The destination path already exists but does not seem to host a valid WAR structure"
        print "~"
        sys.exit(-1)

    if isParentOf(app.path, war_path):
        print "~ Oops. Please specify a destination directory outside of the application"
        print "~"
        sys.exit(-1)

    print "~ Packaging current version of the framework and the application to %s ..." % (os.path.normpath(war_path))
    if os.path.exists(war_path): shutil.rmtree(war_path)
    if os.path.exists(os.path.join(app.path, 'war')):
        shutil.copytree(os.path.join(app.path, 'war'), war_path)
    else:
        os.mkdir(war_path)
    if not os.path.exists(os.path.join(war_path, 'WEB-INF')): os.mkdir(os.path.join(war_path, 'WEB-INF'))
    if not os.path.exists(os.path.join(war_path, 'WEB-INF/web.xml')):
        shutil.copyfile(os.path.join(env["basedir"], 'resources/war/web.xml'), os.path.join(war_path, 'WEB-INF/web.xml'))
    application_name = app.readConf('application.name')
    replaceAll(os.path.join(war_path, 'WEB-INF/web.xml'), r'%APPLICATION_NAME%', application_name)
    if env["id"] is not "":
        replaceAll(os.path.join(war_path, 'WEB-INF/web.xml'), r'%PLAY_ID%', env["id"])
    else:
        replaceAll(os.path.join(war_path, 'WEB-INF/web.xml'), r'%PLAY_ID%', 'war')
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application')): shutil.rmtree(os.path.join(war_path, 'WEB-INF/application'))
    shutil.copytree(app.path, os.path.join(war_path, 'WEB-INF/application'))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application/war')):
        shutil.rmtree(os.path.join(war_path, 'WEB-INF/application/war'))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application/logs')):
        shutil.rmtree(os.path.join(war_path, 'WEB-INF/application/logs'))
    shutil.copytree(os.path.join(app.path, 'conf'), os.path.join(war_path, 'WEB-INF/classes'))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/lib')): shutil.rmtree(os.path.join(war_path, 'WEB-INF/lib'))
    os.mkdir(os.path.join(war_path, 'WEB-INF/lib'))
    for jar in classpath:
        if jar.endswith('.jar') and jar.find('provided-') == -1:
            shutil.copyfile(jar, os.path.join(war_path, 'WEB-INF/lib/%s' % os.path.split(jar)[1]))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/framework')): shutil.rmtree(os.path.join(war_path, 'WEB-INF/framework'))
    os.mkdir(os.path.join(war_path, 'WEB-INF/framework'))
    shutil.copytree(os.path.join(env["basedir"], 'framework/templates'), os.path.join(war_path, 'WEB-INF/framework/templates'))
    
    # modules
    for module in modules:
        to = os.path.join(war_path, 'WEB-INF/modules/%s' % os.path.basename(module))
        shutil.copytree(module, to)
        if os.path.exists(os.path.join(to, 'src')):
            shutil.rmtree(os.path.join(to, 'src'))
        if os.path.exists(os.path.join(to, 'dist')):
            shutil.rmtree(os.path.join(to, 'dist'))
        if os.path.exists(os.path.join(to, 'samples-and-tests')):
            shutil.rmtree(os.path.join(to, 'samples-and-tests'))
        if os.path.exists(os.path.join(to, 'build.xml')):
            os.remove(os.path.join(to, 'build.xml'))
        if os.path.exists(os.path.join(to, 'commands.py')):
            os.remove(os.path.join(to, 'commands.py'))
        if os.path.exists(os.path.join(to, 'lib')):
            shutil.rmtree(os.path.join(to, 'lib'))
        if os.path.exists(os.path.join(to, 'nbproject')):
            shutil.rmtree(os.path.join(to, 'nbproject'))
        if os.path.exists(os.path.join(to, 'documentation')):
            shutil.rmtree(os.path.join(to, 'documentation'))
    pm = app.readConfs('module.')
    for m in pm:
        nm = os.path.basename(m)
        replaceAll(os.path.join(war_path, 'WEB-INF/application/conf/application.conf'), m, '../modules/%s' % nm)

    if not os.path.exists(os.path.join(war_path, 'WEB-INF/resources')): os.mkdir(os.path.join(war_path, 'WEB-INF/resources'))
    shutil.copyfile(os.path.join(env["basedir"], 'resources/messages'), os.path.join(war_path, 'WEB-INF/resources/messages'))

    deleteFrom(war_path, app.readConf('war.exclude').split("|"))

    if war_zip_path:
        print "~ Creating zipped archive to %s ..." % (os.path.normpath(war_zip_path))
        if os.path.exists(war_zip_path):
            os.remove(war_zip_path)
        zip = zipfile.ZipFile(war_zip_path, 'w', zipfile.ZIP_STORED)
        dist_dir = os.path.join(app.path, 'dist')
        for (dirpath, dirnames, filenames) in os.walk(war_path):
            if dirpath == dist_dir:
                continue
            if dirpath.find('/.') > -1:
                continue
            for file in filenames:
                if file.find('~') > -1 or file.startswith('.'):
                    continue
                zip.write(os.path.join(dirpath, file), os.path.join(dirpath[len(war_path):], file))

        zip.close()

# Recursively delete all files/folders in root whose name equals to filename
# We could pass a "ignore" parameter to copytree, but that's not supported in Python 2.5

def deleteFrom(root, filenames):
    for f in os.listdir(root):
        fullpath = os.path.join(root, f)
        if f in filenames:
            delete(fullpath)
        elif os.path.isdir(fullpath):
            deleteFrom(fullpath, filenames)

def delete(filename):
    if os.path.isdir(filename):
        shutil.rmtree(filename)
    else:
        os.remove(filename)