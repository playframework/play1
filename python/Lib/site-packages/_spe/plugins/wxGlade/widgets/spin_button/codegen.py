# codegen.py: code generator functions for wxSpinButton objects
# $Id: codegen.py,v 1.2 2004/12/13 18:45:13 agriggio Exp $
#
# Copyright (c) 2004 D.H. aka crazyinsomniac at users.sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY
# based on wxGlade/widgets/spin_ctrl/

import common


class PythonCodeGenerator:
    def get_code(self, obj):
        pygen = common.code_writers['python']
        prop = obj.properties
        id_name, id = pygen.generate_code_id(obj)
#        value = prop.get('value', '')
#        try: min_v, max_v = [ s.strip() for s in \
#                              prop.get('range', '0, 100').split(',') ]
#        except: min_v, max_v = '0', '100'

        if not obj.parent.is_toplevel: parent = 'self.%s' % obj.parent.name
        else: parent = 'self'
        style = prop.get("style")
        if style: style = ", style=%s" % pygen.cn_f(style)
        else: style = ''
        init = []
        if id_name: init.append(id_name)
        klass = obj.klass
        if klass == obj.base: klass = pygen.cn(klass)
        init.append('self.%s = %s(%s, %s %s)\n' %
                    (obj.name, klass, parent, id, style))
        props_buf = pygen.generate_common_properties(obj)
        return init, props_buf, []

# end of class PythonCodeGenerator


def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']
    class SpinButtonXrcObject(xrcgen.DefaultXrcObject):
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

    # end of class SpinButtonXrcObject
    
    return SpinButtonXrcObject(obj)


class CppCodeGenerator:
    extra_headers = ['<wx/spinbutt.h>']
    
    def get_code(self, obj):
        """\
        generates C++ code for wxSpinButton objects.
        """
        cppgen = common.code_writers['C++']
        prop = obj.properties
        id_name, id = cppgen.generate_code_id(obj)
        if id_name: ids = [ id_name ]
        else: ids = []
#        value = prop.get('value', '')
#        try: min_v, max_v = [ s.strip() for s in \
#                              prop.get('range', '0, 100').split(',') ]
#        except: min_v, max_v = '0', '100'

        if not obj.parent.is_toplevel: parent = '%s' % obj.parent.name
        else: parent = 'this'
        style = prop.get('style')
        if not style: style = 'wxSP_ARROW_KEYS'
        init = ['%s = new %s(%s, %s, wxDefaultPosition, wxDefaultSize,'
                ' %s);\n' %
                (obj.name, obj.klass, parent, id, style)]
        props_buf = cppgen.generate_common_properties(obj)
        return init, ids, props_buf, []

    def get_events(self, obj):
        cppgen = common.code_writers['C++']
        return cppgen.get_events_with_type(obj, 'wxSpinEvent')

# end of class CppCodeGenerator


def initialize():
    common.class_names['EditSpinButton'] = 'wxSpinButton'
    
    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('wxSpinButton', PythonCodeGenerator())
    xrcgen = common.code_writers.get("XRC")
    if xrcgen:
        xrcgen.add_widget_handler('wxSpinButton', xrc_code_generator)
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxSpinButton', CppCodeGenerator())
