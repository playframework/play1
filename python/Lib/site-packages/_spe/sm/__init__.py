#(c)www.stani.be                                                                
INFO={
    'author'         : "www.stani.be",
    'title'          : "SM general python library",
    'date'           : "13-9-2003",
    'doc'            : "%(titleFull)s by %(author)s\n\n%(description)s\n\n%(links)s\n\n%(requirements)s\n\n%(copyright)s",
    'version'        : "1.0",
    'pyVersion'      : "2.2",
    'wxVersion'      : "2.4.1.2.",
}

INFO['titleFull']="%(title)s %(version)s"%INFO

INFO['description']=\
"""Collection of python scripts I often use."""

INFO['links']=\
"""Homepage : http://www.stani.be
Contact  : http://www.pycs.net/system/mailto.py?usernum=0000167"""

INFO['requirements']=\
"""Developped with Python v%(pyVersion)s"""%INFO

INFO['copyright']=\
"""Copyright (C)%(author)s (%(date)s)

This library (sm.*) is NOT released under the GPL, but you may use it for free
and adapt it to your own needs, provided you list my name and website in the
copyright."""%INFO

__doc__=INFO['doc']%INFO

from python import *

def initHtml():
    """Initializes html and css components of the sm library.
    
    >>> import sm
    >>> sm.initHtml()
    >>> print sm.html
    >>> print sm.css
    """
    global html, css
##    try:
    from htmlCss.html import html
    from htmlCss.css  import css
##    except:
##        print "Sm html library is not installed on this system."

def ChangeDisplaySettings(xres=None, yres=None, BitsPerPixel=None):
    """Changes the display resolution and bit depth on Windows."""

    import ctypes
    import struct

    DM_BITSPERPEL = 0x00040000
    DM_PELSWIDTH = 0x00080000
    DM_PELSHEIGHT = 0x00100000
    CDS_FULLSCREEN = 0x00000004
    SIZEOF_DEVMODE = 148

    user32 = ctypes.WinDLL('user32.dll')
    DevModeData = struct.calcsize("32BHH") * '\x00'
    DevModeData += struct.pack("H", SIZEOF_DEVMODE)
    DevModeData += struct.calcsize("H") * '\x00'
    dwFields = (xres and DM_PELSWIDTH or 0) | (yres and DM_PELSHEIGHT or 0) | (BitsPerPixel and DM_BITSPERPEL or 0)
    DevModeData += struct.pack("L", dwFields)
    DevModeData += struct.calcsize("l9h32BHL") * '\x00'
    DevModeData += struct.pack("LLL", BitsPerPixel or 0, xres or 0, yres or 0)
    DevModeData += struct.calcsize("8L") * '\x00'
    print dir(user32)
    result = user32.ChangeDisplaySettingsA(DevModeData, CDS_FULLSCREEN)
    return result == 0 # success if zero, some failure otherwise
    
