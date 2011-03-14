# common.py: global variables
# $Id: common.py,v 1.61 2007/08/07 12:21:56 agriggio Exp $
# 
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import os
# if False, the program is invoked from the command-line in "batch" mode (for
# code generation only)
use_gui = True

# version identification string
version = '0.6.2'

# program path, set in wxglade.py
wxglade_path = '.'

# widgets dictionary: each key is the name of some EditWidget class; the mapped
# value is a 'factory' function which actually builds the object. Each of these
# functions accept 3 parameters: the parent of the widget, the sizer by which
# such widget is controlled, and the position inside this sizer.
widgets = {}

# widgets_from_xml dictionary: table of factory functions to build objects from
# an xml file
widgets_from_xml = {}

# property_panel wxPanel: container inside which Properties of the current
# focused widget are displayed
property_panel = None

# app_tree Tree: represents the widget hierarchy of the application; the root 
# is the application itself 
app_tree = None

# if True, the user is adding a widget to some sizer
adding_widget = False
# needed to add toplevel sizers
adding_sizer = False

# reference to the widget that is being added: this is a key in the
# 'widgets' dictionary
widget_to_add = None

# reference to the main window (the one which contains the various buttons to
# add the different widgets)
palette = None

# dictionary which maps the ids used in the event handlers to the
# corresponding widgets: used to call the appropriate builder function
# when a dropping of a widget occurs, knowing only the id of the event
refs = {}

# dictionary which maps the name of the classes used by wxGlade to the
# correspondent classes of wxWindows
class_names = {}

# names of the Edit* classes that can be toplevels, i.e. widgets for which to
# generate a class declaration in the code
toplevels = {}

# dictionary of objects used to generate the code in a given language.
# NOTE: a code writer object must implement this interface:
#   - initialize(out_path, multi_files)
#   - language
#   - add_widget_handler(widget_name, handler[, properties_handler])
#   - add_property_handler(property_name, handler[, widget_name])
#   - add_object(top_obj, sub_obj)
#   - add_class(obj)
#   - add_sizeritem(toplevel, sizer, obj_name, option, flag, border)
#   - add_app(app_attrs, top_win_class)
#   - ...
code_writers = {}


def load_code_writers():
    """\
    Fills the common.code_writers dictionary: to do so, loads the modules
    found in the 'codegen/' subdir
    """
    import sys
    codegen_path = os.path.join(wxglade_path, 'codegen')
    sys.path.insert(0, codegen_path)
    for module in os.listdir(codegen_path):
        name, ext = os.path.splitext(module)
        if name not in sys.modules and \
               os.path.isfile(os.path.join(codegen_path, module)):
            try: writer = __import__(name).writer
            except (ImportError, AttributeError, ValueError):
                if use_gui:
                    print _('"%s" is not a valid code generator module') % \
                          module
            else:
                code_writers[writer.language] = writer
                if hasattr(writer, 'setup'): writer.setup()
                if use_gui:
                    print _('loaded code generator for %s') % writer.language

def load_widgets():
    """\
    Scans the 'widgets/' directory to find the installed widgets,
    and returns 2 lists of buttons to handle them: the first contains the
    ``core'' components, the second the user-defined ones
    """
    import config
    buttons = []
    # load the "built-in" widgets
    built_in_dir = os.path.join(wxglade_path, 'widgets')
    buttons.extend(__load_widgets(built_in_dir))
    
    # load the "local" widgets
    local_widgets_dir = config.preferences.local_widget_path
    return buttons, __load_widgets(local_widgets_dir)


def __load_widgets(widget_dir):
    buttons = []
    # test if the "widgets.txt" file exists
    widgets_file = os.path.join(widget_dir, 'widgets.txt')
    if not os.path.isfile(widgets_file):
        return buttons
        
    # add the dir to the sys.path
    import sys
    sys.path.append(widget_dir)
    modules = open(widgets_file)
    if use_gui:
        print _('Found widgets listing -> %s') % widgets_file 
        print _('loading widget modules:')
    for line in modules:
        module = line.strip()
        if not module or module.startswith('#'): continue
        module = module.split('#')[0].strip()
        try:
            try:
                b = __import__(module).initialize()
            except ImportError:
                # try importing from a zip archive
                if os.path.exists(os.path.join(widget_dir, module + '.zip')):
                    sys.path.append(os.path.join(widget_dir, module + '.zip'))
                    try: b = __import__(module).initialize()
                    finally: sys.path.pop()
                else:
                    raise
        except (ImportError, AttributeError):
            if use_gui:
                print _('ERROR loading "%s"') % module
                import traceback; traceback.print_exc()
        else:
            if use_gui: print '\t' + module
            buttons.append(b)
    modules.close()
    return buttons
    

def load_sizers():
    import edit_sizers
    return edit_sizers.init_all()


def add_object(event):
    """\
    Adds a widget or a sizer to the current app.
    """
    global adding_widget, adding_sizer, widget_to_add
    adding_widget = True
    adding_sizer = False
    tmp = event.GetId()
    widget_to_add = refs[tmp]
    # TODO: find a better way
    if widget_to_add.find('Sizer') != -1:
        adding_sizer = True


def add_toplevel_object(event):
    """\
    Adds a toplevel widget (Frame or Dialog) to the current app.
    """
    widgets[refs[event.GetId()]](None, None, 0)
    app_tree.app.saved = False


# function used by the various widget modules to add a button to the widgets
# toolbar
def make_object_button(widget, icon_path, toplevel=False, tip=None):
    """\
    creates a button for the widgets toolbar.
    Params:
      - widget: (name of) the widget the button will add to the app
      - icon_path: path to the icon used for the button
      - toplevel: true if the widget is a toplevel object (frame, dialog)
      - tip: tool tip to display
    Returns:
      the newly created wxBitmapButton
    """
    #from wxPython import wx
    import wx
    from tree import WidgetTree
    id = wx.NewId()
    if not os.path.isabs(icon_path):
        icon_path = os.path.join(wxglade_path, icon_path)
    if wx.Platform == '__WXGTK__': style = wx.NO_BORDER
    else: style = wx.BU_AUTODRAW
    import misc
    bmp = misc.get_xpm_bitmap(icon_path)
    tmp = wx.BitmapButton(palette, id, bmp, size=(31, 31), style=style)
    if not toplevel:
        wx.EVT_BUTTON(tmp, id, add_object)
    else:
        wx.EVT_BUTTON(tmp, id, add_toplevel_object)
    refs[id] = widget
    if not tip:
        tip = _('Add a %s') % widget.replace(_('Edit'), '')
    tmp.SetToolTip(wx.ToolTip(tip))

    WidgetTree.images[widget] = icon_path

    # add support for ESC key. We bind the handler to the button, because
    # (at least on GTK) EVT_CHAR are not generated for wxFrame objects...
    def on_char(event):
        #print 'on_char'
        if event.HasModifiers() or event.GetKeyCode() != wx.WXK_ESCAPE:
            event.Skip()
            return
        global adding_widget, adding_sizer, widget_to_add
        adding_widget = False
        adding_sizer = False
        widget_to_add = None
        import misc
        if misc._currently_under_mouse is not None:
            misc._currently_under_mouse.SetCursor(wx.STANDARD_CURSOR)
        event.Skip()
    wx.EVT_CHAR(tmp, on_char)

    return tmp


def _encode_from_xml(label, encoding=None):
    """\
    Returns a str which is the encoded version of the unicode label
    """
    if encoding is None:
        encoding = app_tree.app.encoding
    return label.encode(encoding, 'replace')

def _encode_to_xml(label, encoding=None):
    """\
    returns a utf-8 encoded representation of label. This is equivalent to:
    str(label).decode(encoding).encode('utf-8')
    """
    if encoding is None:
        encoding = app_tree.app.encoding
    if type(label) == type(u''):
        return label.encode('utf-8')
    return str(label).decode(encoding).encode('utf-8')


_backed_up = {} # set of filenames already backed up during this session

def save_file(filename, content, which='wxg'):
    """\
    Saves 'filename' and, if user's preferences say so and 'filename' exists,
    makes a backup copy of it. Exceptions that may occur while performing the
    operations are not handled.
    'content' is the string to store into 'filename'
    'which' is the kind of backup: 'wxg' or 'codegen'
    """
    import os, os.path, config
    if which == 'wxg': ok = config.preferences.wxg_backup
    else: ok = config.preferences.codegen_backup
    try:
        if ok and filename not in _backed_up and os.path.isfile(filename):
            # make a backup copy of filename
            infile = open(filename)
            outfile = open(filename + config.preferences.backup_suffix, 'w')
            outfile.write(infile.read())
            infile.close()
            outfile.close()
            _backed_up[filename] = 1
        # save content to file (but only if content has changed)
        savecontent = 1
        if os.path.isfile(filename):
            oldfile = open(filename)
            savecontent = (oldfile.read() != content)
            oldfile.close()
        if savecontent:
            if not os.path.isdir(os.path.dirname(filename)):
                os.mkdir(os.path.dirname(filename))
            outfile = open(filename, 'w')
            outfile.write(content)
            outfile.close()
    finally:
        if 'infile' in locals(): infile.close()
        if 'outfile' in locals(): outfile.close()
        if 'oldfile' in locals(): oldfile.close()


#------------------------------------------------------------------------------
# Autosaving, added 2004-10-15
#------------------------------------------------------------------------------

def get_name_for_autosave(filename=None):
    if filename is None: filename = app_tree.app.filename
    if not filename:
        import config
        path, name = config._get_home(), ""
    else:
        path, name = os.path.split(filename)
    ret = os.path.join(path, "#~wxg.autosave~%s#" % name)
    return ret


def autosave_current():
    if app_tree.app.saved:
        return False # do nothing in this case...
    try:
        outfile = open(get_name_for_autosave(), 'w')
        app_tree.write(outfile)
        outfile.close()
    except Exception, e:
        print e
        return False
    return True


def remove_autosaved(filename=None):
    autosaved = get_name_for_autosave(filename)
    if os.path.exists(autosaved):
        try:
            os.unlink(autosaved)
        except OSError, e:
            print e


def check_autosaved(filename):
    """\
    Returns True iff there are some auto saved data for filename
    """
    if filename is not None and filename == app_tree.app.filename:
        # this happens when reloading, no autosave-restoring in this case...
        return False
    autosaved = get_name_for_autosave(filename)
    try:
        if filename:
            orig = os.stat(filename)
            auto = os.stat(autosaved)
            return orig.st_mtime < auto.st_mtime
        else:
            return os.path.exists(autosaved)
    except OSError, e:
        if e.errno != 2: print e
        return False
    

def restore_from_autosaved(filename):
    autosaved = get_name_for_autosave(filename)
    # when restoring, make a backup copy (if user's preferences say so...)
    if os.access(autosaved, os.R_OK):
        try:
            save_file(filename, open(autosaved).read(), 'wxg')
        except OSError, e:
            print e
            return False
        return True
    return False


def generated_from():
    import config
    if config.preferences.write_generated_from and app_tree.app.filename:
        return ' from "' + app_tree.app.filename + '"'
    return ""


class MessageLogger(object):
    def __init__(self):
        self.disabled = False
        self.lines = []
        self.logger = None

    def _setup_logger(self):
        import msgdialog
        self.logger = msgdialog.MessageDialog(None, -1, "")
        self.logger.msg_list.InsertColumn(0, "")

    def __call__(self, kind, fmt, *args):
        if self.disabled:
            return
        kind = kind.upper()
        if use_gui:
            import wx, misc
            if args:
                msg = misc.wxstr(fmt) % tuple([misc.wxstr(a) for a in args])
            else:
                msg = misc.wxstr(fmt)
            self.lines.extend(msg.splitlines())
##             if kind == 'WARNING':
##                 wx.LogWarning(msg)
##             else:
##                 wx.LogMessage(msg)
        else:
            if args: msg = fmt % tuple(args)
            else: msg = fmt
            print "%s: %s" % (kind, msg)

    def flush(self):
        if self.lines and use_gui:
            if not self.logger: self._setup_logger()
            self.logger.msg_list.Freeze()
            self.logger.msg_list.DeleteAllItems()
            for line in self.lines:
                self.logger.msg_list.Append([line])
            self.lines = []            
            self.logger.msg_list.SetColumnWidth(0, -1)
            self.logger.msg_list.Thaw()
            self.logger.ShowModal()

# end of class MessageLogger

message = MessageLogger()
