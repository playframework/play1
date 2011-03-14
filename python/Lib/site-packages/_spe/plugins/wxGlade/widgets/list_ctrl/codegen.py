# codegen.py: code generator functions for wxListCtrl objects
# $Id: codegen.py,v 1.8 2007/03/27 07:01:57 agriggio Exp $
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
        if not obj.parent.is_toplevel: parent = 'self.%s' % obj.parent.name
        else: parent = 'self'
        style = prop.get("style")
        if style and style != 'wxLC_ICON': # default style
            style = ", style=%s" % pygen.cn_f(style)
        else: style = ''
        init = []
        if id_name: init.append(id_name)
        klass = obj.klass
        if klass == obj.base: klass = pygen.cn(klass)
        init.append('self.%s = %s(%s, %s%s)\n' %
                    (obj.name, klass, parent, id, style))
        props_buf = pygen.generate_common_properties(obj)
        return init, props_buf, []

# end of class PythonCodeGenerator


class CppCodeGenerator:
    extra_headers = ['<wx/listctrl.h>']

    def get_code(self, obj):
        """\
        generates C++ code for wxListCtrl objects.
        """
        cppgen = common.code_writers['C++']
        prop = obj.properties
        id_name, id = cppgen.generate_code_id(obj)
        if id_name: ids = [ id_name ]
        else: ids = []
        if not obj.parent.is_toplevel: parent = '%s' % obj.parent.name
        else: parent = 'this'
        extra = ''
        style = prop.get('style')
        if style and style != 'wxLC_ICON':
            extra = ', wxDefaultPosition, wxDefaultSize, %s' % style
        init = ['%s = new %s(%s, %s%s);\n' %
                (obj.name, obj.klass, parent, id, extra)]
        props_buf = cppgen.generate_common_properties(obj)
        return init, ids, props_buf, []

    def get_events(self, obj):
        cppgen = common.code_writers['C++']
        return cppgen.get_events_with_type(obj, 'wxListEvent')
        
# end of class CppCodeGenerator


def initialize():
    common.class_names['EditListCtrl'] = 'wxListCtrl'
    
    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('wxListCtrl', PythonCodeGenerator())
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxListCtrl', CppCodeGenerator())
