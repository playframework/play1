# text_ctrl.py: wxListCtrl objects
# $Id: list_ctrl.py,v 1.15 2007/03/27 07:01:57 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
from edit_windows import ManagedBase
from tree import Tree
import common, misc
from widget_properties import *

class EditListCtrl(ManagedBase):
    """\
    Class to handle wxListCtrl objects
    """

    events = [
        'EVT_LIST_BEGIN_DRAG',
        'EVT_LIST_BEGIN_RDRAG',
        'EVT_LIST_BEGIN_LABEL_EDIT',
        'EVT_LIST_END_LABEL_EDIT',
        'EVT_LIST_DELETE_ITEM',
        'EVT_LIST_DELETE_ALL_ITEMS',
        'EVT_LIST_ITEM_SELECTED',
        'EVT_LIST_ITEM_DESELECTED',
        'EVT_LIST_ITEM_ACTIVATED',
        'EVT_LIST_ITEM_FOCUSED',
        'EVT_LIST_ITEM_MIDDLE_CLICK',
        'EVT_LIST_ITEM_RIGHT_CLICK',
        'EVT_LIST_KEY_DOWN',
        'EVT_LIST_INSERT_ITEM',
        'EVT_LIST_COL_CLICK',
        'EVT_LIST_COL_RIGHT_CLICK',
        'EVT_LIST_COL_BEGIN_DRAG',
        'EVT_LIST_COL_DRAGGING',
        'EVT_LIST_COL_END_DRAG',
        'EVT_LIST_CACHE_HINT',
        ]
    
    def __init__(self, name, parent, id, sizer, pos, property_window,
                 show=True, style=wx.LC_REPORT|wx.SUNKEN_BORDER):
        ManagedBase.__init__(self, name, 'wxListCtrl', parent, id, sizer, pos,
                             property_window, show=show)
        self.style = style
        self.access_functions['style'] = (self.get_style, self.set_style)
        # style property
        self.style_pos  = (wx.LC_LIST, wx.LC_REPORT, wx.LC_ICON, wx.LC_VIRTUAL,
                           wx.LC_SMALL_ICON, wx.LC_ALIGN_TOP, wx.LC_ALIGN_LEFT,
                           wx.LC_AUTOARRANGE, wx.LC_EDIT_LABELS, wx.LC_NO_HEADER,
                           wx.LC_SINGLE_SEL, wx.LC_SORT_ASCENDING,
                           wx.LC_SORT_DESCENDING, wx.LC_HRULES, wx.LC_VRULES,
                           wx.SIMPLE_BORDER,
                           wx.DOUBLE_BORDER, wx.SUNKEN_BORDER, wx.RAISED_BORDER,
                           wx.STATIC_BORDER, wx.NO_BORDER,
                           wx.WANTS_CHARS, wx.NO_FULL_REPAINT_ON_RESIZE,
                           wx.FULL_REPAINT_ON_RESIZE)
        style_labels = ('#section#' + _('Style'),
                        'wxLC_LIST', 'wxLC_REPORT', 'wxLC_ICON',
                        'wxLC_VIRTUAL', 'wxLC_SMALL_ICON',
                        'wxLC_ALIGN_TOP', 'wxLC_ALIGN_LEFT',
                        'wxLC_AUTOARRANGE', 'wxLC_EDIT_LABELS',
                        'wxLC_NO_HEADER', 'wxLC_SINGLE_SEL',
                        'wxLC_SORT_ASCENDING', 'wxLC_SORT_DESCENDING',
                        'wxLC_HRULES', 'wxLC_VRULES', 'wxSIMPLE_BORDER',
                        'wxDOUBLE_BORDER', 'wxSUNKEN_BORDER',
                        'wxRAISED_BORDER', 'wxSTATIC_BORDER', 'wxNO_BORDER',
                        'wxWANTS_CHARS', 'wxNO_FULL_REPAINT_ON_RESIZE',
                        'wxFULL_REPAINT_ON_RESIZE')
        self.style_tooltips = (_("Multicolumn list view, with optional small icons. Columns are computed automatically, i.e. you don't set columns as in wxLC_REPORT. In other words, the list wraps, unlike a wxListBox."),
            _("Single or multicolumn report view, with optional header."),
            _("Large icon view, with optional labels."),
            _("The application provides items text on demand. May only be used with wxLC_REPORT."),
            _("Small icon view, with optional labels."),
            _("Icons align to the top. Win32 default, Win32 only."),
            _("Icons align to the left."),
            _("Icons arrange themselves. Win32 only."),
            _("Labels are editable: the application will be notified when editing starts."),
            _("No header in report mode."),
            _("Single selection (default is multiple)."),
            _("Sort in ascending order (must still supply a comparison callback in SortItems."),
            _("Sort in descending order (must still supply a comparison callback in SortItems."),
            _("Draws light horizontal rules between rows in report mode."),
            _("Draws light vertical rules between columns in report mode"),
            _("Displays a thin border around the window. wxBORDER is the old name for this style."),
            _("Displays a double border. Windows and Mac only."),
            _("Displays a sunken border."),
            _("Displays a raised border."),
            _("Displays a border suitable for a static control. Windows only."),
            _("Displays no border, overriding the default border style for the window."),
            _("Use this to indicate that the window wants to get all char/key events for all keys - even for keys like TAB or ENTER which are usually used for dialog navigation and which wouldn't be generated without this style. If you need to use this style in order to get the arrows or etc., but would still like to have normal keyboard navigation take place, you should create and send a wxNavigationKeyEvent in response to the key events for Tab and Shift-Tab."),
            _("On Windows, this style used to disable repainting the window completely when its size is changed. Since this behaviour is now the default, the style is now obsolete and no longer has an effect."),
            _("Use this style to force a complete redraw of the window whenever it is resized instead of redrawing just the part of the window affected by resizing. Note that this was the behaviour by default before 2.5.1 release and that if you experience redraw problems with code which previously used to work you may want to try this. Currently this style applies on GTK+ 2 and Windows only, and full repainting is always done on other platforms."))
        self.properties['style'] = CheckListProperty(
            self, 'style', None, style_labels, tooltips=self.style_tooltips)

    def create_widget(self):
        self.widget = wx.ListCtrl(self.parent.widget, self.id,
                                 style=wx.LC_REPORT|wx.SUNKEN_BORDER)
        # add a couple of columns just for a better appearence (for now)
        self.widget.InsertColumn(0, _('List Control:'))
        self.widget.InsertColumn(1, self.name)
        wx.EVT_LIST_COL_CLICK(self.widget, self.widget.GetId(),
                              self.on_set_focus)

    def finish_widget_creation(self):
        ManagedBase.finish_widget_creation(self, sel_marker_parent=self.widget)

    def set_name(self, name):
        ManagedBase.set_name(self, name)
        if self.widget:
            col = self.widget.GetColumn(1)
            col.SetText(self.name)
            self.widget.SetColumn(1, col)

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
        self.notebook.AddPage(panel, 'Widget')
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

# end of class EditListCtrl


def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditListCtrl objects.
    """
    name = 'list_ctrl_%d' % number[0]
    while common.app_tree.has_name(name):
        number[0] += 1
        name = 'list_ctrl_%d' % number[0]
    list_ctrl = EditListCtrl(name, parent, wx.NewId(), sizer, pos,
                             common.property_panel)
    node = Tree.Node(list_ctrl)
    list_ctrl.node = node
    list_ctrl.set_option(1)
    list_ctrl.set_flag("wxEXPAND")
    list_ctrl.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1)
    sizer.set_item(list_ctrl.pos, 1, wx.EXPAND)


def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory function to build EditListCtrl objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: name = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    if sizer is None or sizeritem is None:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    list_ctrl = EditListCtrl(name, parent, wx.NewId(), sizer, pos,
                             common.property_panel, style=0)
    sizer.set_item(list_ctrl.pos, option=sizeritem.option, flag=sizeritem.flag,
                   border=sizeritem.border)
    node = Tree.Node(list_ctrl)
    list_ctrl.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return list_ctrl


def initialize():
    """\
    initialization function for the module: returns a wx.BitmapButton to be
    added to the main palette.
    """
    common.widgets['EditListCtrl'] = builder
    common.widgets_from_xml['EditListCtrl'] = xml_builder
        
    return common.make_object_button('EditListCtrl', 'icons/list_ctrl.xpm')
