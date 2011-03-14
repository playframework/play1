# codegen.py: code generator functions for wxDatePickerCtrl objects
# $Header: /home/alb/tmp/wxglade_cvs_backup/wxGlade/widgets/datepicker_ctrl/codegen.py,v 1.2 2007/03/27 07:02:01 agriggio Exp $

# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common

class PythonCodeGenerator:
    #def __init__(self):
        #self.pygen = common.code_writers['python']
     
    #def __get_import_modules(self):
        #if self.pygen.use_new_namespace:
            #return ['import wx.calendar\n']
        #else:
            #return ['from wxPython.calendar import *\n']
    #import_modules = property(__get_import_modules)

    #def cn(self, c):
        #""" Create names according to if the new namescace (wx) was selected
        #@type c: string
        #@param c: the name which should be altered
        #@rtype: string
        #@return: the orignial name with a prefix according to which namespace the user selected
        #"""
        #if self.pygen.use_new_namespace:
            #if c[:2] == 'wx':
                #c = c[2:]
            #return 'wx.calendar.' + c
        #else:
            #return c

    #def cn_f(self, flags):
        #""" Same as cn(c) but for flags
        #@rtype: string
        #"""
        #if self.pygen.use_new_namespace:
            #return "|".join([self.cn(f) for f in str(flags).split('|')])
        #else:
            #return str(flags)
    
    def get_code(self, obj):
        pygen = common.code_writers['python']
        prop = obj.properties
        id_name, id = pygen.generate_code_id(obj)
        #label = pygen.quote_str(prop.get('label', ''))
        if not obj.parent.is_toplevel: parent = 'self.%s' % obj.parent.name
        else: parent = 'self'
        style = prop.get("style")
        if style: style = ", style=%s" % pygen.cn_f(style)
        else: style = ''
        init = []
        if id_name: init.append(id_name)
        klass = obj.klass
        if klass == obj.base: klass = pygen.cn(klass)
        init.append('self.%s = %s(%s, %s%s)\n' %
        #            (obj.name, klass, parent, id, label, style))
                     (obj.name, klass,parent, id, style))
        props_buf = pygen.generate_common_properties(obj)
        if prop.get('default', False):
            props_buf.append('self.%s.SetDefault()\n' % obj.name)
        return init, props_buf, []

# end of class PythonCodeGenerator


def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']
    class DatePickerCtrlXrcObject(xrcgen.DefaultXrcObject):
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
    # end of class DatePickerCtrlXrcObject

    return DatePickerCtrlXrcObject(obj)


class CppCodeGenerator:
    extra_headers = ['<wx/datectrl.h>']
    
    def get_code(self, obj):
        """\
        fuction that generates python code for wxDatePickerCtrl objects.
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
        if style: extra = ', wxDefaultDateTime, wxDefaultPosition, wxDefaultSize, %s' % style
        #label = cppgen.quote_str(prop.get('label', ''))
        init = [ '%s = new %s(%s, %s%s);\n' % 
        #         (obj.name, obj.klass, parent, id, label, extra) ]
                  (obj.name, obj.klass, parent, id, extra) ]
        props_buf = cppgen.generate_common_properties(obj)
        if prop.get('default', False):
            props_buf.append('%s->SetDefault();\n' % obj.name)
        return init, ids, props_buf, []

# end of class CppCodeGenerator


def initialize():
    common.class_names['EditDatePickerCtrl'] = 'wxDatePickerCtrl'
    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('wxDatePickerCtrl', PythonCodeGenerator())
    xrcgen = common.code_writers.get("XRC")
    if xrcgen:
        xrcgen.add_widget_handler('wxDatePickerCtrl', xrc_code_generator)
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxDatePickerCtrl', CppCodeGenerator())
