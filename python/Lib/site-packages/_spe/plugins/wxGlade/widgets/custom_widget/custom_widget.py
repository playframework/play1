# custom_widget.py: custom wxWindow objects
# $Id: custom_widget.py,v 1.21 2007/08/07 12:13:43 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, misc
from tree import Tree
from widget_properties import *
from edit_windows import ManagedBase

class ArgumentsProperty(GridProperty):
    def write(self, outfile, tabs):
        from xml.sax.saxutils import escape
        if self.getter: values = self.getter()
        else: values = self.owner[self.name][0]()
        if values:
            write = outfile.write
            write('    ' * tabs + '<arguments>\n')
            stab = '    ' * (tabs+1)
            for value in values:
                write('%s<argument>%s</argument>\n' % (stab, escape(value[0])))
            write('    ' * tabs + '</arguments>\n')

# end of class ArgumentsProperty


class ArgumentsHandler:
    def __init__(self, parent):
        self.parent = parent
        self.arguments = []
        self.curr_arg = []

    def start_elem(self, name, attrs):
        pass

    def end_elem(self, name):
        if name == 'arguments':
            self.parent.arguments = self.arguments
            self.parent.properties['arguments'].set_value(self.arguments)
            return True
        elif name == 'argument':
            self.arguments.append(["".join(self.curr_arg)])
            self.curr_arg = []
        return False

    def char_data(self, data):
        self.curr_arg.append(data)

# end of class ArgumentsHandler


class CustomWidget(ManagedBase):
    def __init__(self, name, klass, parent, id, sizer, pos, property_window,
                 show=True):
        ManagedBase.__init__(self, name, klass, parent, id, sizer, pos,
                             property_window, show)
        self.arguments = [['$parent'], ['$id']] #,['$width'],['$height']]
        self.access_functions['arguments'] = (self.get_arguments,
                                              self.set_arguments)
        
        cols = [('Arguments', GridProperty.STRING)]
        self.properties['arguments'] = ArgumentsProperty(self, 'arguments',
                                                         None, cols, 2, label=_("arguments"))

    def set_klass(self, value):
        ManagedBase.set_klass(self, value)
        if self.widget: self.widget.Refresh()

    def create_widget(self):
        self.widget = wx.Window(self.parent.widget, self.id,
                               style=wx.SUNKEN_BORDER|wx.FULL_REPAINT_ON_RESIZE)
        wx.EVT_PAINT(self.widget, self.on_paint)

    def finish_widget_creation(self):
        ManagedBase.finish_widget_creation(self, sel_marker_parent=self.widget)

    def on_paint(self, event):
        dc = wx.PaintDC(self.widget)
        dc.BeginDrawing()
        dc.SetBrush(wx.WHITE_BRUSH)
        dc.SetPen(wx.BLACK_PEN)
        dc.SetBackground(wx.WHITE_BRUSH)
        dc.Clear()
        w, h = self.widget.GetClientSize()
        dc.DrawLine(0, 0, w, h)
        dc.DrawLine(w, 0, 0, h)
        text = _('Custom Widget: %s') % self.klass
        tw, th = dc.GetTextExtent(text)
        x = (w - tw)/2
        y = (h - th)/2
        dc.SetPen(wx.ThePenList.FindOrCreatePen(wx.BLACK, 0, wx.TRANSPARENT))
        dc.DrawRectangle(x-1, y-1, tw+2, th+2)
        dc.DrawText(text, x, y)
        dc.EndDrawing()

    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.ScrolledWindow(self.notebook, -1)
        szr = wx.BoxSizer(wx.VERTICAL)
        args = self.properties['arguments']
        args.display(panel)
        szr.Add(args.panel, 1, wx.ALL|wx.EXPAND, 5)
        help_btn = wx.Button(panel, -1, _('Help on "Arguments" property'))
        text = _("""\
The 'Arguments' property behaves differently when generating
XRC code wrt C++ or python: you can use it to add custom attributes
to the resource object. To do so, arguments must have the following
format: ATTRIBUTE_NAME: ATTRIBUTE_VALUE
For instance:
    default_value: 10
is translated to:
    <default_value>10</default_value>
Invalid entries are silently ignored""")
        def show_help(event):
            wx.MessageBox(text, _('Help on "Arguments" property'),
                         wx.OK|wx.CENTRE|wx.ICON_INFORMATION)
        wx.EVT_BUTTON(help_btn, -1, show_help)
        szr.Add(help_btn, 0, wx.BOTTOM|wx.LEFT|wx.RIGHT|wx.EXPAND, 5)
        panel.SetAutoLayout(True)
        panel.SetSizer(szr)
        szr.Fit(panel)
        self.notebook.AddPage(panel, 'Widget')
        args.set_col_sizes([-1])

    def get_arguments(self):
        return self.arguments

    def set_arguments(self, value):
        self.arguments = [[misc.wxstr(v) for v in val] for val in value]

    def get_property_handler(self, name):
        if name == 'arguments': return ArgumentsHandler(self)
        return ManagedBase.get_property_handler(self, name)
       
# end of class CustomWidget
        

def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for CustomWidget objects.
    """
    class Dialog(wx.Dialog):
        def __init__(self, number=[0]):
            title = _('Select widget class')
            wx.Dialog.__init__(self, None, -1, title)
            self.klass = 'CustomWidget'
            if number[0]: self.klass = 'CustomWidget%s' % (number[0]-1)
            number[0] += 1
            klass_prop = TextProperty(self, 'class', self, label=_("class"))
            szr = wx.BoxSizer(wx.VERTICAL)
            szr.Add(klass_prop.panel, 0, wx.ALL|wx.EXPAND, 5)
            szr.Add(wx.Button(self, wx.ID_OK, _('OK')), 0,
                    wx.ALL|wx.ALIGN_CENTER, 5)
            self.SetAutoLayout(True)
            self.SetSizer(szr)
            szr.Fit(self)
            w = self.GetTextExtent(title)[0] + 50
            if self.GetSize()[0] < w:
                self.SetSize((w, -1))
            self.CenterOnScreen()
                
        def __getitem__(self, value):
            def set_klass(c): self.klass = c
            return (lambda : self.klass, set_klass)
    # end of inner class

    dialog = Dialog()
    dialog.ShowModal()

    name = 'window_%d' % number[0]
    while common.app_tree.has_name(name):
        number[0] += 1
        name = 'window_%d' % number[0]
    win = CustomWidget(name, dialog.klass, parent, wx.NewId(), sizer, pos,
                       common.property_panel)
    node = Tree.Node(win)
    win.node = node

    win.set_option(1)
    win.set_flag("wxEXPAND")
    win.show_widget(True)

    common.app_tree.insert(node, sizer.node, pos-1)
    sizer.set_item(win.pos, 1, wx.EXPAND)

def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build CustomWidget objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: name = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    if not sizer or not sizeritem:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    win = CustomWidget(name, 'CustomWidget', parent, wx.NewId(), sizer, pos,
                       common.property_panel, True)
    sizer.set_item(win.pos, option=sizeritem.option, flag=sizeritem.flag,
                   border=sizeritem.border)
    node = Tree.Node(win)
    win.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return win


def initialize():
    """\
    initialization function for the module: returns a wx.BitmapButton to be
    added to the main palette.
    """
    common.widgets['CustomWidget'] = builder
    common.widgets_from_xml['CustomWidget'] = xml_builder

    return common.make_object_button('CustomWidget', 'icons/custom.xpm',
                                     tip='Add a custom widget')
    
