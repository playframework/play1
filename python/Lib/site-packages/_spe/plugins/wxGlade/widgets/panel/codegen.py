# codegen.py: code generator functions for wxPanel objects
# $Id: codegen.py,v 1.19 2007/08/07 12:15:21 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common


class PythonCodeGenerator:
    def get_code(self, panel):
        pygen = common.code_writers['python']
        cn = pygen.cn
        cn_f = pygen.cn_f
        prop = panel.properties
        try: scrollable = int(prop['scrollable'])
        except: scrollable = False
        id_name, id = pygen.generate_code_id(panel)
        if not panel.parent.is_toplevel: parent = 'self.%s' % panel.parent.name
        else: parent = 'self'
        if panel.is_toplevel:
            l = []
            if id_name: l.append(id_name)
            l.append('self.%s = %s(%s, %s)\n' %
                     (panel.name, pygen.without_package(panel.klass),
                      parent, id))
            return l, [], []
        init = []
        if id_name: init.append(id_name)
        style = prop.get("style", 'wxTAB_TRAVERSAL')
        if scrollable or style != 'wxTAB_TRAVERSAL':
            style = ", style=%s" % cn_f(style)
        else: style = ''
        # ALB 2005-11-19
        if not int(panel.properties.get('no_custom_class', False)) \
               or panel.preview:
            if scrollable: klass = cn('wxScrolledWindow')
            else: klass = cn('wxPanel')
        else:
            klass = panel.klass
        init.append('self.%s = %s(%s, %s%s)\n' % \
                    (panel.name, klass, parent, id, style))
        props_buf = pygen.generate_common_properties(panel)
        if scrollable:
            sr = prop.get('scroll_rate', '0, 0')
            props_buf.append('self.%s.SetScrollRate(%s)\n' % (panel.name, sr))
        return init, props_buf, []

    def get_properties_code(self, obj):
        pygen = common.code_writers['python']
        prop = obj.properties
        try: scrollable = int(prop['scrollable'])
        except: scrollable = False
        props_buf = pygen.generate_common_properties(obj)
        if scrollable:
            sr = prop.get('scroll_rate', '0, 0')
            props_buf.append('self.SetScrollRate(%s)\n' % sr)
        return props_buf

# end of class PythonCodeGenerator


class CppCodeGenerator:
    constructor = [('wxWindow*', 'parent'), ('int', 'id'),
                   ('const wxPoint&', 'pos', 'wxDefaultPosition'),
                   ('const wxSize&', 'size', 'wxDefaultSize'),
                   ('long', 'style', '0')]

    def get_code(self, panel):
        """\
        generates the C++ code for wxPanel objects
        """
        cppgen = common.code_writers['C++']
        prop = panel.properties
        try: scrollable = int(prop['scrollable'])
        except: scrollable = False
        id_name, id = cppgen.generate_code_id(panel)
        if id_name: ids = [ id_name ]
        else: ids = []
        if not panel.parent.is_toplevel: parent = '%s' % panel.parent.name
        else: parent = 'this'
        if panel.is_toplevel:
            l = ['%s = new %s(%s, %s);\n' %
                 (panel.name, panel.klass, parent, id)]
            return l, ids, [], []
        extra = ''
        style = prop.get("style", 'wxTAB_TRAVERSAL')
        if scrollable or style != 'wxTAB_TRAVERSAL':
            extra = ', wxDefaultPosition, wxDefaultSize, %s' % style
        # ALB 2005-11-19
        if not int(panel.properties.get('no_custom_class', False)):
            if scrollable: klass = 'wxScrolledWindow'
            else: klass = 'wxPanel'
        else:
            klass = panel.klass
        init = ['%s = new %s(%s, %s%s);\n' %
                (panel.name, klass, parent, id, extra) ]
        props_buf = cppgen.generate_common_properties(panel)
        if scrollable:
            sr = prop.get('scroll_rate', '0, 0')
            props_buf.append('%s->SetScrollRate(%s);\n' % (panel.name, sr))
        return init, ids, props_buf, []

    def get_properties_code(self, obj):
        cppgen = common.code_writers['C++']
        prop = obj.properties
        try: scrollable = int(prop['scrollable'])
        except: scrollable = False
        props_buf = cppgen.generate_common_properties(obj)
        if scrollable:
            sr = prop.get('scroll_rate', '0, 0')
            props_buf.append('SetScrollRate(%s);\n' % sr)
        return props_buf

# end of class CppCodeGenerator


def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']

    class XrcCodeGenerator(xrcgen.DefaultXrcObject):
        def write(self, *args, **kwds):
            if 'scrollable' in self.properties:
                style = self.properties.get('style', '').split('|')
                try: style.remove('wxTAB_TRAVERSAL')
                except ValueError: pass
                self.properties['style'] = '|'.join(style)
            for prop in ('scrollable', 'scroll_rate'):
                try: del self.properties[prop]
                except KeyError: pass
            if 'no_custom_class' in self.properties:
                del self.properties['no_custom_class']
            xrcgen.DefaultXrcObject.write(self, *args, **kwds)

    return XrcCodeGenerator(obj)


def initialize():
    common.class_names['EditPanel'] = 'wxPanel'
    common.class_names['EditTopLevelPanel'] = 'wxPanel'
    common.toplevels['EditPanel'] = 1
    common.toplevels['EditTopLevelPanel'] = 1

    common.class_names['EditScrolledWindow'] = 'wxScrolledWindow'
    common.class_names['EditTopLevelScrolledWindow'] = 'wxScrolledWindow'
    common.toplevels['EditScrolledWindow'] = 1
    common.toplevels['EditTopLevelScrolledWindow'] = 1

    # python code generation functions
    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('wxPanel', PythonCodeGenerator())
        pygen.add_widget_handler('wxScrolledWindow', PythonCodeGenerator())
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxPanel', CppCodeGenerator())
        cppgen.add_widget_handler('wxScrolledWindow', CppCodeGenerator())
    xrcgen = common.code_writers.get('XRC')
    if xrcgen:
        xrcgen.add_widget_handler('wxPanel', xrc_code_generator)
        xrcgen.add_widget_handler('wxScrolledWindow', xrc_code_generator)
