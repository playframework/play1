# spin_ctrl.py: wxSpinCtrl objects
# $Id: spin_ctrl.py,v 1.16 2007/03/27 07:01:53 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
from edit_windows import ManagedBase
from tree import Tree
import common, misc
from widget_properties import *
   
class EditSpinCtrl(ManagedBase):
    """\
    Class to handle wxSpinCtrl objects
    """

    events = ['EVT_SPINCTRL']
    
    def __init__(self, name, parent, id, sizer, pos, property_window,
                 show=True):
        import config
        ManagedBase.__init__(self, name, 'wxSpinCtrl', parent, id, sizer, pos,
                             property_window, show=show)
        self.style = 0
        self.value = 0
        self.range = (0, 100) # Default values in wxSpinCtrl constructor.

        prop = self.properties
        self.access_functions['style'] = (self.get_style, self.set_style)
        self.access_functions['value'] = (self.get_value, self.set_value)
        self.access_functions['range'] = (self.get_range, self.set_range)
        style_labels = ('#section#' + _('Style'), 'wxSP_ARROW_KEYS', 'wxSP_WRAP',
                        'wxTE_PROCESS_ENTER',
                        'wxTE_PROCESS_TAB', 'wxTE_MULTILINE', 'wxTE_PASSWORD',
                        'wxTE_READONLY', 'wxHSCROLL', 'wxTE_RICH',
                        'wxTE_RICH2', 'wxTE_AUTO_URL', 'wxTE_NOHIDESEL',
                        'wxTE_CENTRE', 'wxTE_RIGHT', 'wxTE_LINEWRAP',
                        'wxTE_WORDWRAP', 'wxNO_BORDER')
        self.style_pos = (wx.SP_ARROW_KEYS, wx.SP_WRAP,
                          wx.TE_PROCESS_ENTER, wx.TE_PROCESS_TAB,
                          wx.TE_MULTILINE,wx.TE_PASSWORD, wx.TE_READONLY,
                          wx.HSCROLL, wx.TE_RICH, wx.TE_RICH2, wx.TE_AUTO_URL,
                          wx.TE_NOHIDESEL, wx.TE_CENTRE, wx.TE_RIGHT,
                          wx.TE_LINEWRAP, wx.TE_WORDWRAP, wx.NO_BORDER)
        prop['style'] = CheckListProperty(self, 'style', None, style_labels)
        prop['range'] = TextProperty(self, 'range', None, can_disable=True, label=_("range"))
        prop['value'] = SpinProperty(self, 'value', None, can_disable=True, label=_("value"))
        # 2003-09-04 added default_border
        if config.preferences.default_border:
            self.border = config.preferences.default_border_size
            self.flag = wx.ALL

    def create_widget(self):
        self.widget = wx.SpinCtrl(self.parent.widget, self.id,
                                  min=self.range[0], max=self.range[1],
                                  initial=self.value)

    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.ScrolledWindow(self.notebook, -1, style=wx.TAB_TRAVERSAL)
        szr = wx.BoxSizer(wx.VERTICAL)
        prop = self.properties
        prop['range'].display(panel)
        prop['value'].display(panel)
        prop['style'].display(panel)
        szr.Add(prop['range'].panel, 0, wx.EXPAND)
        szr.Add(prop['value'].panel, 0, wx.EXPAND)
        szr.Add(prop['style'].panel, 0, wx.EXPAND)
        panel.SetAutoLayout(True)
        panel.SetSizer(szr)
        szr.Fit(panel)
        self.notebook.AddPage(panel, 'Widget')        

    def get_style(self):
        retval = [0] * len(self.style_pos)
        try:
            for i in range(len(self.style_pos)):
                if self.style & self.style_pos[i]:
                    retval[i] = 1
        except AttributeError: pass
        return retval

    def set_style(self, value):
        value = self.properties['style'].prepare_value(value)
        self.style = 0
        for v in range(len(value)):
            if value[v]:
                self.style |= self.style_pos[v]
        if self.widget: self.widget.SetWindowStyleFlag(self.style)

    def get_range(self):
        # we cannot return self.range since this would become a "(0, 100)"
        # string, and we don't want the parens
        return "%s, %s" % self.range

    def set_range(self, val):
        try: min_v, max_v = map(int, val.split(','))
        except: self.properties['range'].set_value(self.get_range())
        else:
            self.range = (min_v, max_v)
            self.properties['value'].set_range(min_v, max_v)
            if self.widget: self.widget.SetRange(min_v, max_v)

    def get_value(self):
        return self.value

    def set_value(self, value):
        value = int(value)
        if self.value != value:
            self.value = value
            if self.widget: self.widget.SetValue(self.value)

# end of class EditSpinCtrl


def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditSpinCtrl objects.
    """
    name = 'spin_ctrl_%d' % number[0]
    while common.app_tree.has_name(name):
        number[0] += 1
        name = 'spin_ctrl_%d' % number[0]
    text = EditSpinCtrl(name, parent, wx.NewId(), sizer, pos,
                        common.property_panel)
    node = Tree.Node(text)
    text.node = node
    text.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1)

def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory function to build EditSpinCtrl objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: name = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    if sizer is None or sizeritem is None:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    text = EditSpinCtrl(name, parent, wx.NewId(), sizer, pos,
                        common.property_panel)
    sizer.set_item(text.pos, option=sizeritem.option, flag=sizeritem.flag,
                   border=sizeritem.border)
    node = Tree.Node(text)
    text.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return text



def initialize():
    """\
    initialization function for the module: returns a wxBitmapButton to be
    added to the main palette.
    """
    common.widgets['EditSpinCtrl'] = builder
    common.widgets_from_xml['EditSpinCtrl'] = xml_builder
        
    return common.make_object_button('EditSpinCtrl', 'icons/spin_ctrl.xpm')
