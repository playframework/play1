# radio_box.py: wxRadioBox objects
# $Id: radio_box.py,v 1.18 2007/03/28 12:40:12 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc
from edit_windows import ManagedBase
from tree import Tree
from widget_properties import *
from misc import wxGladeRadioButton

from ChoicesProperty import *


class EditRadioBox(ManagedBase):

    events = ['EVT_RADIOBOX']
    
    def __init__(self, name, parent, id, label, choices, major_dim, style,
                 sizer, pos, property_window, show=True):
        """\
        Class to handle wxRadioBox objects
        """
        ManagedBase.__init__(self, name, 'wxRadioBox', parent, id, sizer,
                             pos, property_window, show=show)
        self.static_box = None 
        self.selection = 0
        self.choices = choices
        self.buttons = None 
        self.major_dim = major_dim

        if not style: self.style = wx.RA_SPECIFY_ROWS
        else: self.style = style
        self.label = label
        # properties
        self.access_functions['label'] = (self.get_label, self.set_label)
        self.access_functions['choices'] = (self.get_choices, self.set_choices)
        self.access_functions['style'] = (self.get_style, self.set_style)
        self.access_functions['dimension'] = (self.get_major_dimension,
                                              self.set_major_dimension)
        self.access_functions['selection'] = (self.get_selection,
                                              self.set_selection)
        self.properties['label'] = TextProperty(self, 'label', None, label=_("label"))
        self.properties['selection'] = SpinProperty(self, 'selection', None,
                                                    r=(0, len(choices)-1), label=_("selection"))
        self.properties['choices'] = ChoicesProperty(self, 'choices', None,
                                                     [('Label',
                                                       GridProperty.STRING)],
                                                     len(choices), label=_("choices"))
        self.style_pos = [wx.RA_SPECIFY_ROWS, wx.RA_SPECIFY_COLS]
        self.properties['style'] = RadioProperty(self, 'style', None,
                                                 ['wxRA_SPECIFY_ROWS',
                                                  'wxRA_SPECIFY_COLS'], label=_("style"))
        self.properties['dimension'] = SpinProperty(self, 'dimension', None, label=_("dimension"))

    def create_widget(self):
        self.widget = wx.Panel(self.parent.widget, self.id)
        self.static_box = self.create_static_box()
        self.buttons = [ self.create_button(c) for c in self.choices ]
        if self.buttons: self.buttons[0].SetValue(True)
        self.widget.GetBestSize = self.GetBestSize
        self.widget.SetForegroundColour = self.SetForegroundColour
        self.widget.SetBackgroundColour = self.SetBackgroundColour
        self.widget.SetFont = self.SetFont
        self.set_selection(self.selection)
        self.do_layout()
        
    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.Panel(self.notebook, -1)
        szr = wx.BoxSizer(wx.VERTICAL)
        self.properties['label'].display(panel)
        self.properties['style'].display(panel)
        self.properties['dimension'].display(panel)
        self.properties['selection'].display(panel)
        self.properties['choices'].display(panel)

        self.properties['style'].set_value(self.get_style())

        szr.Add(self.properties['label'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['style'].panel, 0, wx.ALL|wx.EXPAND, 3)
        szr.Add(self.properties['dimension'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['selection'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['choices'].panel, 1, wx.ALL|wx.EXPAND, 3)
        panel.SetAutoLayout(True)
        panel.SetSizer(szr)
        szr.Fit(panel)
        self.notebook.AddPage(panel, 'Widget')
        self.properties['choices'].set_col_sizes([-1])

    def create_button(self, label):
        r = wxGladeRadioButton(self.widget, -1, label)
        wx.EVT_LEFT_DOWN(r, self.on_set_focus)
        wx.EVT_RIGHT_DOWN(r, self.popup_menu)
        return r

    def create_static_box(self):
        sb = wx.StaticBox(self.widget, -1, self.label)        
        wx.EVT_LEFT_DOWN(sb, self.on_set_focus)
        wx.EVT_RIGHT_DOWN(sb, self.popup_menu)
        return sb

    def do_layout(self):
        """\
        Lays out the radio buttons according to the values of self.style and
        self.major_dim
        """
        if not self.widget: return
        buttons_layout = self.buttons
        if self.major_dim:
            if self.style & wx.RA_SPECIFY_COLS: cols = self.major_dim; rows = 0
            else: cols = 0; rows = self.major_dim
            sizer = wx.GridSizer(rows, cols)
            if wx.Platform == '__WXGTK__':
                # we need to reorder self.buttons 'cos wxRadioBox lays out its
                # elements by colums, while wxGridSizer by rows
                import math
                if not rows: step = int(math.ceil(1.0*len(self.buttons)/cols))
                else: step = rows
                start = 0
                tmp = [ [] for i in range(step) ]
                for i in range(len(self.buttons)):
                    tmp[i%step].append(self.buttons[i])
                buttons_layout = []
                for t in tmp: buttons_layout.extend(t)
        else:
            sizer = wx.BoxSizer(wx.VERTICAL)
        for button in buttons_layout:
            w, h = button.GetBestSize()
            sizer.Add(button, 0, wx.EXPAND)
            sizer.SetItemMinSize(button, w, h)
        self.widget.SetAutoLayout(True)
        sb_sizer = wx.StaticBoxSizer(self.static_box, wx.VERTICAL)
        self.widget.SetSizer(sb_sizer)
        sb_sizer.Add(sizer, 1, wx.EXPAND)
        sb_sizer.SetMinSize(sizer.GetMinSize())
        sb_sizer.Fit(self.widget)
        sp = self.sizer_properties
        self.sizer.set_item(self.pos, size=self.widget.GetBestSize())

    def get_label(self):
        return self.label
    
    def set_label(self, value):
        value = misc.wxstr(value)
        if not misc.streq(value, self.label):
            self.label = value
            if self.static_box:
                self.static_box.SetLabel(value)
                if not self.properties['size'].is_active():
                    self.sizer.set_item(self.pos,
                                        size=self.widget.GetBestSize())

    def get_style(self):
        if self.style == wx.RA_SPECIFY_ROWS: return 0
        else: return 1

    def set_style(self, value):
        if value == 0 or value == 'wxRA_SPECIFY_ROWS':
            self.style = wx.RA_SPECIFY_ROWS
        else: self.style = wx.RA_SPECIFY_COLS
        self.set_choices(self.get_choices())
        #self.do_layout()

    def get_major_dimension(self):
        return self.major_dim

    def set_major_dimension(self, value):
        self.major_dim = int(value)
        self.set_choices(self.get_choices())
        #self.do_layout()

    def get_choices(self):
        return zip(self.choices)

    def set_choices(self, values):
        self.choices = [ misc.wxstr(v[0]) for v in values ]
        self.properties['selection'].set_range(0, len(self.choices)-1)
        if not self.widget: return

##         delta = len(values) - len(self.buttons)
##         if delta > 0:
##             self.buttons.extend([ self.create_button("")
##                                   for i in range(delta) ])
##         elif delta < 0:
##             to_remove = self.buttons[delta:]
##             self.buttons = self.buttons[:delta]
##             for b in to_remove: b.Hide(); b.Destroy()
        for b in self.buttons:
            b.Hide()
            b.Destroy()
        self.static_box = self.create_static_box()
        self.buttons = [ self.create_button("") for i in range(len(values)) ]
        for i in range(len(values)):
            self.buttons[i].SetLabel(values[i][0])
        self.do_layout()

    def get_selection(self): return self.selection

    def set_selection(self, index):
        self.selection = int(index)
        if self.widget:
            for b in self.buttons: b.SetValue(False)
            try: self.buttons[self.selection].SetValue(True)
            except IndexError: pass

    def get_property_handler(self, prop_name):
        if prop_name == 'choices':
            return ChoicesHandler(self)
        return ManagedBase.get_property_handler(self, prop_name)

    def GetBestSize(self):
        w, h = self.widget.GetSizer().GetMinSize()
        w2, h2 = self.static_box.GetBestSize()
        return max(w, w2), h

    def SetBackgroundColour(self, colour):
        wx.Panel.SetBackgroundColour(self.widget, colour)
        self.static_box.SetBackgroundColour(colour)
        for b in self.buttons: b.SetBackgroundColour(colour)
        self.widget.Refresh()
        
    def SetForegroundColour(self, colour):
        wx.Panel.SetForegroundColour(self.widget, colour)
        self.static_box.SetForegroundColour(colour)
        for b in self.buttons: b.SetForegroundColour(colour)
        self.widget.Refresh()

    def SetFont(self, font):
        wx.Panel.SetFont(self.widget, font)
        self.static_box.SetFont(font)
        for b in self.buttons: b.SetFont(font)
        self.widget.Refresh()

# end of class EditRadioBox

        
def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditRadioBox objects.
    """
    label = 'radio_box_%d' % number[0]
    while common.app_tree.has_name(label):
        number[0] += 1
        label = 'radio_box_%d' % number[0]
    radio_box = EditRadioBox(label, parent, wx.NewId(), label,
                             [misc._encode('choice 1')],
                             0, 0, sizer, pos, common.property_panel)
    #sizer.set_item(pos, 0, 0, size=radio_box.GetSize())
    node = Tree.Node(radio_box)
    radio_box.node = node
    radio_box.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1)

def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditRadioBox objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: label = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    if sizer is None or sizeritem is None:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    radio_box = EditRadioBox(label, parent, wx.NewId(), '', [], 0, 0,
                             sizer, pos, common.property_panel) 
    sizer.set_item(radio_box.pos, option=sizeritem.option,
                   flag=sizeritem.flag, border=sizeritem.border)
##                    size=radio_box.GetBestSize())
    node = Tree.Node(radio_box)
    radio_box.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return radio_box

    
def initialize():
    """\
    initialization function for the module: returns a wx.BitmapButton to be
    added to the main palette.
    """
    common.widgets['EditRadioBox'] = builder
    common.widgets_from_xml['EditRadioBox'] = xml_builder
        
    return common.make_object_button('EditRadioBox', 'icons/radio_box.xpm')
