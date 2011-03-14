# checkbox.py: wxCheckBox objects
# $Id: checkbox.py,v 1.14 2007/03/27 07:02:03 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc
from edit_windows import ManagedBase
from tree import Tree
from widget_properties import *

class EditCheckBox(ManagedBase):

    events = ['EVT_CHECKBOX']
    
    def __init__(self, name, parent, id, label, sizer, pos, property_window,
                 show=True):
        """\
        Class to handle wxCheckBox objects
        """
        import config
        ManagedBase.__init__(self, name, 'wxCheckBox', parent, id, sizer,
                             pos, property_window, show=show)
        self.label = label
        self.value = 0 # if nonzero, che checkbox is checked

        self.access_functions['label'] = (self.get_label, self.set_label)
        self.access_functions['checked'] = (self.get_value, self.set_value)
        self.properties['label'] = TextProperty(self, 'label', None,
                                                multiline=True, label=_("label"))
        self.properties['checked'] = CheckBoxProperty(self, 'checked', None,
                                                      _('Checked'))
        # 2003-09-04 added default_border
        if config.preferences.default_border:
            self.border = config.preferences.default_border_size
            self.flag = wx.ALL

    def create_widget(self):
        self.widget = wx.CheckBox(self.parent.widget, self.id, self.label)
        self.widget.SetValue(self.value)
        def on_checkbox(event):
            self.set_value(self.value)
        wx.EVT_CHECKBOX(self.widget, self.id, on_checkbox)

    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.Panel(self.notebook, -1)
        self.properties['label'].display(panel)
        self.properties['checked'].display(panel)
        szr = wx.BoxSizer(wx.VERTICAL)
        szr.Add(self.properties['label'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['checked'].panel, 0, wx.EXPAND)
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
        self.value = int(value)
        if self.widget:
            self.widget.SetValue(self.value)
            self.sizer.set_item(self.pos, size=self.widget.GetBestSize())

# end of class EditCheckBox
   
        
def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditCheckBox objects.
    """
    label = 'checkbox_%d' % number[0]
    while common.app_tree.has_name(label):
        number[0] += 1
        label = 'checkbox_%d' % number[0]
    checkbox = EditCheckBox(label, parent, wx.NewId(), label, sizer, pos,
                            common.property_panel)
    node = Tree.Node(checkbox)
    checkbox.node = node
    checkbox.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1)

def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditCheckBox objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: label = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    if sizer is None or sizeritem is None:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    checkbox = EditCheckBox(label, parent, wx.NewId(),
                            "", sizer, pos,
                            common.property_panel, show=False) 
    sizer.set_item(checkbox.pos, option=sizeritem.option,
                   flag=sizeritem.flag, border=sizeritem.border) #,
##                   size=checkbox.GetBestSize())
    node = Tree.Node(checkbox)
    checkbox.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return checkbox


def initialize():
    """\
    initialization function for the module: returns a wx.BitmapButton to be
    added to the main palette.
    """
    common.widgets['EditCheckBox'] = builder
    common.widgets_from_xml['EditCheckBox'] = xml_builder

    return common.make_object_button('EditCheckBox', 'icons/checkbox.xpm')
