# bitmap_button.py: wxBitmapButton objects
# $Id: bitmap_button.py,v 1.26 2007/04/12 07:15:34 guyru Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc, os
from edit_windows import ManagedBase
from tree import Tree
from widget_properties import *


class EditBitmapButton(ManagedBase):

    events = ['EVT_BUTTON']
    
    def __init__(self, name, parent, id, bmp_file, sizer, pos, property_window,
                 show=True):
        """\
        Class to handle wxBitmapButton objects
        """
        import config
        ManagedBase.__init__(self, name, 'wxBitmapButton', parent, id, sizer,
                             pos, property_window, show=show)
        self.default = False
        self.set_bitmap(bmp_file)
        # bitmap property
        self.access_functions['bitmap'] = (self.get_bitmap, self.set_bitmap)
        self.properties['bitmap'] = FileDialogProperty(self, 'bitmap', None,
                                                       style=wx.OPEN |
                                                       wx.FILE_MUST_EXIST,
                                                       can_disable=False, label=_("bitmap"))
        self.access_functions['default'] = (self.get_default, self.set_default)
        self.access_functions['style'] = (self.get_style, self.set_style)
        self.properties['default'] = CheckBoxProperty(self, 'default', None, label=_("default"))
        # 2003-08-07: added 'disabled_bitmap' property
        self.disabled_bitmap = ""
        self.access_functions['disabled_bitmap'] = (self.get_disabled_bitmap,
                                                    self.set_disabled_bitmap)
        self.properties['disabled_bitmap'] = FileDialogProperty(
            self, 'disabled_bitmap', None, style=wx.OPEN|wx.FILE_MUST_EXIST, label=_("disabled bitmap"))
        # 2003-09-04 added default_border
        if config.preferences.default_border:
            self.border = config.preferences.default_border_size
            self.flag = wx.ALL
        
        self.style_pos = (wx.BU_AUTODRAW, wx.BU_LEFT, wx.BU_RIGHT, wx.BU_TOP,
            wx.BU_BOTTOM, wx.NO_BORDER)
        style_labels = ('#section#' + _('Style'), 'wxBU_AUTODRAW', 'wxBU_LEFT', 'wxBU_RIGHT', 
            'wxBU_TOP', 'wxBU_BOTTOM', 'wxNO_BORDER')
        
        #The tooltips tuple
        self.tooltips=(_("If this is specified, the button will be drawn "
                        "automatically using the label bitmap only, providing"
                        " a 3D-look border. If this style is not specified, the "
                        "button will be drawn without borders and using all "
                        "provided bitmaps. WIN32 only."
                        "Left-justifies the bitmap label. WIN32 only."),
                        _("Right-justifies the bitmap label. WIN32 only."),
                        _("Aligns the bitmap label to the top of the button."
                        " WIN32 only."),
                        _("Aligns the bitmap label to the bottom of the button."
                        " WIN32 only."),
                        _("Creates a flat button. Windows and GTK+ only."))
        self.properties['style'] = CheckListProperty(self, 'style', None,
                                                     style_labels,tooltips=self.tooltips)


    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.Panel(self.notebook, -1)
        self.properties['bitmap'].display(panel)
        self.properties['disabled_bitmap'].display(panel)
        self.properties['default'].display(panel)
        self.properties['style'].display(panel)
        szr = wx.BoxSizer(wx.VERTICAL)
        szr.Add(self.properties['bitmap'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['disabled_bitmap'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['default'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['style'].panel, 0, wx.EXPAND)
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
        except AttributeError:
            pass
        return retval

    def set_style(self, value):
        value = self.properties['style'].prepare_value(value)
        self.style = 0
        for v in range(len(value)):
            if value[v]:
                self.style |= self.style_pos[v]
        if self.widget: self.widget.SetWindowStyleFlag(self.style)

    def get_bitmap(self):
        return self.bitmap

    def set_bitmap(self, value):
        self.bitmap = value
        if self.widget:
            bmp = self.load_bitmap()
            self.widget.SetBitmapLabel(bmp)
            self.widget.SetBitmapSelected(bmp)
            self.widget.SetBitmapFocus(bmp)
            self.set_size("%s, %s" % tuple(self.widget.GetBestSize()))

    def get_disabled_bitmap(self):
        return self.disabled_bitmap

    def set_disabled_bitmap(self, value):
        self.disabled_bitmap = value
        if self.widget:
            bmp = self.load_bitmap(self.disabled_bitmap)
            self.widget.SetBitmapDisabled(bmp)
            self.set_size("%s, %s" % tuple(self.widget.GetBestSize()))

    def create_widget(self):
        bmp = self.load_bitmap()
        try:
            self.widget = wx.BitmapButton(self.parent.widget, self.id, bmp,
                                          style=self.style)
        except AttributeError:
            self.widget = wx.BitmapButton(self.parent.widget, self.id, bmp)

    def load_bitmap(self, which=None, empty=[None]):
        if which is None: which = self.bitmap
        if which and \
               not (which.startswith('var:') or which.startswith('code:')):
            which = misc.get_relative_path(which)
            return wx.Bitmap(which, wx.BITMAP_TYPE_ANY)
        else:
            if empty[0] is None:
                empty[0] = wx.EmptyBitmap(1, 1)         
            return empty[0]

    def get_default(self):
        return self.default

    def set_default(self, value):
        self.default = bool(int(value))
##         if value and self.widget:
##             self.widget.SetDefault()

# end of class EditBitmapButton
        

def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditBitmapButton objects.
    """
    
    name = 'bitmap_button_%s' % number[0]
    while common.app_tree.has_name(name):
        number[0] += 1
        name = 'bitmap_button_%s' % number[0]
    bitmap = misc.FileSelector(_("Select the image for the button"))
    button = EditBitmapButton(name, parent, wx.NewId(), bitmap, sizer, pos,
                              common.property_panel)
    node = Tree.Node(button)
    button.node = node
    button.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1)

def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditBitmapButton objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: label = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    if sizer is None or sizeritem is None:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    button = EditBitmapButton(label, parent, wx.NewId(), '', sizer, pos,
                              common.property_panel, show=False)
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
    common.widgets['EditBitmapButton'] = builder
    common.widgets_from_xml['EditBitmapButton'] = xml_builder

    return common.make_object_button('EditBitmapButton',
                                     'icons/bitmap_button.xpm')
