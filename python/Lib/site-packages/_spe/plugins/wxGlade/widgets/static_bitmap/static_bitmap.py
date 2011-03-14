# static_bitmap.py: wxStaticBitmap objects
# $Id: static_bitmap.py,v 1.21 2007/03/27 07:01:53 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc
from edit_windows import ManagedBase
from tree import Tree
from widget_properties import *
import os


class EditStaticBitmap(ManagedBase):
    def __init__(self, name, parent, id, bmp_file, sizer, pos, property_window,
                 show=True):
        """\
        Class to handle wxStaticBitmap objects
        """
        self.attribute = True
        ManagedBase.__init__(self, name, 'wxStaticBitmap', parent, id, sizer,
                             pos, property_window, show=show)
        self.set_bitmap(bmp_file)
        # bitmap property
        self.access_functions['bitmap'] = (self.get_bitmap, self.set_bitmap)
        def set_attribute(v): self.attribute = int(v)
        self.access_functions['attribute'] = (lambda : self.attribute,
                                              set_attribute)
        self.bitmap_prop = FileDialogProperty(self, 'bitmap', None, #panel,
                                              style=wx.OPEN|wx.FILE_MUST_EXIST,
                                              can_disable=False, label=_("bitmap"))
        self.properties['bitmap'] = self.bitmap_prop
        self.properties['attribute'] = CheckBoxProperty(
            self, 'attribute', None, _('Store as attribute'), write_always=True)
        self.style = 0
        self.access_functions['style'] = (self.get_style, self.set_style)
        self.style_pos  = (wx.SIMPLE_BORDER, wx.DOUBLE_BORDER, wx.SUNKEN_BORDER,
                           wx.RAISED_BORDER, wx.STATIC_BORDER, wx.NO_3D,
                           wx.TAB_TRAVERSAL, wx.WANTS_CHARS,
                           wx.NO_FULL_REPAINT_ON_RESIZE,
                           wx.FULL_REPAINT_ON_RESIZE,
                           wx.CLIP_CHILDREN)
        style_labels = (u'#section#' + _('Style'), 'wxSIMPLE_BORDER', 'wxDOUBLE_BORDER',
                        'wxSUNKEN_BORDER', 'wxRAISED_BORDER',
                        'wxSTATIC_BORDER', 'wxNO_3D', 'wxTAB_TRAVERSAL',
                        'wxWANTS_CHARS', 'wxNO_FULL_REPAINT_ON_RESIZE',
                        'wxFULL_REPAINT_ON_RESIZE',
                        'wxCLIP_CHILDREN')
        self.properties['style'] = CheckListProperty(self, 'style', None,
                                                     style_labels)  

    def create_widget(self):
        bmp = self.load_bitmap()
        self.widget = wx.StaticBitmap(self.parent.widget, self.id, bmp)
        if wx.Platform == '__WXMSW__':
            def get_best_size():
                bmp = self.widget.GetBitmap()
                if bmp and bmp.Ok():
                    return bmp.GetWidth(), bmp.GetHeight()
                return wx.StaticBitmap.GetBestSize(self.widget)
            self.widget.GetBestSize = get_best_size

    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.ScrolledWindow(self.notebook, -1, style=wx.TAB_TRAVERSAL)
        szr = wx.BoxSizer(wx.VERTICAL)
        self.properties['bitmap'].display(panel)
        self.properties['attribute'].display(panel)
        self.properties['style'].display(panel)
        szr.Add(self.properties['bitmap'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['attribute'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['style'].panel, 0, wx.EXPAND)
        panel.SetAutoLayout(True)
        panel.SetSizer(szr)
        szr.Fit(panel)
        w, h = panel.GetClientSize()
        self.notebook.AddPage(panel, "Widget")
        self.property_window.Layout()
        import math
        panel.SetScrollbars(1, 5, 1, int(math.ceil(h/5.0)))

    def get_bitmap(self):
        return self.bitmap

    def set_bitmap(self, value):
        self.bitmap = value
        if self.widget:
            bmp = self.load_bitmap()
            self.widget.SetBitmap(bmp)
            self.set_size("%s, %s" % tuple(self.widget.GetBestSize()))

    def load_bitmap(self, empty=[None]):
        if self.bitmap and \
               not (self.bitmap.startswith('var:') or
                    self.bitmap.startswith('code:')):
            path = misc.get_relative_path(self.bitmap)
            print "LOADING FROM:", path
            return wx.Bitmap(path, wx.BITMAP_TYPE_ANY)
        else:
            if empty[0] is None:
                empty[0] = wx.EmptyBitmap(1, 1)
            return empty[0]

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

# end of class EditStaticBitmap
        

def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditStaticBitmap objects.
    """
    name = 'bitmap_%s' % number[0]
    while common.app_tree.has_name(name):
        number[0] += 1
        name = 'bitmap_%s' % number[0]
    bitmap = misc.FileSelector("Select the image")
    static_bitmap = EditStaticBitmap(name, parent, wx.NewId(), bitmap, sizer,
                                     pos, common.property_panel)
    node = Tree.Node(static_bitmap)
    static_bitmap.node = node
    static_bitmap.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1)

def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditStaticBitmap objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: label = attrs['name']
    except KeyError: raise XmlParsingError, "'name' attribute missing"
    if sizer is None or sizeritem is None:
        raise XmlParsingError, "sizer or sizeritem object cannot be None"
    bitmap = EditStaticBitmap(label, parent, wx.NewId(), '', sizer, pos,
                              common.property_panel)
    sizer.set_item(bitmap.pos, option=sizeritem.option, flag=sizeritem.flag,
                   border=sizeritem.border) #, size=bitmap.GetBestSize())
    node = Tree.Node(bitmap)
    bitmap.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return bitmap


def initialize():
    """\
    initialization function for the module: returns a wx.BitmapButton to be
    added to the main palette.
    """
    common.widgets['EditStaticBitmap'] = builder
    common.widgets_from_xml['EditStaticBitmap'] = xml_builder
        
    return common.make_object_button('EditStaticBitmap',
                                     'icons/static_bitmap.xpm')
