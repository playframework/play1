# application.py: Application class to store properties of the application
#                 being created
# $Id: application.py,v 1.64 2007/08/07 12:13:44 agriggio Exp $
# 
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
from widget_properties import *
from tree import Tree, WidgetTree
import common, math, misc, os, config
import traceback, re

class FileDirDialog:
    """\
    Custom class which displays a FileDialog or a DirDialog, according to the
    value of the codegen_opt of its parent (instance of Application)
    """
    def __init__(self, owner, parent, wildcard=_("All Files|*"),
                 file_message=_("Choose a file"), dir_message=None, style=0):
        self.owner = owner
        self.prev_dir = config.preferences.codegen_path
        self.wildcard = wildcard
        self.file_message = file_message
        self.dir_message = dir_message
        self.file_style = style
        self.dir_style = wx.DD_DEFAULT_STYLE|wx.DD_NEW_DIR_BUTTON
        self.parent = parent
        self.value = None

    def ShowModal(self):
        if self.owner.codegen_opt == 0:
            self.value = misc.FileSelector(
                self.file_message, self.prev_dir or "",
                wildcard=self.wildcard, flags=self.file_style)
        else:
            self.value = misc.DirSelector(
                self.dir_message, self.prev_dir or "", style=self.dir_style)
        if self.value:
            self.prev_dir = self.value
            if not os.path.isdir(self.prev_dir):
                self.prev_dir = os.path.dirname(self.prev_dir)
            return wx.ID_OK
        return wx.ID_CANCEL

    def get_value(self):
        return self.value

    def set_wildcard(self, wildcard):
        self.wildcard = wildcard
        
# end of class FileDirDialog


class Application(object):
    """\
    properties of the application being created
    """
    def __init__(self, property_window):
        self.property_window = property_window
        self.notebook = wx.Notebook(self.property_window, -1)
        if not misc.check_wx_version(2, 5, 2):
            nb_sizer = wx.NotebookSizer(self.notebook)
            self.notebook.sizer = nb_sizer
        else:
            self.notebook.sizer = None
        self.notebook.SetAutoLayout(True)
        self.notebook.Hide()
        panel = wx.ScrolledWindow(
            self.notebook, -1, style=wx.TAB_TRAVERSAL|wx.FULL_REPAINT_ON_RESIZE)
        self.name = "app" # name of the wxApp instance to generate
        self.__saved = True # if True, there are no changes to save
        self.__filename = None # name of the output xml file
        self.klass = "MyApp"
        self.codegen_opt = 0 # if != 0, generates a separate file
                             # for each class
        def set_codegen_opt(value):
            try: opt = int(value)
            except ValueError: pass
            else: self.codegen_opt = opt
        self.output_path = ""
        self.language = 'python' # output language
        def get_output_path(): return os.path.expanduser(self.output_path)
        def set_output_path(value): self.output_path = value
        self.is_template = False
        self.use_gettext = False
        def set_use_gettext(value): self.use_gettext = bool(int(value))
        self.for_version = wx.VERSION_STRING[:3]
        def set_for_version(value):
            self.for_version = self.for_version_prop.get_str_value()
        self.access_functions = {
            'name': (lambda : self.name, self.set_name),
            'class': (lambda : self.klass, self.set_klass), 
            'code_generation': (lambda : self.codegen_opt, set_codegen_opt),
            'output_path': (get_output_path, set_output_path),
            'language': (self.get_language, self.set_language),
            'encoding': (self.get_encoding, self.set_encoding),
            'use_gettext': (lambda : self.use_gettext, set_use_gettext),
            'for_version': (lambda : self.for_version, set_for_version),
            }
        self.name_prop = TextProperty(self, "name", panel, True)
        self.klass_prop = TextProperty(self, "class", panel, True)

        self.encoding = self._get_default_encoding()
        self.encoding_prop = TextProperty(self, 'encoding', panel)

        self.use_gettext_prop = CheckBoxProperty(self, "use_gettext", panel,
                                                 _("Enable gettext support"))
        TOP_WIN_ID = wx.NewId()
        self.top_win_prop = wx.Choice(panel, TOP_WIN_ID, choices=[],
                                     size=(1, -1))
        self.top_window = '' # name of the top window of the generated app

        
        self.codegen_prop = RadioProperty(self, "code_generation", panel,
                                          [_("Single file"),
                                           _("Separate file for" \
                                           " each class")],
                                          label=_("Code Generation"))

        ext = getattr(common.code_writers.get('python'),
                      'default_extensions', [])
        wildcard = []
        for e in ext:
            wildcard.append('%s files (*.%s)|*.%s' % ('Python', e, e))
        wildcard.append('All files|*')
        dialog = FileDirDialog(self, panel, '|'.join(wildcard),
                               _("Select output file"), _("Select output directory"),
                               wx.SAVE|wx.OVERWRITE_PROMPT)

        _writers = common.code_writers.keys()
        columns = 3

        self.codewriters_prop = RadioProperty(self, "language", panel,
                                              _writers, columns=columns)

        self.codewriters_prop.set_str_value('python')

        self.for_version_prop = RadioProperty(self, "for_version", panel,
                                              ['2.4', '2.6', '2.8'], columns=3,
                                              label=_("wxWidgets compatibility"))
        self.for_version_prop.set_str_value(self.for_version)
        
        # ALB 2004-01-18
        self.access_functions['use_new_namespace'] = (
            self.get_use_old_namespace, self.set_use_old_namespace)
        self.use_old_namespace_prop = CheckBoxProperty(
            self, 'use_new_namespace', panel, _('Use old "from wxPython.wx"\n'
            'import (python output only)'))
        
        # `overwrite' property - added 2003-07-15
        self.overwrite = False
        def get_overwrite(): return self.overwrite
        def set_overwrite(val): self.overwrite = bool(int(val))
        self.access_functions['overwrite'] = (get_overwrite, set_overwrite)
        self.overwrite_prop = CheckBoxProperty(self, 'overwrite', panel,
                                               _('Overwrite existing sources'))

        self.outpath_prop = DialogProperty(self, "output_path", panel,
                                           dialog, label=_('Output path'))
        BTN_ID = wx.NewId()
        btn = wx.Button(panel, BTN_ID, _("Generate code"))

        # layout of self.notebook
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer.Add(self.name_prop.panel, 0, wx.EXPAND)
        sizer.Add(self.klass_prop.panel, 0, wx.EXPAND)
        sizer.Add(self.encoding_prop.panel, 0, wx.EXPAND)
        sizer.Add(self.use_gettext_prop.panel, 0, wx.EXPAND)
        szr = wx.BoxSizer(wx.HORIZONTAL)
        from widget_properties import _label_initial_width as _w
        label = wx.StaticText(panel, -1, _("Top window"), size=(_w, -1))
        label.SetToolTip(wx.ToolTip(_("Top window")))
        szr.Add(label, 2, wx.ALL|wx.ALIGN_CENTER, 3)
        szr.Add(self.top_win_prop, 5, wx.ALL|wx.ALIGN_CENTER, 3)
        sizer.Add(szr, 0, wx.EXPAND)
        sizer.Add(self.codegen_prop.panel, 0, wx.ALL|wx.EXPAND, 4)
        sizer.Add(self.codewriters_prop.panel, 0, wx.ALL|wx.EXPAND, 4)
        sizer.Add(self.for_version_prop.panel, 0, wx.ALL|wx.EXPAND, 4)
        sizer.Add(self.use_old_namespace_prop.panel, 0, wx.EXPAND)
        sizer.Add(self.overwrite_prop.panel, 0, wx.EXPAND)
        sizer.Add(self.outpath_prop.panel, 0, wx.EXPAND)
        sizer.Add(btn, 0, wx.ALL|wx.EXPAND, 5)
        
        panel.SetAutoLayout(True)
        panel.SetSizer(sizer)
        sizer.Layout()
        sizer.Fit(panel)
        h = panel.GetSize()[1]
        self.notebook.AddPage(panel, _("Application"))
        import math
        panel.SetScrollbars(1, 5, 1, int(math.ceil(h/5.0)))

        wx.EVT_BUTTON(btn, BTN_ID, self.generate_code)
        wx.EVT_CHOICE(self.top_win_prop, TOP_WIN_ID, self.set_top_window)

        # this is here to keep the interface similar to the various widgets
        # (to simplify Tree)
        self.widget = None # this is always None

    def set_name(self, value):
        value = "%s" % value
        if not re.match(self.set_name_pattern, value):
            self.name_prop.set_value(self.name)
        else:
            self.name = value
    set_name_pattern = re.compile('^[a-zA-Z]+[\w0-9-]*$')

    def set_klass(self, value):
        value = "%s" % value
        if not re.match(self.set_klass_pattern, value):
            self.klass_prop.set_value(self.klass)
        else:
            self.klass = value
    set_klass_pattern = re.compile('^[a-zA-Z]+[\w:.0-9-]*$')

    def _get_default_encoding(self):
        """\
        Returns the name of the default character encoding of this machine
        """
        import locale
        locale.setlocale(locale.LC_ALL)
        try: return locale.nl_langinfo(locale.CODESET)
        except AttributeError: return 'ISO-8859-15' # this is what I use...

    def get_encoding(self):
        return self.encoding

    def set_encoding(self, value):
        try: unicode('a', value)
        except LookupError, e:
            wx.MessageBox(str(e), _("Error"), wx.OK|wx.CENTRE|wx.ICON_ERROR)
            self.encoding_prop.set_value(self.encoding)
        else:
            self.encoding = value

    def set_language(self, value):
        language = self.codewriters_prop.get_str_value()
        ext = getattr(common.code_writers[language], 'default_extensions', [])
        wildcard = []
        for e in ext:
            wildcard.append(_('%s files (*.%s)|*.%s') % (language.capitalize(),
                                                      e, e))
        wildcard.append(_('All files|*'))
        self.outpath_prop.dialog.set_wildcard('|'.join(wildcard))
        # check that the new language supports all the widgets in the tree
        if self.language != language:
            self.language = language
            self.check_codegen()

    def get_language(self):
        return self.language #codewriters_prop.get_str_value()

    def _get_saved(self): return self.__saved
    def _set_saved(self, value):
        if self.__saved != value:
            self.__saved = value
            t = common.app_tree.get_title().strip()
            if not value: common.app_tree.set_title('* ' + t)
            else:
                if t[0] == '*': common.app_tree.set_title(t[1:].strip())
    saved = property(_get_saved, _set_saved)

    def _get_filename(self): return self.__filename
    def _set_filename(self, value):
        if not misc.streq(self.__filename, value):
            self.__filename = value
            if self.__saved: flag = ' '
            else: flag = '* '
            if self.__filename is not None:
                common.app_tree.set_title('%s(%s)' % (flag, self.__filename))
            else:
                common.app_tree.set_title(flag)
    filename = property(_get_filename, _set_filename)
       
    def get_top_window(self): return self.top_window

    def set_top_window(self, *args):
        self.top_window = self.top_win_prop.GetStringSelection()

    def add_top_window(self, name):
        self.top_win_prop.Append("%s" % name)
        if not self.top_window:
            self.top_win_prop.SetSelection(self.top_win_prop.GetCount()-1)
            self.set_top_window()
            
    def remove_top_window(self, name):
        index = self.top_win_prop.FindString("%s" % name)
        if index != -1:
            if wx.Platform == '__WXGTK__':
                choices = [ self.top_win_prop.GetString(i) for i in \
                            range(self.top_win_prop.GetCount()) if i != index ]
                self.top_win_prop.Clear()
                for c in choices:
                    self.top_win_prop.Append(c)
            else:
                self.top_win_prop.Delete(index)

    def update_top_window_name(self, oldname, newname):
        index = self.top_win_prop.FindString(oldname)
        if index != -1:
            if self.top_window == oldname:
                self.top_window = newname
            if wx.Platform == '__WXGTK__':
                sel_index = self.top_win_prop.GetSelection()
                choices = [ self.top_win_prop.GetString(i) for i in \
                            range(self.top_win_prop.GetCount()) ]
                choices[index] = newname
                self.top_win_prop.Clear()
                for c in choices:
                    self.top_win_prop.Append(c)
                self.top_win_prop.SetSelection(sel_index)
            else:
                self.top_win_prop.SetString(index, newname)
        
    def reset(self):
        """\
        resets the default values of the attributes of the app
        """
        self.klass = "MyApp"; self.klass_prop.set_value("MyApp")
        self.klass_prop.toggle_active(False)
        self.name = "app"; self.name_prop.set_value("app")
        self.name_prop.toggle_active(False)
        self.codegen_opt = 0; self.codegen_prop.set_value(0)
        self.output_path = ""; self.outpath_prop.set_value("")
        # do not reset language, but call set_language anyway to update the
        # wildcard of the file dialog
        self.set_language(self.get_language())
        self.top_window = ''
        self.top_win_prop.Clear()
        # ALB 2004-01-18
        #self.set_use_new_namespace(True)
        #self.use_new_namespace_prop.set_value(True)
        self.set_use_old_namespace(False)
        self.use_old_namespace_prop.set_value(False)
        
    def show_properties(self, *args):
        sizer_tmp = self.property_window.GetSizer()
        child = sizer_tmp.GetChildren()[0]
        w = child.GetWindow()
        if w is self.notebook: return
        w.Hide()

        self.notebook.Reparent(self.property_window)
        child.SetWindow(self.notebook)
        w.Reparent(misc.hidden_property_panel)
        
        self.notebook.Show(True)
        self.property_window.Layout()
        self.property_window.SetTitle(_('Properties - <%s>') % self.name)
        try: common.app_tree.select_item(self.node)
        except AttributeError: pass

    def __getitem__(self, name):
        return self.access_functions[name]

    def generate_code(self, *args, **kwds):
        preview = kwds.get('preview', False)
        if not self.output_path:
            return wx.MessageBox(_("You must specify an output file\n"
                                "before generating any code"), _("Error"),
                                wx.OK|wx.CENTRE|wx.ICON_EXCLAMATION,
                                self.notebook)
        if not preview and \
               ((self.name_prop.is_active() or self.klass_prop.is_active()) \
                and self.top_win_prop.GetSelection() < 0):
            return wx.MessageBox(_("Please select a top window "
                                "for the application"), _("Error"), wx.OK |
                                wx.CENTRE | wx.ICON_EXCLAMATION, self.notebook)
                
        from cStringIO import StringIO
        out = StringIO()
        #common.app_tree.write(out) # write the xml onto a temporary buffer
        from xml_parse import CodeWriter
        try:
            # generate the code from the xml buffer
            cw = self.get_language() #self.codewriters_prop.get_str_value()
            if preview and cw == 'python': # of course cw == 'python', but...
                old = common.code_writers[cw].use_new_namespace
                common.code_writers[cw].use_new_namespace = True #False
                overwrite = self.overwrite
                self.overwrite = True
            class_names = common.app_tree.write(out) # write the xml onto a
                                                     # temporary buffer
            if not os.path.isabs(self.output_path) and \
               self.filename is not None:
                out_path = os.path.join(os.path.dirname(self.filename),
                                        self.output_path)
            else:
                out_path = None
            CodeWriter(common.code_writers[cw], out.getvalue(), True,
                       preview=preview, out_path=out_path,
                       class_names=class_names)
            if preview and cw == 'python':
                common.code_writers[cw].use_new_namespace = old
                self.overwrite = overwrite
        except (IOError, OSError), msg:
            wx.MessageBox(_("Error generating code:\n%s") % msg, _("Error"),
                         wx.OK|wx.CENTRE|wx.ICON_ERROR)
        except Exception, msg:
            import traceback; traceback.print_exc()
            wx.MessageBox(_("An exception occurred while generating the code "
                         "for the application.\n"
                         "This is the error message associated with it:\n"
                         "        %s\n"
                         "For more details, look at the full traceback "
                         "on the console.\nIf you think this is a wxGlade bug,"
                         " please report it.") % msg, _("Error"),
                         wx.OK|wx.CENTRE|wx.ICON_ERROR)
        else:
            if not preview:
                wx.MessageBox(_("Code generation completed successfully"),
                             _("Information"), wx.OK|wx.CENTRE|wx.ICON_INFORMATION)

    def get_name(self):
        if self.name_prop.is_active(): return self.name
        return ''

    def get_class(self):
        if self.klass_prop.is_active(): return self.klass
        return ''

    def update_view(self, *args): pass

    def is_visible(self): return True

    def preview(self, widget, out_name=[None]):
        if out_name[0] is None:
            import warnings
            warnings.filterwarnings("ignore", "tempnam", RuntimeWarning,
                                    "application")
            out_name[0] = os.tempnam(None, 'wxg') + '.py'
            #print 'Temporary name:', out_name[0]
        widget_class_name = widget.klass

        # make a valid name for the class (this can be invalid for
        # some sensible reasons...)
        widget.klass = widget.klass[widget.klass.rfind('.')+1:]
        widget.klass = widget.klass[widget.klass.rfind(':')+1:]
        #if widget.klass == widget.base:
        # ALB 2003-11-08: always randomize the class name: this is to make
        # preview work even when there are multiple classes with the same name
        # (which makes sense for XRC output...)
        import random
        widget.klass = '_%d_%s' % \
                       (random.randrange(10**8, 10**9), widget.klass)
            
        self.real_output_path = self.output_path
        self.output_path = out_name[0]
        real_codegen_opt = self.codegen_opt
        real_language = self.language
        real_use_gettext = self.use_gettext
        self.use_gettext = False
        self.language = 'python'
        self.codegen_opt = 0
        overwrite = self.overwrite
        self.overwrite = 0
        
        frame = None
        try:
            self.generate_code(preview=True)
            # dynamically import the generated module
            FrameClass = misc.import_name(self.output_path, widget.klass)
            if issubclass(FrameClass, wx.MDIChildFrame):
                frame = wx.MDIParentFrame(None, -1, '')
                child = FrameClass(frame, -1, '')
                child.SetTitle('<Preview> - ' + child.GetTitle())
                w, h = child.GetSize()
                frame.SetClientSize((w+20, h+20))
            elif not (issubclass(FrameClass, wx.Frame) or
                      issubclass(FrameClass, wx.Dialog)):
                # the toplevel class isn't really toplevel, add a frame...
                frame = wx.Frame(None, -1, widget_class_name)
                if issubclass(FrameClass, wx.MenuBar):
                    menubar = FrameClass()
                    frame.SetMenuBar(menubar)
                elif issubclass(FrameClass, wx.ToolBar):
                    toolbar = FrameClass(frame, -1)
                    frame.SetToolBar(toolbar)
                else:
                    panel = FrameClass(frame, -1)
                frame.Fit()
            else:
                frame = FrameClass(None, -1, '')
                # make sure we don't get a modal dialog...
                s = frame.GetWindowStyleFlag()
                frame.SetWindowStyleFlag(s & ~wx.DIALOG_MODAL)
            def on_close(event):
                frame.Destroy()
                widget.preview_widget = None
                widget.preview_button.SetLabel(_('Preview'))
            wx.EVT_CLOSE(frame, on_close)
            frame.SetTitle(_('<Preview> - %s') % frame.GetTitle())
            # raise the frame
            frame.CenterOnScreen()
            frame.Show()
            # remove the temporary file (and the .pyc/.pyo ones too)
            for ext in '', 'c', 'o', '~':
                name = self.output_path + ext
                if os.path.isfile(name):
                    os.unlink(name)
        except Exception, e:
            #traceback.print_exc()
            widget.preview_widget = None
            widget.preview_button.SetLabel(_('Preview'))
            wx.MessageBox(_("Problem previewing gui: %s") % str(e), _("Error"),
                         wx.OK|wx.CENTRE|wx.ICON_EXCLAMATION)#, self.notebook)
        # restore app state
        widget.klass = widget_class_name
        self.output_path = self.real_output_path
        del self.real_output_path
        self.codegen_opt = real_codegen_opt
        self.language = real_language
        self.use_gettext = real_use_gettext
        self.overwrite = overwrite
        return frame

    def get_use_old_namespace(self):
        try: return not common.code_writers['python'].use_new_namespace
        except: return False

    def set_use_old_namespace(self, val):
        #print "set use old namespace"
        try:
            common.code_writers['python'].use_new_namespace = not bool(int(val))
        except:
            pass

    def check_codegen(self, widget=None, language=None):
        """\
        Checks whether widget has a suitable code generator for the given
        language (default: the current active language). If not, the user is
        informed with a message.
        """
        if language is None: language = self.language
        if widget is not None:
            cname = common.class_names[widget.__class__.__name__]
            if language != 'XRC':
                ok = cname in common.code_writers[language].obj_builders
            else:
                # xrc is special...
                xrcgen = common.code_writers['XRC']
                ok = xrcgen.obj_builders.get(cname, None) is not \
                     xrcgen.NotImplementedXrcObject
            if not ok:
                common.message(_('WARNING'),
                               _('No %s code generator for %s (of type %s)'
                               ' available'),
                               language.capitalize(), widget.name, cname)
        else:
            # in this case, we check all the widgets in the tree
            def check_rec(node):
                if node.widget is not None:
                    self.check_codegen(node.widget)
                if node.children:
                    for c in node.children:
                        check_rec(c)
            if common.app_tree.root.children:
                for c in common.app_tree.root.children:
                    check_rec(c)

# end of class Application
