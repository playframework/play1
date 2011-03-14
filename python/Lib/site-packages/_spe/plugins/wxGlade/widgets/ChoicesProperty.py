# ChoicesProperty.py: defines a Property and two handlers used by choice,
# combo_box, radio_box, list_box
# $Id: ChoicesProperty.py,v 1.8 2007/03/27 07:02:05 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import widget_properties

class ChoicesProperty(widget_properties.GridProperty):
    def write(self, outfile, tabs):
        from xml.sax.saxutils import escape
        write = outfile.write
        write('    ' * tabs + '<choices>\n')
        tab_s = '    ' * (tabs+1)
        for val in self.get_value():
            v = widget_properties._encode(val[0])
            try:
                checked = int(val[1])
            except (IndexError, ValueError):
                checked = None
            if checked is None:
                write('%s<choice>%s</choice>\n' % (tab_s, escape(v)))
            else:
                write('%s<choice checked="%d">%s</choice>\n' % \
                      (tab_s, checked, escape(v)))
        write('    ' * tabs + '</choices>\n')

# end of class ChoicesProperty


class ChoicesHandler:
    def __init__(self, owner):
        self.choices = []
        self.curr_choice = []
        self.cur_checked = None
        self.owner = owner
        
    def start_elem(self, name, attrs):
        if name == 'choice':
            try:
                self.cur_checked = int(attrs['checked'])
            except (KeyError, ValueError):
                self.cur_checked = None
    
    def end_elem(self, name):
        if name == 'choice':
            if self.cur_checked is None:
                self.choices.append(["".join(self.curr_choice)])
            else:
                self.choices.append(["".join(self.curr_choice),
                                     self.cur_checked])
            self.curr_choice = []
            self.cur_checked = None
        elif name == 'choices':
            self.owner.set_choices(self.choices)
            self.owner.properties['choices'].set_value(
                self.owner.get_choices())
            self.choices = []
            return True # remove the handler
        
    def char_data(self, data):
        self.curr_choice.append(data)

# end of class ChoicesHandler

