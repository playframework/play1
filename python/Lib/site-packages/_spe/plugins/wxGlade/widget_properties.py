# widget_properties.py: classes to handle the various properties of the widgets
# (name, size, color, etc.)
# $Id: widget_properties.py,v 1.65 2007/08/07 12:21:56 agriggio Exp $
# 
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY


# this is needed for wx >= 2.3.4 to clip the label showing the name of the
# property, otherwise on the properties tabs horizontal scrollbars are shown
_label_initial_width = 5 

#from wxPython.wx import *
#from wxPython.grid import *
import wx
import wx.grid
from xml.sax.saxutils import escape
import common, misc
try:
    #from wxPython.lib.stattext import *
    import wx.lib.stattext
    wxGenStaticText = wx.lib.stattext.GenStaticText
except ImportError:
    # wxGenStaticText has been added in wx 2.3.3, so it may not be available
    #wxGenStaticText = wxStaticText
    wxGenStaticText = wx.StaticText

def _mangle(label):
    """\
    returns a mangled version of the str label, suitable for displaying
    the name of a property
    """
    return misc.wxstr(label.capitalize().replace('_', ' '))


import common
_encode = common._encode_to_xml


class Property:
    """\
    A class to handle a single property of a widget.
    """
    def __init__(self, owner, name, parent, getter=None, setter=None, label=None):
        # owner: the widget this property belongs to
        # parent: the widget inside which the property is displayed
        """\
        Access to the property is made through the getter and setter functions,
        which are invoked also in the default event handler. If they are None,
        they default to owner[name][0] and owner[name][1]
        """
        self.val = None 
        self.parent = parent
        self.owner = owner
        self.getter = getter
        self.setter = setter
        self.name = name
        if label:
            self.dispName = label
        else:
            self.dispName = name

    def on_change_val(self, event, first=[True]):
        """\
        Event handler called to notify owner that the value of the Property
        has changed
        """
        val = self.get_value()
        if not misc.streq(self.val, val):
            common.app_tree.app.saved = False # update the status of the app
            if self.setter: self.setter(val)
            else:
                self.owner[self.name][1](val)
            self.val = self.get_value()
        first[0] = False
        event.Skip()
        
    def write(self, outfile=None, tabs=0):
        """\
        Writes the xml code for this property onto the given file.
        """
        if self.getter: value = self.getter()
        else: value = self.owner[self.name][0]() 
        if not misc.streq(value, ''):
            fwrite = outfile.write
            fwrite('    ' * tabs + '<%s>' % self.name)
            fwrite(escape(_encode(value)))
            fwrite('</%s>\n' % self.name)

    def bind_event(self, function):
        """\
        sets the default event handler for this property.
        """
        raise NotImplementedError

    def get_value(self):
        raise NotImplementedError

    def set_value(self, value):
        raise NotImplementedError

# end of class Property


class HiddenProperty(Property):
    """\
    Properties not associated to any control, i.e. not editable by the user.
    """
    def __init__(self, owner, name, value=None, label=None):
        try: getter, setter = owner[name]
        except KeyError:
            import traceback; traceback.print_exc()
            if callable(value): getter = value
            else:
                def getter(): return value
            def setter(v): pass
            
        self.panel = None # this is needed to provide an uniform treatment,
                          # but is always None
        Property.__init__(self, owner, name, None, getter, setter, label=label)
        if value is not None:
            if callable(value): self.value = value()
            else: self.value = value

    def bind_event(self, function):
        pass

    def get_value(self):
        return self.value

    def set_value(self, val):
        self.value = val

# end of class HiddenProperty


class _activator:
    """\
    a utility class which provides a method, toggle_active, to activate or
    deactivate a Property of a widget
    """
    def __init__(self, target=None, enabler=None):
        # target: name of the object to Enable/Disable
        # enabler: check box which provides the ability to Enable/Disable the
        #          Property
        self._target = target
        self._enabler = enabler
        if self._target: self._active = self._target.IsEnabled()
        else: self._active = True

    def toggle_active(self, active):
        self._active = active
        if not self._target: return
        self._target.Enable(active)
        import common
        try: common.app_tree.app.saved = False
        except AttributeError: pass # why does this happen on win at startup?
        try: self._enabler.SetValue(active)
        except AttributeError: pass

    def is_active(self):
        if self._target: return self._target.IsEnabled()
        return self._active

# end of class _activator


class TextProperty(Property, _activator):
    """\
    Properties associated to a text control.
    """
    def __init__(self, owner, name, parent=None, can_disable=False,
                 enabled=False, readonly=False, multiline=False, label=None):
        Property.__init__(self, owner, name, parent, label=label)
        self.val = misc.wxstr(owner[name][0]())
        self.can_disable = can_disable
        self.readonly = readonly
        self.multiline = multiline
        _activator.__init__(self)
        if can_disable: self.toggle_active(enabled)
        self.panel = None
        if parent is not None: self.display(parent)

    def display(self, parent):
        """\
        Actually builds the text control to set the value of the property
        interactively
        """
        self.id = wx.NewId()
        #self.panel = wxPanel(parent, -1)
        #self.panel = parent
        if self.readonly: style = wx.TE_READONLY
        else: style = 0
        if self.multiline: style |= wx.TE_MULTILINE
        val = self.get_value()
        if self.multiline: val = val.replace('\\n', '\n')
        lbl = getattr(self, 'label', None)
        if lbl is None: lbl = _mangle(self.dispName)
        #label = wxStaticText(self.panel, -1, _mangle(self.dispName))
        label = wxGenStaticText(parent, -1, lbl,
                                size=(_label_initial_width, -1))
        if self.can_disable:
            self._enabler = wx.CheckBox(parent, self.id+1, '', size=(1, -1))
        self.text = wx.TextCtrl(parent, self.id, val, style=style, size=(1, -1))
        if hasattr(self, 'tooltip'):
            label.SetToolTip(wx.ToolTip(self.tooltip))
        else:
            label.SetToolTip(wx.ToolTip(_mangle(self.dispName)))
        if self.can_disable:
            wx.EVT_CHECKBOX(self._enabler, self.id+1,
                         lambda event: self.toggle_active(event.IsChecked()))
            self.text.Enable(self.is_active())
            self._enabler.SetValue(self.is_active())
            self._target = self.text
        sizer = wx.BoxSizer(wx.HORIZONTAL)
        sizer.Add(label, 2, wx.ALL|wx.ALIGN_CENTER, 3)
        if getattr(self, '_enabler', None) is not None:
            sizer.Add(self._enabler, 1, wx.ALL|wx.ALIGN_CENTER, 3)
            option = 4
        else:
            option = 5
        sizer.Add(self.text, option, wx.ALL|wx.ALIGN_CENTER, 3)
        if self.multiline:
            h = self.text.GetCharHeight()
            sizer.SetItemMinSize(self.text, -1, h*3)
        self.panel = sizer
        self.bind_event(self.on_change_val)
        wx.EVT_CHAR(self.text, self.on_char)

    def on_char(self, event):
        if event.GetKeyCode() == wx.WXK_ESCAPE:
            self.text.SetValue(self.val)
            self.text.SetInsertionPointEnd()
        event.Skip()

    def bind_event(self, function):
        def func_2(event):
            if self.text.IsEnabled():
                #misc.wxCallAfter(function, event)
                function(event)
            event.Skip()
        wx.EVT_KILL_FOCUS(self.text, func_2)

    def get_value(self):
        try:
            val = self.text.GetValue()
            if self.multiline: return val.replace('\n', '\\n')
            return val
        except AttributeError:
            return self.val

    def set_value(self, value):
        value = misc.wxstr(value)
        if self.multiline:
            self.val = value.replace('\n', '\\n')
            value = value.replace('\\n', '\n')
        else: self.val = value
        try: self.text.SetValue(value)
        except AttributeError: pass

    def write(self, outfile, tabs):
        if self.is_active():
            Property.write(self, outfile, tabs)

# end of class TextProperty


class CheckBoxProperty(Property):
    """\
    Properties whose values can be changed by one checkbox.
    """
    def __init__(self, owner, name, parent=None, label=None,
                 write_always=False):
        Property.__init__(self, owner, name, parent, label=label)
        self.val = int(owner[name][0]())
        if label is None or label == name: label = _mangle(name)
        self.label = label
        self.panel = None
        self.write_always = write_always
        if parent is not None: self.display(parent)

    def display(self, parent):
        """\
        Actually builds the check box to set the value of the property
        interactively
        """
        self.id = wx.NewId()
        #self.panel = wxPanel(parent, -1)
        self.cb = wx.CheckBox(parent, self.id, '')
        self.cb.SetValue(self.val)
        label = wxGenStaticText(parent, -1, self.label)
        sizer = wx.BoxSizer(wx.HORIZONTAL)
        sizer.Add(label, 5, wx.ALIGN_CENTER_VERTICAL|wx.ALL, 3)
        sizer.Add(self.cb, 0, wx.ALIGN_CENTER|wx.ALL, 3)
##         self.panel.SetAutoLayout(True)
##         self.panel.SetSizer(sizer)
##         self.panel.SetSize(sizer.GetMinSize())
        self.panel = sizer
        self.bind_event(self.on_change_val)
        
    def bind_event(self, function):
        wx.EVT_CHECKBOX(self.cb, self.id, function)

    def get_value(self):
        try: return int(self.cb.GetValue())
        except AttributeError: return int(self.val)

    def set_value(self, val):
        self.val = int(val)
        try: self.cb.SetValue(self.val)
        except AttributeError: pass

    def write(self, outfile, tabs):
        if self.write_always or self.get_value():
            if self.getter: value = int(self.getter())
            else: value = int(self.owner[self.name][0]())
            fwrite = outfile.write
            fwrite('    ' * tabs + '<%s>' % self.name)
            fwrite(escape(_encode(value)))
            fwrite('</%s>\n' % self.name)

# end of class CheckBoxProperty


class CheckListProperty(Property):
    """\
    Properties whose values can be changed by a list of checkboxes.
    """
    def __init__(self, owner, name, parent=None, labels=None, writer=None,
                 tooltips=None):
        """
        @type labels: list of strings
        @param labels: list of names of the labels of the checkboxes; a
        label that begins with the string "#section#" is used as the
        title of a static box that encloses the checkboxes that
        follow
        @type tooltips: tuple of strings
        @param tooltips: a list of strings to be displayed as the tool-tips for
        the properties
        """ #" # ALB - fix for emacs's syntax highlight, don't remove please!
        Property.__init__(self, owner, name, parent, label=None)
        self.values = owner[name][0]()
        self.labels = labels
        self.tooltips = tooltips
        # the writer param is a function to customize the generation of the xml
        # for this property
        self.writer = writer
        self.panel = None
        if parent is not None: self.display(parent)

    def display(self, parent):
        """\
        Actually builds the list of checkboxes to set the value of the property
        interactively
        """
        self.id = wx.NewId()
        #self.panel = wxPanel(parent, -1)

        self.choices = []
        tmp_sizer = wx.BoxSizer(wx.VERTICAL)
        #self.panel.SetAutoLayout(True)
        i = j = 0
        tmp = tmp_sizer
        while 1:
            if i >= len(self.labels): break
            if self.labels[i].startswith('#section#'):
                if tmp != tmp_sizer:
                    tmp_sizer.Add(tmp, 1, wx.ALL|wx.EXPAND, 5)
                lbl = _mangle(self.labels[i].replace(u'#section#', ''))
                s = wx.FULL_REPAINT_ON_RESIZE
                tmp = wx.StaticBoxSizer(wx.StaticBox(parent, -1, lbl, style=s),
                                       wx.VERTICAL)
            else:
                c = wx.CheckBox(parent, self.id+j, self.labels[i])
                self.choices.append(c)
                tmp.Add(c)
                j += 1
            i += 1
        if tmp != tmp_sizer:
            tmp_sizer.Add(tmp, 0, wx.ALL|wx.EXPAND, 5)

        for i in range(len(self.values)):
            self.choices[i].SetValue(self.values[i])
            
        #Set the tool-tips for the properties
        if self.tooltips is not None:
                for i in range(len(self.tooltips)):
                        if i >= len(self.choices): break
                        self.choices[i].SetToolTip(wx.ToolTip(self.tooltips[i]))
                  
##         self.panel.SetSizer(tmp_sizer)
##         self.panel.SetSize(tmp_sizer.GetMinSize())
        self.panel = tmp_sizer
        self.bind_event(self.on_change_val)
        
    def bind_event(self, function):
        for i in range(len(self.choices)):
            wx.EVT_CHECKBOX(self.choices[i], self.id+i, function)

    def get_value(self):
        try: return [c.GetValue() for c in self.choices]
        except AttributeError: return self.values

    def set_value(self, value):
        self.values = self.prepare_value(value)
        try:
            for i in range(len(self.values)):
                self.choices[i].SetValue(self.values[i])
        except AttributeError: pass

    def write(self, outfile, tabs):
        if self.writer is not None:
            return self.writer(outfile, tabs)
        val = self.owner[self.name][0]() #self.getter()
        def filter_func(label): return not label.startswith('#section#')
        labels = filter(filter_func, self.labels)
        tmp = '|'.join([ labels[c] for c in range(len(labels)) if val[c] ])
        if tmp: 
            fwrite = outfile.write
            fwrite('    ' * tabs + '<%s>' % self.name)
            fwrite(escape(_encode(tmp)))
            fwrite('</%s>\n' % self.name)

    def prepare_value(self, old_val):
        """\
        turns a string of tokens separated by '|' into a list of
        boolean values
        """
        try: old_val = old_val.split("|")
        except AttributeError: return list(old_val)
        ret = []
        for l in self.labels:
            if l in old_val: ret.append(1)
            elif not l.startswith('#section#'): ret.append(0)
        return ret

# end of class CheckListProperty


class SpinProperty(Property, _activator):
    """\
    Properties associated to a spin control.
    """
    def __init__(self, owner, name, parent=None, can_disable=False,
                 r=None, enabled=False, immediate=False, label=None):
        # r = range of the spin (min, max)
        Property.__init__(self, owner, name, parent, label=label)
        self.can_disable = can_disable
        self.immediate = immediate # if true, changes to this property have an
                                   # immediate effect (instead of waiting the
                                   # focus change...)
        _activator.__init__(self)
        if can_disable: self.toggle_active(enabled)
        if r is not None:
            self.val_range = (r[0], max(r[0], r[1]))
        else:
            self.val_range = None
        self.panel = None
        if parent is not None: self.display(parent)
        self.val = owner[name][0]()

    def display(self, parent):
        """\
        Actually builds the spin control to set the value of the property
        interactively
        """
        self.id = wx.NewId()
        if self.val_range is None: self.val_range = (0, 1000)
        lbl = getattr(self, 'label', None)
        if lbl is None: lbl = _mangle(self.dispName)
        label = wxGenStaticText(parent, -1, lbl,
                                size=(_label_initial_width, -1))
        tip = getattr(self, 'tooltip', None)
        if tip is None: tip = lbl
        label.SetToolTip(wx.ToolTip(tip))
        if self.can_disable:
            self._enabler = wx.CheckBox(parent, self.id+1, '', size=(1, -1))
        self.spin = wx.SpinCtrl(parent, self.id, min=self.val_range[0],
                               max=self.val_range[1])
        val = int(self.owner[self.name][0]())
        if not val:
            self.spin.SetValue(1) # needed for GTK to display a '0'
        self.spin.SetValue(val) #int(self.owner[self.name][0]()))
        if self.can_disable:
            #self._enabler = wxCheckBox(parent, self.id+1, '', size=(1, -1))
            wx.EVT_CHECKBOX(self._enabler, self.id+1,
                         lambda event: self.toggle_active(event.IsChecked()))
            self.spin.Enable(self.is_active())
            self._enabler.SetValue(self.is_active())
            self._target = self.spin
        sizer = wx.BoxSizer(wx.HORIZONTAL)
        sizer.Add(label, 2, wx.ALL|wx.ALIGN_CENTER, 3)
        if getattr(self, '_enabler', None) is not None:
            sizer.Add(self._enabler, 1, wx.ALL|wx.ALIGN_CENTER, 3)
            option = 4
        else:
            option = 5
        sizer.Add(self.spin, option, wx.ALL|wx.ALIGN_CENTER, 3)
        self.panel = sizer
        self.bind_event(self.on_change_val)

    def bind_event(self, function):
        def func_2(event):
            if self.is_active():
                function(event)
            event.Skip()
        wx.EVT_KILL_FOCUS(self.spin, func_2)
        if wx.Platform == '__WXMAC__' or self.immediate:
            wx.EVT_TEXT(self.spin, self.spin.GetId(), func_2)
            wx.EVT_SPINCTRL(self.spin, self.spin.GetId(), func_2)

    def get_value(self):
        try: return self.spin.GetValue()
        except AttributeError: return self.val

    def set_value(self, value):
        self.val = int(value)
        try: self.spin.SetValue(int(value))
        except AttributeError: pass

    def set_range(self, min_v, max_v):
        self.val_range = (min_v, max(min_v, max_v))
        try: self.spin.SetRange(min_v, max_v)
        except AttributeError: pass

    def write(self, outfile, tabs):
        if self.is_active(): 
            Property.write(self, outfile, tabs)

# end of class SpinProperty


class DialogProperty(Property, _activator):
    """\
    Property which selection is made through a dialog, which must provide a
    get_value method.
    """
    def __init__(self, owner, name, parent, dialog, can_disable=False,
                 enabled=False, label=None):
        Property.__init__(self, owner, name, parent, label=label)
        self.dialog = dialog
        self.panel = None
        self.can_disable = can_disable
        _activator.__init__(self)
        if can_disable: self.toggle_active(enabled)
        if parent is not None: self.display(parent)
        self.val = "%s" % owner[name][0]()

    def display(self, parent):
        """\
        Actually builds the panel (with the text ctrl and the button to display
        the dialog) to set the value of the property interactively
        """
        self.id = wx.NewId()
        val = misc.wxstr(self.owner[self.name][0]())
        label = wxGenStaticText(parent, -1, _mangle(self.dispName),
                                size=(_label_initial_width, -1))
        label.SetToolTip(wx.ToolTip(_mangle(self.dispName)))
        if self.can_disable:
            self._enabler = wx.CheckBox(parent, self.id+1, '', size=(1, -1))
        self.text = wx.TextCtrl(parent, self.id, val, size=(1, -1))
        self.btn = wx.Button(parent, self.id+1, " ... ",
                            size=(_label_initial_width, -1))
        if self.can_disable:
            #self._enabler = wxCheckBox(parent, self.id+1, '', size=(1, -1))
            wx.EVT_CHECKBOX(self._enabler, self.id+1,
                         lambda event: self.toggle_active(event.IsChecked()))
            self.text.Enable(self.is_active())
            self.btn.Enable(self.is_active())
            self._enabler.SetValue(self.is_active())
            self._target = self.text
        wx.EVT_BUTTON(self.btn, self.id+1, self.display_dialog)
        sizer = wx.BoxSizer(wx.HORIZONTAL)
        sizer.Add(label, 2, wx.ALL|wx.ALIGN_CENTER, 3)
        if getattr(self, '_enabler', None) is not None:
            sizer.Add(self._enabler, 1, wx.ALL|wx.ALIGN_CENTER, 3)
            option = 3
        else:
            option = 4
        sizer.Add(self.text, option, wx.ALL|wx.ALIGN_CENTER, 3)
        sizer.Add(self.btn, 1, wx.ALL|wx.ALIGN_CENTER, 3)
        self.panel = sizer
        
        self.bind_event(self.on_change_val)
        wx.EVT_CHAR(self.text, self.on_char)

    def on_char(self, event):
        if event.GetKeyCode() == wx.WXK_ESCAPE:
            self.text.SetValue(self.val)
            self.text.SetInsertionPointEnd()
        event.Skip()

    def display_dialog(self, event):
        if self.dialog.ShowModal() == wx.ID_OK:
            self.text.SetValue(misc.wxstr(self.dialog.get_value()))
        self.text.ProcessEvent(wx.FocusEvent(wx.wxEVT_KILL_FOCUS, self.id))

    def bind_event(self, function):
        wx.EVT_KILL_FOCUS(self.text, function)

    def get_value(self):
        try: return self.text.GetValue()
        except AttributeError: return self.val

    def set_value(self, value):
        self.val = misc.wxstr(value)
        try: self.text.SetValue(self.val)
        except AttributeError: pass

    def write(self, dest_file=None, tabs=0):
        if self.is_active():
            Property.write(self, dest_file, tabs)

    def toggle_active(self, active):
        _activator.toggle_active(self, active)
        try:
            self.btn.Enable(active)
        except AttributeError:
            pass

# end of class DialogProperty


class FileDialogProperty(DialogProperty):
    dialog = [None]

    class FileDialog:
        def __init__(self, parent, message, wildcard, style, label=None):
            self.parent = parent
            self.message = message
            self.wildcard = wildcard
            self.style = style
            self.value = None

        def ShowModal(self):
            self.value = misc.FileSelector(
                self.message, wildcard=self.wildcard, flags=self.style)
            if self.value:
                return wx.ID_OK

        def get_value(self):
            return self.value

    # end of class FileDialog
    
    def __init__(self, owner, name, parent=None, wildcard=_("All Files|*"),
                 message=_("Choose a file"), can_disable=True, style=0, label=None):
        if not self.dialog[0]:
##             self.dialog[0] = wxFileDialog(parent, message,
##                                           wildcard=wildcard, style=style)
##             self.dialog[0].get_value = self.dialog[0].GetPath
            self.dialog[0] = self.FileDialog(
                parent, message, wildcard, style)
        DialogProperty.__init__(self, owner, name, parent, self.dialog[0],
                                can_disable, label=label)

# end of class FileDialogProperty


from misc import _reverse_dict


class ColorDialogProperty(DialogProperty):
    str_to_colors = {
        'wxSYS_COLOUR_SCROLLBAR': wx.SYS_COLOUR_SCROLLBAR,
        'wxSYS_COLOUR_BACKGROUND': wx.SYS_COLOUR_BACKGROUND,
        'wxSYS_COLOUR_ACTIVECAPTION': wx.SYS_COLOUR_ACTIVECAPTION,
        'wxSYS_COLOUR_INACTIVECAPTION': wx.SYS_COLOUR_INACTIVECAPTION,
        'wxSYS_COLOUR_MENU': wx.SYS_COLOUR_MENU,
        'wxSYS_COLOUR_WINDOW': wx.SYS_COLOUR_WINDOW,
        'wxSYS_COLOUR_WINDOWFRAME': wx.SYS_COLOUR_WINDOWFRAME,
        'wxSYS_COLOUR_MENUTEXT': wx.SYS_COLOUR_MENUTEXT,
        'wxSYS_COLOUR_WINDOWTEXT': wx.SYS_COLOUR_WINDOWTEXT,
        'wxSYS_COLOUR_CAPTIONTEXT': wx.SYS_COLOUR_CAPTIONTEXT,
        'wxSYS_COLOUR_ACTIVEBORDER': wx.SYS_COLOUR_ACTIVEBORDER,
        'wxSYS_COLOUR_INACTIVEBORDER': wx.SYS_COLOUR_INACTIVEBORDER,
        'wxSYS_COLOUR_APPWORKSPACE': wx.SYS_COLOUR_APPWORKSPACE,
        'wxSYS_COLOUR_HIGHLIGHT': wx.SYS_COLOUR_HIGHLIGHT,
        'wxSYS_COLOUR_HIGHLIGHTTEXT': wx.SYS_COLOUR_HIGHLIGHTTEXT,
        'wxSYS_COLOUR_BTNFACE': wx.SYS_COLOUR_BTNFACE,
        'wxSYS_COLOUR_BTNSHADOW': wx.SYS_COLOUR_BTNSHADOW,
        'wxSYS_COLOUR_GRAYTEXT': wx.SYS_COLOUR_GRAYTEXT,
        'wxSYS_COLOUR_BTNTEXT': wx.SYS_COLOUR_BTNTEXT,
        'wxSYS_COLOUR_INACTIVECAPTIONTEXT': wx.SYS_COLOUR_INACTIVECAPTIONTEXT,
        'wxSYS_COLOUR_BTNHIGHLIGHT': wx.SYS_COLOUR_BTNHIGHLIGHT,
        'wxSYS_COLOUR_3DDKSHADOW': wx.SYS_COLOUR_3DDKSHADOW,
        'wxSYS_COLOUR_3DLIGHT': wx.SYS_COLOUR_3DLIGHT,
        'wxSYS_COLOUR_INFOTEXT': wx.SYS_COLOUR_INFOTEXT,
        'wxSYS_COLOUR_INFOBK': wx.SYS_COLOUR_INFOBK,
        'wxSYS_COLOUR_DESKTOP': wx.SYS_COLOUR_DESKTOP,
        'wxSYS_COLOUR_3DFACE': wx.SYS_COLOUR_3DFACE,
        'wxSYS_COLOUR_3DSHADOW': wx.SYS_COLOUR_3DSHADOW,
        'wxSYS_COLOUR_3DHIGHLIGHT': wx.SYS_COLOUR_3DHIGHLIGHT,
        'wxSYS_COLOUR_3DHILIGHT': wx.SYS_COLOUR_3DHILIGHT,
        'wxSYS_COLOUR_BTNHILIGHT': wx.SYS_COLOUR_BTNHILIGHT
        }

    colors_to_str = _reverse_dict(str_to_colors)

    dialog = [None]
    def __init__(self, owner, name, parent=None, can_disable=True, label=None):
        if not self.dialog[0]:
            from color_dialog import wxGladeColorDialog
            self.dialog[0] = wxGladeColorDialog(self.str_to_colors)
        DialogProperty.__init__(self, owner, name, parent, self.dialog[0],
                                can_disable, label=label)

    def display_dialog(self, event):
        self.dialog.set_value(self.get_value())
        DialogProperty.display_dialog(self, event)

    def toggle_active(self, active):
        DialogProperty.toggle_active(self, active)
        if not active:
            # restore the original value if toggled off
            color = self.owner._original.get(self.name, None)
            if color is not None and self.owner.widget is not None:
                which = 'Set%sColour' % self.name.capitalize()
                func = getattr(self.owner.widget, which, lambda c: None)
                func(color)
                self.owner.widget.Refresh()
        else:
            # restore the saved value
            getval, setval = self.owner[self.name]
            setval(getval())

# end of class ColorDialogProperty


class FontDialogProperty(DialogProperty):
    font_families_to = { 'default': wx.DEFAULT, 'decorative': wx.DECORATIVE,
                         'roman': wx.ROMAN, 'swiss': wx.SWISS,
                         'script':wx.SCRIPT, 'modern': wx.MODERN }
    font_families_from = _reverse_dict(font_families_to)
    font_styles_to = { 'normal': wx.NORMAL, 'slant': wx.SLANT,
                       'italic': wx.ITALIC }
    font_styles_from = _reverse_dict(font_styles_to)
    font_weights_to = {'normal': wx.NORMAL, 'light': wx.LIGHT, 'bold': wx.BOLD }
    font_weights_from = _reverse_dict(font_weights_to)
    
    if misc.check_wx_version(2, 3, 3):
        font_families_to['teletype'] = wx.TELETYPE 
        font_families_from[wx.TELETYPE] = 'teletype' 

    dialog = [None]

    def __init__(self, owner, name, parent=None, can_disable=True, label=None):
        if not self.dialog[0]:
            import font_dialog
            self.dialog[0] = font_dialog.wxGladeFontDialog(parent, -1, "")
        DialogProperty.__init__(self, owner, name, parent, self.dialog[0],
                                can_disable, label=label)

    def display_dialog(self, event):
        try: props = eval(self.get_value())
        except:
            import traceback; traceback.print_exc()
        else:
            if len(props) == 6: self.dialog.set_value(props)
        DialogProperty.display_dialog(self, event)

    def write(self, outfile=None, tabs=0):
        if self.is_active():
            try:
                props = [_encode(s) for s in eval(self.get_value().strip())]
            except:
                import traceback
                traceback.print_exc()
                return
            if len(props) < 6:
                print _('error in the value of the property "%s"') % self.name
                return
            fwrite = outfile.write
            fwrite('    ' * tabs + '<%s>\n' % self.name)
            tstr = '    ' * (tabs+1)
            fwrite('%s<size>%s</size>\n' % (tstr, escape(props[0])))
            fwrite('%s<family>%s</family>\n' % (tstr, escape(props[1])))
            fwrite('%s<style>%s</style>\n' % (tstr, escape(props[2])))
            fwrite('%s<weight>%s</weight>\n' % (tstr, escape(props[3])))
            fwrite('%s<underlined>%s</underlined>\n' % (tstr,
                                                        escape(props[4])))
            fwrite('%s<face>%s</face>\n' % (tstr, escape(props[5])))
            fwrite('    ' * tabs + '</%s>\n' % self.name)

    def toggle_active(self, active):
        DialogProperty.toggle_active(self, active)
        if not active:
            # restore the original value if toggled off
            font = self.owner._original['font']
            if font is not None and self.owner.widget is not None:
                self.owner.widget.SetFont(font)
                self.owner.widget.Refresh()
        else:
            # restore the saved value
            getval, setval = self.owner[self.name]
            setval(getval())

# end of class FontDialogProperty


class RadioProperty(Property, _activator):
    """\
    properties controlled by a series of radio buttons.
    """
    def __init__(self, owner, name, parent, choices, can_disable=False,
                 enabled=False, columns=1, label=None):
        Property.__init__(self, owner, name, parent, label=label)
        self.can_disable = can_disable
        _activator.__init__(self)
        if can_disable: self.toggle_active(enabled)
        self.choices = choices
        self.columns = columns
        self.panel = None
        self.label = label
        if label is None:
            self.label = _mangle(name)
        if parent is not None: self.display(parent)
        self.val = owner[name][0]()

    def display(self, parent):
        """\
        Actually builds the radio box to set the value of the property
        interactively
        """
        self.id = wx.NewId()
        style = wx.RA_SPECIFY_COLS|wx.NO_BORDER|wx.CLIP_CHILDREN
        if not self.can_disable: 
            szr = wx.BoxSizer(wx.HORIZONTAL)
            style=wx.RA_SPECIFY_COLS
        else: 
            szr = wx.StaticBoxSizer(wx.StaticBox(parent, -1, self.label),
                                   wx.HORIZONTAL)
        self.options = wx.RadioBox(parent, self.id, self.label,
                                  choices=self.choices,
                                  majorDimension=self.columns,
                                  style=style)
        try: self.options.SetSelection(int(self.val))
        except: pass
        if self.can_disable:
            self._enabler = wx.CheckBox(parent, self.id+1, "")
            szr.Add(self._enabler)
            wx.EVT_CHECKBOX(self._enabler, self.id+1,
                         lambda e: self.toggle_active(e.IsChecked()))
            self.options.Enable(self.is_active())
            self.options.SetLabel("")
            self._enabler.SetValue(self.is_active())
        szr.Add(self.options, 1, wx.EXPAND)
        self.panel = szr
        self.bind_event(self.on_change_val)

    def bind_event(self, function):
        def func_2(event, function=function, self=self):
            if self.options.IsEnabled():
                function(event)
        wx.EVT_RADIOBOX(self.options, self.id, func_2)

    def get_value(self):
        try: return self.options.GetSelection()
        except AttributeError: return self.val

    def get_str_value(self):
        try: return self.options.GetStringSelection()
        except AttributeError:
            if 0 <= self.val < len(self.choices):
                return self.choices[self.val]
            else: return ''

    def set_value(self, value):
        try: self.val = int(value)
        except ValueError: self.val = self.choices.index(value)
        try: self.options.SetSelection(self.val)
        except AttributeError: pass

    def set_str_value(self, value):
        try:
            self.val = self.choices.index(value)
            self.options.SetSelection(self.val)
        except (AttributeError, ValueError): pass

    def write(self, outfile, tabs):
        if self.is_active():
            outfile.write('    ' * tabs + '<%s>%s</%s>\n' %
                          (self.name, escape(_encode(self.get_str_value())),
                           self.name))

# end of class RadioProperty


class GridProperty(Property): 
    """\
    Property whose values are modified through a wxGrid table.
    """
    STRING, INT, FLOAT, BOOL = 0, 1, 2, 3
    col_format = [lambda g, c: None,
                  lambda g, c: g.SetColFormatNumber(c),
                  lambda g, c: g.SetColFormatFloat(c),
                  lambda g, c: g.SetColFormatBool(c)]
    def __init__(self, owner, name, parent, cols, rows=1, can_add=True,
                 can_remove=True, can_insert=True, label=None):
        # cols: list of 2-tuples with these fields:
        #     - label for the column
        #     - type: GridProperty.STRING, GridProperty.INT, GridProperty.FLOAT
        # rows: number of rows
        Property.__init__(self, owner, name, parent, label=label)
        self.val = owner[name][0]()
        self.set_value(self.val)
        self.rows, self.cols = rows, cols
        self.can_add = can_add
        self.can_remove = can_remove
        self.can_insert = can_insert
        self.panel = None
        self.cur_row = 0
        if parent is not None: self.display(parent)

    def display(self, parent):
        """\
        Actually builds the grid to set the value of the property
        interactively
        """
        self.panel = wx.Panel(parent, -1) # why if the grid is not on this
                                          # panel it is not displayed???
        label = getattr(self, 'label', _mangle(self.dispName))
        sizer = wx.StaticBoxSizer(wx.StaticBox(self.panel, -1, label),
                                  wx.VERTICAL)
        self.btn_id = wx.NewId()
        self.btn = wx.Button(self.panel, self.btn_id, _("  Apply  "),
                             style=wx.BU_EXACTFIT)
        if self.can_add:
            self.add_btn = wx.Button(self.panel, self.btn_id+1, _("  Add  "),
                                     style=wx.BU_EXACTFIT)
        if self.can_insert:
            self.insert_btn = wx.Button(self.panel, self.btn_id+3,
                                        _("  Insert  "),
                                        style=wx.BU_EXACTFIT)
        if self.can_remove:
            self.remove_btn = wx.Button(self.panel, self.btn_id+2,
                                        _("  Remove  "),
                                        style=wx.BU_EXACTFIT)
        self.grid = wx.grid.Grid(self.panel, -1)
        self.grid.CreateGrid(self.rows, len(self.cols))
        if misc.check_wx_version(2, 3, 3):
            self.grid.SetMargins(0, 0)
        else:
            # wx 2.3.2 seems to have some problems with grid scrollbars...
            self.grid.SetMargins(0, self.grid.GetDefaultRowSize())

        for i in range(len(self.cols)):
            self.grid.SetColLabelValue(i, misc.wxstr(self.cols[i][0]))
            GridProperty.col_format[self.cols[i][1]](self.grid, i)

        self.cols = len(self.cols)
        self.grid.SetRowLabelSize(0)
        self.grid.SetColLabelSize(20)

        self.btn_sizer = wx.BoxSizer(wx.HORIZONTAL)
        _w = self.btn.GetTextExtent(self.btn.GetLabel())[0]
        if misc.check_wx_version(2, 5, 2): extra_flag = wx.FIXED_MINSIZE
        else: extra_flag = 0
        #self.btn.SetSize((_w, -1))
        self.btn_sizer.Add(self.btn, 0, extra_flag)
        if self.can_add:
            _w = self.add_btn.GetTextExtent(self.add_btn.GetLabel())[0]
            #self.add_btn.SetSize((_w, -1))
            self.btn_sizer.Add(self.add_btn, 0, wx.LEFT|wx.RIGHT|extra_flag, 4)
            wx.EVT_BUTTON(self.add_btn, self.btn_id+1, self.add_row)
        if self.can_insert: 
            _w = self.insert_btn.GetTextExtent(self.insert_btn.GetLabel())[0]
            #self.insert_btn.SetSize((_w, -1))
            self.btn_sizer.Add(
                self.insert_btn, 0, wx.LEFT|wx.RIGHT|extra_flag, 4)
            wx.EVT_BUTTON(self.insert_btn, self.btn_id+3, self.insert_row)
        if self.can_remove:
            _w = self.remove_btn.GetTextExtent(self.remove_btn.GetLabel())[0]
            #self.remove_btn.SetSize((_w, -1))
            self.btn_sizer.Add(self.remove_btn, 0, extra_flag)
            wx.EVT_BUTTON(self.remove_btn, self.btn_id+2, self.remove_row)
        sizer.Add(self.btn_sizer, 0, wx.BOTTOM|wx.EXPAND, 2)
        sizer.Add(self.grid, 1, wx.EXPAND)
        self.panel.SetAutoLayout(1)
        self.panel.SetSizer(sizer)
        self.panel.SetSize(sizer.GetMinSize())

        wx.grid.EVT_GRID_SELECT_CELL(self.grid, self.on_select_cell)
        self.bind_event(self.on_change_val)
        self.set_value(self.val)

    def on_select_cell(self, event):
        self.cur_row = event.GetRow()
        event.Skip()

    def bind_event(self, function):
        def func(event):
            self.grid.SaveEditControlValue()
            function(event)
        wx.EVT_BUTTON(self.btn, self.btn_id, func)

    def get_value(self):
        if not hasattr(self, 'grid'): return self.val
        l = []
        for i in range(self.rows):
            l2 = []
            for j in range(self.cols):
                l2.append(self.grid.GetCellValue(i, j))
            l.append(l2)
        return l

    def on_change_val(self, event, first=[True]):
        """\
        Event handler called to notify owner that the value of the Property
        has changed
        """
        val = self.get_value()
        def ok():
            if len(self.val) != len(val): return True
            for i in range(len(val)):
                for j in range(len(val[i])):
                    if not misc.streq(val[i][j], self.val[i][j]): return True
            return False
        if ok():
            common.app_tree.app.saved = False # update the status of the app
            if self.setter: self.setter(val)
            else:
                self.owner[self.name][1](val)
            self.val = val
        first[0] = False
        event.Skip()

    def set_value(self, values):
        #self.val = values
        self.val = [[misc.wxstr(v) for v in val] for val in values]
        if not hasattr(self, 'grid'): return
        # values is a list of lists with the values of the cells
        size = len(values)
        if self.rows < size:
            self.grid.AppendRows(size-self.rows)
            self.rows = size
        elif self.rows != size: self.grid.DeleteRows(size, self.rows-size)
        for i in range(len(self.val)):
            for j in range(len(self.val[i])):
                self.grid.SetCellValue(i, j, self.val[i][j])
            
    def add_row(self, event):
        self.grid.AppendRows()
        self.grid.MakeCellVisible(self.rows, 0)
        self.grid.ForceRefresh()
        self.rows += 1

    def remove_row(self, event):
        if self.rows > 0: #1:
            self.grid.DeleteRows(self.cur_row)
            self.rows -= 1

    def insert_row(self, event):
        self.grid.InsertRows(self.cur_row)
        self.grid.MakeCellVisible(self.cur_row, 0)
        self.grid.ForceRefresh()
        self.rows += 1

    def set_col_sizes(self, sizes):
        """\
        sets the width of the columns.
        sizes is a list of integers with the size of each column: a value of 0
        stands for a default size, while -1 means to expand the column to fit
        the available space (at most one column can have size -1)
        """
        col_to_expand = -1
        total_w = 0
        for i in range(self.grid.GetNumberCols()):
            try: w = sizes[i]
            except IndexError: return
            if not w:
                self.grid.AutoSizeColumn(i)
                total_w += self.grid.GetColSize(i)
            elif w < 0: col_to_expand = i
            else:
                self.grid.SetColSize(i, w)
                total_w += w
        if col_to_expand >= 0:
            self.grid.AutoSizeColumn(col_to_expand)
            w = self.grid.GetSize()[0] - total_w
            if w >= self.grid.GetColSize(col_to_expand):
                self.grid.SetColSize(col_to_expand, w)

# end of class GridProperty

class ComboBoxProperty(Property, _activator):
    """\
    Properties whose values can be changed with a combobox.
    """
    def __init__(self, owner, name, choices, parent=None, label=None,
                 can_disable=False, enabled=False,
                 write_always=False):
        Property.__init__(self, owner, name, parent, label=label)
        self.val = misc.wxstr(owner[name][0]())
        if label is None: label = _mangle(name)
        self.label = label
        self.panel = None
        self.write_always = write_always
        self.choices = choices
        self.can_disable = can_disable
        _activator.__init__(self)
        if can_disable: self.toggle_active(enabled)
        if parent is not None: self.display(parent)

    def display(self, parent):
        """\
        Actually builds the check box to set the value of the property
        interactively
        """
        self.id = wx.NewId()
        self.cb = wx.ComboBox(parent, self.id, choices=self.choices,
                              style=wx.CB_DROPDOWN|wx.CB_READONLY)
        self.cb.SetValue(self.val)
        if self.can_disable:
            self._enabler = wx.CheckBox(parent, self.id+1, '', size=(1, -1))
        label = wx.StaticText(parent, -1, self.label)
        if self.can_disable:
            wx.EVT_CHECKBOX(self._enabler, self.id+1,
                         lambda event: self.toggle_active(event.IsChecked()))
            self.cb.Enable(self.is_active())
            self._enabler.SetValue(self.is_active())
            self._target = self.cb
        sizer = wx.BoxSizer(wx.HORIZONTAL)
        sizer.Add(label, 2, wx.ALIGN_CENTER_VERTICAL|wx.ALL, 3)
        if getattr(self, '_enabler', None) is not None:
            sizer.Add(self._enabler, 1, wx.ALL|wx.ALIGN_CENTER, 3)
            option = 4
        else:
            option = 5
        sizer.Add(self.cb, option, wx.ALIGN_CENTER|wx.ALL, 3)
        self.panel = sizer
        self.bind_event(self.on_change_val)
        
    def bind_event(self, function):
        wx.EVT_COMBOBOX(self.cb, self.id, function)

    def get_value(self):
        try: return misc.wxstr(self.cb.GetValue())
        except AttributeError: return misc.wxstr(self.val)

    def set_value(self, val):
        self.val = misc.wxstr(val)
        try: self.cb.SetValue(self.val)
        except AttributeError: pass

    def write(self, outfile, tabs):
        if self.write_always or self.get_value():
            if self.getter: value = misc.wxstr(self.getter())
            else: value = misc.wxstr(self.owner[self.name][0]())
            if value != 'None':
                fwrite = outfile.write
                fwrite('    ' * tabs + '<%s>' % self.name)
                fwrite(escape(_encode(value)))
                fwrite('</%s>\n' % self.name)

# end of class ComboBoxProperty

