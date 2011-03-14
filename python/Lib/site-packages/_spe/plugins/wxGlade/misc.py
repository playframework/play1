# misc.py: Miscellaneus stuff, used in many parts of wxGlade
# $Id: misc.py,v 1.47 2007/08/07 12:21:56 agriggio Exp $
# 
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

#from wxPython.wx import *
import wx

if wx.Platform == '__WXMSW__':
    class wxGladeRadioButton(wx.RadioButton):
        """
        custom wxRadioButton class which tries to implement a better
        GetBestSize than the default one for WXMSW (mostly copied from
        wxCheckBox::DoGetBestSize in checkbox.cpp)
        """
        __radio_size = None
        def GetBestSize(self):
            if not self.__radio_size:
                dc = wx.ScreenDC()
                dc.SetFont(wx.SystemSettings_GetFont(
                    wx.SYS_DEFAULT_GUI_FONT))
                self.__radio_size = (3*dc.GetCharHeight())/2
            label = self.GetLabel()
            if label:
                w, h = self.GetTextExtent(label)
                w += self.__radio_size + self.GetCharWidth()
                if h < self.__radio_size: h = self.__radio_size
            else: w = h = self.__radio_size;
            return w, h

    # end of class wxGladeRadioButton

else:
    wxGladeRadioButton = wx.RadioButton


# ALB 2004-10-27
FileSelector = wx.FileSelector
DirSelector = wx.DirSelector

#---------------------  Selection Markers  ----------------------------------

class SelectionTag(wx.Window):
    """\
    This is one of the small black squares that appear at the corners of the
    active widgets
    """
    def __init__(self, parent, pos=None):
        kwds = { 'size': (7, 7) }
        if pos: kwds['position'] = pos
        wx.Window.__init__(self, parent, -1, **kwds)
        self.SetBackgroundColour(wx.BLUE) #wx.BLACK)
        self.Hide()

# end of class SelectionTag


class SelectionMarker:
    """\
    Collection of the 4 SelectionTagS for each widget
    """
    def __init__(self, owner, parent, visible=False):
        self.visible = visible
        self.owner = owner
        self.parent = parent
        if wx.Platform == '__WXMSW__': self.parent = owner
        self.tag_pos = None
        self.tags = None
        #self.tags = [ SelectionTag(self.parent) for i in range(4) ]
        self.update()
        if visible:
            for t in self.tags: t.Show()

    def update(self, event=None):
        if self.owner is self.parent: x, y = 0, 0
        else: x, y = self.owner.GetPosition()
        w, h = self.owner.GetClientSize()
        def position(j):
            if not j: return x, y            # top-left
            elif j == 1: return x+w-7, y     # top-right
            elif j == 2: return x+w-7, y+h-7 # bottom-right
            else: return x, y+h-7            # bottom-left
##         for i in range(len(self.tags)):
##             self.tags[i].SetPosition(position(i))
        self.tag_pos = [ position(i) for i in range(4) ]
        if self.visible:
            if not self.tags:
                self.tags = [ SelectionTag(self.parent) for i in range(4) ]
            for i in range(4):
                self.tags[i].SetPosition(self.tag_pos[i])
        if event: event.Skip()

    def Show(self, visible):
##         self.visible = visible
##         for tag in self.tags: tag.Show(visible)
        if self.visible != visible:
            self.visible = visible
            if self.visible:
                if not self.tags:
                    self.tags = [ SelectionTag(self.parent) for i in range(4) ]
                for i in range(4):
                    self.tags[i].SetPosition(self.tag_pos[i])
                    self.tags[i].Show()
            else:
                for tag in self.tags: tag.Destroy()
                self.tags = None

    def Destroy(self):
        if self.tags:
            for tag in self.tags: tag.Destroy()
            self.tags = None

    def Reparent(self, parent):
        self.parent = parent
        if self.tags:
            for tag in self.tags: tag.Reparent(parent)

# end of class SelectionMarker

#----------------------------------------------------------------------------

import common
_encode = common._encode_from_xml

def bound(number, lower, upper):
    return min(max(lower, number), upper)

def color_to_string(color):
    """\
    returns the hexadecimal string representation of the given color:
    for example: wxWHITE ==> #ffffff
    """
    import operator
    return '#' + reduce(operator.add, ['%02x' % bound(c, 0, 255) for c in
                                       color.Get()])

def string_to_color(color):
    """\
    returns the wxColour which corresponds to the given
    hexadecimal string representation:
    for example: #ffffff ==> wxColour(255, 255, 255)
    """
    if len(color) != 7: raise ValueError
    return apply(wx.Colour, [int(color[i:i+2], 16) for i in range(1, 7, 2)])

    
def get_toplevel_parent(obj):
    if not isinstance(obj, wx.Window): window = obj.widget
    else: window = obj
    while window and not window.IsTopLevel():
        window = window.GetParent()
    return window


def get_toplevel_widget(widget):
    from edit_windows import EditBase, TopLevelBase
    from edit_sizers import Sizer
    if isinstance(widget, Sizer):
        widget = widget.window
    assert isinstance(widget, EditBase), _("EditBase or SizerBase object needed")
    while widget and not isinstance(widget, TopLevelBase):
        widget = widget.parent
    return widget


if wx.Platform == '__WXGTK__':
    # default wxMenu seems to have probles with SetTitle on GTK
    class wxGladePopupMenu(wx.Menu):
        def __init__(self, title):
            wx.Menu.__init__(self)
            self.TITLE_ID = wx.NewId()
            item = self.Append(self.TITLE_ID, title)
            self.AppendSeparator()
            font = item.GetFont()
            font.SetWeight(wx.BOLD)
            item.SetFont(wx.Font(font.GetPointSize(), font.GetFamily(),
                                 font.GetStyle(), wx.BOLD))

        def SetTitle(self, title):
            self.SetLabel(self.TITLE_ID, title)

else: wxGladePopupMenu = wx.Menu


def check_wx_version(major, minor=0, release=0, revision=0):
    """\
    returns True if the current wxPython version is at least
    major.minor.release
    """
    #from wxPython import wx
    import wx
    #return wx.__version__ >= "%d.%d.%d.%d" % (major, minor, release, revision)
    return wx.VERSION[:-1] >= (major, minor, release, revision)


if not check_wx_version(2, 3, 3):
    # the following is copied from wx.py of version 2.3.3, as 2.3.2 doesn't
    # have it
    _wxCallAfterId = None

    def wxCallAfter(callable, *args, **kw):
        """
        Call the specified function after the current and pending event
        handlers have been completed.  This is also good for making GUI
        method calls from non-GUI threads.
        """
        app = wxGetApp()
        assert app, _('No wxApp created yet')

        global _wxCallAfterId
        if _wxCallAfterId is None:
            _wxCallAfterId = wxNewId()
            app.Connect(-1, -1, _wxCallAfterId,
                  lambda event: apply(event.callable, event.args, event.kw) )
        evt = wxPyEvent()
        evt.SetEventType(_wxCallAfterId)
        evt.callable = callable
        evt.args = args
        evt.kw = kw
        wxPostEvent(app, evt)
else:
    wxCallAfter = wx.CallAfter

#----------------------------------------------------------------------

use_menu_icons = None

_item_bitmaps = {}
def append_item(menu, id, text, xpm_file_or_artid=None):
    global use_menu_icons
    if use_menu_icons is None:
        import config
        use_menu_icons = config.preferences.use_menu_icons
    if wx.Platform == '__WXGTK__' and wx.VERSION == (2, 4, 1, 2, ''):
        use_menu_icons = 0
    import common, os.path
    item = wx.MenuItem(menu, id, text)
    if wx.Platform == '__WXMSW__': path = 'icons/msw/'
    else: path = 'icons/gtk/'
    path = os.path.join(common.wxglade_path, path)
    if use_menu_icons and xpm_file_or_artid is not None:
        bmp = None
        if not xpm_file_or_artid.startswith('wxART_'):
            try: bmp = _item_bitmaps[xpm_file_or_artid]
            except KeyError:
                f = os.path.join(path, xpm_file_or_artid)
                if os.path.isfile(f):
                    bmp = _item_bitmaps[xpm_file_or_artid] = \
                          wx.Bitmap(f, wx.BITMAP_TYPE_XPM)
                else: bmp = None
        else:
            # xpm_file_or_artid is an id for wx.ArtProvider
            bmp = wx.ArtProvider.GetBitmap(
                xpm_file_or_artid, wx.ART_MENU, (16, 16))
        if bmp is not None:
            try: item.SetBitmap(bmp)
            except AttributeError: pass
    menu.AppendItem(item)


#----------- 2002-11-01 ------------------------------------------------------
# if not None, this is the currently selected widget - This is different from
# tree.WidgetTree.cur_widget because it takes into account also SizerSlot
# objects
# this is an implementation hack, used to handle keyboard shortcuts for
# popup menus properly (for example, to ensure that the object to remove is
# the currently highlighted one, ecc...)
focused_widget = None


def _remove():
    global focused_widget
    if focused_widget is not None:
        focused_widget.remove()
        focused_widget = None
        
def _cut():
    global focused_widget
    if focused_widget is not None:
        try: focused_widget.clipboard_cut()
        except AttributeError: pass
        else: focused_widget = None
        
def _copy():
    if focused_widget is not None:
        try: focused_widget.clipboard_copy()
        except AttributeError: pass

def _paste():
    if focused_widget is not None:
        try: focused_widget.clipboard_paste()
        except AttributeError: pass

# accelerator table to enable keyboard shortcuts for the popup menus of the
# various widgets (remove, cut, copy, paste)
accel_table = [
    (0, wx.WXK_DELETE, _remove),
    (wx.ACCEL_CTRL, ord('C'), _copy),
    (wx.ACCEL_CTRL, ord('X'), _cut),
    (wx.ACCEL_CTRL, ord('V'), _paste),
    ]
#-----------------------------------------------------------------------------

def _reverse_dict(src):
    """\
    Returns a dictionary whose keys are 'src' values and values 'src' keys.
    """
    ret = {}
    for key, val in src.iteritems():
        ret[val] = key
    return ret


#-----------------------------------------------------------------------------
def sizer_fixed_Insert(self, *args, **kw):
    """\
    This function fixes a bug in wxPython 2.4.0.2, which fails to call
    InsertSizer when the 2nd argument is a Sizer
    """
    if type(args[1]) == type(1):
        apply(self.InsertSpacer, args, kw)
    elif isinstance(args[1], wxSizerPtr):
        apply(self.InsertSizer, args, kw)
    else:
        apply(self.InsertWindow, args, kw)

#-----
# if not None, this is the SizerSlot wich has the "mouse focus": this is used
# to restore the mouse cursor if the user cancelled the addition of a widget
_currently_under_mouse = None


#-----------------------------------------------------------------------------
def get_geometry(win):
    x, y = win.GetPosition()
    w, h = win.GetSize()
    if 0 <= x <= wx.SystemSettings_GetMetric(wx.SYS_SCREEN_X) and \
       0 <= y <= wx.SystemSettings_GetMetric(wx.SYS_SCREEN_Y):
        return (x, y, w, h)
    return None


def set_geometry(win, geometry):
    if geometry is None: return
    try:
        if len(geometry) == 4:
            win.SetDimensions(*[int(x) for x in geometry])
        else:
            win.SetPosition([int(x) for x in geometry])
    except Exception, e:
        print e


#-----------------------------------------------------------------------------
# snagged out of the Python cookbook
def import_name(module_path, name):
    import imp, os
    path, mname = os.path.split(module_path)
    #print 'path, mname =', path, mname
    mname = os.path.splitext(mname)[0]
    #print 'mname:', mname
    try:
        mfile, pathname, description = imp.find_module(mname, [path])
        try:
            module = imp.load_module(mname, mfile, pathname, description)
        finally:
            mfile.close()
    except ImportError:
        import traceback; traceback.print_exc()
        return None
    return vars(module)[name]


#------------------------------------------------------------------------------
# helper functions to work with a Unicode-enabled wxPython
#------------------------------------------------------------------------------

def streq(s1, s2):
    """\
    Returns True if the strings or unicode objects s1 and s2 are equal, i.e.
    contain the same text. Appropriate encoding/decoding are performed to
    make the comparison
    """
    try:
        return s1 == s2
    except UnicodeError:
        if type(s1) == type(u''):
            s1 = s1.encode(common.app_tree.app.encoding)
        else:
            s2 = s2.encode(common.app_tree.app.encoding)
        return s1 == s2


def wxstr(s, encoding=None):
    """\
    Converts the object s to str or unicode, according to what wxPython expects
    """
    if encoding is None:
        if common.app_tree is None:
            return str(s)
        else:
            encoding = common.app_tree.app.encoding
    if wx.USE_UNICODE:
        if type(s) != type(u''):
            return unicode(str(s), encoding)
        else:
            return s
    else:
        if type(s) == type(u''):
            return s.encode(encoding)
        else:
            return str(s)


#------------------------------------------------------------------------------
# wxPanel used to reparent the property-notebooks when they are hidden. This
# has been added on 2003-06-22 to fix what seems to me a (wx)GTK2 bug
#------------------------------------------------------------------------------
hidden_property_panel = None


#------------------------------------------------------------------------------

try:
    enumerate = enumerate
except NameError:
    class enumerate(object):
        """\
        Python 2.2.x replacement for the `enumerate' builtin.
        """
        def __init__(self, iterable):
            self.iterable = iterable
            self.index = -1

        def __iter__(self):
            self.iterable = iter(self.iterable)
            return self

        def next(self):
            val = self.iterable.next()
            self.index += 1
            return self.index, val

    # end of class enumerate


def design_title(title):
    return _('<Design> - ') + title


import re
_get_xpm_bitmap_re = re.compile(r'"(?:[^"]|\\")*"')
del re
    
def get_xpm_bitmap(path):
    import os
    bmp = wx.NullBitmap
    if not os.path.exists(path):
        if '.zip' in path:
            import zipfile
            archive, name = path.split('.zip', 1)
            archive += '.zip'
            if name.startswith(os.sep):
                name = name.split(os.sep, 1)[1]
            if zipfile.is_zipfile(archive):
                # extract the XPM lines...
                try:
                    data = zipfile.ZipFile(archive).read(name)
                    data = [d[1:-1] for d in _get_xpm_bitmap_re.findall(data)]
##                     print "DATA:"
##                     for d in data: print d
                    bmp = wx.BitmapFromXPMData(data)
                except:
                    import traceback; traceback.print_exc()
                    bmp = wx.NullBitmap
    else:
        bmp = wx.Bitmap(path, wx.BITMAP_TYPE_XPM)
    return bmp


def get_relative_path(path, for_preview=False):
    """\
    Get an absolute path relative to the current output directory (where the
    code is generated).
    """
    import os
    if os.path.isabs(path):
        return path
    p = common.app_tree.app.output_path
    if for_preview:
        p = getattr(common.app_tree.app, 'real_output_path', '')
        p = common._encode_from_xml(common._encode_to_xml(p))
    d = os.path.dirname(p)
    if d:
        path = os.path.join(d, path)
    else:
        path = os.path.abspath(path)
    return path
