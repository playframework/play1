import sys
import os, os.path
import re
import random
import fileinput


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
