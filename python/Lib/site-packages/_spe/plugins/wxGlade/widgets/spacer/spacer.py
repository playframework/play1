# spacer.py: spacers to use in sizers
# $Id: spacer.py,v 1.13 2007/08/07 12:18:34 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc
from tree import Tree
from widget_properties import *
from edit_windows import ManagedBase

class EditSpacer(ManagedBase):
    def __init__(self, name, parent, id, width, height, sizer, pos,
                 property_window, show=True):
        """\
        Class to handle spacers for sizers
        """
        ManagedBase.__init__(self, name, 'spacer', parent, id, sizer,
                             pos, property_window, show=show)
        self.__size = [width, height]

        self.access_functions['width'] = (self.get_width, self.set_width)
        self.access_functions['height'] = (self.get_height, self.set_height)

        self.properties['width'] = SpinProperty(self, 'width', None, label=_("width"))
        self.properties['height'] = SpinProperty(self, 'height', None, label=_("height"))

    def create_widget(self):
        self.widget = wx.Window(self.parent.widget, self.id, size=self.__size,
                                style=wx.SIMPLE_BORDER)
        self.widget.GetBestSize = self.widget.GetSize
        wx.EVT_PAINT(self.widget, self.on_paint)
        
    def create_properties(self):
        ManagedBase.create_properties(self)
        page = self.notebook.GetPage(1)
        wp = self.properties['width']
        hp = self.properties['height']
        wp.display(page)
        hp.display(page)
        szr = page.GetSizer()
        szr.Insert(0, hp.panel, 0, wx.EXPAND)
        szr.Insert(0, wp.panel, 0, wx.EXPAND)
        szr.Layout()
        szr.Fit(page)
        import math
        w, h = page.GetClientSize()
        page.SetScrollbars(1, 5, 1, int(math.ceil(h/5.0)))
        common_page = self.notebook.GetPage(0)
        common_page.Hide()
        self.notebook.RemovePage(0)
        self.notebook.SetSelection(0)
        
    def get_width(self):
        return self.__size[0]

    def get_height(self):
        return self.__size[1]

    def set_width(self, value):
        value = int(value)
        self.__size[0] = value
        if self.widget:
            self.widget.SetSize(self.__size)
        self.sizer.set_item(self.pos, size=self.__size)

    def set_height(self, value):
        value = int(value)
        self.__size[1] = value
        if self.widget:
            self.widget.SetSize(self.__size)
        self.sizer.set_item(self.pos, size=self.__size)

    def set_flag(self, value):
        ManagedBase.set_flag(self, value)
        if not (self.get_int_flag() & wx.EXPAND):
            self.sizer.set_item(self.pos, size=self.__size)

    def on_paint(self, event):
        dc = wx.PaintDC(self.widget)
        dc.BeginDrawing()
        brush = wx.TheBrushList.FindOrCreateBrush(
            self.widget.GetBackgroundColour())
        dc.SetBrush(brush)
        dc.SetPen(wx.ThePenList.FindOrCreatePen(wx.BLACK, 1, wx.SOLID))
        dc.SetBackground(brush)
        dc.Clear()
        w, h = self.widget.GetClientSize()
        dc.DrawLine(0, 0, w, h)
        dc.DrawLine(w, 0, 0, h)
        text = _('Spacer')
        tw, th = dc.GetTextExtent(text)
        x = (w - tw)/2
        y = (h - th)/2
        dc.SetPen(wx.ThePenList.FindOrCreatePen(wx.BLACK, 0, wx.TRANSPARENT))
        dc.DrawRectangle(x-1, y-1, tw+2, th+2)
        dc.DrawText(text, x, y)
        dc.EndDrawing()

# end of class EditSpacer
        

def builder(parent, sizer, pos):
    """\
    factory function for EditSpacer objects.
    """
    class Dialog(wx.Dialog):
        def __init__(self):
            wx.Dialog.__init__(self, misc.get_toplevel_parent(parent), -1,
                              _("Enter size"))
            
            self.width = SpinProperty(self, 'width', self, label=_("width"))
            self.height = SpinProperty(self, 'height', self, label=_("height"))
            self.width.set_value(20)
            self.height.set_value(20)
            
            szr = wx.BoxSizer(wx.VERTICAL)
            szr.Add(self.width.panel, 0, wx.EXPAND)
            szr.Add(self.height.panel, 0, wx.EXPAND)
            sz = wx.BoxSizer(wx.HORIZONTAL)
            sz.Add(wx.Button(self, wx.ID_OK, _('OK')))
            szr.Add(sz, 0, wx.ALL|wx.ALIGN_CENTER, 4)
            self.SetAutoLayout(True)
            self.SetSizer(szr)
            szr.Fit(self)
            self.CenterOnScreen()

        def __getitem__(self, name):
            return (lambda : 0, lambda v: None)

    # end of inner class

    dialog = Dialog()
    dialog.ShowModal()
    name = 'spacer'
    spacer = EditSpacer(name, parent, wx.NewId(), dialog.width.get_value(),
                        dialog.height.get_value(), sizer, pos,
                        common.property_panel)
    node = Tree.Node(spacer)
    spacer.node = node
    spacer.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1) 
    #sizer.set_item(spacer.pos, size=spacer.GetSize())

def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditSpacer objects from an xml file
    """
    from xml_parse import XmlParsingError
    if not sizer or not sizeritem:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    spacer = EditSpacer('spacer', parent, wx.NewId(), 1, 1, sizer, pos,
                        common.property_panel, True)
    sizer.set_item(spacer.pos, option=sizeritem.option, flag=sizeritem.flag,
                   border=sizeritem.border)
    node = Tree.Node(spacer)
    spacer.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return spacer


def initialize():
    """\
    initialization function for the module: returns a wx.BitmapButton to be
    added to the main palette.
    """
    common.widgets['EditSpacer'] = builder
    common.widgets_from_xml['EditSpacer'] = xml_builder
        
    return common.make_object_button('EditSpacer', 'icons/spacer.xpm')
    
