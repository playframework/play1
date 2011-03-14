# codegen.py: code generator functions for wxStaticLine objects
# $Id: codegen.py,v 1.14 2007/03/27 07:01:52 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common


class PythonCodeGenerator:
    def get_code(self, obj):
        pygen = common.code_writers['python']
        prop = obj.properties

        attribute = pygen.test_attribute(obj)

        id_name, id = pygen.generate_code_id(obj)
        if not obj.parent.is_toplevel: parent = 'self.%s' % obj.parent.name
        else: parent = 'self'
        style = prop.get("style")
        if style and style != 'wxLI_HORIZONTAL':
            style = ", style=%s" % pygen.cn_f(style)
        else:
            style = ''
        init = []
        if id_name: init.append(id_name)
        if attribute: prefix = 'self.'
        else: prefix = ''
        klass = obj.klass
        if klass == obj.base: klass = pygen.cn(klass)
        init.append('%s%s = %s(%s, %s%s)\n' %
                    (prefix, obj.name, klass, parent, id, style))
        props_buf = pygen.generate_common_properties(obj)
        if not attribute:
            return [], [], init + props_buf
        return init, props_buf, []

# end of class PythonCodeGenerator


class CppCodeGenerator:
    extra_headers = ['<wx/statline.h>']
    
    def get_code(self, obj):
        """\
        generates the C++ code for wxStaticLine objects
        """
        cppgen = common.code_writers['C++']
        prop = obj.properties

        attribute = cppgen.test_attribute(obj)

        id_name, id = cppgen.generate_code_id(obj)
        if id_name: ids = [ id_name ]
        else: ids = []
        if not obj.parent.is_toplevel: parent = '%s' % obj.parent.name
        else: parent = 'this'
        extra = ''
        style = prop.get("style")
        if style and style != 'wxLI_HORIZONTAL':
            extra = ', wxDefaultPosition, wxDefaultSize, %s' % style
        if attribute: prefix = ''
        else: prefix = '%s* ' % obj.klass
        init = ['%s%s = new %s(%s, %s%s);\n' %
                (prefix, obj.name, obj.klass, parent, id, extra) ]
        if id_name: init.append(id_name)
        props_buf = cppgen.generate_common_properties(obj)
        if not attribute:
            return [], ids, [], init + props_buf
        return init, ids, props_buf, []

# end of class CppCodeGenerator


def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']

    class XrcCodeGenerator(xrcgen.DefaultXrcObject):
        def write(self, *args, **kwds):
            try: del self.properties['attribute']
            except KeyError: pass
            xrcgen.DefaultXrcObject.write(self, *args, **kwds)

    return XrcCodeGenerator(obj)


def initialize():
    common.class_names['EditStaticLine'] = 'wxStaticLine'

    pygen = common.code_writers.get("python")
    if pygen:
        pygen.add_widget_handler('wxStaticLine', PythonCodeGenerator())
    cppgen = common.code_writers.get("C++")
    if cppgen:
        cppgen.add_widget_handler('wxStaticLine', CppCodeGenerator())
    xrcgen = common.code_writers.get('XRC')
    if xrcgen:
        xrcgen.add_widget_handler('wxStaticLine', xrc_code_generator)
