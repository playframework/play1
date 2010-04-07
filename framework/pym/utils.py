import sys
import os, os.path
import re
import random
import fileinput
import getopt


def replaceAll(file, searchExp, replaceExp):
    replaceExp = replaceExp.replace('\\', '\\\\')
    searchExp = searchExp.replace('$', '\\$')
    searchExp = searchExp.replace('{', '\\{')
    searchExp = searchExp.replace('}', '\\}')
    searchExp = searchExp.replace('.', '\\.')
    for line in fileinput.input(file, inplace=1):
        line = re.sub(searchExp, replaceExp, line)
        sys.stdout.write(line)

def secretKey():
    return ''.join([random.choice('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789') for i in range(64)])

def isParentOf(path1, path2):
    if len(path2) < len(path1) or len(path2) < 2:
        return False
    if (path1 == path2):
        return True
    return isParentOf(path1, os.path.dirname(path2))

def override(f, t):
    fromFile = None
    for module in modules:
        pc = os.path.join(module, f)
        if os.path.exists(pc): fromFile = pc
    if not fromFile:
        print "~ %s not found in any modules" % f
        print "~ "
        sys.exit(-1)
    toFile = os.path.join(application_path, t)
    if os.path.exists(toFile):
        response = raw_input("~ Warning! %s already exists and will be overriden (y/n)? " % toFile)
        if not response == 'y':
            return
    if not os.path.exists(os.path.dirname(toFile)):
        os.makedirs(os.path.dirname(toFile))
    shutil.copyfile(fromFile, toFile)
    print "~ Copied %s to %s " % (fromFile, toFile)

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
