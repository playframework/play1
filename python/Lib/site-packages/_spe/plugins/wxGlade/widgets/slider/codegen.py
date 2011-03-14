# codegen.py: code generator functions for wxSlider objects
# $Id: codegen.py,v 1.15 2007/03/27 07:01:54 agriggio Exp $
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
        value = prop.get('value', '0')
        try: min_v, max_v = [ s.strip() for s in prop['range'].split(',') ]
        except: min_v, max_v = '0', '10'
        if not obj.parent.is_toplevel: parent = 'self.%s' % obj.parent.name
        else: parent = 'self'
        style = prop.get("style")
        if style and style != 'wxSL_HORIZONTAL':
            style = ", style=%s" % pygen.cn_f(style)
        else:
            style = ''
        init = []
        if id_name: init.append(id_name)
        klass = obj.klass
        if klass == obj.base: klass = pygen.cn(klass)
        init.append('self.%s = %s(%s, %s, %s, %s, %s%s)\n' %
                    (obj.name, klass, parent, id, value, min_v,
                     max_v, style))
        props_buf = pygen.generate_common_properties(obj)
        return init, props_buf, []

# end of class PythonCodeGenerator


def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']
    class SliderXrcObject(xrcgen.DefaultXrcObject):
        def write_property(self, name, val, outfile, tabs):
            if name == 'range':
                try: min, max = val.split(',')
                except ValueError: pass
                else:
                    tab_s = '    '*tabs
                    outfile.write(tab_s + '<min>%s</min>\n' % min)
                    outfile.write(tab_s + '<max>%s</max>\n' % max)
            else:
                xrcgen.DefaultXrcObject.write_property(self, name, val,
                                                       outfile, tabs)

    # end of class SliderXrcObject

    return SliderXrcObject(obj)


class CppCodeGenerator:
    def get_code(self, obj):
        """\
        generates the C++ code for wxSlider objects
        """
        cppgen = common.code_writers['C++']
        prop = obj.properties
        id_name, id = cppgen.generate_code_id(obj)
        if id_name: ids = [ id_name ]
        else: ids = []
        value = prop.get('value', '0')
        try: min_v, max_v = [ s.strip() for s in prop['range'].split(',') ]
        except: min_v, max_v = '0', '10'
        if not obj.parent.is_toplevel: parent = '%s' % obj.parent.name
        else: parent = 'this'
        extra = ''
        style = prop.get("style")
        if style and style != 'wxSL_HORIZONTAL':
            extra = ', wxDefaultPosition, wxDefaultSize, %s' % style
        init = ['%s = new %s(%s, %s, %s, %s, %s%s);\n' %
                (obj.name, obj.klass, parent, id, value, min_v, max_v, extra)]
        props_buf = cppgen.generate_common_properties(obj)
        return init, ids, props_buf, []

    def get_events(self, obj):
        cppgen = common.code_writers['C++']
        return cppgen.get_events_with_type(obj, 'wxScrollEvent')

# end of class CppCodeGenerator


def initialize():
    common.class_names['EditSlider'] = 'wxSlider'

    pygen = common.code_writers.get("python")
    if pygen:
        pygen.add_widget_handler('wxSlider', PythonCodeGenerator())
    xrcgen = common.code_writers.get("XRC")
    if xrcgen:
        xrcgen.add_widget_handler('wxSlider', xrc_code_generator)
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxSlider', CppCodeGenerator())
