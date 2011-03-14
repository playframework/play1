# notebook.py: wxNotebook objects
# $Id: notebook.py,v 1.32 2007/08/07 12:15:21 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc
from tree import Tree
from widget_properties import *
from edit_windows import ManagedBase, WindowBase
from edit_sizers.edit_sizers import Sizer, SizerSlot

try:
    from panel import EditPanel
    _has_panel = True
except ImportError:
    _has_panel = False

def _ugly_hack_for_win32_notebook_bug(notebook_widget):
    """\
    The name should say all. The problem is hard to explain, so let me
    just illustrate a way to reproduce the bug:
    1. create a frame in wxGlade, add a notebook with two pages
    2. put a button on the first page, and a text ctrl on the second one
    3. save the app
    4. exit wxGlade, and comment out the body of this function
    5. restart wxGlade and load the previous app
    6. Try to click on the button on the first page of the notebook, and see
       what happens...

    If you don't see what I mean, please drop me an email with your version of
    Windows, Python and wxPython, because I really want to understand what's
    going on...

    So far I've not been able to reproduce the problem on a standalone minimal
    app, but as time permits I'll try again... if you succeed, please let me
    know.
    """
    #print '_ugly_hack_for_win32_notebook_bug'
    index_ok = notebook_widget.GetSelection()
    for i in range(notebook_widget.GetPageCount()):
        notebook_widget.GetPage(i).Hide()
    notebook_widget.GetPage(index_ok).Show()
    
    
class NotebookVirtualSizer(Sizer):
    '''\
    "Virtual sizer" responsible for the management of the pages of a Notebook.
    '''
    def __init__(self, *args, **kwds):
        Sizer.__init__(self, *args, **kwds)
        self._itempos = 0
    
    def set_item(self, pos, option=None, flag=None, border=None, size=None,
                 force_layout=True):
        """\
        Updates the layout of the item at the given pos.
        """
        if not self.window.widget:
            return
        pos -= 1
        label, item = self.window.tabs[pos]
        if not item or not item.widget:
            return
        if not (pos < self.window.widget.GetPageCount()):
            self.window.widget.AddPage(item.widget, label)
        elif self.window.widget.GetPage(pos) is not item.widget:
            #self.window.widget.RemovePage(pos)
            self.window.widget.DeletePage(pos)
            self.window.widget.InsertPage(pos, item.widget, label)
            self.window.widget.SetSelection(pos)
            try:
                misc.wxCallAfter(item.sel_marker.update)
            except AttributeError, e:
                #print e
                pass
        if self.window.sizer is not None:
            self.window.sizer.set_item(
                self.window.pos, size=self.window.widget.GetBestSize())
            
    def add_item(self, item, pos=None, option=0, flag=0, border=0, size=None,
                 force_layout=True):
        """\
        Adds an item to self.window.
        """
        #print 'pos:', pos, 'item.name:', item.name
        self.window.tabs[pos-1][1] = item
        item._dont_destroy = True

    def free_slot(self, pos, force_layout=True):
        """\
        Replaces the element at pos with an empty slot
        """
        if self.window._is_removing_pages or not self.window.widget:
            return
        slot = SizerSlot(self.window, self, pos)
        #print 'free:', slot, slot.pos, pos
        slot.show_widget(True)
        pos = pos-1
        label, item = self.window.tabs[pos]
        self.window.widget.RemovePage(pos)
        self.window.widget.InsertPage(pos, slot.widget, label)
        self.window.widget.SetSelection(pos)
    
    def get_itempos(self, attrs):
        """\
        Get position of sizer item (used in xml_parse)
        """
        self._itempos += 1
        return self._itempos
    
    def is_virtual(self):
        return True

# end of class NotebookVirtualSizer


class NotebookPagesProperty(GridProperty):      
    def write(self, outfile, tabs):
        from xml.sax.saxutils import escape, quoteattr
        write = outfile.write
        write('    ' * tabs + '<tabs>\n')
        tab_s = '    ' * (tabs+1)
        import widget_properties
        value = self.get_value()
        for i in range(len(value)):
            val = value[i]
            v = escape(widget_properties._encode(val[0]))
            window = None
            try:
                t = self.owner.tabs[i]
                if t[0] == val[0]: window = t[1]
            except: pass
            if window:
                write('%s<tab window=%s>' % (tab_s, quoteattr(window.name)))
                write(v)
                write('</tab>\n')
        write('    ' * tabs + '</tabs>\n')

# end of class NotebookPagesProperty


class TabsHandler:
    def __init__(self, parent):
        self.parent = parent
        self.tab_names = []
        self.curr_tab = []

    def start_elem(self, name, attrs):
        pass

    def end_elem(self, name):
        if name == 'tabs':
            self.parent.tabs = [[misc.wxstr(name), None] for name in \
                                self.tab_names]
            self.parent.properties['tabs'].set_value([[name] for name in \
                                                      self.tab_names])
            return True
        elif name == 'tab':
            self.tab_names.append("".join(self.curr_tab))
            self.curr_tab = []
        return False

    def char_data(self, data):
        self.curr_tab.append(data)

# end of class TabsHandler


class EditNotebook(ManagedBase):

    _custom_base_classes = True
    events = ['EVT_NOTEBOOK_PAGE_CHANGED', 'EVT_NOTEBOOK_PAGE_CHANGING']
    
    def __init__(self, name, parent, id, style, sizer, pos,
                 property_window, show=True):
        """\
        Class to handle wxNotebook objects
        """
        ManagedBase.__init__(self, name, 'wxNotebook', parent, id, sizer,
                             pos, property_window, show=show)
        self.virtual_sizer = NotebookVirtualSizer(self)
        self._is_removing_pages = False
        self.style = style
        self.tabs = [ ['tab1', None] ] # list of pages of this notebook
                                       # (actually a list of
                                       # 2-list label, window)

        self.access_functions['style'] = (self.get_tab_pos, self.set_tab_pos)
        self.properties['style'] = HiddenProperty(self, 'style', label=_("style"))
        self.access_functions['tabs'] = (self.get_tabs, self.set_tabs)
        tab_cols = [('Tab label', GridProperty.STRING)]
        self.properties['tabs'] = NotebookPagesProperty(self, 'tabs', None,
                                                        tab_cols, label=_("tabs"))
        del tab_cols
        self.nb_sizer = None
        self._create_slots = False

        self.no_custom_class = False
        self.access_functions['no_custom_class'] = (self.get_no_custom_class,
                                                    self.set_no_custom_class)
        self.properties['no_custom_class'] = CheckBoxProperty(
            self, 'no_custom_class',
            label=_("Don't generate code for this custom class"))

    def create_widget(self):
        self.widget = wx.Notebook(self.parent.widget, self.id, style=self.style)
        if not misc.check_wx_version(2, 5, 2):
            self.nb_sizer = wx.NotebookSizer(self.widget)

    def show_widget(self, yes):
        ManagedBase.show_widget(self, yes)
        if yes and wx.Platform in ('__WXMSW__', '__WXMAC__'):
            misc.wxCallAfter(_ugly_hack_for_win32_notebook_bug, self.widget)
        if self._create_slots:
            self._create_slots = False
            for i in range(len(self.tabs)):
                if self.tabs[i][1] is None:
                    self.tabs = self.tabs[:i]
                    self.properties['tabs'].set_value(self.get_tabs())

    def finish_widget_creation(self):
        ManagedBase.finish_widget_creation(self)
        # replace 'self' with 'self.nb_sizer' in 'self.sizer'
        if not misc.check_wx_version(2, 5, 2):
            self.sizer._fix_notebook(self.pos, self.nb_sizer)

    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.ScrolledWindow(self.notebook, -1, style=wx.TAB_TRAVERSAL)
        self.properties['no_custom_class'].display(panel)
        self.properties['tabs'].display(panel)
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer.Add(self.properties['no_custom_class'].panel, 0,
                  wx.ALL|wx.EXPAND, 3)
        sizer.Add(self.properties['tabs'].panel, 1, wx.ALL|wx.EXPAND, 3)
        panel.SetAutoLayout(True)
        panel.SetSizer(sizer)
        sizer.Fit(panel)
        self.notebook.AddPage(panel, _('Widget'))
        self.properties['tabs'].set_col_sizes([-1])

    def on_set_focus(self, event):
        self.show_properties()
        event.Skip()

    def _add_tab(self, window, pos):
        if window is None:
            window = SizerSlot(self, self.virtual_sizer, pos)
            self.tabs[pos-1][1] = window
        else:
            window._dont_destroy = True
            node = Tree.Node(window)
            window.node = node
            common.app_tree.add(node, self.node)
        if self.widget:
            window.show_widget(True)
            self.virtual_sizer.set_item(pos)
            try:
                misc.wxCallAfter(window.sel_marker.update)
            except AttributeError, e:
                #print e
                pass

    def get_tabs(self):
        return [ [n] for n, w in self.tabs ]

    def set_tabs(self, tabs):
        delta = len(self.tabs) - len(tabs)
        if delta > 0:
            self._is_removing_pages = True
            # we have to remove some pages
            i = len(tabs)
            if self.widget:
                for n, window in self.tabs[i:]:
                    self.widget.RemovePage(i)
                    window.remove(False)
            del self.tabs[i:]
            if self.widget: self.widget.SetSelection(0)
            self._is_removing_pages = False
        elif delta < 0:
            # we have to add some pages
            number = len(self.tabs)+1
            while common.app_tree.has_name(self.name + '_pane_%s' % number):
                number += 1
            pos = len(self.tabs)
            for i in range(-delta):
                self.tabs.append(['', None])
                pos += 1
                if _has_panel:
                    window = EditPanel(self.name + '_pane_%s' % number, self,
                                       -1, self.virtual_sizer, pos,
                                       self.property_window)
                    self._add_tab(window, pos)
                else:
                    self._add_tab(None, pos)
                number += 1
            if self.widget:
                self.widget.SetSelection(self.widget.GetPageCount()-1)
        # finally, we must update the labels of the tabs
        for i in range(len(tabs)):
            tt = misc.wxstr(tabs[i][0])
            if self.widget:
                self.widget.SetPageText(i, tt)
            self.tabs[i][0] = tt

    def delete(self):
        if self.widget:
            self.widget.DeleteAllPages()
        ManagedBase.delete(self)

    def get_property_handler(self, name):
        if name == 'tabs': return TabsHandler(self)
        return ManagedBase.get_property_handler(self, name)

    def find_page(self, page):
        """\
        returns the index of the given page in the notebook, or -1 if the page
        cannot be found
        """
        if not self.widget: return -1
        for i in range(len(self.tabs)):
            if self.tabs[i][1] is page:
                if i < self.widget.GetPageCount(): return i
                else: return -1
        return -1

    def get_tab_pos(self): 
        styles = { wx.NB_LEFT: 'wxNB_LEFT', wx.NB_RIGHT: 'wxNB_RIGHT',
                   wx.NB_BOTTOM: 'wxNB_BOTTOM' }
        return styles.get(self.style, '0')
    
    def set_tab_pos(self, value):
        styles = { 'wxNB_LEFT': wx.NB_LEFT, 'wxNB_RIGHT': wx.NB_RIGHT,
                   'wxNB_BOTTOM': wx.NB_BOTTOM }
        self.style = styles.get(value, 0)

    def get_no_custom_class(self):
        return self.no_custom_class

    def set_no_custom_class(self, value):
        self.no_custom_class = bool(int(value))

# end of class EditNotebook
        

def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditNotebook objects.
    """
    class Dialog(wx.Dialog):
        def __init__(self):
            wx.Dialog.__init__(self, None, -1, _('Select tab placement'))
            self.styles = [ 0, wx.NB_BOTTOM, wx.NB_LEFT, wx.NB_RIGHT ]
            self.style = 0
            prop = RadioProperty(self, 'tab_placement', self,
                                 [_('Top'), _('Bottom'), _('Left'), _('Right')],
                                 columns=2, label=_('tab_placement'))
            szr = wx.BoxSizer(wx.VERTICAL)
            szr.Add(prop.panel, 0, wx.ALL|wx.EXPAND, 10)
            btn = wx.Button(self, wx.ID_OK, _('OK'))
            btn.SetDefault()
            szr.Add(btn, 0, wx.BOTTOM|wx.ALIGN_CENTER, 10)
            self.SetAutoLayout(True)
            self.SetSizer(szr)
            szr.Fit(self)
            self.CenterOnScreen()
        def __getitem__(self, value):
            def set_style(s): self.style = self.styles[s]
            return (lambda: self.style, set_style)
    # end of inner class

    dialog = Dialog()
    dialog.ShowModal()
    name = 'notebook_%d' % number[0]
    while common.app_tree.has_name(name):
        number[0] += 1
        name = 'notebook_%d' % number[0]
    window = EditNotebook(name, parent, wx.NewId(), dialog.style,
                          sizer, pos, common.property_panel, show=False)
    if _has_panel:
        pane1 = EditPanel(name + '_pane_1', window, wx.NewId(),
                          window.virtual_sizer, 1, common.property_panel)

    node = Tree.Node(window)
    window.node = node
    window.virtual_sizer.node = node
    
    window.set_option(1)
    window.set_flag("wxEXPAND")
    window.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1)

    if _has_panel:
        window._add_tab(pane1, 1)

    sizer.set_item(window.pos, 1, wx.EXPAND)


def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditNotebook objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: name = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    if not sizer or not sizeritem:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    window = EditNotebook(name, parent, wx.NewId(), 0, sizer, pos,
                          common.property_panel, True)
    window._create_slots = True

    sizer.set_item(window.pos, option=sizeritem.option, flag=sizeritem.flag,
                   border=sizeritem.border)
    node = Tree.Node(window)
    window.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return window


def initialize():
    """\
    initialization function for the module: returns a wxBitmapButton to be
    added to the main palette.
    """
    common.widgets['EditNotebook'] = builder
    common.widgets_from_xml['EditNotebook'] = xml_builder

    return common.make_object_button('EditNotebook', 'icons/notebook.xpm')
    
