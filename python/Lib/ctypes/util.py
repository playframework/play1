######################################################################
#  This file should be kept compatible with Python 2.3, see PEP 291. #
######################################################################
import sys, os

# find_library(name) returns the pathname of a library, or None.
if os.name == "nt":

    def _get_build_version():
        """Return the version of MSVC that was used to build Python.

        For Python 2.3 and up, the version number is included in
        sys.version.  For earlier versions, assume the compiler is MSVC 6.
        """
        # This function was copied from Lib/distutils/msvccompiler.py
        prefix = "MSC v."
        i = sys.version.find(prefix)
        if i == -1:
            return 6
        i = i + len(prefix)
        s, rest = sys.version[i:].split(" ", 1)
        majorVersion = int(s[:-2]) - 6
        minorVersion = int(s[2:3]) / 10.0
        # I don't think paths are affected by minor version in version 6
        if majorVersion == 6:
            minorVersion = 0
        if majorVersion >= 6:
            return majorVersion + minorVersion
        # else we don't know what version of the compiler this is
        return None

    def find_msvcrt():
        """Return the name of the VC runtime dll"""
        version = _get_build_version()
        if version is None:
            # better be safe than sorry
            return None
        if version <= 6:
            clibname = 'msvcrt'
        else:
            clibname = 'msvcr%d' % (version * 10)

        # If python was built with in debug mode
        import imp
        if imp.get_suffixes()[0][0] == '_d.pyd':
            clibname += 'd'
        return clibname+'.dll'

    def find_library(name):
        if name in ('c', 'm'):
            return find_msvcrt()
        # See MSDN for the REAL search order.
        for directory in os.environ['PATH'].split(os.pathsep):
            fname = os.path.join(directory, name)
            if os.path.exists(fname):
                return fname
            if fname.lower().endswith(".dll"):
                continue
            fname = fname + ".dll"
            if os.path.exists(fname):
                return fname
        return None

if os.name == "ce":
    # search path according to MSDN:
    # - absolute path specified by filename
    # - The .exe launch directory
    # - the Windows directory
    # - ROM dll files (where are they?)
    # - OEM specified search path: HKLM\Loader\SystemPath
    def find_library(name):
        return name

if os.name == "posix" and sys.platform == "darwin":
    from ctypes.macholib.dyld import dyld_find as _dyld_find
    def find_library(name):
        possible = ['lib%s.dylib' % name,
                    '%s.dylib' % name,
                    '%s.framework/%s' % (name, name)]
        for name in possible:
            try:
                return _dyld_find(name)
            except ValueError:
                continue
        return None

elif os.name == "posix":
    # Andreas Degert's find functions, using gcc, /sbin/ldconfig, objdump
    import re, tempfile, errno

    def _findLib_gcc(name):
        expr = r'[^\(\)\s]*lib%s\.[^\(\)\s]*' % re.escape(name)
        fdout, ccout = tempfile.mkstemp()
        os.close(fdout)
        cmd = 'if type gcc >/dev/null 2>&1; then CC=gcc; else CC=cc; fi;' \
              '$CC -Wl,-t -o ' + ccout + ' 2>&1 -l' + name
        try:
            f = os.popen(cmd)
            trace = f.read()
            f.close()
        finally:
            try:
                os.unlink(ccout)
            except OSError, e:
                if e.errno != errno.ENOENT:
                    raise
        res = re.search(expr, trace)
        if not res:
            return None
        return res.group(0)


    if sys.platform == "sunos5":
        # use /usr/ccs/bin/dump on solaris
        def _get_soname(f):
            if not f:
                return None
            cmd = "/usr/ccs/bin/dump -Lpv 2>/dev/null " + f
            res = re.search(r'\[.*\]\sSONAME\s+([^\s]+)', os.popen(cmd).read())
            if not res:
                return None
            return res.group(1)
    else:
        def _get_soname(f):
            # assuming GNU binutils / ELF
            if not f:
                return None
            cmd = "objdump -p -j .dynamic 2>/dev/null " + f
            res = re.search(r'\sSONAME\s+([^\s]+)', os.popen(cmd).read())
            if not res:
                return None
            return res.group(1)

    if (sys.platform.startswith("freebsd")
        or sys.platform.startswith("openbsd")
        or sys.platform.startswith("dragonfly")):

        def _num_version(libname):
            # "libxyz.so.MAJOR.MINOR" => [ MAJOR, MINOR ]
            parts = libname.split(".")
            nums = []
            try:
                while parts:
                    nums.insert(0, int(parts.pop()))
            except ValueError:
                pass
            return nums or [ sys.maxint ]

        def find_library(name):
            ename = re.escape(name)
            expr = r':-l%s\.\S+ => \S*/(lib%s\.\S+)' % (ename, ename)
            res = re.findall(expr,
                             os.popen('/sbin/ldconfig -r 2>/dev/null').read())
            if not res:
                return _get_soname(_findLib_gcc(name))
            res.sort(cmp= lambda x,y: cmp(_num_version(x), _num_version(y)))
            return res[-1]

    else:

        def _findLib_ldconfig(name):
            # XXX assuming GLIBC's ldconfig (with option -p)
            expr = r'/[^\(\)\s]*lib%s\.[^\(\)\s]*' % re.escape(name)
            res = re.search(expr,
                            os.popen('/sbin/ldconfig -p 2>/dev/null').read())
            if not res:
                # Hm, this works only for libs needed by the python executable.
                cmd = 'ldd %s 2>/dev/null' % sys.executable
                res = re.search(expr, os.popen(cmd).read())
                if not res:
                    return None
            return res.group(0)

        def find_library(name):
            return _get_soname(_findLib_ldconfig(name) or _findLib_gcc(name))

################################################################
# test code

def test():
    from ctypes import cdll
    if os.name == "nt":
        print cdll.msvcrt
        print cdll.load("msvcrt")
        print find_library("msvcrt")

    if os.name == "posix":
        # find and load_version
        print find_library("m")
        print find_library("c")
        print find_library("bz2")

        # getattr
##        print cdll.m
##        print cdll.bz2

        # load
        if sys.platform == "darwin":
            print cdll.LoadLibrary("libm.dylib")
            print cdll.LoadLibrary("libcrypto.dylib")
            print cdll.LoadLibrary("libSystem.dylib")
            print cdll.LoadLibrary("System.framework/System")
        else:
            print cdll.LoadLibrary("libm.so")
            print cdll.LoadLibrary("libcrypt.so")
            print find_library("crypt")

if __name__ == "__main__":
    test()
