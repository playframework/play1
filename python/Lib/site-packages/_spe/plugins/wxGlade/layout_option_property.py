# layout_option_property.py: Property class for the 'option' layout property of
# widgets and non-toplevel sizers
# $Id: layout_option_property.py,v 1.5 2007/08/07 12:21:56 agriggio Exp $
# 
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

#from wxPython.wx import *
import wx

import widget_properties
#from edit_sizers import EditGridBagSizer

def _is_gridbag(sizer):
    return False


class LayoutOptionProperty(widget_properties.Property):
    def __init__(self, owner, sizer, parent=None):
        self.is_gridbag = _is_gridbag(sizer)
        widget_properties.Property.__init__(self, owner, 'option', parent)
        self.panel = None
        if parent is not None:
            self.display(parent)
        self.val = owner['option'][0]()

    def display(self, parent):
        if not self.is_gridbag:
            self._display_spin(parent)

    def _display_spin(self, parent):
        """\
        Actually builds the spin control to set the value of the property
        interactively
        """
        self.id = wx.NewId()
        self.val_range = (0, 1000)
        size = (widget_properties._label_initial_width, -1)
        label = widget_properties.wxGenStaticText(parent, -1, _('Proportion'),
                                                  size=size)
        label.SetToolTip(wx.ToolTip(_('Proportion')))
        self.spin = wx.SpinCtrl(parent, self.id, min=self.val_range[0],
                               max=self.val_range[1])
        val = int(self.owner[self.name][0]())
        if not val:
            self.spin.SetValue(1) # needed for GTK to display a '0'
        self.spin.SetValue(val) #int(self.owner[self.name][0]()))
        sizer = wx.BoxSizer(wx.HORIZONTAL)
        sizer.Add(label, 2, wx.ALL|wx.ALIGN_CENTER, 3)
        option = 5
        sizer.Add(self.spin, option, wx.ALL|wx.ALIGN_CENTER, 3)
        self.panel = sizer
        self.bind_event(self.on_change_val)

    def _display_gridbag(self, parent):
        sizer = wx.BoxSizer(wx.VERTICAL)
        size = (widget_properties._label_initial_width, -1)
        val = self.owner[self.name][0]()
        
        szr = wx.BoxSizer(wx.HORIZONTAL)
        label = widget_properties.wxGenStaticText(parent, -1, _('Position'),
                                                  size=size)
        label.SetToolTip(wx.ToolTip(_('Position')))
        szr.Add(label, 2, wx.ALL|wx.ALIGN_CENTER, 3)
        self.position = wx.TextCtrl(parent, -1)
        self.position.SetValue(val[:2])
        szr.Add(self.position, 5, wx.ALL|wx.ALIGN_CENTER, 3)
        sizer.Add(szr, 0, wx.EXPAND)

        szr = wx.BoxSizer(wx.HORIZONTAL)
        label = widget_properties.wxGenStaticText(parent, -1, _('Span'),
                                                  size=size)
        label.SetToolTip(wx.ToolTip(_('Span')))
        szr.Add(label, 2, wx.ALL|wx.ALIGN_CENTER, 3)
        self.span = wx.TextCtrl(parent, -1)
        self.span.SetValue(val[2:])
        szr.Add(self.span, 5, wx.ALL|wx.ALIGN_CENTER, 3)
        sizer.Add(szr, 0, wx.EXPAND)

        self.panel = sizer
        self.bind_event(self.on_change_val)
        
        
    def bind_event(self, function):
        if not self.is_gridbag:
            self._bind_event_spin(function)
        else:
            self._bind_event_gridbag(function)

    def _bind_event_spin(self, function):
        wx.EVT_KILL_FOCUS(self.spin, function)
        if wx.Platform == '__WXMAC__':
            wx.EVT_TEXT(self.spin, self.spin.GetId(), function)
            wx.EVT_SPINCTRL(self.spin, self.spin.GetId(), function)

    def _bind_event_gridbag(self, function):
        wx.EVT_KILL_FOCUS(self.position, function)
        wx.EVT_KILL_FOCUS(self.span, function)

    def get_value(self):
        if not self.is_gridbag:
            try: return self.spin.GetValue()
            except AttributeError: return self.val
        else:
            try:
                return ", ".join([self.position.GetValue(),
                                  self.span.GetValue()])
            except AttributeError:
                return self.val
            
    def set_value(self, value):
        if not self.is_gridbag:
            self.val = int(value)
            try: self.spin.SetValue(int(value))
            except AttributeError: pass
        else:
            self.val = value
            try:
                self.position.SetValue(value[:2])
                self.span.SetValue(value[2:])
            except AttributeError:
                pass

    def set_range(self, min_v, max_v):
        if not self.is_gridbag:
            self.val_range = (min_v, max_v)
            try: self.spin.SetRange(min_v, max_v)
            except AttributeError: pass

    def set_sizer(self, sizer):
        self.is_gridbag = _is_gridbag(sizer)

# end of class LayoutOptionProperty



class LayoutPosProperty(widget_properties.SpinProperty):
    def __init__(self, owner, sizer, parent=None):
        self.is_gridbag = _is_gridbag(sizer)
        widget_properties.SpinProperty.__init__(
            self, owner, 'pos', parent, 0, (0, 1000))#, immediate=True)
        self.label = _('Position')

    def set_sizer(self, sizer):
        self.is_gridbag = _is_gridbag(sizer)

    def display(self, parent):
        if not self.is_gridbag:
            widget_properties.SpinProperty.display(self, parent)
        else:
            self.panel = (0, 0)

    def write(self, *args, **kwds):
        pass

# end of class LayoutPosProperty
