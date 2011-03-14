# lisp_codegen.py : lisp generator functions for wxToggleButton objects
# $Id: lisp_codegen.py,v 1.1 2005/09/22 06:41:28 efuzzyone Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common

class LispCodeGenerator:
    def get_code(self, obj):
        init = []
        plgen = common.code_writers['lisp']
        prop = obj.properties
        id_name, id = plgen.generate_code_id(obj)
        label = plgen.quote_str(prop.get('label', ''))
        if not obj.parent.is_toplevel:
            parent = '(slot-%s obj)' % obj.parent.name
        else:
            parent = '(slot-top-window obj)'

        if id_name: init.append(id_name)

        init.append('(setf (slot-%s obj) (wxToggleButton_Create %s %s %s -1 -1 -1 -1 0))\n' %
            (obj.name, parent, id, label))
        props_buf = plgen.generate_common_properties(obj)

        value = prop.get('value')
        if value:
            props_buf.append('(wxToggleButton_SetValue (slot-%s obj) %s)\n'
                             % (obj.name, value))
        return init, props_buf, []

# end of class LispCodeGenerator


def initialize():
    common.class_names['EditToggleButton'] = 'wxToggleButton'
    plgen = common.code_writers.get('lisp')

    if plgen:
        plgen.add_widget_handler('wxToggleButton', LispCodeGenerator())

