# perl_codegen.py : perl generator functions for wxGrid objects
# $Id: perl_codegen.py,v 1.7 2005/08/15 07:38:56 crazyinsomniac Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common
from codegen import ColsCodeHandler, _check_label

class PerlCodeGenerator:
    import_modules = ['use Wx::Grid;\n']

    def get_code(self, obj):
        plgen = common.code_writers['perl']
        prop = obj.properties
        id_name, id = plgen.generate_code_id(obj)
        if not obj.parent.is_toplevel:
            parent = '$self->{%s}' % obj.parent.name
        else:
            parent = '$self'
        init = []
        if id_name: init.append(id_name)

        klass = obj.base;
        if klass != obj.klass : klass = obj.klass; 
        else: klass = klass.replace('wx','Wx::',1);

        init.append('$self->{%s} = %s->new(%s, %s);\n' %
                    (obj.name, klass, parent, id))
        props_buf = self.get_properties_code(obj)
        return init, props_buf, []

    def get_properties_code(self, obj):
        plgen = common.code_writers['perl']
        out = []
        name = '$self'
        if not obj.is_toplevel: name += '->{%s}' % obj.name
        prop = obj.properties

        try: create_grid = int(prop['create_grid'])
        except (KeyError, ValueError): create_grid = False
        if not create_grid: return []

        columns = prop.get('columns', [['A', '-1']])
        out.append('%s->CreateGrid(%s, %s);\n' %
                   (name, prop.get('rows_number', '1'), len(columns)))
        if prop.get('row_label_size'):
            out.append('%s->SetRowLabelSize(%s);\n' %
                       (name, prop['row_label_size']))
        if prop.get('col_label_size'):
            out.append('%s->SetColLabelSize(%s);\n' %
                       (name, prop['col_label_size']))
        enable_editing = prop.get('enable_editing', '1')
        if enable_editing != '1':
            out.append('%s->EnableEditing(0);\n' % name)
        enable_grid_lines = prop.get('enable_grid_lines', '1')
        if enable_grid_lines != '1':
            out.append('%s->EnableGridLines(0);\n' % name)
        enable_col_resize = prop.get('enable_col_resize', '1')
        if enable_col_resize != '1':
            out.append('%s->EnableDragColSize(0);\n' % name)
        enable_row_resize = prop.get('enable_row_resize', '1')
        if enable_row_resize != '1':
            out.append('%s->EnableDragRowSize(0);\n' % name)
        enable_grid_resize = prop.get('enable_grid_resize', '1')
        if enable_grid_resize != '1':
            out.append('%s->EnableDragGridSize(0);\n' % name)
        if prop.get('lines_color', False):
            out.append('%s->SetGridLineColour(Wx::Colour->new(%s));\n' %
                       (name, plgen._string_to_colour(prop['lines_color'])))
        if prop.get('label_bg_color', False):
            out.append('%s->SetLabelBackgroundColour(Wx::Colour->new(%s));\n' %
                       (name, plgen._string_to_colour(prop['label_bg_color'])))
        sel_mode = prop.get('selection_mode')
        if sel_mode and sel_mode != 'wxGridSelectCells':
            out.append('%s->SetSelectionMode(%s);\n' %
                (name, sel_mode.replace('wxGrid.','')))

        i = 0
        for label, size in columns:
            if _check_label(label, i):
                out.append('%s->SetColLabelValue(%s, %s);\n' % \
                           (name, i, plgen.quote_str(label)))
            try:
                if int(size) > 0:
                    out.append('%s->SetColSize(%s, %s);\n' % \
                               (name, i, size))
            except ValueError: pass
            i += 1

        out.extend(plgen.generate_common_properties(obj))
        return out

# end of class PerlCodeGenerator

def initialize():
    common.class_names['EditGrid'] = 'wxGrid'

    plgen = common.code_writers.get('perl')
    if plgen:
        plgen.add_widget_handler('wxGrid', PerlCodeGenerator())
        plgen.add_property_handler('columns', ColsCodeHandler)
