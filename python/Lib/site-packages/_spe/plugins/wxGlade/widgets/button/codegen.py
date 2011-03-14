# codegen.py: code generator functions for wxButton objects
# $Id: codegen.py,v 1.18 2007/04/01 12:42:16 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common

class PythonCodeGenerator:
    def get_code(self, obj):
        pygen = common.code_writers['python']
        cn = pygen.cn
        prop = obj.properties
        id_name, id = pygen.generate_code_id(obj)

        stockitem = prop.get('stockitem', 'None')
        if stockitem != 'None':
            id = pygen.cn("wxID_" + stockitem)
            label = pygen.quote_str('')
        else:
            label = pygen.quote_str(prop.get('label', ''))

        if not obj.parent.is_toplevel: parent = 'self.%s' % obj.parent.name
        else: parent = 'self'
        style = prop.get("style")
        if style: style = ", style=%s" % pygen.cn_f(style)
        else: style = ''
        init = []
        if id_name: init.append(id_name)
        klass = obj.klass
        if klass == obj.base: klass = cn(klass)
        init.append('self.%s = %s(%s, %s, %s%s)\n' %
                    (obj.name, klass, parent, id, label, style))
        props_buf = pygen.generate_common_properties(obj)
        if prop.get('default', False):
            props_buf.append('self.%s.SetDefault()\n' % obj.name)
        return init, props_buf, []

# end of class PythonCodeGenerator


def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']
    class ButtonXrcObject(xrcgen.DefaultXrcObject):
        def write(self, out_file, ntabs):
            stockitem = self.properties.get('stockitem', 'None')
            if stockitem != 'None':
                self.name = 'wxID_' + stockitem
                del self.properties['stockitem']
                try: del self.properties['label']
                except KeyError: pass
            xrcgen.DefaultXrcObject.write(self, out_file, ntabs)
            
        def write_property(self, name, val, outfile, tabs):
            if name == 'label':
                # translate & into _ as accelerator marker
                val2 = val.replace('&', '_')
                if val.count('&&') > 0:
                    while True:
                        index = val.find('&&')
                        if index < 0: break
                        val = val2[:index] + '&&' + val2[index+2:]
                else: val = val2
            xrcgen.DefaultXrcObject.write_property(self, name, val,
                                                   outfile, tabs)
    # end of class ButtonXrcObject

    return ButtonXrcObject(obj)


class CppCodeGenerator:
    def get_code(self, obj):
        """\
        fuction that generates python code for wxButton objects.
        """
        cppgen = common.code_writers['C++']
        prop = obj.properties
        id_name, id = cppgen.generate_code_id(obj)
        if id_name: ids = [ id_name ]
        else: ids = []
        if not obj.parent.is_toplevel: parent = '%s' % obj.parent.name
        else: parent = 'this'
        extra = ''
        style = prop.get("style")
        if style: extra = ', wxDefaultPosition, wxDefaultSize, %s' % style

        stockitem = prop.get('stockitem', 'None')
        if stockitem != 'None':
            label = cppgen.quote_str('')
            id = "wxID_" + stockitem
        else:
            label = cppgen.quote_str(prop.get('label', ''))

        init = [ '%s = new %s(%s, %s, %s%s);\n' % 
                 (obj.name, obj.klass, parent, id, label, extra) ]
        props_buf = cppgen.generate_common_properties(obj)
        if prop.get('default', False):
            props_buf.append('%s->SetDefault();\n' % obj.name)
        return init, ids, props_buf, []

# end of class CppCodeGenerator


def initialize():
    common.class_names['EditButton'] = 'wxButton'
    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('wxButton', PythonCodeGenerator())
    xrcgen = common.code_writers.get("XRC")
    if xrcgen:
        xrcgen.add_widget_handler('wxButton', xrc_code_generator)
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxButton', CppCodeGenerator())
