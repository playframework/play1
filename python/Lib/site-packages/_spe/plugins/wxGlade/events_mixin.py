# events_mixin.py: mixin class for 'events' property
# $Id: events_mixin.py,v 1.7 2007/01/29 19:50:35 dinogen Exp $
# 
# Copyright (c) 2002-2004 Alberto Griggio <agriggio@users.sf.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

#from wxPython.wx import *
#from wxPython.grid import *
import wx
import wx.grid

import re

from widget_properties import GridProperty
from misc import enumerate
from xml.sax.saxutils import escape, quoteattr


class EventsProperty(GridProperty):
    def __init__(self, owner):
        cols = [(_('Event'), GridProperty.STRING),
                (_('Handler'), GridProperty.STRING)]
        GridProperty.__init__(self, owner, 'events', None, cols,
                              len(owner.events), False, False, False, label=_('events'))
        self._pos = {}
        for index, name in enumerate(owner.events):
            self._pos[name] = index
        self.validator_re = re.compile(r'^\s*[\w-]+\s*$')
        self.set_value([[name, ''] for name in owner.events])

    def display(self, parent):
        GridProperty.display(self, parent)
        attr = wx.grid.GridCellAttr()
        attr.SetReadOnly(True)
        self.grid.SetColAttr(0, attr)
        self.grid.AutoSizeColumn(0, False)
        self.grid.AutoSizeColumn(1, False)
        wx.grid.EVT_GRID_CELL_CHANGE(self.grid, self.on_change_val)
        szr = self.panel.GetSizer()
        szr.Show(self.btn_sizer, False)
        szr.Layout()

    def set_value_dict(self, values_dict):
        val = self.get_value()
        for row in val:
            row[1] = values_dict.get(row[0], "")
        self.set_value(val)

    def write(self, outfile, tabs):
        if self.getter:
            handlers = self.getter()
        else:
            handlers = self.owner[self.name][0]()
        if handlers:
            written = False
            write = outfile.write
            #write('    ' * tabs + '<events>\n')
            stab = '    ' * (tabs+1)
            for event, handler in handlers:
                if handler:
                    if not written:
                        written = True
                        write('    ' * tabs + '<events>\n')
                    write('%s<handler event=%s>%s</handler>\n' %
                          (stab, quoteattr(event), escape(handler.strip())))
            if written:
                write('    ' * tabs + '</events>\n')

    def on_change_val(self, event):
        val = self.get_value()
        for i in range(len(val)):
            handler = val[i][1].strip()
            if handler and self.validator_re.match(handler) is None:
                self.set_value(self.val)
                return event.Skip()
        GridProperty.on_change_val(self, event)

# end of class EventsProperty


class EventsPropertyHandler(object):
    def __init__(self, owner):
        #print 'EventsPropertyHandler', owner.name
        self.owner = owner
        self.handlers = {}
        self.event_name = None
        self.curr_handler = []
        
    def start_elem(self, name, attrs):
        if name == 'handler':
            self.event_name = attrs['event']

    def end_elem(self, name):
        if name == 'handler':
            if self.event_name and self.curr_handler:
                self.handlers[self.event_name] = ''.join(self.curr_handler)
            self.event_name = None
            self.curr_handler = []
        elif name == 'events':
            self.owner.properties['events'].set_value_dict(self.handlers)
            self.owner.set_events_dict(self.handlers)
            return True # to remove this handler

    def char_data(self, data):
        data = data.strip()
        if data:
            self.curr_handler.append(data)

# end of class EventsPropertyHandler


default_events = []


class EventsMixin:
    def __init__(self):
        if not hasattr(self, 'events'):
            self.events = default_events
        self.handlers = {}

        if self.events:
            self.access_functions['events'] = self.get_events, self.set_events
            self.properties['events'] = EventsProperty(self)

    def get_events(self):
        ret = []
        for e in self.events:
            ret.append([e, self.handlers.get(e, '')])
        return ret

    def set_events(self, handlers_list):
        self.handlers = {}
        for event, val in handlers_list:
            if val.strip():
                self.handlers[event] = val

    def set_events_dict(self, handlers):
        self.handlers = handlers

    def create_events_property(self):
        if not self.events:
            return
        panel = wx.Panel(self.notebook, -1) 
        self.properties['events'].display(panel)
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer.Add(self.properties['events'].panel, 1, wx.ALL|wx.EXPAND, 5)
        panel.SetSizerAndFit(sizer)
        self.notebook.AddPage(panel, _('Events'))

    def get_property_handler(self, name):
        if name == 'events':
            return EventsPropertyHandler(self)
        return None

# end of class EventsMixin
