# text_ctrl.py: wxTreeCtrl objects
# $Id: tree_ctrl.py,v 1.12 2007/03/27 07:01:50 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
from edit_windows import ManagedBase
from tree import Tree
import common, misc
from widget_properties import *


class EditTreeCtrl(ManagedBase):
    """\
    Class to handle wx.TreeCtrl objects
    """

    events = [
        'EVT_TREE_BEGIN_DRAG',
        'EVT_TREE_BEGIN_RDRAG',
        'EVT_TREE_END_DRAG',
        'EVT_TREE_END_RDRAG',
        'EVT_TREE_BEGIN_LABEL_EDIT',
        'EVT_TREE_END_LABEL_EDIT',
        'EVT_TREE_DELETE_ITEM',
        'EVT_TREE_GET_INFO',
        'EVT_TREE_SET_INFO',
        'EVT_TREE_ITEM_ACTIVATED',
        'EVT_TREE_ITEM_COLLAPSED',
        'EVT_TREE_ITEM_COLLAPSING',
        'EVT_TREE_ITEM_EXPANDED',
        'EVT_TREE_ITEM_EXPANDING',
        'EVT_TREE_SEL_CHANGED',
        'EVT_TREE_SEL_CHANGING',
        'EVT_TREE_KEY_DOWN',
        'EVT_TREE_ITEM_GETTOOLTIP',
        ]
    
    def __init__(self, name, parent, id, sizer, pos, property_window,
                 show=True, style=wx.TR_HAS_BUTTONS|wx.SUNKEN_BORDER):
        ManagedBase.__init__(self, name, 'wxTreeCtrl', parent, id, sizer, pos,
                             property_window, show=show)
        self.style = style
        self.access_functions['style'] = (self.get_style, self.set_style)
        # style property
        self.style_pos  = (wx.TR_HAS_BUTTONS, wx.TR_NO_LINES, wx.TR_LINES_AT_ROOT,
                           wx.TR_EDIT_LABELS, wx.TR_MULTIPLE, wx.TR_NO_BUTTONS,
                           wx.TR_TWIST_BUTTONS, wx.TR_FULL_ROW_HIGHLIGHT,
                           wx.TR_HIDE_ROOT, wx.TR_ROW_LINES,
                           wx.TR_HAS_VARIABLE_ROW_HEIGHT,
                           wx.TR_SINGLE, wx.TR_MULTIPLE, wx.TR_EXTENDED,
                           wx.TR_DEFAULT_STYLE, wx.SIMPLE_BORDER, wx.DOUBLE_BORDER,
                           wx.SUNKEN_BORDER, wx.RAISED_BORDER, wx.STATIC_BORDER,
                           wx.NO_BORDER, wx.WANTS_CHARS, 
                           wx.NO_FULL_REPAINT_ON_RESIZE,
                           wx.FULL_REPAINT_ON_RESIZE)
        style_labels = ('#section#' + _('Style'), 'wxTR_HAS_BUTTONS', 'wxTR_NO_LINES',
                        'wxTR_LINES_AT_ROOT', 'wxTR_EDIT_LABELS',
                        'wxTR_MULTIPLE', 'wxTR_NO_BUTTONS',
                        'wxTR_TWIST_BUTTONS', 'wxTR_FULL_ROW_HIGHLIGHT',
                        'wxTR_HIDE_ROOT', 'wxTR_ROW_LINES', 
                        'wxTR_HAS_VARIABLE_ROW_HEIGHT','wxTR_SINGLE', 
                        'wxTR_MULTIPLE', 'wxTR_EXTENDED',
                        'wxTR_DEFAULT_STYLE', 'wxSIMPLE_BORDER',
                        'wxDOUBLE_BORDER', 'wxSUNKEN_BORDER',
                        'wxRAISED_BORDER', 'wxSTATIC_BORDER', 'wxNO_BORDER',
                        'wxWANTS_CHARS', 'wxNO_FULL_REPAINT_ON_RESIZE',
                        'wxFULL_REPAINT_ON_RESIZE')
        self.properties['style'] = CheckListProperty(self, 'style', None,
                                                     style_labels)
        self._item_with_name = None

    def create_widget(self):
        self.widget = wx.TreeCtrl(self.parent.widget, self.id,
                                 style=wx.TR_HAS_BUTTONS|wx.SUNKEN_BORDER)
        # add a couple of items just for a better appearence
        root = self.widget.AddRoot(_(' Tree Control:'))
        self._item_with_name = self.widget.AppendItem(root, ' ' + self.name)
        self.widget.AppendItem(self._item_with_name,
                               _(' on wxGlade %s') % common.version)
        self.widget.Expand(root)
        self.widget.Expand(self._item_with_name)

    def finish_widget_creation(self):
        ManagedBase.finish_widget_creation(self, sel_marker_parent=self.widget)

    def set_name(self, name):
        ManagedBase.set_name(self, name)
        if self.widget and self._item_with_name:
            self.widget.SetItemText(self._item_with_name, ' ' + self.name)
            
    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.ScrolledWindow(self.notebook, -1, style=wx.TAB_TRAVERSAL) 
        prop = self.properties
        prop['style'].display(panel)
        szr = wx.BoxSizer(wx.VERTICAL)
        szr.Add(prop['style'].panel, 0, wx.EXPAND)
        panel.SetAutoLayout(True)
        panel.SetSizer(szr)
        szr.Fit(panel)
        w, h = panel.GetClientSize()
        self.notebook.AddPage(panel, _('Widget'))
        self.property_window.Layout()
        import math
        panel.SetScrollbars(1, 5, 1, int(math.ceil(h/5.0)))

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

# end of class EditTreeCtrl


def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditTreeCtrl objects.
    """
    name = 'tree_ctrl_%d' % number[0]
    while common.app_tree.has_name(name):
        number[0] += 1
        name = 'tree_ctrl_%d' % number[0]
    tree_ctrl = EditTreeCtrl(name, parent, wx.NewId(), sizer, pos,
                             common.property_panel)
    node = Tree.Node(tree_ctrl)
    tree_ctrl.node = node
    tree_ctrl.set_option(1)
    tree_ctrl.set_flag("wxEXPAND")
    tree_ctrl.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1)
    sizer.set_item(tree_ctrl.pos, 1, wx.EXPAND)


def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory function to build EditTreeCtrl objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: name = attrs['name']
    except KeyError: raise XmlParsingError, "'name' attribute missing"
    if sizer is None or sizeritem is None:
        raise XmlParsingError, "sizer or sizeritem object cannot be None"
    tree_ctrl = EditTreeCtrl(name, parent, wx.NewId(), sizer, pos,
                             common.property_panel, style=0)
    sizer.set_item(tree_ctrl.pos, option=sizeritem.option, flag=sizeritem.flag,
                   border=sizeritem.border)
    node = Tree.Node(tree_ctrl)
    tree_ctrl.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return tree_ctrl


def initialize():
    """\
    initialization function for the module: returns a wx.BitmapButton to be
    added to the main palette.
    """
    common.widgets['EditTreeCtrl'] = builder
    common.widgets_from_xml['EditTreeCtrl'] = xml_builder
        
    return common.make_object_button('EditTreeCtrl', 'icons/tree_ctrl.xpm')
