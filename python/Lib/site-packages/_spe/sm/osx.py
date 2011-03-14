#(c)www.stani.be (read __doc__ for more information)
import os
import sm
INFO=sm.INFO.copy()

INFO['description']=\
"""eXtended os related scripts.

Changes:
    - 2003/10/08: New userPath function by Greg Brunet
    - 2003/09/25: Added userPath function from Andrei (http://come.to/project5)"""

__doc__=INFO['doc']%INFO
#_______________________________________________________________________________

####IMPORT----------------------------------------------------------------------
import os,time,shutil,string, stat

####CLASSES---------------------------------------------------------------------
displayTitle=1
class TitleTimer:
    """Display the time left in mm:ss in the title of the console window."""
    def __init__(self,total,refresh=1,title=0):
        self.total=total
        self.refresh=refresh
        self.current=0
        self.alarm = self.start=time.time()
        self.alarm =self.alarm+refresh
        self.title=title

    def tick(self,current=None,comment=""):
        ""
        if current: self.current=current
        else: self.current= self.current+1
        if time.time()>self.alarm:
            self.alarm=time.time()
            seconds=int((self.alarm-self.start)*(self.total-self.current)/self.current)
            self.alarm=self.alarm+self.refresh
            message=str(seconds/60)+':'+string.zfill(seconds%60,2)+comment
            if self.title:
                self.title.SetLabel(message)
            elif displayTitle: os.system('title "'+message+'"')
            else: print message

####FUNCTIONS-------------------------------------------------------------------
def copydirs(base,to,ignore=[]):
    "Copy one level of empty directories."
    baseDirs=listdir(base,"folder")
    ignore.extend(listdir(to,"folder"))
    for dir in baseDirs:
        if not(dir in ignore): os.mkdir(to+"/"+dir)

def copyPython(srcPy,dstPy):
    f=file(srcPy,'r')
    script='\n'.join([line.rstrip() for line in f.readlines()])
    f.close()
    f=file(dstPy,'w')
    f.write(script)
    f.close()

def copytree(src, dst, symlinks=0,extensions=None,excludePrefixFolders=''):
    """Recursively copy a directory tree using copy2().

    The destination directory CAN ALREADY EXIST.
    Error are reported to standard output.

    If the optional symlinks flag is true, symbolic links in the
    source tree result in symbolic links in the destination tree; if
    it is false, the contents of the files pointed to by symbolic
    links are copied.

    XXX Consider this example code rather than the ultimate tool.

    """
    names = os.listdir(src)
    try:os.mkdir(dst)
    except:pass
    ex=len(excludePrefixFolders)
    for name in names:
        srcname = os.path.join(src, name)
        dstname = os.path.join(dst, name)
        try:
            if symlinks and os.path.islink(srcname):
                linkto = os.readlink(srcname)
                os.symlink(linkto, dstname)
            elif os.path.isdir(srcname):
                if not ex or (ex and name[:ex]!=excludePrefixFolders):
                    copytree(srcname, dstname, symlinks, extensions, excludePrefixFolders)
            elif extensions:
                extension=os.path.splitext(name)[-1].lower()
                if extension in extensions:
                    if extension in ['.py','.pyw']:
                        copyPython(srcname, dstname)
                    else:
                        shutil.copy2(srcname, dstname)
            else:
                shutil.copy2(srcname, dstname)
            # XXX What about devices, sockets etc.?
        except (IOError, os.error), why:
            print "Can't copy %s to %s: %s" % (`srcname`, `dstname`, str(why))

def filterByExtension(fileList,extensions):
    "Filters file list by a list of lowercase extensions."
    extensions=sm.assertList(extensions)
    return [f for f in fileList if os.path.splitext(f)[1].lower() in extensions]

def listdir(dir='', extensions=[],absPath=0,recursive=0,
        excludeFolders=[],excludePrefixFolders='',normcase=0):
    """Directory listings with options for extension and folder filtering.
    Parameter:
        - extensions: list of file extensions or 'folder' for folders only
        - ...
    """
    folders= extensions=="folder" or extensions=="folders"
    try:
        fileList=os.listdir(dir)
    except:
        return []
    if normcase:fileList = map(os.path.normcase,fileList)
    fileList.sort()
    if absPath or folders or recursive:fileList= [os.path.join(dir, f) for f in fileList]
    if folders or recursive:
        folderList=filter(os.path.isdir,fileList)
    if recursive and folderList:
        recursiveList=[]
        ex=len(excludePrefixFolders)
        for folder in folderList:
            baseFolder=os.path.basename(folder)
            if (baseFolder not in excludeFolders) or\
                    (ex and baseFolder[:ex]==excludePrefixFolders):
                recursiveList+=listdir(folder,extensions=extensions,
                                    absPath=absPath,recursive=recursive-1)
    else:recursiveList=[]
    if folders:
        if absPath:
            return folderList+recursiveList
        else:
            return map(os.path.basename,folderList)+recursiveList
    elif extensions==[]:
        return fileList+recursiveList
    else:
        return filterByExtension(fileList,extensions)+recursiveList

def lastModified(fileName):
    return os.stat(fileName)[stat.ST_MTIME]

def mkdir(x):
    try:
        os.mkdir(x)
        return 1
    except:
        return 0

def newer(f1,f2):
    return os.path.exists(f2) and lastModified(f1) > lastModified(f2)

def listdirR(folder,recursion=0,extensions=None,absolute=1):
    """List recursively all files with a certain extension and limited recursion depth."""
    files=[os.path.join(folder,file) for file in os.listdir(folder)]
    result=[file for file in files if not os.path.isdir(file) and\
            (not extensions or os.path.splitext(file)[-1].lower() in extensions)]
    if recursion:
        for folder in files:
            if os.path.isdir(folder):
                result.extend(listdirR(folder,recursion-1,extensions))
    if not absolute:result=[os.path.basename(x) for x in result]
    return result

def pathSplit(f):
    "Splits into path,basename,extension."
    s=os.path.split(f)
    t=string.split(s[1],".")
    return (s[0],t[0],t[1])

def dirSplit(f):
    """Splits its directory in a list of all subdirectories.

    Example:
        >>> dirSplit('d:\\hello\\world\\readme.txt')
        ['d:', 'hello', 'world']
    """
    return os.path.dirname(f).replace('\\','/').split('/')

def title(t):
    "Set the title of the console window."
    if displayTitle: os.system('title '+t)

def treeDir(path='',extensions=[],ignore=[],ignorePathPrefix='\\'):
    dir=[os.path.join(path,x) for x in os.listdir(path)]
    return (path,
        [treeDir(x,extensions,ignore,ignorePathPrefix) for x in dir
            if os.path.isdir(x) and not((os.path.basename(x) in ignore) or
            (os.path.basename(x)[:len(ignorePathPrefix)]==ignorePathPrefix))
            ]+
        [x for x in dir if (not os.path.isdir(x)) and
            (not extensions or os.path.splitext(x)[1].lower() in extensions)])

def rmtree(p,output=0):
    "Tries to remove a directory tree, otherwise print warning."
    try:shutil.rmtree(p)
    except:
        if output:print " - Can't remove path",p

##def userPath(dirname=''):
##    """Improved function to get user path (c) Andrei http://come.to/project5"""
##    savedir = os.path.expanduser(os.path.join('~',dirname))
##    if len(savedir)<=len("c:\\/"+dirname): # sometimes $HOME points to root
##        # if this is the case, try using $USERPROFILE (see docstring)
##        temp = os.path.join(os.path.expandvars('$USERPROFILE'), dirname)
##        # if this is a different location, use it!
##        if temp > len("C:\\/"+dirname):
##            savedir = temp
##    return savedir

def userPath(dirname=''):
    """'safer' function to find user path."""
    # 'safer' function to find user path: look for one of these directories
    try:
        path = os.path.expanduser("~")
        if os.path.isdir(path):
            return os.path.join(path, dirname)
    except:
        pass
    for evar in ('HOME', 'USERPROFILE', 'TMP'):
        try:
            path = os.environ[evar]
            if os.path.isdir(path):
                return os.path.join(path, dirname)
        except:
            pass
    #if no match found, use module directory
    return os.path.join(os.path.dirname(os.path.abspath(__file__)), dirname)

def startAppleScript(commandList, activateFlag = True):
    """Start a list of commands in the terminal window.
    Each command is a list of program name, parameters.
    Handles the quoting properly through shell, applescript and shell again.
    """
    def adjustParameter(parameter):
        """Adjust a parameter for the shell.
        Adds single quotes,
        unless the parameter consists of letters only
        (to make shell builtins work)
        or if the parameter is a list
        (to flag that it already is list a parameters).
        """
        if isinstance(parameter, list): return parameter[0]
        if parameter.isalpha(): return parameter
        #the single quote proper is replaced by '\'' since
        #backslashing a single quote doesn't work inside a string
        return "'%s'"%parameter.replace("'",r"'\''")
    command = ';'.join([
        ' '.join([
            adjustParameter(parameter)
            for parameter in command
            ])
        for command in commandList
    ])
    #make Applescript string from this command line:
    #put backslashes before double quotes and backslashes
    command = command.replace('\\','\\\\').replace('"','\\"')
    #make complete Applescript command containing this string
    command = 'tell application "Terminal" to do script "%s"'%command
    #make a shell parameter (single quote handling as above)
    command = command.replace("'","'\\''")
    #make complete shell command
    command = "osascript -e '%s'"%command
    #prepend activate command if needed
    if activateFlag:
        command = "osascript -e 'tell application \"Terminal\" to activate';"+command
    #go!
    os.popen(command)

#---registry--------------------------------------------------------------------

def registerFileCreate(label, action, fileType='Python.File'):
    try:
        import _winreg
        reload(_winreg)
        key='%s\\shell\\%s'%(fileType,label)
        key=_winreg.CreateKey(_winreg.HKEY_CLASSES_ROOT,key)
    except:
        pass
    try:
        print (key,"command",_winreg.REG_SZ,action+' "%1"')
        _winreg.SetValue(key,"command",_winreg.REG_SZ,action+' "%1"')
        return 1
    except:
        return None

def registerPy(label, action, fileType='Python.File'):
    """action is a python file"""
    import sys
    action='"%s" "%s"'%(os.path.join(sys.exec_prefix,"pythonw.exe"),action)
    return registerFileCreate(label=label,action=action,fileType=fileType)

def registerFileDelete(label, fileType='Python.File'):
    try:
        import _winreg
        reload(_winreg)
        key='%s\\shell\\%s'%(fileType,label)
        _winreg.DeleteKey(_winreg.HKEY_CLASSES_ROOT,key+'\\command')
    except:
        pass
    try:
        _winreg.DeleteKey(_winreg.HKEY_CLASSES_ROOT,key)
        return 1
    except:
        return None

####CONSTANTS------------------------------------------------------------------
NOT_FILE_CHARS=['\\','/','<','>','"','|','?',':','*']

if __name__=='__main__':
    print treeDir('c:/temp','PACK')
