# splitter_window.py: wxSplitterWindow objects
# $Id: splitter_window.py,v 1.28 2007/08/07 12:15:21 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc
from tree import Tree
from widget_properties import *
from edit_windows import ManagedBase, WindowBase, EditBase
from edit_sizers.edit_sizers import Sizer, SizerSlot


class SplitterWindowSizer(Sizer):
    """\
    "Virtual sizer" responsible for the management of a SplitterWindow.
    """
    def set_item(self, pos, option=None, flag=None, border=None, size=None,
                 force_layout=True):
        """\
        Updates the layout of the item at the given pos.
        """
        #print 'set_item'
        if self.window.widget and \
                self.window.window_old and self.window.window_old.widget:
            self.window.widget.Unsplit(self.window.window_old.widget)
            self.window.window_old = None
        if self.window.window_1 and self.window.window_2:
            self.window.split()
    
    def add_item(self, item, pos=None, option=0, flag=0, border=0, size=None,
                 force_layout=True):
        """\
        Adds an item to self.window.
        """
        #print 'add_item', item.name
        if pos == 1:
            self.window.window_old = self.window.window_1
            self.window.window_1 = item
            self.window.properties['window_1'].set_value(item.name)
        else:
            self.window.window_old = self.window.window_2
            self.window.window_2 = item
            self.window.properties['window_2'].set_value(item.name)
    
    def free_slot(self, pos, force_layout=True):
        """\
        Replaces the element at pos with an empty slot
        """
        if pos == 1:
            if self.window.widget and \
                    self.window.window_1 and self.window.window_1.widget:
                self.window.widget.Unsplit(self.window.window_1.widget)
            self.window.window_1 = SizerSlot(self.window, self, pos)
            w = self.window.window_1
        else:
            if self.window.widget and \
                    self.window.window_2 and self.window.window_2.widget:
                self.window.widget.Unsplit()
            self.window.window_2 = SizerSlot(self.window, self, pos)
            w = self.window.window_2
        self.window.split()
        w.widget.SetFocus()
        
    def get_itempos(self, attrs):
        """\
        Get position of sizer item (used in xml_parse)
        """
        if hasattr(self.window.properties['window_1'], 'value') and \
                attrs['name'] == self.window.properties['window_1'].value:
            pos = 1
        else:
            pos = 2
        return pos
    
    def is_virtual(self):
        return True

# end of class SplitterWindowSizer


class EditSplitterWindow(ManagedBase):

    _custom_base_classes = True
    events = [
        'EVT_SPLITTER_SASH_POS_CHANGING',
        'EVT_SPLITTER_SASH_POS_CHANGED',
        'EVT_SPLITTER_UNSPLIT',
        'EVT_SPLITTER_DCLICK',
        ]
    
    def __init__(self, name, parent, id, style, win_1, win_2, orientation,
                 sizer, pos, property_window, show=True):
        """\
        Class to handle wxSplitterWindow objects
        """
        ManagedBase.__init__(self, name, 'wxSplitterWindow', parent, id, sizer,
                             pos, property_window, show=show)
        self.virtual_sizer = SplitterWindowSizer(self)
        if style is None: style = wx.SP_3D
        self.style = style
        self.window_1 = win_1 
        self.window_2 = win_2 
        self.orientation = orientation
        self.sash_pos = 0

        self.access_functions['style'] = (self.get_style, self.set_style)
        self.access_functions['sash_pos'] = (self.get_sash_pos,
                                             self.set_sash_pos)

        self.style_pos  = (wx.SP_3D, wx.SP_3DSASH, wx.SP_3DBORDER,
                           #wx.SP_FULLSASH,
                           wx.SP_BORDER, wx.SP_NOBORDER,
                           wx.SP_PERMIT_UNSPLIT, wx.SP_LIVE_UPDATE,
                           wx.CLIP_CHILDREN)
        style_labels = ('#section#' + _('Style'), 'wxSP_3D', 'wxSP_3DSASH',
                        'wxSP_3DBORDER', #'wxSP_FULLSASH',
                        'wxSP_BORDER',
                        'wxSP_NOBORDER', 'wxSP_PERMIT_UNSPLIT',
                        'wxSP_LIVE_UPDATE', 'wxCLIP_CHILDREN')

        self.properties['style'] = CheckListProperty(self, 'style', None,
                                                     style_labels)
        if self.orientation == wx.SPLIT_HORIZONTAL:
            od = 'wxSPLIT_HORIZONTAL'
        else: od = 'wxSPLIT_VERTICAL'
        self.access_functions['orientation'] = (self.get_orientation,
                                                self.set_orientation)
        self.properties['orientation'] = HiddenProperty(self, 'orientation', label=_("orientation"))

        self.access_functions['window_1'] = (self.get_win_1, lambda v: None)
        self.access_functions['window_2'] = (self.get_win_2, lambda v: None)
        self.properties['window_1'] = HiddenProperty(self, 'window_1')
        self.properties['window_2'] = HiddenProperty(self, 'window_2')
        self.window_1 = SizerSlot(self, self.virtual_sizer, 1)
        self.window_2 = SizerSlot(self, self.virtual_sizer, 2)

        self.properties['sash_pos'] = SpinProperty(self, 'sash_pos', None,
                                                   r=(0, 20),
                                                   can_disable=True, label=_("sash_pos")) 
        self.no_custom_class = False
        self.access_functions['no_custom_class'] = (self.get_no_custom_class,
                                                    self.set_no_custom_class)
        self.properties['no_custom_class'] = CheckBoxProperty(
            self, 'no_custom_class',
            label=_("Don't generate code for this custom class"))

    def create_widget(self):
        self.widget = wx.SplitterWindow(self.parent.widget, self.id,
                                        style=self.style)
        self.split()

    def finish_widget_creation(self):
        ManagedBase.finish_widget_creation(self, sel_marker_parent=self.widget)
        sp = self.properties['sash_pos']
        if sp.is_active():
            sp.set_value(self.sash_pos)
            self.widget.SetSashPosition(self.sash_pos)
        else:
            sp.set_value(self.widget.GetSashPosition())
        
        wx.EVT_SPLITTER_SASH_POS_CHANGED(self.widget, self.widget.GetId(),
                                         self.on_sash_pos_changed)
        
    def on_set_focus(self, event):
        self.show_properties()
        # here we must call event.Skip() also on Win32 as this we should be
        # able to move the sash
        event.Skip()

    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.ScrolledWindow(self.notebook, -1, style=wx.TAB_TRAVERSAL)
        sizer = wx.BoxSizer(wx.VERTICAL)
        self.properties['no_custom_class'].display(panel)
        self.properties['style'].display(panel)
        self.properties['sash_pos'].display(panel)
        sizer.Add(self.properties['no_custom_class'].panel, 0,
                  wx.ALL|wx.EXPAND, 3)
        sizer.Add(self.properties['style'].panel, 0, wx.EXPAND)
        sizer.Add(self.properties['sash_pos'].panel, 0, wx.EXPAND)
        panel.SetAutoLayout(True)
        panel.SetSizer(sizer)
        sizer.Fit(panel)
        self.notebook.AddPage(panel, 'Widget')
        
    def split(self):
        if not self.widget: return
        if self.window_1 and self.window_2:
            self.window_1.show_widget(True)
            self.window_2.show_widget(True)
            sp = self.properties['sash_pos'].get_value()
            if not self.properties['sash_pos'].is_active():
                if self.orientation == wx.SPLIT_VERTICAL:
                    max_pos = self.widget.GetClientSize()[0]
                else: max_pos = self.widget.GetClientSize()[1]
                sp = max_pos/2
            if self.orientation == wx.SPLIT_VERTICAL:
                self.widget.SplitVertically(self.window_1.widget,
                                            self.window_2.widget, sp)
            else:
                self.widget.SplitHorizontally(self.window_1.widget,
                                              self.window_2.widget, sp)
            for w in self.window_1, self.window_2:
                if hasattr(w, 'sel_marker'): w.sel_marker.update()

    def get_style(self):
        retval = [0] * len(self.style_pos)
        if not self.style:
            # style is wxSP_NOBORDER
            #retval[5] = 1
            retval[4] = 1
        try:
            for i in range(len(self.style_pos)):
                if self.style & self.style_pos[i]: retval[i] = 1
        except AttributeError: pass
        if retval[1] and retval[2]:
            # wx.SP_3D == wx.SP_3DSASH | wx.SP_3DBORDER
            retval[0] = 1
            retval[1] = retval[2] = 0
        elif retval[1] or retval[2]:
            retval[0] = 0
        return retval

    def set_style(self, value):
        value = self.properties['style'].prepare_value(value)
        self.style = 0
        for v in range(len(value)):
            if value[v]:
                self.style |= self.style_pos[v]
        if self.widget: self.widget.SetWindowStyleFlag(self.style)

    def get_sash_pos(self):
        return self.sash_pos

    def set_sash_pos(self, value):
        try: value = int(value)
        except ValueError: return
        self.sash_pos = value
        if self.widget:
            self.widget.SetSashPosition(value)

    def on_size(self, event):
        if not self.widget: return
        try:
            if self.orientation == wx.SPLIT_VERTICAL:
                max_pos = self.widget.GetClientSize()[0]
            else: max_pos = self.widget.GetClientSize()[1]
            self.properties['sash_pos'].set_range(-max_pos, max_pos)
            if not self.properties['sash_pos'].is_active():
                self.widget.SetSashPosition(max_pos/2)
                self.sash_pos = self.widget.GetSashPosition()
                self.properties['sash_pos'].set_value(self.sash_pos)
        except (AttributeError, KeyError): pass
        ManagedBase.on_size(self, event)

    def on_sash_pos_changed(self, event):
        self.sash_pos = self.widget.GetSashPosition()
        self.properties['sash_pos'].set_value(self.sash_pos)
        event.Skip()

    def get_orientation(self):
        od = { wx.SPLIT_HORIZONTAL: 'wxSPLIT_HORIZONTAL',
               wx.SPLIT_VERTICAL: 'wxSPLIT_VERTICAL' }
        return od.get(self.orientation, 'wxSPLIT_VERTICAL')

    def set_orientation(self, value):
        od = { 'wxSPLIT_HORIZONTAL': wx.SPLIT_HORIZONTAL,
               'wxSPLIT_VERTICAL': wx.SPLIT_VERTICAL }
        self.orientation = od.get(value, wx.SPLIT_VERTICAL)

    def get_win_1(self):
        if not isinstance(self.window_1, SizerSlot):
            return self.window_1.name
        return ''

    def get_win_2(self):
        if not isinstance(self.window_2, SizerSlot):
            return self.window_2.name
        return ''

    def get_no_custom_class(self):
        return self.no_custom_class

    def set_no_custom_class(self, value):
        self.no_custom_class = bool(int(value))

# end of class EditSplitterWindow


def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditSplitterWindow objects.
    """
    class Dialog(wx.Dialog):
        def __init__(self):
            wx.Dialog.__init__(self, None, -1, 'Select orientation')
            self.orientations = [ wx.SPLIT_VERTICAL, wx.SPLIT_HORIZONTAL ]
            self.orientation = wx.SPLIT_VERTICAL
            prop = RadioProperty(self, 'orientation', self,
                                 ['wxSPLIT_VERTICAL', 'wxSPLIT_HORIZONTAL'], label=_("orientation"))
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
            def set_orientation(o): self.orientation = self.orientations[o]
            return (lambda: self.orientation, set_orientation)
    # end of inner class

    dialog = Dialog()
    dialog.ShowModal()
    name = 'window_%d' % number[0]
    while common.app_tree.has_name(name):
        number[0] += 1
        name = 'window_%d' % number[0]
    window = EditSplitterWindow(name, parent, wx.NewId(), None, None, None,
                                dialog.orientation,
                                sizer, pos, common.property_panel, show=False)
    try:
        from panel import EditPanel
        have_panels = True
    except ImportError:
        have_panels = False
    if have_panels:
        pane1 = EditPanel(name + '_pane_1', window, wx.NewId(),
                            window.virtual_sizer, 1, common.property_panel)
        pane2 = EditPanel(name + '_pane_2', window, wx.NewId(),
                            window.virtual_sizer, 2, common.property_panel)
        window.window_1 = pane1
        window.window_2 = pane2
    
    node = Tree.Node(window)
    window.node = node
    window.virtual_sizer.node = node

    window.set_option(1)
    window.set_flag("wxEXPAND")
    window.show_widget(True)

    common.app_tree.insert(node, sizer.node, pos-1)

    if have_panels:
        node2 = Tree.Node(window.window_1)
        window.window_1.node = node2
        common.app_tree.add(node2, window.node)

        node3 = Tree.Node(window.window_2)
        window.window_2.node = node3
        common.app_tree.add(node3, window.node)

    sizer.set_item(window.pos, 1, wx.EXPAND)


def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditSplitterWindow objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: name = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    if not sizer or not sizeritem:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    window = EditSplitterWindow(name, parent, wx.NewId(), None, None, None,
                                wx.SPLIT_VERTICAL,
                                sizer, pos, common.property_panel, True)
    sizer.set_item(window.pos, option=sizeritem.option, flag=sizeritem.flag,
                   border=sizeritem.border)
    node = Tree.Node(window)
    window.node = node
    window.virtual_sizer.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return window


def initialize():
    """\
    initialization function for the module: returns a wxBitmapButton to be
    added to the main palette.
    """
    common.widgets['EditSplitterWindow'] = builder
    common.widgets_from_xml['EditSplitterWindow'] = xml_builder
    
    return common.make_object_button('EditSplitterWindow',
                                     'icons/splitter_window.xpm')
