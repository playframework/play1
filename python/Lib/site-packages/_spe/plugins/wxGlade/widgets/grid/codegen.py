# codegen.py: code generator functions for wxGrid objects
# $Id: codegen.py,v 1.25 2007/03/27 07:01:58 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common

class ColsCodeHandler:
    def __init__(self):
        self.columns = []
        self.col_name = ''
        self.col_size = ''

    def start_elem(self, name, attrs):
        if name == 'column':
            s = attrs.get('size', '')
            self.col_size = s
            self.col_name = ''
    
    def end_elem(self, name, code_obj):
        if name == 'columns':
            code_obj.properties['columns'] = self.columns
            return True
        elif name == 'column':
            self.columns.append([self.col_name, self.col_size])

        return False

    def char_data(self, data):
        self.col_name = self.col_name + data

# end of class ColsCodeHandler


def _check_label(label, col):
    """\
    Checks if 'label' is not the default one for the columns 'col': returns
    True if the label is a custom one, False otherwise
    """
    # build the default value
    s = []
    while True:
        s.append(chr(ord('A') + col % 26))
        col = col/26 - 1
        if col < 0: break
    s.reverse()
    # then compare it with label
    return label != "".join(s)
    

class PythonCodeGenerator(object):
    def __init__(self):
        self.pygen = common.code_writers['python']
        
    def __get_import_modules(self):
        if self.pygen.use_new_namespace:
            return ['import wx.grid\n']
        else:
            return ['from wxPython.grid import *\n']
    import_modules = property(__get_import_modules)

    def cn(self, c):
        #print 'PythonCodeGenerator.cn with arg:', c
        if self.pygen.use_new_namespace:
            if c[:2] == 'wx':
                c = c[2:]
            return 'wx.grid.' + c
        else:
            return c

    def get_code(self, obj):
        pygen = common.code_writers['python']
        prop = obj.properties
        id_name, id = pygen.generate_code_id(obj)
        if not obj.parent.is_toplevel: parent = 'self.%s' % obj.parent.name
        else: parent = 'self'
        init = []
        if id_name: init.append(id_name)
        klass = obj.klass
        if klass == obj.base: klass = self.cn(klass)
        init.append('self.%s = %s(%s, %s, size=(1, 1))\n' %
                    (obj.name, klass, parent, id))
        props_buf = self.get_properties_code(obj)
        return init, props_buf, []

    def get_properties_code(self, obj):
        pygen = common.code_writers['python']
        out = []
        name = 'self'
        if not obj.is_toplevel: name += '.%s' % obj.name
        prop = obj.properties

        try: create_grid = int(prop['create_grid'])
        except (KeyError, ValueError): create_grid = False
        if not create_grid: return []

        columns = prop.get('columns', [['A', '-1']])
        out.append('%s.CreateGrid(%s, %s)\n' %
                   (name, prop.get('rows_number', '1'), len(columns)))
        if prop.get('row_label_size'):
            out.append('%s.SetRowLabelSize(%s)\n' %
                       (name, prop['row_label_size']))
        if prop.get('col_label_size'):
            out.append('%s.SetColLabelSize(%s)\n' %
                       (name, prop['col_label_size']))
        enable_editing = prop.get('enable_editing', '1')
        if enable_editing != '1':
            out.append('%s.EnableEditing(0)\n' % name)
        enable_grid_lines = prop.get('enable_grid_lines', '1')
        if enable_grid_lines != '1':
            out.append('%s.EnableGridLines(0)\n' % name)
        enable_col_resize = prop.get('enable_col_resize', '1')
        if enable_col_resize != '1':
            out.append('%s.EnableDragColSize(0)\n' % name)
        enable_row_resize = prop.get('enable_row_resize', '1')
        if enable_row_resize != '1':
            out.append('%s.EnableDragRowSize(0)\n' % name)
        enable_grid_resize = prop.get('enable_grid_resize', '1')
        if enable_grid_resize != '1':
            out.append('%s.EnableDragGridSize(0)\n' % name)
        if prop.get('lines_color', False):
            out.append(('%s.SetGridLineColour(' + pygen.cn('wxColour') +
                        '(%s))\n') %
                       (name, pygen._string_to_colour(prop['lines_color'])))
        if prop.get('label_bg_color', False):
            out.append(('%s.SetLabelBackgroundColour(' + pygen.cn('wxColour') +
                        '(%s))\n') %
                       (name, pygen._string_to_colour(prop['label_bg_color'])))
        sel_mode = prop.get('selection_mode')
        if sel_mode and sel_mode != 'wxGrid.wxGridSelectCells':
            out.append('%s.SetSelectionMode(%s)\n' % \
                       (name, self.cn('wxGrid') + sel_mode[6:]))

        i = 0
        for label, size in columns:
            if _check_label(label, i):
                out.append('%s.SetColLabelValue(%s, %s)\n' % \
                           (name, i, pygen.quote_str(label)))
            try:
                if int(size) > 0:
                    out.append('%s.SetColSize(%s, %s)\n' % \
                               (name, i, size))
            except ValueError: pass
            i += 1

        out.extend(pygen.generate_common_properties(obj))
        return out

# end of class PythonCodeGenerator


class CppCodeGenerator:
    extra_headers = ['<wx/grid.h>']
    
    def get_code(self, obj):
        """\
        generates C++ code for wxGrid objects.
        """
        cppgen = common.code_writers['C++']
        prop = obj.properties
        id_name, id = cppgen.generate_code_id(obj)
        if id_name: ids = [ id_name ]
        else: ids = []
        if not obj.parent.is_toplevel: parent = '%s' % obj.parent.name
        else: parent = 'this'
        init = [ '%s = new %s(%s, %s);\n' % (obj.name, obj.klass, parent, id) ]
        props_buf = self.get_properties_code(obj)
        return init, ids, props_buf, []

    def get_properties_code(self, obj):
        cppgen = common.code_writers['C++']
        out = []
        name = 'this'
        if not obj.is_toplevel: name = obj.name
        prop = obj.properties

        try: create_grid = int(prop['create_grid'])
        except (KeyError, ValueError): create_grid = False
        if not create_grid: return []

        columns = prop.get('columns', [['A', '-1']])
        out.append('%s->CreateGrid(%s, %s);\n' % (name,
                                                  prop.get('rows_number', '1'),
                                                  len(columns)))
        if prop.get('row_label_size'):
            out.append('%s->SetRowLabelSize(%s);\n' % \
                       (name, prop['row_label_size']))
        if prop.get('col_label_size'):
            out.append('%s->SetColLabelSize(%s);\n' % \
                       (name, prop['col_label_size']))
        enable_editing = prop.get('enable_editing', '1')
        if enable_editing != '1':
            out.append('%s->EnableEditing(false);\n' % name)
        enable_grid_lines = prop.get('enable_grid_lines', '1')
        if enable_grid_lines != '1':
            out.append('%s->EnableGridLines(false);\n' % name)
        enable_col_resize = prop.get('enable_col_resize', '1')
        if enable_col_resize != '1':
            out.append('%s->EnableDragColSize(false);\n' % name)
        enable_row_resize = prop.get('enable_row_resize', '1')
        if enable_row_resize != '1':
            out.append('%s->EnableDragRowSize(false);\n' % name)
        enable_grid_resize = prop.get('enable_grid_resize', '1')
        if enable_grid_resize != '1':
            out.append('%s->EnableDragGridSize(false);\n' % name)
        if prop.get('lines_color', False):
            out.append('%s->SetGridLineColour(wxColour(%s));\n' %
                       (name, cppgen._string_to_colour(prop['lines_color'])))
        if prop.get('label_bg_color', False):
            out.append('%s->SetLabelBackgroundColour(wxColour(%s));\n' %
                       (name,
                        cppgen._string_to_colour(prop['label_bg_color'])))
        sel_mode = prop.get('selection_mode', '').replace('.', '::')
        if sel_mode and sel_mode != 'wxGrid::wxGridSelectCells':
            out.append('%s->SetSelectionMode(%s);\n' % (name, sel_mode))

        i = 0
        for label, size in columns:
            if _check_label(label, i):
                out.append('%s->SetColLabelValue(%s, %s);\n' % \
                           (name, i, cppgen.quote_str(label)))
            try:
                if int(size) > 0:
                    out.append('%s->SetColSize(%s, %s);\n' % \
                               (name, i, size))
            except ValueError: pass
            i += 1

        out.extend(cppgen.generate_common_properties(obj))
        return out

    def get_events(self, obj):
        cppgen = common.code_writers['C++']
        return cppgen.get_events_with_type(obj, 'wxGridEvent')

# end of class CppCodeGenerator


def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']
    class GridXrcObject(xrcgen.DefaultXrcObject):
        unsupported = set(['column', 'create_grid', 'rows_number',
                           'row_label_size', 'col_label_size',
                           'enable_editing', 'enable_grid_lines',
                           'enable_col_resize', 'enable_row_resize',
                           'enable_grid_resize', 'lines_color',
                           'label_bg_color', 'selection_mode'])
        
        def write_property(self, name, val, outfile, tabs):
            if name not in self.unsupported:
                xrcgen.DefaultXrcObject.write_property(self, name, val,
                                                       outfile, tabs)
    return GridXrcObject(obj)


def initialize():
    common.class_names['EditGrid'] = 'wxGrid'
    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_property_handler('columns', ColsCodeHandler, 'wxGrid')
        pygen.add_widget_handler('wxGrid', PythonCodeGenerator())
    xrcgen = common.code_writers.get("XRC")
    if xrcgen:
        xrcgen.add_widget_handler('wxGrid', xrc_code_generator)
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_property_handler('columns', ColsCodeHandler, 'wxGrid')
        cppgen.add_widget_handler('wxGrid', CppCodeGenerator())
