# codegen.py: code generator functions for wxSplitterWindow objects
# $Id: codegen.py,v 1.18 2007/08/07 12:15:21 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common


class PythonCodeGenerator:
    def get_code(self, window):
        pygen = common.code_writers['python']
        prop = window.properties
        id_name, id = pygen.generate_code_id(window)
        if not window.parent.is_toplevel:
            parent = 'self.%s' % window.parent.name
        else: parent = 'self'
        if window.is_toplevel:
            l = []
            if id_name: l.append(id_name)
            l.append('self.%s = %s(%s, %s)\n' %
                     (window.name, pygen.without_package(window.klass),
                      parent,id))
            return l, [], []
        style = prop.get("style")
        if style and style != 'wxSP_3D':
            style = ", style=%s" % pygen.cn_f(style)
        else:
            style = ''
        init = []
        if id_name: init.append(id_name)
        klass = window.klass
        if window.preview: klass = 'wxSplitterWindow'
        init.append(('self.%s = ' + pygen.cn(klass) +
                     '(%s, %s%s)\n') % (window.name, parent, id, style))

        props_buf = pygen.generate_common_properties(window)
        layout_buf = []
        win_1 = prop.get('window_1')
        win_2 = prop.get('window_2')
        orientation = prop.get('orientation', 'wxSPLIT_VERTICAL')
        if win_1 and win_2:
            sash_pos = prop.get('sash_pos', '')
            if sash_pos: sash_pos = ', %s' % sash_pos
            if orientation == 'wxSPLIT_VERTICAL': f_name = 'SplitVertically'
            else: f_name = 'SplitHorizontally'
            layout_buf.append('self.%s.%s(self.%s, self.%s%s)\n' % \
                             (window.name, f_name, win_1, win_2, sash_pos))
        else:
            def add_sub(win):
                layout_buf.append('self.%s.SetSplitMode(%s)\n' % \
                                 (window.name, pygen.cn(orientation)))
                layout_buf.append('self.%s.Initialize(self.%s)\n' % \
                                 (window.name, win))
            if win_1: add_sub(win_1)
            elif win_2: add_sub(win_2)

        return init, props_buf, layout_buf

    def get_layout_code(self, obj):
        prop = obj.properties
        pygen = common.code_writers['python']
        win_1 = prop.get('window_1')
        win_2 = prop.get('window_2')
        orientation = prop.get('orientation', 'wxSPLIT_VERTICAL')
        props_buf = []
        if win_1 and win_2:
            sash_pos = prop.get('sash_pos', '')
            if sash_pos: sash_pos = ', %s' % sash_pos
            if orientation == 'wxSPLIT_VERTICAL': f_name = 'SplitVertically'
            else: f_name = 'SplitHorizontally'
            props_buf.append('self.%s(self.%s, self.%s%s)\n' %
                             (f_name, win_1, win_2, sash_pos))
        else:
            def add_sub(win):
                props_buf.append('self.SetSplitMode(%s)\n' %
                                 pygen.cn(orientation))
                props_buf.append('self.Initialize(self.%s)\n' % win)
            if win_1: add_sub(win_1)
            elif win_2: add_sub(win_2)
        return props_buf    

# end of class PythonCodeGenerator


class CppCodeGenerator:
    constructor = [('wxWindow*', 'parent'), ('int', 'id'),
                   ('const wxPoint&', 'pos', 'wxDefaultPosition'),
                   ('const wxSize&', 'size', 'wxDefaultSize'),
                   ('long', 'style', 'wxSP_3D')]

    extra_headers = ['<wx/splitter.h>']

    def get_code(self, window):
        """\
        generates the C++ code for wxSplitterWindow
        """
        cppgen = common.code_writers['C++']
        prop = window.properties
        id_name, id = cppgen.generate_code_id(window)
        if id_name: ids = [ id_name ]
        else: ids = []
        if not window.parent.is_toplevel: parent = '%s' % window.parent.name
        else: parent = 'this'
        if window.is_toplevel:
            l = ['%s = new %s(%s, %s);\n' %
                 (window.name, window.klass, parent, id)]
            return l, ids, [], []
        extra = ''
        style = prop.get("style")
        if style and style != 'wxSP_3D':
            extra = ', wxDefaultPosition, wxDefaultSize, %s' % style
        init = ['%s = new %s(%s, %s%s);\n' %
                (window.name, window.klass, parent, id, extra) ]

        props_buf = cppgen.generate_common_properties(window)
        layout_buf = []
        win_1 = prop.get('window_1')
        win_2 = prop.get('window_2')
        orientation = prop.get('orientation', 'wxSPLIT_VERTICAL')
        if win_1 and win_2:
            sash_pos = prop.get('sash_pos', '')
            if sash_pos: sash_pos = ', %s' % sash_pos
            if orientation == 'wxSPLIT_VERTICAL': f_name = 'SplitVertically'
            else: f_name = 'SplitHorizontally'
            layout_buf.append('%s->%s(%s, %s%s);\n' % \
                              (window.name, f_name, win_1, win_2, sash_pos))
        else:
            def add_sub(win):
                layout_buf.append('%s->SetSplitMode(%s);\n' % (window.name,
                                                               orientation))
                layout_buf.append('%s->Initialize(%s);\n' % (window.name, win))
            if win_1: add_sub(win_1)
            elif win_2: add_sub(win_2)

        return init, ids, props_buf, layout_buf

    def get_layout_code(self, obj):
        prop = obj.properties
        cppgen = common.code_writers['C++']
        win_1 = prop.get('window_1')
        win_2 = prop.get('window_2')
        orientation = prop.get('orientation', 'wxSPLIT_VERTICAL')
        props_buf = []
        if win_1 and win_2:
            sash_pos = prop.get('sash_pos', '')
            if sash_pos: sash_pos = ', %s' % sash_pos
            if orientation == 'wxSPLIT_VERTICAL': f_name = 'SplitVertically'
            else: f_name = 'SplitHorizontally'
            props_buf.append('%s(%s, %s%s);\n' % \
                             (f_name, win_1, win_2, sash_pos))
        else:
            def add_sub(win):
                props_buf.append('SetSplitMode(%s);\n' % orientation)
                props_buf.append('Initialize(%s);\n' % win)
            if win_1: add_sub(win_1)
            elif win_2: add_sub(win_2)
        return props_buf

    def get_events(self, obj):
        cppgen = common.code_writers['C++']
        return cppgen.get_events_with_type(obj, 'wxSplitterEvent')

# end of class CppCodeGenerator


def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']

    class XrcCodeGenerator(xrcgen.DefaultXrcObject):
        props_map = {
            'sash_pos': 'sashpos',
            'window_1': '',
            'window_2': '',
            }
        orient_map = {
            'wxSPLIT_VERTICAL': 'vertical',
            'wxSPLIT_HORIZONTAL': 'horizontal',
            }
        def write_property(self, name, val, outfile, ntabs):
            try:
                prop = self.props_map.get(name, name)
                if not prop: return
                if prop == 'orientation':
                    val = self.orient_map[val]
                xrcgen.DefaultXrcObject.write_property(
                    self, prop, val, outfile, ntabs)
            except KeyError:
                return

        def write(self, *args, **kwds):
            if 'no_custom_class' in self.properties:
                del self.properties['no_custom_class']            
            xrcgen.DefaultXrcObject.write(self, *args, **kwds)
            
    # end of class XrcCodeGenerator

    return XrcCodeGenerator(obj)


def initialize():
    common.class_names['EditSplitterWindow'] = 'wxSplitterWindow'
    common.class_names['SplitterPane'] = 'wxPanel'
    common.toplevels['EditSplitterWindow'] = 1
    common.toplevels['SplitterPane'] = 1

    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('wxSplitterWindow', PythonCodeGenerator())
    xrcgen = common.code_writers.get('XRC')
    if xrcgen:
        xrcgen.add_widget_handler('wxSplitterWindow', xrc_code_generator)#xrcgen.NotImplementedXrcObject)
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxSplitterWindow', CppCodeGenerator())
