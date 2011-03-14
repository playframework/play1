# lisp_codegen.py : lisp generator functions for wxTreeCtrl objects
# $Id: lisp_codegen.py,v 1.1 2005/09/22 06:39:48 efuzzyone Exp $
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

        if not obj.parent.is_toplevel:
            parent = '(object-%s obj)' % obj.parent.name
        else:
            parent = '(slot-top-window obj)'

        style = prop.get("style")

        if not( style and style != 'wxLI_HORIZONTAL' ): # default style
            style = ''
        else:
            style = style.strip().replace('|',' ')
            if style.find(' ') != -1:
                style = '(logior %s)' % style
    
        init = []

        if id_name: init.append(id_name)

        init.append('(setf (slot-%s obj) (wxTreeCtrl_Create %s %s -1 -1 -1 -1 %s))\n'
                    % (obj.name, parent, id, style))
        props_buf = plgen.generate_common_properties(obj)

        return init, props_buf, []

# end of class LispCodeGenerator


def initialize():
    common.class_names['EditTreeCtrl'] = 'wxTreeCtrl'
    plgen = common.code_writers.get('lisp')

    if plgen:
        plgen.add_widget_handler('wxTreeCtrl', LispCodeGenerator())

