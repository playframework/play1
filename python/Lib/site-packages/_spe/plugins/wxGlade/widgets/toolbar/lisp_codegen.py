# lisp_codegen.py : lisp generator functions for wxMenuBar objects
# $Id: lisp_codegen.py,v 1.2 2005/09/25 08:23:40 efuzzyone Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY


import common
from tool import *

# yay
from codegen import ToolsHandler



class LispCodeGenerator:
    def get_properties_code(self, obj):
        prop = obj.properties
        plgen = common.code_writers['lisp']
        out = []
        append = out.append
        
        obj_name = '(slot-%s obj)' % obj.name
        
        bitmapsize = prop.get('bitmapsize')
        if bitmapsize:
            try:
                w, h = [int(i) for i in bitmapsize.split(',')]
                append('(wxToolBar_SetToolBitmapSize %s %s %s)\n' % \
                       (obj_name, w, h))
            except:
                pass

        margins = prop.get('margins')
        if margins:
            try:
                w, h = [int(i) for i in margins.split(',')]
                append('(wxToolBar_SetMargins %s %s %s)\n'
                       % (obj_name, w, h))
            except:
                pass

        packing = prop.get('packing')
        if packing:
            append('(wxToolBar_SetToolPacking %s %s)\n' % (obj_name, packing))

        separation = prop.get('separation')
        if separation:
            append('(wxToolBar_SetToolSeparation %s %s)\n' % (obj_name, separation))
        append('(wxToolBar_Realize %s)\n' % obj_name)

        return out


    def get_init_code(self, obj):
        prop = obj.properties
        plgen = common.code_writers['lisp']
        out = []
        append = out.append
        tools = obj.properties['toolbar']
        ids = []

        obj_name = '(slot-%s obj)' % obj.name

        def _get_bitmap(bitmap):
            if not bitmap:
                return 'wxNullBitmap'
            elif bitmap.startswith('var:'):
                # this is a variable holding bitmap path
                var = bitmap[4:].strip()
                if var[0] != "$":
                    var = "$" + var
                return '(wxBitmap:wxBitmap_CreateLoad %s wxBITMAP_TYPE_ANY)' % var
            elif bitmap.startswith('code:'):
                return '(%s)' % bitmap[5:].strip()
            else:
                return '(wxBitmap:wxBitmap_CreateLoad %s wxBITMAP_TYPE_ANY)' % \
                       plgen.quote_str(bitmap)

        for tool in tools:
            if tool.id == '---': # item is a separator
                append('(wxToolBar_AddSeparator %s)\n' % obj_name)
            else:
                name, val = plgen.generate_code_id(None, tool.id)
                if not name and (not val or val == '-1'):
                    id = 'Wx::NewId()'
                else:
                    if name: ids.append(name)
                    id = val
                kinds = ['wxITEM_NORMAL', 'wxITEM_CHECK', 'wxITEM_RADIO']
                try:
                    kind = kinds[int(tool.type)]
                except (IndexError, ValueError):
                    kind = 'wxITEM_NORMAL'
                bmp1 = _get_bitmap(tool.bitmap1)
                bmp2 = _get_bitmap(tool.bitmap2)
#                append('%s->AddLabelTool(%s, %s, %s, %s, %s, %s, %s);\n' %
                append('(wxToolBar_AddTool %s %s %s %s %s %s %s %s)\n' %
                       (obj_name, id, plgen.quote_str(tool.label),
                        bmp1, bmp2, kind, plgen.quote_str(tool.short_help),
                        plgen.quote_str(tool.long_help)))
        
        return ids + out


    def get_code(self, obj):
        """\
        function that generates Lisp code for the toolbar of a wxFrame.
        """
        plgen = common.code_writers['lisp']
        style = obj.properties.get('style')
        if style:
            style = style.strip().replace('|',' ')
            if style.find(' ') != -1:
                style = '(logior wxTB_HORIZONTAL %s)' % style
        else:
            style = 'wxTB_HORIZONTAL'

        if not obj.parent.is_toplevel:
            parent = '(slot-%s obj)' % obj.parent.name
        else:
            parent = '(slot-top-window obj)'

        init = [
            '\n\t;;; Tool Bar\n',
            '(setf (slot-%s obj) (wxToolBar_Create %s -1 -1 -1 -1 -1 %s))\n' % (obj.name, parent, style),
                 '(wxFrame_SetToolBar (slot-top-window obj) (slot-%s obj))\n' % obj.name 
            ]
        init.extend(self.get_init_code(obj))
        init.append(';;; Tool Bar end\n')
        return init, self.get_properties_code(obj), []

# end of class LispCodeGenerator

def initialize():
    common.class_names['EditToolBar'] = 'wxToolBar'
    common.toplevels['EditToolBar'] = 1

    plgen = common.code_writers.get('lisp')

    if plgen:
        plgen.add_widget_handler('wxToolBar', LispCodeGenerator())
        plgen.add_property_handler('tools', ToolsHandler)
