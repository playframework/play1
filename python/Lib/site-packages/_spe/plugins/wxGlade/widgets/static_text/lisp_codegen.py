# lisp_codegen.py : lisp generator functions for wxStaticText objects
# $Id: lisp_codegen.py,v 1.1 2005/09/22 06:43:12 efuzzyone Exp $
#
# Copyright (c) 2002-2004 D. H. aka crazyinsomniac on sourceforge
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common

class LispCodeGenerator:
    def get_code(self, obj):
        init = []
        plgen = common.code_writers['lisp']
        prop = obj.properties

        attribute = plgen.test_attribute(obj)

        id_name, id = plgen.generate_code_id(obj) 
        label = plgen.quote_str(prop.get('label', ''))
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

        init.append('(setf (slot-%s obj) (wxStaticText_Create %s %s %s -1 -1 -1 -1 %s))\n'
                    % (obj.name, parent, id, label, style))

        props_buf = plgen.generate_common_properties(obj)
        if not attribute:
            # the object doesn't have to be stored as an attribute of the
            # custom class, but it is just considered part of the layout
            return [], [], init + props_buf
        return init, props_buf, []

# end of class LispCodeGenerator

def initialize():

    common.class_names['EditStaticText'] = 'wxStaticText'
    plgen = common.code_writers.get('lisp')

    if plgen:
        plgen.add_widget_handler('wxStaticText', LispCodeGenerator())
