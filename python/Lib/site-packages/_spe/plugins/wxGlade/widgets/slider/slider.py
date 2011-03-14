# slider.py: wxSlider objects
# $Id: slider.py,v 1.15 2007/08/07 12:18:34 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc
from edit_windows import ManagedBase
from tree import Tree
from widget_properties import *

class EditSlider(ManagedBase):

    events = [
        'EVT_COMMAND_SCROLL',
        'EVT_COMMAND_SCROLL_TOP',
        'EVT_COMMAND_SCROLL_BOTTOM',
        'EVT_COMMAND_SCROLL_LINEUP',
        'EVT_COMMAND_SCROLL_LINEDOWN',
        'EVT_COMMAND_SCROLL_PAGEUP',
        'EVT_COMMAND_SCROLL_PAGEDOWN',
        'EVT_COMMAND_SCROLL_THUMBTRACK',
        'EVT_COMMAND_SCROLL_THUMBRELEASE',
        'EVT_COMMAND_SCROLL_ENDSCROLL',
        ]
    
    def __init__(self, name, parent, id, style, sizer, pos,
                 property_window, show=True):
        """\
        Class to handle wxSlider objects
        """
        ManagedBase.__init__(self, name, 'wxSlider', parent, id, sizer,
                             pos, property_window, show=show)
        self.style = style
        self.value = 0
        self.range = (0, 10)

        prop = self.properties
        self.access_functions['style'] = (self.get_style, self.set_style)
        self.access_functions['value'] = (self.get_value, self.set_value)
        self.access_functions['range'] = (self.get_range, self.set_range)
        style_labels = ('#section#' + _('Style'), 'wxSL_HORIZONTAL', 'wxSL_VERTICAL',
                        'wxSL_AUTOTICKS', 'wxSL_LABELS', 'wxSL_LEFT',
                        'wxSL_RIGHT', 'wxSL_TOP', 'wxSL_BOTTOM',
                        'wxSL_SELRANGE', 'wxSL_INVERSE')
        self.style_pos = (wx.SL_HORIZONTAL, wx.SL_VERTICAL,
                          wx.SL_AUTOTICKS, wx.SL_LABELS, wx.SL_LEFT,
                          wx.SL_RIGHT, wx.SL_TOP, wx.SL_BOTTOM,
                          wx.SL_SELRANGE, wx.SL_INVERSE)
        tooltips = (_("Displays the slider horizontally (this is the default)."),
                    _("Displays the slider vertically."),
                    _("Displays tick marks."),
                    _("Displays minimum, maximum and value labels."),
                    _("Displays ticks on the left and forces the slider to be vertical."),
                    _("Displays ticks on the right and forces the slider to be vertical."),
                    _("Displays ticks on the top."),
                    _("Displays ticks on the bottom (this is the default)."),
                    _("Allows the user to select a range on the slider. Windows only."),
                    _("Inverses the mininum and maximum endpoints on the slider. Not compatible with wxSL_SELRANGE."))
        prop['style'] = CheckListProperty(self, 'style', None, style_labels, tooltips=tooltips)
        prop['range'] = TextProperty(self, 'range', None, can_disable=True, label=_("range"))
        prop['value'] = SpinProperty(self, 'value', None, can_disable=True, label=_("value"))

    def create_widget(self):
        self.widget = wx.Slider(self.parent.widget, self.id, self.value,
                                self.range[0], self.range[1], style=self.style)

    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.ScrolledWindow(self.notebook, -1, style=wx.TAB_TRAVERSAL)
        prop = self.properties
        szr = wx.BoxSizer(wx.VERTICAL)
        prop['range'].display(panel)
        prop['value'].display(panel)
        prop['style'].display(panel)
        szr.Add(prop['range'].panel, 0, wx.EXPAND)
        szr.Add(prop['value'].panel, 0, wx.EXPAND)
        szr.Add(prop['style'].panel, 0, wx.EXPAND)
        panel.SetAutoLayout(True)
        panel.SetSizer(szr)
        szr.Fit(panel)
        self.notebook.AddPage(panel, _('Widget'))

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
        return "%s, %s" % self.range

    def set_range(self, val):
        try: min_v, max_v = map(int, val.split(','))
        except: self.properties['range'].set_value(self.get_range())
        else: self.range = (min_v, max_v)
        self.properties['value'].set_range(min_v, max_v)
        if self.widget: self.widget.SetRange(min_v, max_v)

    def get_value(self):
        return self.value

    def set_value(self, value):
        value = int(value)
        if value != self.value:
            self.value = value
            if self.widget: self.widget.SetValue(value)

# end of class EditSlider

        
def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditStaticLine objects.
    """
    class Dialog(wx.Dialog):
        def __init__(self):
            wx.Dialog.__init__(self, None, -1, _('Select style'))
            self.orientations = [ wx.SL_HORIZONTAL, wx.SL_VERTICAL ]
            self.orientation = wx.SL_HORIZONTAL
            prop = RadioProperty(self, 'orientation', self,
                                 ['wxSL_HORIZONTAL', 'wxSL_VERTICAL'], label=_("orientation"))
            szr = wx.BoxSizer(wx.VERTICAL)
            szr.Add(prop.panel, 0, wx.ALL|wx.EXPAND, 10)
            style_labels = ('#section#', 'wxSL_AUTOTICKS', 'wxSL_LABELS',
                            'wxSL_LEFT', 'wxSL_RIGHT', 'wxSL_TOP')
            self.style_pos = (wx.SL_AUTOTICKS, wx.SL_LABELS, wx.SL_LEFT,
                              wx.SL_RIGHT, wx.SL_TOP)
            self.style = 0
            self.style_prop = CheckListProperty(self, 'style', self,
                                                style_labels)
            szr.Add(self.style_prop.panel, 0, wx.ALL|wx.EXPAND, 10)
            btn = wx.Button(self, wx.ID_OK, _('OK'))
            btn.SetDefault()
            szr.Add(btn, 0, wx.BOTTOM|wx.ALIGN_CENTER, 10)
            self.SetAutoLayout(True)
            self.SetSizer(szr)
            szr.Fit(self)
            self.CenterOnScreen()
            
        def __getitem__(self, value):
            if value == 'orientation':
                def set_orientation(o): self.orientation = self.orientations[o]
                return (lambda: self.orientation, set_orientation)
            else: return (self.get_style, self.set_style)
            
        def get_style(self):
            retval = [0] * len(self.style_pos)
            try:
                style = self.style
                for i in range(len(self.style_pos)):
                    if style & self.style_pos[i]:
                        retval[i] = 1
            except AttributeError: pass
            return retval

        def set_style(self, value):
            value = self.style_prop.prepare_value(value)
            style = 0
            for v in range(len(value)):
                if value[v]:
                    style |= self.style_pos[v]
            self.style = style

    # end of inner class

    dialog = Dialog()
    dialog.ShowModal()
    
    label = 'slider_%d' % number[0]
    while common.app_tree.has_name(label):
        number[0] += 1
        label = 'slider_%d' % number[0]
    slider = EditSlider(label, parent, wx.NewId(), dialog.orientation |
                        dialog.style, sizer, pos, common.property_panel)
    node = Tree.Node(slider)
    slider.node = node
    slider.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1) 


def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditSlider objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: name = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    style = 0
    if sizer is None or sizeritem is None:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    slider = EditSlider(name, parent, wx.NewId(), style, sizer,
                        pos, common.property_panel) 
    sizer.set_item(slider.pos, option=sizeritem.option,
                   flag=sizeritem.flag, border=sizeritem.border)
    node = Tree.Node(slider)
    slider.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return slider
   

def initialize():
    """\
    initialization function for the module: returns a wxBitmapButton to be
    added to the main palette.
    """
    common.widgets['EditSlider'] = builder
    common.widgets_from_xml['EditSlider'] = xml_builder
    
    return common.make_object_button('EditSlider', 'icons/slider.xpm')
