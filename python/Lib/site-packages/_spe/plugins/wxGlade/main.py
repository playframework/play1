# main.py: Main wxGlade module: defines wxGladeFrame which contains the buttons
# to add widgets and initializes all the stuff (tree, property_frame, etc.)
# $Id: main.py,v 1.84 2007/08/07 12:21:56 agriggio Exp $
# 
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wxversion
wxversion.ensureMinimal("2.6")

#from wxPython.wx import *
import wx
from widget_properties import *
from tree import Tree, WidgetTree
import edit_sizers
import common, os, os.path, misc, config
import clipboard

import xml_parse
import template


class wxGladePropertyPanel(wx.Panel):
    """\
    Panel used to display the Properties of the various widgets
    """
    def SetTitle(self, title):
        try: self.GetParent().SetTitle(title)
        except AttributeError: pass

    def Layout(self):
        if self.is_visible():
            wx.Panel.Layout(self)
            self.GetParent().Layout()

    def is_visible(self):
        return self.GetParent().IsShown()

# end of class wxGladePropertyPanel


TOGGLE_BOX_EVENT = wx.NewEventType()

def EVT_TOGGLE_BOX(win, id, func):
    win.Connect(id, -1, TOGGLE_BOX_EVENT, func)

class ToggleBoxEvent(wx.PyCommandEvent):
    def __init__(self, id, value, strval):
        wx.PyCommandEvent.__init__(self)
        self.SetId(id)
        self.SetEventType(TOGGLE_BOX_EVENT)
        self.value = value
        self.strval = strval

    def GetValue(self):
        return self.value

    def GetStringValue(self):
        return self.strval

# end of class ToggleBoxEvent


class ToggleButtonBox(wx.Panel):
    def __init__(self, parent, id, choices=[], value=0):
        wx.Panel.__init__(self, parent, id)
        self.buttons = [wx.ToggleButton(self, -1, c) for c in choices]
        self.selected = None
        self.SetValue(value)
        for b in self.buttons:
            def handler(event, b=b):
                self.on_toggle(b, event)
            wx.EVT_TOGGLEBUTTON(self, b.GetId(), handler)
        sizer = wx.BoxSizer(wx.VERTICAL)
        for b in self.buttons:
            sizer.Add(b, 0, wx.ALL|wx.EXPAND, 1)
        self.SetAutoLayout(True)
        self.SetSizer(sizer)
        sizer.Fit(self)
        sizer.SetSizeHints(self)

    def on_toggle(self, button, event):
        if self.selected is button:
            self.selected.SetValue(True)
            return
        if self.selected is not None:
            self.selected.SetValue(False)
        self.selected = button
        wx.PostEvent(self, ToggleBoxEvent(self.GetId(), self.GetValue(),
                                         self.GetStringValue()))

    def GetValue(self):
        if self.selected is not None:
            return self.buttons.index(self.selected)
        return -1

    def GetStringValue(self):
        if self.selected is None: return None
        return self.selected.GetLabel()

    def SetValue(self, index):
        if self.selected is not None:
            self.selected.SetValue(False)
        if -1 < index < len(self.buttons):
            self.selected = self.buttons[index]
            self.selected.SetValue(True)

    def SetStringValue(self, strval):
        index = -1
        for i in range(len(self.buttons)):
            if self.buttons[i].GetLabel() == strval:
                index = i
                break
        self.SetValue(index)

# end of class ToggleButtonBox


class wxGladeArtProvider(wx.ArtProvider):
    def CreateBitmap(self, artid, client, size):
        if wx.Platform == '__WXGTK__' and artid == wx.ART_FOLDER:
            return wx.Bitmap(os.path.join(common.wxglade_path, 'icons',
                                         'closed_folder.xpm'),
                            wx.BITMAP_TYPE_XPM)
        return wx.NullBitmap

# end of class wxGladeArtProvider


class wxGladeFrame(wx.Frame):
    """\
    Main frame of wxGlade (palette)
    """
    def __init__(self, parent=None):
        style = wx.SYSTEM_MENU|wx.CAPTION|wx.MINIMIZE_BOX|wx.RESIZE_BORDER
        if misc.check_wx_version(2, 5):
            style |= wx.CLOSE_BOX
        wx.Frame.__init__(self, parent, -1, "wxGlade v%s" % common.version,
                         style=style)
        self.CreateStatusBar(1)

        if parent is None: parent = self
        common.palette = self # to provide a reference accessible
                              # by the various widget classes
        icon = wx.EmptyIcon()
        bmp = wx.Bitmap(os.path.join(common.wxglade_path, "icons/icon.xpm"),
                       wx.BITMAP_TYPE_XPM)
        icon.CopyFromBitmap(bmp)
        self.SetIcon(icon)
        self.SetBackgroundColour(wx.SystemSettings_GetColour(
            wx.SYS_COLOUR_BTNFACE))
        menu_bar = wx.MenuBar()
        file_menu = wx.Menu(style=wx.MENU_TEAROFF)
        view_menu = wx.Menu(style=wx.MENU_TEAROFF)
        help_menu = wx.Menu(style=wx.MENU_TEAROFF)
        wx.ToolTip_SetDelay(1000)

        # load the available code generators
        common.load_code_writers()
        # load the available widgets and sizers
        core_btns, custom_btns = common.load_widgets()
        sizer_btns = common.load_sizers()
        
        append_item = misc.append_item
        self.TREE_ID = TREE_ID = wx.NewId()
        append_item(view_menu, TREE_ID, _("Show &Tree\tF2"))
        self.PROPS_ID = PROPS_ID = wx.NewId()
        self.RAISE_ID = RAISE_ID = wx.NewId()
        append_item(view_menu, PROPS_ID, _("Show &Properties\tF3"))
        append_item(view_menu, RAISE_ID, _("&Raise All\tF4"))
        NEW_ID = wx.NewId()
        append_item(file_menu, NEW_ID, _("&New\tCtrl+N"), wx.ART_NEW)
        NEW_FROM_TEMPLATE_ID = wx.NewId()
        append_item(file_menu, NEW_FROM_TEMPLATE_ID,
                    _("New from &Template...\tShift+Ctrl+N"))
        OPEN_ID = wx.NewId()
        append_item(file_menu, OPEN_ID, _("&Open...\tCtrl+O"), wx.ART_FILE_OPEN)
        SAVE_ID = wx.NewId()
        append_item(file_menu, SAVE_ID, _("&Save\tCtrl+S"), wx.ART_FILE_SAVE)
        SAVE_AS_ID = wx.NewId()
        append_item(file_menu, SAVE_AS_ID, _("Save As...\tShift+Ctrl+S"),
                    wx.ART_FILE_SAVE_AS)
        SAVE_TEMPLATE_ID = wx.NewId()
        append_item(file_menu, SAVE_TEMPLATE_ID, _("Save As Template..."))
        file_menu.AppendSeparator()
        RELOAD_ID = wx.ID_REFRESH #wx.NewId()
        append_item(file_menu, RELOAD_ID, _("&Refresh\tf5")) #, wx.ART_REDO)
        GENERATE_CODE_ID = wx.NewId()
        append_item(file_menu, GENERATE_CODE_ID, _("&Generate Code\tCtrl+G"),
                    wx.ART_EXECUTABLE_FILE)
        
        file_menu.AppendSeparator()
        IMPORT_ID = wx.NewId()
        append_item(file_menu, IMPORT_ID, _("&Import from XRC...\tCtrl+I"))
        
        EXIT_ID = wx.NewId()
        file_menu.AppendSeparator()
        append_item(file_menu, EXIT_ID, _('E&xit\tCtrl+Q'), wx.ART_QUIT)
        PREFS_ID = wx.ID_PREFERENCES #NewId()
        view_menu.AppendSeparator()
        MANAGE_TEMPLATES_ID = wx.NewId()
        append_item(view_menu, MANAGE_TEMPLATES_ID, _('Templates Manager...'))
        view_menu.AppendSeparator()
        append_item(view_menu, PREFS_ID, _('Preferences...'))
        #wx.ART_HELP_SETTINGS)
        menu_bar.Append(file_menu, _("&File"))
        menu_bar.Append(view_menu, _("&View"))
        TUT_ID = wx.NewId()
        append_item(help_menu, TUT_ID, _('Contents\tF1'), wx.ART_HELP)
        ABOUT_ID = wx.ID_ABOUT #wx.NewId()
        append_item(help_menu, ABOUT_ID, _('About...'))#, wx.ART_QUESTION)
        menu_bar.Append(help_menu, _('&Help'))
        parent.SetMenuBar(menu_bar)
        # Mac tweaks...
        if wx.Platform == "__WXMAC__":
            wx.App_SetMacAboutMenuItemId(ABOUT_ID)
            wx.App_SetMacPreferencesMenuItemId(PREFS_ID)
            wx.App_SetMacExitMenuItemId(EXIT_ID)
            wx.App_SetMacHelpMenuTitleName(_('&Help'))

        # file history support
        if misc.check_wx_version(2, 3, 3):
            self.file_history = wx.FileHistory(
                config.preferences.number_history)
            self.file_history.UseMenu(file_menu)
            files = config.load_history()
            files.reverse()
            for path in files:
                self.file_history.AddFileToHistory(path.strip())
                
            def open_from_history(event):
                if not self.ask_save(): return
                infile = self.file_history.GetHistoryFile(
                    event.GetId() - wx.ID_FILE1)
                # ALB 2004-10-15 try to restore possible autosave content...
                if common.check_autosaved(infile) and \
                       wx.MessageBox(_("There seems to be auto saved data for "
                                    "this file: do you want to restore it?"),
                                    _("Auto save detected"),
                                    style=wx.ICON_QUESTION|wx.YES_NO) == wx.YES:
                    common.restore_from_autosaved(infile)
                else:
                    common.remove_autosaved(infile)
                self._open_app(infile)
                
            wx.EVT_MENU_RANGE(self, wx.ID_FILE1, wx.ID_FILE9, open_from_history)
        
        wx.EVT_MENU(self, TREE_ID, self.show_tree)
        wx.EVT_MENU(self, PROPS_ID, self.show_props_window)
        wx.EVT_MENU(self, RAISE_ID, self.raise_all)
        wx.EVT_MENU(self, NEW_ID, self.new_app)
        wx.EVT_MENU(self, NEW_FROM_TEMPLATE_ID, self.new_app_from_template)
        wx.EVT_MENU(self, OPEN_ID, self.open_app)
        wx.EVT_MENU(self, SAVE_ID, self.save_app)
        wx.EVT_MENU(self, SAVE_AS_ID, self.save_app_as)
        wx.EVT_MENU(self, SAVE_TEMPLATE_ID, self.save_app_as_template)
        def generate_code(event):
            common.app_tree.app.generate_code()
        wx.EVT_MENU(self, GENERATE_CODE_ID, generate_code)
        wx.EVT_MENU(self, EXIT_ID, lambda e: self.Close())
        wx.EVT_MENU(self, TUT_ID, self.show_tutorial)
        wx.EVT_MENU(self, ABOUT_ID, self.show_about_box)
        wx.EVT_MENU(self, PREFS_ID, self.edit_preferences)
        wx.EVT_MENU(self, MANAGE_TEMPLATES_ID, self.manage_templates)
        wx.EVT_MENU(self, IMPORT_ID, self.import_xrc)
        wx.EVT_MENU(self, RELOAD_ID, self.reload_app)

        PREVIEW_ID = wx.NewId()
        def preview(event):
            if common.app_tree.cur_widget is not None:
                p = misc.get_toplevel_widget(common.app_tree.cur_widget)
                if p is not None:
                    p.preview(None)
        wx.EVT_MENU(self, PREVIEW_ID, preview)

        self.accel_table = wx.AcceleratorTable([
            (wx.ACCEL_CTRL, ord('N'), NEW_ID),
            (wx.ACCEL_CTRL, ord('O'), OPEN_ID),
            (wx.ACCEL_CTRL, ord('S'), SAVE_ID),
            (wx.ACCEL_CTRL|wx.ACCEL_SHIFT, ord('S'), SAVE_AS_ID),
            (wx.ACCEL_CTRL, ord('G'), GENERATE_CODE_ID),
            (wx.ACCEL_CTRL, ord('I'), IMPORT_ID),
            (0, wx.WXK_F1, TUT_ID),
            (wx.ACCEL_CTRL, ord('Q'), EXIT_ID),
            (0, wx.WXK_F5, RELOAD_ID),
            (0, wx.WXK_F2, TREE_ID),
            (0, wx.WXK_F3, PROPS_ID),
            (0, wx.WXK_F4, RAISE_ID),
            (wx.ACCEL_CTRL, ord('P'), PREVIEW_ID),
            ])

        # Tutorial window
##         self.tut_frame = None
        # layout
        # if there are custom components, add the toggle box...
        if custom_btns:
            main_sizer = wx.BoxSizer(wx.VERTICAL)
            show_core_custom = ToggleButtonBox(
                self, -1, [_("Core components"), _("Custom components")], 0)

            if misc.check_wx_version(2, 5):
                core_sizer = wx.FlexGridSizer(
                    0, config.preferences.buttons_per_row)
                custom_sizer = wx.FlexGridSizer(
                    0, config.preferences.buttons_per_row)
            else:
                core_sizer = wx.GridSizer(
                    0, config.preferences.buttons_per_row)
                custom_sizer = wx.GridSizer(
                    0, config.preferences.buttons_per_row)                
            self.SetAutoLayout(True)
            # core components
            for b in core_btns: core_sizer.Add(b)
            for sb in sizer_btns: core_sizer.Add(sb)
            # custom components
            for b in custom_btns:
                custom_sizer.Add(b)
                if misc.check_wx_version(2, 5):
                    custom_sizer.Show(b, False)
            custom_sizer.Layout()
            main_sizer.Add(show_core_custom, 0, wx.EXPAND)
            main_sizer.Add(core_sizer, 0, wx.EXPAND)
            main_sizer.Add(custom_sizer, 0, wx.EXPAND)
            self.SetSizer(main_sizer)
            if not misc.check_wx_version(2, 5):
                main_sizer.Show(custom_sizer, False)
            #main_sizer.Show(1, False)
            main_sizer.Fit(self)
            # events to display core/custom components
            if misc.check_wx_version(2, 5):
                def on_show_core_custom(event):
                    show_core = True
                    show_custom = False
                    if event.GetValue() == 1:
                        show_core = False
                        show_custom = True
                    for b in custom_btns:
                        custom_sizer.Show(b, show_custom)
                    for b in core_btns:
                        core_sizer.Show(b, show_core)
                    for b in sizer_btns:
                        core_sizer.Show(b, show_core)
                    core_sizer.Layout()
                    custom_sizer.Layout()
                    main_sizer.Layout()
            else:
                def on_show_core_custom(event):
                    to_show = core_sizer
                    to_hide = custom_sizer
                    if event.GetValue() == 1:
                        to_show, to_hide = to_hide, to_show
                    main_sizer.Show(to_show, True)
                    main_sizer.Show(to_hide, False)
                    main_sizer.Layout()           
            EVT_TOGGLE_BOX(self, show_core_custom.GetId(), on_show_core_custom)
        # ... otherwise (the common case), just add the palette of core buttons
        else:
            sizer = wx.GridSizer(0, config.preferences.buttons_per_row)
            self.SetAutoLayout(True)
            # core components
            for b in core_btns: sizer.Add(b)
            for sb in sizer_btns: sizer.Add(sb)
            self.SetSizer(sizer)
            sizer.Fit(self)
        
        # Properties window
        frame_style = wx.DEFAULT_FRAME_STYLE
        frame_tool_win = config.preferences.frame_tool_win
        if frame_tool_win:
            frame_style |= wx.FRAME_NO_TASKBAR | wx.FRAME_FLOAT_ON_PARENT
            frame_style &= ~wx.MINIMIZE_BOX
            if wx.Platform != '__WXGTK__': frame_style |= wx.FRAME_TOOL_WINDOW
        
        self.frame2 = wx.Frame(self, -1, _('Properties - <app>'),
                              style=frame_style)
        self.frame2.SetBackgroundColour(wx.SystemSettings_GetColour(
            wx.SYS_COLOUR_BTNFACE))
        self.frame2.SetIcon(icon)
        
        sizer_tmp = wx.BoxSizer(wx.VERTICAL)
        property_panel = wxGladePropertyPanel(self.frame2, -1)

        #---- 2003-06-22 Fix for what seems to be a GTK2 bug (notebooks)
        misc.hidden_property_panel = wx.Panel(self.frame2, -1)
        sz = wx.BoxSizer(wx.VERTICAL)
        sz.Add(property_panel, 1, wx.EXPAND)
        sz.Add(misc.hidden_property_panel, 1, wx.EXPAND)
        self.frame2.SetSizer(sz)
        sz.Show(misc.hidden_property_panel, False)
        self.property_frame = self.frame2
        #--------------------------------------------------------
        
        property_panel.SetAutoLayout(True)
        self.hidden_frame = wx.Frame(self, -1, "")
        self.hidden_frame.Hide()
        sizer_tmp.Add(property_panel, 1, wx.EXPAND)
        self.frame2.SetAutoLayout(True)
        self.frame2.SetSizer(sizer_tmp)
        sizer_tmp = wx.BoxSizer(wx.VERTICAL)
        def hide_frame2(event):
            #menu_bar.Check(PROPS_ID, False)
            self.frame2.Hide()
        wx.EVT_CLOSE(self.frame2, hide_frame2)
        wx.EVT_CLOSE(self, self.cleanup)
        common.property_panel = property_panel
        # Tree of widgets
        self.tree_frame = wx.Frame(self, -1, _('wxGlade: Tree'),
                                  style=frame_style)
        self.tree_frame.SetIcon(icon)
        
        import application
        app = application.Application(common.property_panel)
        common.app_tree = WidgetTree(self.tree_frame, app)
        self.tree_frame.SetSize((300, 300))

        app.notebook.Show()
        sizer_tmp.Add(app.notebook, 1, wx.EXPAND)
        property_panel.SetSizer(sizer_tmp)
        sizer_tmp.Fit(property_panel)
        
        def on_tree_frame_close(event):
            #menu_bar.Check(TREE_ID, False)
            self.tree_frame.Hide()
        wx.EVT_CLOSE(self.tree_frame, on_tree_frame_close)
        # check to see if there are some remembered values
        prefs = config.preferences
        if prefs.remember_geometry:
            #print 'initializing geometry'
            try:
                x, y, w, h = prefs.get_geometry('main')
                misc.set_geometry(self, (x, y))
            except Exception, e:
                pass
            misc.set_geometry(self.frame2, prefs.get_geometry('properties'))
            misc.set_geometry(self.tree_frame, prefs.get_geometry('tree'))
        else:
            if wx.Platform == '__WXMAC__':
                self.frame2.SetSize((345, 384)) # I've been told this is OK...
                self.SetPosition((0, 45)) # to avoid the OS X menubar
            else:
                self.frame2.SetSize((max(self.GetSize()[0], 250), 350))
                self.SetPosition((0, 0))
            x, y = self.GetPosition()
            h = self.GetSize()[1]
            w = self.frame2.GetSize()[0]
            if wx.Platform != '__WXMSW__':
                # under X, IceWM (and Sawfish, too), GetSize seems to ignore
                # window decorations
                h += 60
                w += 10
            self.frame2.SetPosition((x, y+h))
            self.tree_frame.SetPosition((x+w, y))
        self.Show()
        self.tree_frame.Show()
        self.frame2.Show()    

        #self._skip_activate = False
##         if frame_tool_win:
##             def on_iconize(event):
##                 if event.Iconized():
##                     self.hide_all()
##                 else:
##                     self.show_and_raise()
##                 event.Skip()
##             wx.EVT_ICONIZE(self, on_iconize)

        if wx.Platform == '__WXMSW__':
            import about
            # I'll pay a beer to anyone who can explain to me why this prevents
            # a segfault on Win32 when you exit without doing anything!!
            self.about_box = about.wxGladeAboutBox(self.GetParent())
        else:
            self.about_box = None

        # last visited directory, used on GTK for wxFileDialog
        self.cur_dir = config.preferences.open_save_path

        # set a drop target for us...
        self._droptarget = clipboard.FileDropTarget(self)
        self.SetDropTarget(self._droptarget)
        #self.tree_frame.SetDropTarget(self._droptarget)
        #self.frame2.SetDropTarget(self._droptarget)

        # ALB 2004-10-15, autosave support...
        self.autosave_timer = None
        if config.preferences.autosave:
            TIMER_ID = wx.NewId()
            self.autosave_timer = wx.Timer(self, TIMER_ID)
            wx.EVT_TIMER(self, TIMER_ID, self.on_autosave_timer)
            self.autosave_timer.Start(
                int(config.preferences.autosave_delay) * 1000)
        # ALB 2004-10-15
        CLEAR_SB_TIMER_ID = wx.NewId()
        self.clear_sb_timer = wx.Timer(self, CLEAR_SB_TIMER_ID)
        wx.EVT_TIMER(self, CLEAR_SB_TIMER_ID, self.on_clear_sb_timer)

        self.frame2.SetAcceleratorTable(self.accel_table)
        self.tree_frame.SetAcceleratorTable(self.accel_table)

        self.Raise()

        # ALB 2004-10-16
        if common.check_autosaved(None) and \
               wx.MessageBox(_("There seems to be auto saved data "
                            "from last wxGlade session: "
                            "do you want to restore it?"),
                            _("Auto save detected"),
                            style=wx.ICON_QUESTION|wx.YES_NO) == wx.YES:
            if self._open_app(common.get_name_for_autosave(),
                              add_to_history=False):
                common.app_tree.app.saved = False
                common.app_tree.app.filename = None
                self.user_message(_("Recovery from auto save complete"))
                common.remove_autosaved()
        else:
            common.remove_autosaved()

    def on_autosave_timer(self, event):
        if common.autosave_current():
            self.user_message(_("Auto saving... done"))
        
    def edit_preferences(self, event):
        config.edit_preferences()

    def show_tree(self, event):
        self.tree_frame.Show()
        self.tree_frame.Raise()
        common.app_tree.SetFocus()

    def show_props_window(self, event):
        self.frame2.Show()
        self.frame2.Raise()
        try:
            c = self.frame2.GetSizer().GetChildren()
            if c: c[0].GetWindow().SetFocus()
        except (AttributeError, TypeError):
            self.frame2.SetFocus()

    def raise_all(self, event):
        children = self.GetChildren()
        for child in children:
            child = misc.get_toplevel_parent(child)
            if child.IsShown() and child.GetTitle(): child.Raise()
        self.Raise()

    def user_message(self, msg):
        sb = self.GetStatusBar()
        if sb:
            sb.SetStatusText(msg)
            self.clear_sb_timer.Start(5000, True)

    def on_clear_sb_timer(self, event):
        sb = self.GetStatusBar()
        if sb:
            sb.SetStatusText("")

    def ask_save(self):
        """\
        checks whether the current app has changed and needs to be saved:
        if so, prompts the user;
        returns False if the operation has been cancelled
        """
        if not common.app_tree.app.saved:
            ok = wx.MessageBox(_("Save changes to the current app?"),
                               _("Confirm"),
                               wx.YES_NO|wx.CANCEL|wx.CENTRE|wx.ICON_QUESTION)
            if ok == wx.YES:
                self.save_app(None)
            return ok != wx.CANCEL
        return True

    def new_app(self, event):
        """\
        creates a new wxGlade project
        """
        if self.ask_save():
            common.app_tree.clear()
            common.app_tree.app.filename = None
            common.app_tree.app.saved = True
            self.user_message("")
            # ALB 2004-10-15
            common.remove_autosaved()
            if config.preferences.autosave and self.autosave_timer is not None:
                self.autosave_timer.Start()

    def new_app_from_template(self, event):
        """\
        creates a new wxGlade project from an existing template file
        """
        if not self.ask_save(): return
        infile = template.select_template()
        if infile:
            self._open_app(infile, add_to_history=False)
            common.app_tree.app.template_data = None

    def reload_app(self, event):
        self.ask_save()
        if not common.app_tree.app.filename:
            wx.MessageBox(_("Impossible to reload an unsaved application"),
                          _("Alert"), style=wx.OK|wx.ICON_INFORMATION)
            return
        path = common.app_tree.get_selected_path()
        #print 'path:', path
        self._open_app(common.app_tree.app.filename, add_to_history=False)
        common.app_tree.select_path(path)
        
    def open_app(self, event_unused):
        """\
        loads a wxGlade project from an xml file
        NOTE: this is very slow and needs optimisation efforts
        NOTE2: the note above should not be True anymore :)
        """
        if not self.ask_save(): return
        from xml_parse import XmlWidgetBuilder, ProgressXmlWidgetBuilder
        infile = misc.FileSelector(_("Open file"),
                                   wildcard="wxGlade files (*.wxg)|*.wxg|"
                                   "wxGlade Template files (*.wgt)|*.wgt|"
                                   "XML files (*.xml)|*.xml|All files|*",
                                   flags=wx.OPEN|wx.FILE_MUST_EXIST,
                                   default_path=self.cur_dir)
        if infile:
            # ALB 2004-10-15 try to restore possible autosave content...
            if common.check_autosaved(infile) and \
                   wx.MessageBox(_("There seems to be auto saved data for "
                                "this file: do you want to restore it?"),
                                _("Auto save detected"),
                                style=wx.ICON_QUESTION|wx.YES_NO) == wx.YES:
                common.restore_from_autosaved(infile)
            else:
                common.remove_autosaved(infile)
            self._open_app(infile)
            self.cur_dir = os.path.dirname(infile)

    def _open_app(self, infilename, use_progress_dialog=True,
                  is_filelike=False, add_to_history=True):
        import time
        from xml_parse import XmlWidgetBuilder, ProgressXmlWidgetBuilder, \
             XmlParsingError
        from xml.sax import SAXParseException

        start = time.clock()

        common.app_tree.clear()
        if not is_filelike:
            common.app_tree.app.filename = infilename
        else:
            common.app_tree.filename = getattr(infilename, 'name', None)
        common.property_panel.Reparent(self.hidden_frame)
        # prevent the auto-expansion of nodes
        common.app_tree.auto_expand = False

        old_dir = os.getcwd()
        try:
            if not is_filelike:
                os.chdir(os.path.dirname(infilename))
                infile = open(infilename)
            else:
                infile = infilename
                infilename = getattr(infile, 'name', None)
            if use_progress_dialog and config.preferences.show_progress:
                p = ProgressXmlWidgetBuilder(input_file=infile)
            else:
                p = XmlWidgetBuilder()
            p.parse(infile)
        except (IOError, OSError, SAXParseException, XmlParsingError), msg:
            if locals().has_key('infile') and not is_filelike: infile.close()
            common.app_tree.clear()
            common.property_panel.Reparent(self.frame2)
            common.app_tree.app.saved = True
            wx.MessageBox(_("Error loading file %s: %s") % \
                          (misc.wxstr(infilename), misc.wxstr(msg)),
                          _("Error"), wx.OK|wx.CENTRE|wx.ICON_ERROR)
            # reset the auto-expansion of nodes
            common.app_tree.auto_expand = True
            os.chdir(old_dir)
            return False            
        except Exception, msg:
            import traceback; traceback.print_exc()

            if locals().has_key('infile') and not is_filelike: infile.close()
            common.app_tree.clear()
            common.property_panel.Reparent(self.frame2)
            common.app_tree.app.saved = True
            wx.MessageBox(_("An exception occurred while loading file \"%s\".\n"
                            "This is the error message associated with it:\n"
                            "        %s\n"
                            "For more details, look at the full traceback "
                            "on the console.\n"
                            "If you think this is a wxGlade bug,"
                            " please report it.") % \
                          (misc.wxstr(infilename), misc.wxstr(msg)),
                          _("Error"),
                          wx.OK|wx.CENTRE|wx.ICON_ERROR)
            # reset the auto-expansion of nodes
            common.app_tree.auto_expand = True
            os.chdir(old_dir)
            return False

        if not is_filelike:
            infile.close()
        common.app_tree.select_item(common.app_tree.root)
        common.app_tree.root.widget.show_properties()
        common.property_panel.Reparent(self.frame2)
        # reset the auto-expansion of nodes
        common.app_tree.auto_expand = True
        common.app_tree.expand()
        if common.app_tree.app.is_template:
            print _("Loaded template")
            common.app_tree.app.template_data = template.Template(infilename)
            common.app_tree.app.filename = None

        end = time.clock()
        print _('Loading time: %.5f') % (end-start)

        common.app_tree.app.saved = True
        
        if hasattr(self, 'file_history') and infilename is not None and \
               add_to_history and (not common.app_tree.app.is_template):
            self.file_history.AddFileToHistory(misc.wxstr(infilename))

        # ALB 2004-10-15
        if config.preferences.autosave and self.autosave_timer is not None:
            self.autosave_timer.Start()

        self.user_message(_("Loaded %s (%.2f seconds)") % \
                          (misc.wxstr(common.app_tree.app.filename), end-start))

        return True

    def save_app(self, event):
        """\
        saves a wxGlade project onto an xml file
        """
        if not common.app_tree.app.filename or common.app_tree.app.is_template:
            self.save_app_as(event)
        else:
            # check whether we are saving a template
            if os.path.splitext(common.app_tree.app.filename)[1] == ".wgt":
                common.app_tree.app.is_template = True
            self._save_app(common.app_tree.app.filename)

    def _save_app(self, filename):
        try:
            from cStringIO import StringIO
            buffer = StringIO()
            common.app_tree.write(buffer)
            common.save_file(filename,
                             buffer.getvalue(), 'wxg')
        except (IOError, OSError), msg:
            common.app_tree.app.saved = False
            fn = filename
            wx.MessageBox(_("Error saving app:\n%s") % msg, _("Error"),
                         wx.OK|wx.CENTRE|wx.ICON_ERROR)
        except Exception, msg:
            import traceback; traceback.print_exc()
            common.app_tree.app.saved = False
            fn = filename
            wx.MessageBox(_("An exception occurred while saving file "
                            "\"%s\".\n"
                            "This is the error message associated with it:"
                            "\n        %s\n"
                            "For more details, look at the full traceback "
                            "on the console.\nIf you think this is a "
                            "wxGlade bug,"
                            " please report it.") % (fn, msg), _("Error"),
                          wx.OK|wx.CENTRE|wx.ICON_ERROR)
        else:
            common.app_tree.app.saved = True
            common.remove_autosaved() # ALB 2004-10-15
            # ALB 2004-10-15
            if config.preferences.autosave and \
                   self.autosave_timer is not None:
                self.autosave_timer.Start()
            self.user_message(_("Saved %s") % filename)

    def save_app_as(self, event):
        """\
        saves a wxGlade project onto an xml file chosen by the user
        """
        fn = misc.FileSelector(_("Save project as..."),
                               wildcard="wxGlade files (*.wxg)|*.wxg|"
                               "wxGlade Template files (*.wgt) |*.wgt|"
                               "XML files (*.xml)|*.xml|All files|*",
                               flags=wx.SAVE|wx.OVERWRITE_PROMPT,
                               default_path=self.cur_dir)
        if fn:
            common.app_tree.app.filename = fn
            #remove the template flag so we can save the file.
            common.app_tree.app.is_template = False

            self.save_app(event)
            self.cur_dir = os.path.dirname(fn)
            if misc.check_wx_version(2, 3, 3):
                self.file_history.AddFileToHistory(fn)

    def save_app_as_template(self, event):
        data = getattr(common.app_tree.app, 'template_data', None)
        outfile, data = template.save_template(data)
        if outfile:
            common.app_tree.app.is_template = True
            common.app_tree.app.template_data = data
            self._save_app(outfile)

    def cleanup(self, event):
        if self.ask_save():
            # first, let's see if we have to save the geometry...
            prefs = config.preferences
            if prefs.remember_geometry:
                prefs.set_geometry('main', misc.get_geometry(self))
                prefs.set_geometry('tree',
                                   misc.get_geometry(self.tree_frame))
                prefs.set_geometry('properties',
                                   misc.get_geometry(self.frame2))
                prefs.changed = True
            common.app_tree.clear()
            if self.about_box: self.about_box.Destroy()
            try: config.save_preferences()
            except Exception, e:
                wx.MessageBox(_('Error saving preferences:\n%s') % e,
                              _('Error'),
                              wx.OK|wx.CENTRE|wx.ICON_ERROR)
            #self._skip_activate = True
            self.frame2.Destroy()
            self.tree_frame.Destroy()
            self.Destroy()
            common.remove_autosaved() # ALB 2004-10-15
            misc.wxCallAfter(wx.GetApp().ExitMainLoop)

    def show_about_box(self, event):
        if self.about_box is None:
            import about
            self.about_box = about.wxGladeAboutBox(None)
        self.about_box.ShowModal()

    def show_tutorial(self, event):
        docs_path = os.path.join(common.wxglade_path, 'docs', 'index.html')
        if wx.Platform == "__WXMAC__":
            os.system('open -a Help\ Viewer.app %s' % docs_path)
        else:
            import webbrowser, threading
            # ALB 2004-08-15: why did this block the program?????
            # (at least on linux - GTK)
            def go():
                webbrowser.open_new(docs_path)
            t = threading.Thread(target=go)
            t.setDaemon(True)
            t.start()

    def show_and_raise(self):
        self.frame2.Show()#self.GetMenuBar().IsChecked(self.PROPS_ID))
        self.tree_frame.Show()#self.GetMenuBar().IsChecked(self.TREE_ID))
        self.frame2.Raise()
        self.tree_frame.Raise()
        self.Raise()

    def hide_all(self):
        self.tree_frame.Hide()
        self.frame2.Hide()

    def import_xrc(self, event):
        import xrc2wxg, cStringIO

        if not self.ask_save():
            return
        
        infile = misc.FileSelector(_("Import file"),
                                   wildcard="XRC files (*.xrc)"
                                   "|*.xrc|All files|*",
                                   flags=wx.OPEN|wx.FILE_MUST_EXIST,
                                   default_path=self.cur_dir)
        if infile:
            buf = cStringIO.StringIO()
            try:
                xrc2wxg.convert(infile, buf)
                buf.seek(0)
                self._open_app(buf, is_filelike=True)
                common.app_tree.app.saved = False
            except Exception, msg:
                import traceback; traceback.print_exc()
                wx.MessageBox(_("An exception occurred while importing file "
                                "\"%s\".\nThis is the error message associated "
                                "with it:\n        %s\n"
                                "For more details, look at the full traceback "
                                "on the console.\nIf you think this is a "
                                "wxGlade bug, please report it.") % \
                              (infile, msg), _("Error"),
                              wx.OK|wx.CENTRE|wx.ICON_ERROR)

    def manage_templates(self, event):
        to_edit = template.manage_templates()
        if to_edit is not None and self.ask_save():
            # edit the template
            # TODO, you still need to save it manually...
            self._open_app(to_edit, add_to_history=False)
            wx.MessageBox(_("To save the changes to the template, edit the "
                            "GUI as usual,\nand then click "
                            "File->Save as Template..."), _("Information"),
                          style=wx.OK|wx.ICON_INFORMATION)

# end of class wxGladeFrame


class wxGlade(wx.App):
    def OnInit(self):
        import sys
        sys.stdout = sys.__stdout__
        sys.stderr = sys.__stderr__
        # needed for wx >= 2.3.4 to disable wxPyAssertionError exceptions
        if misc.check_wx_version(2, 3, 4):
            self.SetAssertMode(0)
        wx.InitAllImageHandlers()
        config.init_preferences()

        # ALB 2004-10-27
        if wx.Platform == '__WXGTK__' and config.preferences.use_kde_dialogs:
            import kdefiledialog
            if kdefiledialog.test_kde():
                misc.FileSelector = kdefiledialog.kde_file_selector
                misc.DirSelector = kdefiledialog.kde_dir_selector

        wx.ArtProvider.PushProvider(wxGladeArtProvider())

        frame = wxGladeFrame()
##         if wx.Platform == '__WXMSW__':
##             def on_activate(event):
##                 if event.GetActive() and not frame.IsIconized():
##                     frame.show_and_raise()
##                 event.Skip()
##             wx.EVT_ACTIVATE_APP(self, on_activate)

        self.SetTopWindow(frame)
        self.SetExitOnFrameDelete(True)

        wx.EVT_IDLE(self, self.on_idle)
        
        return True

    def on_idle(self, event):
        common.message.flush()
        event.Skip()

# end of class wxGlade


def main(filename=None):
    """\
    if filename is not None, loads it
    """
    # first thing to do, patch wxSizerPtr's Insert if needed...
##     from wxPython import wx
##     if wx.__version__ == '2.4.0.2':
##         wxSizerPtr.Insert = misc.sizer_fixed_Insert

    # now, silence a deprecation warining for py2.3
    import warnings
    warnings.filterwarnings("ignore", "integer", DeprecationWarning,
                            "wxPython.gdi")
    
    app = wxGlade()
    if filename is not None:
        app.GetTopWindow()._open_app(filename, False)
    app.MainLoop()
