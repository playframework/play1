# codegen.py: code generator functions for wxToggleButton objects
# $Id: codegen.py,v 1.13 2007/03/27 07:01:51 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common


class PythonCodeGenerator:
    def get_code(self, obj):
        pygen = common.code_writers['python']
        prop = obj.properties
        id_name, id = pygen.generate_code_id(obj)
        label = pygen.quote_str(prop.get('label', ''))
        if not obj.parent.is_toplevel: parent = 'self.%s' % obj.parent.name
        else: parent = 'self'
        init = []
        if id_name: init.append(id_name)
        klass = obj.klass
        if klass == obj.base: klass = pygen.cn(klass)
        init.append('self.%s = %s(%s, %s, %s)\n' % 
                    (obj.name, klass, parent, id, label))
        props_buf = pygen.generate_common_properties(obj)
        value = prop.get('value')
        if value: props_buf.append('self.%s.SetValue(%s)\n' %
                                   (obj.name, value))
        return init, props_buf, []

# end of class PythonCodeGenerator


class CppCodeGenerator:
    extra_headers = ['<wx/tglbtn.h>']
                                  
    def get_code(self, obj):
        """\
        generates C++ code for wxToggleButton objects.
        """
        cppgen = common.code_writers['C++']
        prop = obj.properties
        id_name, id = cppgen.generate_code_id(obj)
        if id_name: ids = [ id_name ]
        else: ids = []
        label = cppgen.quote_str(prop.get('label', ''))
        if not obj.parent.is_toplevel: parent = '%s' % obj.parent.name
        else: parent = 'this'
        init = [ '%s = new %s(%s, %s, %s);\n' % 
                 (obj.name, obj.klass, parent, id, label) ]
        props_buf = cppgen.generate_common_properties(obj)
        value = prop.get('value')
        if value: props_buf.append('%s->SetValue(%s);\n' % (obj.name, value))
        return init, ids, props_buf, []

# end of class CppCodeGenerator


def initialize():
    common.class_names['EditToggleButton'] = 'wxToggleButton'

    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('wxToggleButton', PythonCodeGenerator())
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxToggleButton', CppCodeGenerator())
