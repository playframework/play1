# toggle_button.py: wxToggleButton objects
# $Id: toggle_button.py,v 1.14 2007/03/27 07:01:51 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc
from edit_windows import ManagedBase
from tree import Tree
from widget_properties import *


class EditToggleButton(ManagedBase):

    events = ['EVT_TOGGLEBUTTON']
    
    def __init__(self, name, parent, id, label, sizer, pos, property_window,
                 show=True):
        """\
        Class to handle wxToggleButton objects
        """
        import config
        ManagedBase.__init__(self, name, 'wxToggleButton', parent, id, sizer,
                             pos, property_window, show=show)
        self.label = label
        self.value = 0

        self.access_functions['label'] = (self.get_label, self.set_label)
        self.access_functions['value'] = (self.get_value, self.set_value)
        self.properties['label'] = TextProperty(self, 'label', None,
                                                multiline=True, label=_("label"))
        self.properties['value'] = CheckBoxProperty(self, 'value', None,
                                                    _('Clicked'))
        # 2003-09-04 added default_border
        if config.preferences.default_border:
            self.border = config.preferences.default_border_size
            self.flag = wx.ALL

    def create_widget(self):
        self.widget = wx.ToggleButton(self.parent.widget, self.id, self.label)
        self.widget.SetValue(self.value)
        wx.EVT_TOGGLEBUTTON(self.widget, self.id, self.on_set_focus)        

    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.Panel(self.notebook, -1)
        szr = wx.BoxSizer(wx.VERTICAL)
        self.properties['label'].display(panel)
        self.properties['value'].display(panel)
        szr.Add(self.properties['label'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['value'].panel, 0, wx.EXPAND)
        panel.SetAutoLayout(True)
        panel.SetSizer(szr)
        szr.Fit(panel)
        self.notebook.AddPage(panel, 'Widget')

    def get_label(self):
        return self.label

    def set_label(self, value):
        value = misc.wxstr(value)
        if not misc.streq(value, self.label):
            self.label = value
            if self.widget:
                self.widget.SetLabel(value.replace('\\n', '\n'))
                if not self.properties['size'].is_active():
                    self.sizer.set_item(self.pos,
                                        size=self.widget.GetBestSize())

    def get_value(self):
        return self.value

    def set_value(self, value):
        # !!! This should be done with bool.
        # 2003-03-21 NO! bools are evil here: bool('0') == True != int('0')
        value = int(value)
        if value != self.value:
            self.value = value
            if self.widget: self.widget.SetValue(value)

# end of class EditToggleButton

        
def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditToggleButton objects.
    """
    label = 'button_%d' % number[0]
    while common.app_tree.has_name(label):
        number[0] += 1
        label = 'button_%d' % number[0]
    button = EditToggleButton(label, parent, wx.NewId(), misc._encode(label),
                              sizer, pos, common.property_panel)
    node = Tree.Node(button)
    button.node = node
    button.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1)

def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditToggleButton objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: label = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    if sizer is None or sizeritem is None:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    button = EditToggleButton(label, parent, wx.NewId(), '',
                              sizer, pos, common.property_panel)
    sizer.set_item(button.pos, option=sizeritem.option, flag=sizeritem.flag,
                   border=sizeritem.border) #, size=button.GetBestSize())
    node = Tree.Node(button)
    button.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return button

def initialize():
    """\
    initialization function for the module: returns a wxBitmapButton to be
    added to the main palette.
    """
    common.widgets['EditToggleButton'] = builder
    common.widgets_from_xml['EditToggleButton'] = xml_builder
    
    return common.make_object_button('EditToggleButton',
                                     'icons/toggle_button.xpm')
