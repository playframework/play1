#(c)www.stani.be (read __doc__ for more information)
import sm
INFO=sm.INFO.copy()

INFO['author']      = 'Mark Hammond'
INFO['copyright']   = '(c) Mark Hammond (See PythonWin distribution)'
INFO['title']       = INFO['titleFull'] = 'scriptutils'
INFO['description'] =\
"""Changes:
    - may 2003: www.stani.be to make it platform and PythonWin independent.
"""

__doc__=INFO['doc']%INFO
#_______________________________________________________________________________

import string,os,sys,types

import __main__
#---run: from pywin.framework.scriptutils (c)Mark Hammond-----------------------
def run(fileName=None,source=None,mainDict=__main__.__dict__,profiling=0):
    import traceback
    print
    bWorked = 0
    exitCode = 0
    osPath=None
    try:
        if fileName:
            path, base              = os.path.split(fileName)
            if path:
                if path not in sys.path:sys.path.insert(0,path)
                osPath              = os.getcwd()
                os.chdir(path)
        if source:
            if type(source) == types.UnicodeType:
                source              = source.encode(sys.getdefaultencoding(),'replace')
            source                  = '__name__="__main__"\n%s'%source
            fileName                = base = '<source>'
        else:
            f                       = open(fileName,'r')
            source                  = f.read()
            f.close()
            if type(source) == types.UnicodeType:
                source              = source.encode(sys.getdefaultencoding(),'replace')
        codeObject                  = compile(source.replace('\r\n','\n')+"\n", fileName, "exec")
        if profiling:
            import profile, pstats
            prof                    = profile.Profile()
            mainDict['codeObject']  = codeObject
            prof                    = prof.runctx('exec codeObject in mainDict', locals(), mainDict)
            stats                   = pstats.Stats(prof)
            stats.sort_stats('cum','time').print_stats()
        else:
            exec codeObject in mainDict
        bWorked = 1
    except SystemExit, code:
        exitCode = code
        bWorked = 1
    except KeyboardInterrupt:
        traceback.print_exc()
        bWorked = 1
    except:
        traceback.print_exc()
    try:
        sys.stdout.flush()
    except AttributeError:
        pass
    if bWorked:
        print "Script '%s' returned exit code %s" %(base, exitCode)
    else:
        print 'Exception raised while running script  %s' % base
    if osPath:
        os.chdir(osPath)


#---Import Module: from pywin.framework.scriptutils  (c)Mark Hammond------------
def importMod(pathName,mainDict=None):
    import os,string,sys,__main__
    print
    # If already imported, dont look for package
    path, modName = os.path.split(pathName)
    modName, modExt = os.path.splitext(modName)
    newPath = None
    for key, mod in sys.modules.items():
        if hasattr(mod, '__file__'):
            fname = mod.__file__
            base, ext = os.path.splitext(fname)
            if string.lower(ext) in ['.pyo', '.pyc']:
                ext = '.py'
            fname = base + ext
            if os.path.abspath(fname)==os.path.abspath(pathName):
                modName = key
                break
    else: # for not broken
        modName, newPath = GetPackageModuleName(pathName)
        if newPath and newPath not in sys.path:
            sys.path.insert(0,newPath)
    if sys.modules.has_key(modName):
        bNeedReload = 1
        what = "reload"
    else:
        what = "import"
        bNeedReload = 0
    try:
        # always do an import, as it is cheap is already loaded.  This ensures
        # it is in our name space.
        if path not in sys.path:sys.path.append(path)
        codeObj = compile('import '+modName,'<auto import>','exec')
        if not mainDict:mainDict=__main__.__dict__
        exec codeObj in mainDict
        if bNeedReload:
            reload(sys.modules[modName])
        print 'Successfully ' + what + 'ed module "'+modName+'"'
    except Exception,message:
        print 'Failed to ' + what + ' module "'+modName+'" (%s)'%message

def GetPackageModuleName(fileName):
    """Given a filename, return (module name, new path).
       eg - given "c:\a\b\c\my.py", return ("b.c.my",None) if "c:\a" is on sys.path.
       If no package found, will return ("my", "c:\a\b\c")
    """
    import os,string
    path, fname = os.path.split(fileName)
    origPath=path
    fname = os.path.splitext(fname)[0]
    modBits = []
    newPathReturn = None
    if not IsOnPythonPath(path):
        # Module not directly on the search path - see if under a package.
        while len(path)>3: # ie 'C:\'
            path, modBit = os.path.split(path)
            modBits.append(modBit)
            # If on path, _and_ existing package of that name loaded.
            if IsOnPythonPath(path) and sys.modules.has_key(modBit) and \
               ( os.path.exists(os.path.join(path, '__init__.py')) or \
                 os.path.exists(os.path.join(path, '__init__.pyc')) or \
                 os.path.exists(os.path.join(path, '__init__.pyo')) \
               ):
                modBits.reverse()
                return string.join(modBits, ".") + "." + fname, newPathReturn
            # Not found - look a level higher
        else:
            newPathReturn = origPath

    return fname, newPathReturn

def IsOnPythonPath(path):
    "Given a path only, see if it is on the Pythonpath.  Assumes path is a full path spec."
    # must check that the command line arg's path is in sys.path
    import os,sys
    for syspath in sys.path:
        try:
            # Python 1.5 and later allows an empty sys.path entry.
            if syspath:# and os.path.abspath(syspath)==os.path.normpath(path):
                return 1
        except Exception, details:
            print "Warning: The sys.path entry '%s' is invalid\n%s" \
                % (syspath, details)
    return 0

#---CheckFile: from pywin.framework.scriptutils  (c)Mark Hammond----------------
import os,sys,traceback
#some improvised helper functions to make it environment independent
def smPrintStatus(x):
    print x

def smJumpToPosition(fileName, lineno, col = 1):
    print '-> fileName = "%s", lineno = %s, col = %s'%(fileName, lineno, col)

#adapted from pywin.framework
def CheckFile(pathName,source=None,status=smPrintStatus,jump=smJumpToPosition):
    """ This code looks for the current window, and gets Python to check it
    without actually executing any code (ie, by compiling only)

    status: function to set statusbar, otherwise print
    jump: function(fileName, lineno, col = 1)
    """
    what='check'
    status(what+'ing module...')
    if not source:
        try:
            f = open(pathName)
        except IOError, details:
            print "Can't open file '%s' - %s" % (pathName, details)
            return
        try:
            source = f.read()
        finally:
            f.close()
    code=source.replace('\r\n','\n') + "\n"
    try:
        codeObj = compile(code, pathName,'exec')
        if RunTabNanny(pathName,status=status,jump=jump):
            status("Python and the TabNanny successfully checked the file '"+
                os.path.basename(pathName)+"'")
            return 1
    except SyntaxError:
        _HandlePythonFailure(what, pathName,status=status,jump=jump)
    except:
        traceback.print_exc()
        _HandlePythonFailure(what,status=status,jump=jump)

def RunTabNanny(filename,status=smPrintStatus,jump=smJumpToPosition):
    try:
        import cStringIO, tabnanny
    except Exception, message:
        print message
    # Capture the tab-nanny output
    newout = cStringIO.StringIO()
    old_out = sys.stderr, sys.stdout
    sys.stderr = sys.stdout = newout
    try:
        tabnanny.check(filename)
    finally:
        # Restore output
        sys.stderr, sys.stdout = old_out
    data = newout.getvalue()
    if data:
        try:
            lineno = string.split(data)[1]
            lineno = int(lineno)
            status("The TabNanny found trouble at line %d" % lineno)
            jump(filename, lineno)
        except (IndexError, TypeError, ValueError):
            print "The tab nanny complained, but I cant see where!"
            print data
        return 0
    return 1

def _HandlePythonFailure(what, syntaxErrorPathName = None,status=smPrintStatus,
        jump=smJumpToPosition):
    typ, details, tb = sys.exc_info()
    if typ == SyntaxError:
        try:
            msg, (fileName, line, col, text) = details
            if (not fileName or fileName in ["<string>","<source>"]) \
                    and syntaxErrorPathName:
                fileName = syntaxErrorPathName
            jump(fileName, line, col)
        except (TypeError, ValueError):
            msg = str(details)
        status('Failed to ' + what + ' - syntax error - %s' % msg)
    else:
        traceback.print_exc()
        status('Failed to ' + what + ' - ' + str(details) )
    tb = None # Clean up a cycle.
