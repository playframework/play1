# calendar_ctrl.py: wxCalendarCtrl objects
# $Header: /home/alb/tmp/wxglade_cvs_backup/wxGlade/widgets/calendar_ctrl/calendar_ctrl.py,v 1.11 2007/03/27 07:02:04 agriggio Exp $

# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
from edit_windows import ManagedBase
from tree import Tree
import common, misc
from widget_properties import *
#import needed modules for the wxCalendarCtrl
from wx.calendar import *

class EditCalendarCtrl(ManagedBase):

    events = [
    'EVT_CALENDAR',
    'EVT_CALENDAR_SEL_CHANGE',
    'EVT_CALENDAR_DAY',
    'EVT_CALENDAR_MONTH',
    'EVT_CALENDAR_YEAR',
    'EVT_CALENDAR_WEEKDAY_CLICKED']

    def __init__(self, name, parent, id, sizer, pos, property_window,
                 show=True):
        """\
        Class to handle wxCalendarCtrl objects
        """
        import config
        self.default = False
        ManagedBase.__init__(self, name, 'CalendarCtrl', parent, id, sizer, pos,
                             property_window, show=show)
        #self.access_functions['label'] = (self.get_label, self.set_label)
        #self.properties['label'] = TextProperty(self, 'label', None,
        #                                       multiline=True)
        self.access_functions['default'] = (self.get_default, self.set_default)
        self.access_functions['style'] = (self.get_style, self.set_style)
        self.properties['default'] = CheckBoxProperty(self, 'default', None, label=_("default"))
        style_labels = ('#section#' + _('Style'), 'wxCAL_SUNDAY_FIRST', 'wxCAL_MONDAY_FIRST', 
            'wxCAL_SHOW_HOLIDAYS', 'wxCAL_NO_YEAR_CHANGE', 'wxCAL_NO_MONTH_CHANGE',
            'wxCAL_SHOW_SURROUNDING_WEEKS','wxCAL_SEQUENTIAL_MONTH_SELECTION')
        self.style_pos = (CAL_SUNDAY_FIRST, CAL_MONDAY_FIRST, 
            CAL_SHOW_HOLIDAYS, CAL_NO_YEAR_CHANGE, CAL_NO_MONTH_CHANGE,
            CAL_SHOW_SURROUNDING_WEEKS, CAL_SEQUENTIAL_MONTH_SELECTION)
	self.tooltips=(_("Show Sunday as the first day in the week"),
			_("Show Monday as the first day in the week"),
			_("Highlight holidays in the calendar"),
			_("Disable the year changing"),
			_("Disable the month (and, implicitly, the year) changing"),
			_("Show the neighbouring weeks in the previous and next months"),
			_("Use alternative, more compact, style for the month and year selection controls."))
        self.properties['style'] = CheckListProperty(self, 'style', None,
                                                     style_labels,tooltips=self.tooltips)
        
        if config.preferences.default_border:
            self.border = config.preferences.default_border_size
            self.flag = wx.ALL

    def create_properties(self):
        ManagedBase.create_properties(self)
        panel = wx.Panel(self.notebook, -1)
        #self.properties['label'].display(panel)
        self.properties['default'].display(panel)
        self.properties['style'].display(panel)
        szr = wx.BoxSizer(wx.VERTICAL)
        #szr.Add(self.properties['label'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['default'].panel, 0, wx.EXPAND)
        szr.Add(self.properties['style'].panel, 0, wx.EXPAND)
        panel.SetAutoLayout(1)
        panel.SetSizer(szr)
        szr.Fit(panel)
        self.notebook.AddPage(panel, 'Widget')

    def create_widget(self):
        try:
            #TODO add all the other parameters for the CalendarCtrl especialy style=self.style and the initial date
            self.widget = CalendarCtrl(self.parent.widget, self.id ,style=self.style)
        except AttributeError:
            self.widget = CalendarCtrl(self.parent.widget, self.id)

    def get_default(self):
        return self.default

    def set_default(self, value):
        self.default = bool(int(value))

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


# end of class EditCalendarCtrl
        

def builder(parent, sizer, pos, number=[1]):
    """\
    factory function for EditCalendarCtrl objects.
    """
    label = 'calendar_ctrl_%d' % number[0]
    while common.app_tree.has_name(label):
        number[0] += 1
        label = 'calendar_ctrl_%d' % number[0]
    calendar_ctrl = EditCalendarCtrl(label, parent, wx.NewId(), sizer,
                        pos, common.property_panel)
    node = Tree.Node(calendar_ctrl)
    calendar_ctrl.node = node
    calendar_ctrl.show_widget(True)
    common.app_tree.insert(node, sizer.node, pos-1)


def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditCalendarCtrl objects from an xml file
    """
    from xml_parse import XmlParsingError
    try: label = attrs['name']
    except KeyError: raise XmlParsingError, _("'name' attribute missing")
    if sizer is None or sizeritem is None:
        raise XmlParsingError, _("sizer or sizeritem object cannot be None")
    calendar_ctrl = EditCalendarCtrl(label, parent, wx.NewId(), sizer,
                        pos, common.property_panel, show=False)
    node = Tree.Node(calendar_ctrl)
    calendar_ctrl.node = node
    if pos is None: common.app_tree.add(node, sizer.node)
    else: common.app_tree.insert(node, sizer.node, pos-1)
    return calendar_ctrl


def initialize():
    """\
    initialization function for the module.
    @rtype: wxBitmapButton
    @return: an icon to be added to the main palette. 
    """
    common.widgets['EditCalendarCtrl'] = builder
    common.widgets_from_xml['EditCalendarCtrl'] = xml_builder

    return common.make_object_button('EditCalendarCtrl', 'icons/calendar_ctrl.xpm')
