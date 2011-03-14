# codegen.py: code generator functions for wxRadioButton objects
# $Id: codegen.py,v 1.14 2007/03/27 07:01:54 agriggio Exp $
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
        style = prop.get("style")
        if style: style = ", style=%s" % pygen.cn_f(style)
        else: style = ''
        init = []
        if id_name: init.append(id_name)
        klass = obj.klass
        if klass == obj.base: klass = pygen.cn(klass)
        init.append('self.%s = %s(%s, %s, %s%s)\n' %
                    (obj.name, klass, parent, id, label, style))
        props_buf = pygen.generate_common_properties(obj)
        clicked = prop.get('clicked')
        if clicked: props_buf.append('self.%s.SetValue(1)\n' % obj.name)
        return init, props_buf, []

# end of class PythonCodeGenerator


class CppCodeGenerator:
    def get_code(self, obj):
        """\
        generates the C++ code for wxRadioButton objects
        """
        cppgen = common.code_writers['C++']
        prop = obj.properties
        id_name, id = cppgen.generate_code_id(obj)
        if id_name: ids = [ id_name ]
        else: ids = []
        label = cppgen.quote_str(prop.get('label', ''))
        if not obj.parent.is_toplevel: parent = '%s' % obj.parent.name
        else: parent = 'this'
        extra = ''
        style = prop.get("style")
        if style: extra = ', wxDefaultPosition, wxDefaultSize, %s' % style
        init = ['%s = new %s(%s, %s, %s%s);\n' %
                (obj.name, obj.klass, parent, id, label, extra) ]
        props_buf = cppgen.generate_common_properties(obj)
        clicked = prop.get('clicked')
        if clicked: props_buf.append('%s->SetValue(1);\n' % obj.name)
        return init, ids, props_buf, []

# end of class CppCodeGenerator


def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']

    class XrcCodeGenerator(xrcgen.DefaultXrcObject):
        def write(self, *args, **kwds):
            try:
                self.properties['value'] = self.properties['clicked']
                del self.properties['clicked']
            except KeyError:
                pass
            xrcgen.DefaultXrcObject.write(self, *args, **kwds)

    return XrcCodeGenerator(obj)


def initialize():
    common.class_names['EditRadioButton'] = 'wxRadioButton'

    # python code generation functions
    pygen = common.code_writers.get("python")
    if pygen:
        pygen.add_widget_handler('wxRadioButton', PythonCodeGenerator())
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxRadioButton', CppCodeGenerator())
    xrcgen = common.code_writers.get('XRC')
    if xrcgen:
        xrcgen.add_widget_handler('wxRadioButton', xrc_code_generator)
