#!/usr/bin/env python
import sys

if sys.platform.startswith('win') and sys.executable.lower().endswith('pythonw.exe'):
    from cStringIO import StringIO
    sys.stdout = StringIO()

MIN_WX_VERSION  = '2.5.4.1'
GET_WXPYTHON    = 'Get it from http://www.wxpython.org!'

try:
    import wxversion
    if sys.modules.has_key('wx') or sys.modules.has_key('wxPython'):
        pass    #probably not the first call to this module: wxPython already loaded
    else:
        wxversion.ensureMinimal(MIN_WX_VERSION)
except ImportError:
    #the old fashioned way as not everyone seems to have wxversion installed
    try:
        import wx
        if wx.VERSION_STRING < MIN_WX_VERSION:
            print 'You need to upgrade wxPython to v%s (or higher) to run SPE.'%MIN_WX_VERSION
            print GET_WXPYTHON
            sys.exit()
    except ImportError:
            print "Error: SPE requires wxPython, which doesn't seem to be installed."
            print GET_WXPYTHON
            sys.exit()
    print 'Warning: the package python-wxversion was not found, please install it!'
    print 'SPE will continue anyway, but not all features (such as wxGlade) might work.'

    
import info
INFO    = info.copy()

INFO['description']=\
"""This is the main SPE application created with SPE and wxGlade."""
__doc__ = INFO['doc']%INFO
    
print """
SPE v%(version)s (c)2003-2007 www.stani.be

If spe fails to start:
 - type "%(python)s SPE.py --debug > debug.txt 2>&1" at the command prompt
   (or if you use tcsh: "%(python)s SPE.py --debug >& debug.txt")
 - send debug.txt with some info to spe.stani.be[at]gmail.com
 """%INFO
 
####Import Modules

#---General
import ConfigParser, sys, os, wx
import sm.wxp.smdi as smdi
import Menu,Parent,Child
from optparse import OptionParser
#---Blender
print "Blender support",
try:
    import Blender
    redraw      = Blender.Redraw
    print 'enabled.'
except ImportError:
    Blender     = None
    redraw      = None
    print 'disabled (run SPE inside Blender to enable).'
    
#---Crypto
try:
    from Crypto.Cipher import DES
    fCrypto     = True
    print "Encrypted debugging enabled.\n"
except ImportError:
    fCrypto     = False
    print """\nEncrypted debugging disabled. 
  If you prefer encrypted debugging, install the "Python Cryptography Toolkit"
  from http://www.amk.ca/python/code/crypto\n"""

####Constants
MDI         = 0
DEBUG       = 0
IMAGE_PATH  = info.INFO['skinLocation']#os.path.join(info.path,'skins','default')

####Command line arguments
openFiles   = []
if DEBUG:
    __debug     = DEBUG
else:
    __debug     = DEBUG
    openFiles   = []

__workspace = None

parser      = OptionParser(usage="%prog [--debug] [ -w <WORKSPACE> | --workspace=<WORKSPACE>] [file1.py file2.py ... ]",version="SPE v%s (c)2003-2007 www.stani.be"%INFO['version'])
parser.add_option("-w","--workspace",help="open a workspace file")
parser.add_option("-d","--debug",action="store_true",help="turn on debug output")
opts, args  = parser.parse_args()

if not __debug: 
    __debug = (opts.debug)
if opts.workspace: 
    __workspace=opts.workspace
else: 
    openFiles=args

####Preferences
config=ConfigParser.ConfigParser()
config.readfp(open(INFO['defaults']))
try:
    config.read(INFO['defaultsUser'])
except:
    print 'Spe warning: could not load user options'
   
# If there is a preference in the user's defaults that is not in
# the regular defaults file,  add it
baseConfig=ConfigParser.ConfigParser()
baseConfig.read(os.path.join(info.PATH,"defaults.cfg"))
for section in baseConfig.sections():
    for option in baseConfig.options(section):
        if not config.has_option(section,option):
            config.set(section,option,baseConfig.get(section,option))



#---Workspace
if __workspace is not None: 
    config.set("Default","currentworkspace",__workspace)
    fp  = open(INFO['defaultsUser'],"w")
    config.write(fp)
    fp.close()

#---Maximize    
style   = smdi.STYLE_PARENTFRAME
try:
    maximize=eval(config.get("Default","maximize"))
except:
    maximize=True
if maximize: style |= wx.MAXIMIZE

#---Size
try:
    sizeX   = int(config.get("Default","sizex"))
    sizeY   = int(config.get("Default","sizey"))
    posX    = max(0,int(config.get("Default","posx")))
    posY    = max(0,int(config.get("Default","posy")))
except:
    sizeX   = 800
    sizeY   = 600
    posX    = 0
    posY    = 0
    
#---MDI
mdi         = config.get('Default','Mdi')
if not smdi.DI.has_key(mdi):
    mdi     = smdi.Default
    config.set('Default','Mdi',mdi)
    
#---Single Instance Application
try:
    singleInstance = eval(config.get('Default','SingleInstanceApp'))
except:
    singleInstance = False
            
####Shortcuts
class Translate:
    def __init__(self,keys):
        self.keys = keys
        
    def __call__(self,entry):
        entry           = entry.split('\t')
        if len(entry)==2:
            label, shortcut = entry
        else:
            label           = entry[0]
            shortcut        = ''
        l               = self.strip(label)
        if self.keys.has_key(l):
            shortcut    = self.keys[l]
        if shortcut:
            return '%s\t%s'%(label,shortcut)
        else:
            return label
            
    def strip(self,x):
        return x.replace('&','').replace('.','')
        
shortcuts    = config.get("Default","shortcuts")
if shortcuts == smdi.DEFAULT:
    if smdi.DARWIN:
        _shortcuts  = 'Macintosh'
    else:
        _shortcuts  = 'Windows'
else:
    _shortcuts      = shortcuts
import _spe.shortcuts as sc
execfile(os.path.join(os.path.dirname(sc.__file__),'%s.py'%_shortcuts))
import wxgMenu
wxgMenu._   = Translate(keys)

#---feedback
if __debug:
    print """Spe is running in debugging mode with this configuration:
- platform  : %s
- python    : %s
- wxPython  : %s
- interface : %s
- encoding  : %s
"""%(smdi.PLATFORM,INFO['pyVersionC'],INFO['wxVersionC'],mdi,INFO['encoding'])
    
####Application
app = smdi.App(\
        ParentPanel     = Parent.Panel,
        ChildPanel      = Child.Panel,
        MenuBar         = Menu.Bar,
        ToolBar         = Menu.Tool,
        StatusBar       = Menu.Status,
        Palette         = Menu.Palette,
        mdi             = mdi,
        debug           = __debug,
        fCrypto         = fCrypto,
        title           = 'SPE %s'%INFO['version'],
        panelFrameTitle = 'Shell',
        redraw          = redraw,
        Blender         = Blender,
        openFiles       = openFiles,
        size            = wx.Size(sizeX,sizeY),
        config          = config,
        pos             = wx.Point(posX,posY),
        shortcuts       = shortcuts,
        imagePath       = IMAGE_PATH,
        singleInstance  = singleInstance,
        style           = style)

app.MainLoop()

print "\nThank you for using SPE, please donate to support further development."

if __debug:
    try:
        import msvcrt
        print "\nPress any key to quit..."
        msvcrt.getch( )
    except:
        import time
        print "\nPress Ctrl+C to quit..."
        #time.sleep(10)




