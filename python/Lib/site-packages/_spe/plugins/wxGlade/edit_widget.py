# edit_widget.py: base class for EditFoo objects
#
# Copyright (c) 2002-2004 Richard Lawson <richard.lawson@colinx.com>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc, config
from edit_windows import ManagedBase
from tree import Tree
from widget_properties import *

class EditWidget(ManagedBase):
    def __init__(self, name, klass, parent, id, label, sizer, pos,
                 property_window, show=True):
        """\
        Class to handle wxFoo objects
        """
        self.label = label
        self.default = False
        ManagedBase.__init__(self, name, klass, parent, id, sizer, pos,
                             property_window, show=show)
        
        # introspect subclass looking for properties
        # and widgets
        self.property_names = []
        self.property_proportion = {}
        attrs = dir(self)
        for attr in attrs:
            prefix = attr[0:4]
            if prefix == 'get_':
                getter = attr
                #print 'found getter ', getter
                # extract the property name
                prefix, name = attr.split('_', 1)
                #print 'getter ', name
                # check for a setter
                setter = 'set_%s' % name
                if not hasattr(self, setter):
                    #print 'no setter for %s, skipping ' % name
                    continue
                # check for a get_name_widget
                getter_widget = 'get_%s_widget' % name
                if not hasattr(self, getter_widget):
                    #print 'no widget getter for %s, skipping ' % name
                    continue
                #print 'adding property: %s' % name
                self.property_names.append(name)
                self.access_functions[name] = (getattr(self, getter),
                                               getattr(self, setter))
                prop = getattr(self, getter_widget)()
                try:
                    prop, proportion = prop
                except TypeError:
                    proportion = 0
                self.properties[name] = prop
                self.property_proportion[name] = proportion
        

    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.ScrolledWindow(self.notebook, -1, style=wx.TAB_TRAVERSAL)
        szr = wx.BoxSizer(wx.VERTICAL)
        for name in self.property_names:
            self.properties[name].display(panel)
            szr.Add(self.properties[name].panel, self.property_proportion[name],
                    wx.EXPAND)
        panel.SetAutoLayout(1)
        panel.SetSizer(szr)
        szr.Fit(panel)
        w, h = panel.GetClientSize()
        self.notebook.AddPage(panel, 'Widget')
        import math
        panel.SetScrollbars(1, 5, 1, int(math.ceil(h/5.0)))

# end of class EditWidget
        
    
def increment_label(label, number=[1]):
    _label = '%s_%d' % (label, number[0])
    while common.app_tree.has_name(_label):
        number[0] += 1
        _label = '%s_%d' % (label, number[0])
    return _label
        

def add_widget_node(widget, sizer, pos, from_xml=False,
                    option=0, flag=0, border=0):
    node = Tree.Node(widget)
    widget.node = node

    if not border and config.preferences.default_border:
        flag |= wx.ALL
        border = config.preferences.default_border_size

    if option: widget.set_option(option)
    if flag: widget.set_int_flag(flag)
    if border: widget.set_border(border)   
    if not from_xml: widget.show_widget(True)
    sizer.set_item(widget.pos, option, flag, border)

    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)


def get_label_from_xml(attrs):
    from xml_parse import XmlParsingError
    try:
        return attrs['name']
    except KeyError:
        raise XmlParsingError, _("'name' attribute missing")


def initialize(edit_klass, builder, xml_builder, icon_path):
    """\
    initialization function for the module: returns a wx.BitmapButton to be
    added to the main palette.
    """
    common.widgets[edit_klass] = builder
    common.widgets_from_xml[edit_klass] = xml_builder

    return common.make_object_button(edit_klass, icon_path)
