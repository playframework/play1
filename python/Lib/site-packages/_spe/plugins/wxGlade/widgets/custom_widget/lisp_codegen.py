# perl_codegen.py : perl generator functions for CustomWidget objects
# $Id: lisp_codegen.py,v 1.1 2005/09/22 06:59:00 efuzzyone Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY


import common
from codegen import ArgumentsCodeHandler, _fix_arguments


class PerlCodeGenerator:
    def get_code(self, widget):
        init = []
        plgen = common.code_writers['perl']
        prop = widget.properties
        id_name, id = plgen.generate_code_id(widget)

        if not widget.parent.is_toplevel:
            parent = '(object-%s self)' % widget.parent.name
        else:
            parent = 'nil'

        
        if id_name: init.append(id_name)
        arguments = _fix_arguments(prop.get('arguments', []), parent, id, prop.get('size', "-1, -1"))
        init.append('use %s;\n' % widget.klass ) # yuck
        init.append('$self->{%s} = %s->new(%s);\n' %
            (widget.name, widget.klass, ", ".join(arguments)))
        props_buf = plgen.generate_common_properties(widget)

        return init, props_buf, []

# end of class PerlCodeGenerator

def initialize():
    common.class_names['CustomWidget'] = 'CustomWidget'

    plgen = common.code_writers.get('perl')
    if plgen:
        plgen.add_widget_handler('CustomWidget', PerlCodeGenerator())
        plgen.add_property_handler('arguments', ArgumentsCodeHandler,
                                    'CustomWidget')
