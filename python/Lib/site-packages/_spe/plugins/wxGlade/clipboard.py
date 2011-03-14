# clipboard.py: support for cut & paste of wxGlade widgets
# $Id: clipboard.py,v 1.19 2007/07/21 11:30:29 agriggio Exp $
# 
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

#from wxPython.wx import *
import wx

# Format used by wxGlade for the clipboard.
_widget_data_format = wx.CustomDataFormat("wxglade_widget")

class _WidgetDataObject(wx.CustomDataObject):
    """\
    Object representig a widget in the clipboard.
    """
    def __init__(self, *args):
        wx.CustomDataObject.__init__(self, _widget_data_format)
        if args:
            data = apply(self._widget2repr, args)
            self.SetData(data)

    def _widget2repr(self, *args):
        """\
        Convert *args into a string and returns it.
        *args contains option, flag, border, xml_str.
        """
        assert len(args) == 4
        return ":".join([str(elem) for elem in args])

    def GetWidgetData(self):
        """\
        Convert a string into option, flag, border and xml_string
        and returns them in a list.
        """
        ret = self.GetData().split(":", 3)
        assert len(ret) == 4, _("Invalid data in the clipboard")
        # remove the dirt at the end of xml_str
        bound = ret[3].rfind('>')+1
        ret[3] = ret[3][:bound]
        for i in range(3):
            # option, flag and border are integers.
            ret[i] = int(ret[i])
        return ret


def copy(widget):
    """\
    Copies widget and all its children to the clipboard.
    """
    from cStringIO import StringIO
    xml_str = StringIO()
    widget.node.write(xml_str, 0)
    flag = widget.get_int_flag() 
    option = widget.get_option()
    border = widget.get_border()
    if wx.TheClipboard.Open():
        try:
            wdo = _WidgetDataObject(option, flag, border,
                                    xml_str.getvalue())
            if not wx.TheClipboard.SetData(wdo):
                print _("Data can't be copied to clipboard.")
                return False
            return True
        finally:
            wx.TheClipboard.Close()
    else:
        print _("Clipboard can't be opened.")
        return False


def cut(widget):
    """\
    Copies widget and all its children to the clipboard and then
    removes them.
    """
    if copy(widget):
        widget.remove()
        return True
    else:
        return False


def paste(parent, sizer, pos):
    """\
    Copies a widget (and all its children) from the clipboard to the given
    destination (parent, sizer and position inside the sizer)
    returns True if there was something to paste, False otherwise.
    """
    if wx.TheClipboard.Open():
        try:
            if wx.TheClipboard.IsSupported(_widget_data_format):
                wdo = _WidgetDataObject()
                if not wx.TheClipboard.GetData(wdo):
                    print _("Data can't be copied from clipboard.")
                    return False
            else:
                return False
        finally:
            wx.TheClipboard.Close()
    else:
        print _("Clipboard can't be opened.")
        return False

    option, flag, border, xml_str = wdo.GetWidgetData()
    if xml_str:
        import xml_parse
        try:
            wx.BeginBusyCursor()
            parser = xml_parse.ClipboardXmlWidgetBuilder(parent, sizer, pos,
                                                         option, flag, border)
            parser.parse_string(xml_str)
            return True # Widget hierarchy pasted.
        finally:
            wx.EndBusyCursor()
    return False # There's nothing to paste.


#-----------------------------------------------------------------------------
# 2004-02-19 ALB: D&D support (thanks to Chris Liechti)
#-----------------------------------------------------------------------------

class FileDropTarget(wx.FileDropTarget):
    def __init__(self, parent):
        wx.FileDropTarget.__init__(self)
        self.parent = parent

    def OnDropFiles(self, x, y, filenames):
        if len(filenames) > 1:
            wx.MessageBox(_("Please only drop one file at a time"),
                "wxGlade", wx.ICON_ERROR)
        elif filenames:
            path = filenames[0]
            if self.parent.ask_save(): 
                self.parent._open_app(path)

# end of class FileDropTarget
