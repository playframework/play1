# perl_codegen.py : perl generator functions for wxGauge objects
# $Id: lisp_codegen.py,v 1.1 2005/09/22 06:59:38 efuzzyone Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common

class PerlCodeGenerator:
    def get_code(self, obj):
        init = []

        plgen = common.code_writers['perl']
        prop = obj.properties
        id_name, id = plgen.generate_code_id(obj)
        g_range = prop.get('range', '10')

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

        init.append('(setf (slot-%s obj) (wxGauge_Create %s %s %s -1 -1 -1 -1 %s))\n' %
                    (obj.name, parent, id, g_range, style))
        props_buf = plgen.generate_common_properties(obj)

        return init, props_buf, []

# end of class PerlCodeGenerator

def initialize():
    common.class_names['EditGauge'] = 'wxGauge'

    plgen = common.code_writers.get('perl')
    if plgen:
        plgen.add_widget_handler('wxGauge', PerlCodeGenerator())

