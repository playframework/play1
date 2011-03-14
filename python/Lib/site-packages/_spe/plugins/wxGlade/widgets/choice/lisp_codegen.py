# lisp_codegen.py : lisp generator functions for wxChoice objects
# $Id: lisp_codegen.py,v 1.1 2005/09/22 06:58:29 efuzzyone Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY


import common
from ChoicesCodeHandler import *

class LispCodeGenerator:
    def get_code(self, obj):
        init = []
        plgen = common.code_writers['lisp']
        prop = obj.properties
        id_name, id = plgen.generate_code_id(obj)
        choices = prop.get('choices', [])

        if not obj.parent.is_toplevel:
            parent = '(slot-%s obj)' % obj.parent.name
        else:
            parent = '(slot-top-window obj)'

        style = prop.get("style")
        if not style:
            style = '0'
        else:
            style = style.strip().replace('|',' ')
            if style.find(' ') != -1:
                style = '(logior %s)' % style

        if id_name: init.append(id_name)
        length = len(choices)
        choices = ' '.join([plgen.quote_str(c) for c in choices])
        init.append('(setf (slot-%s obj) (wxChoice_Create %s %s -1 -1 -1 -1 %s (vector %s)  %s))\n' %
                    (obj.name, parent, id, length, choices, style))
        props_buf = plgen.generate_common_properties(obj)

        selection = prop.get('selection')
        if selection is not None:
            props_buf.append('(wxChoice_SetSelection (slot-%s obj) %s)\n' %
                             (obj.name, selection))

        return init, props_buf, []


# end of class LispCodeGenerator

def initialize():
    common.class_names['EditChoice'] = 'wxChoice'

    plgen = common.code_writers.get('lisp')
    if plgen:
        plgen.add_widget_handler('wxChoice', LispCodeGenerator())
        plgen.add_property_handler('choices', ChoicesCodeHandler)
