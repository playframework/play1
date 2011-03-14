# lisp_codegen.py : lisp generator functions for wxSlider objects
# $Id: lisp_codegen.py,v 1.1 2005/09/22 07:00:30 efuzzyone Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common


class LispCodeGenerator:
    def get_code(self, obj):
        plgen = common.code_writers['lisp']
        prop = obj.properties
        id_name, id = plgen.generate_code_id(obj)
        value = prop.get('value', '0')

        try:
            min_v, max_v = [ s.strip() for s in prop['range'].split(',') ]
        except:
            min_v, max_v = '0', '10'

        if not obj.parent.is_toplevel:
            parent = '(slot-%s obj)' % obj.parent.name
        else:
            parent = '(slot-top-window obj)'

        style = prop.get("style")
        if not( style and style != 'wxSL_HORIZONTAL'): # default style
            style = '0'
        else:    
            style = style.strip().replace('|',' ')
            if style.find(' ') != -1:
                style = '(logior %s)' % style
            
        init = []

        if id_name: init.append(id_name)

        init.append('(setf (slot-%s obj) (wxSlider_Create %s %s %s %s %s -1 -1 -1 -1 %s))\n'
                    % (obj.name, parent, id, value, min_v, max_v, style))

        props_buf = plgen.generate_common_properties(obj)

        return init, props_buf, []

# end of class LispCodeGenerator


def initialize():
    common.class_names['EditSlider'] = 'wxSlider'

    plgen = common.code_writers.get('lisp')
    if plgen:
        plgen.add_widget_handler('wxSlider', LispCodeGenerator())

