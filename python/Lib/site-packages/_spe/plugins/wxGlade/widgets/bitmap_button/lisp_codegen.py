# lisp_codegen.py : lisp generator functions for wxBitmapButton objects
# $Id: lisp_codegen.py,v 1.1 2005/09/22 06:33:57 efuzzyone Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common, os

#this should be in common 
_bmp_str_types = {
    '.bmp' : 'wxBITMAP_TYPE_BMP',
    '.gif' : 'wxBITMAP_TYPE_GIF',
    '.xpm' : 'wxBITMAP_TYPE_XPM',
    '.jpg' : 'wxBITMAP_TYPE_JPEG',
    '.jpeg': 'wxBITMAP_TYPE_JPEG',
    '.png' : 'wxBITMAP_TYPE_PNG',
    '.pcx' : 'wxBITMAP_TYPE_PCX'
    }

class LispCodeGenerator:
    def get_code(self, obj):
        plgen = common.code_writers['lisp']
        prop = obj.properties
        id_name, id = plgen.generate_code_id(obj) 
        bmp_file = prop.get('bitmap', '')

        if not obj.parent.is_toplevel:
            parent = '(slot-%s obj)' % obj.parent.name
        else:
            parent = '(slot-top-window obj)'

        if not bmp_file:
            bmp = 'wxNullBitmap'
        elif bmp_file.startswith('var:'):
            # this is a variable holding pathname of bitmap
            var = bmp_file[4:].strip()
            if var[0] != "$":
                var = "$" + var
            bmp = '(wxBitmap_CreateLoad %s wxBITMAP_TYPE_ANY)' % var
        elif bmp_file.startswith('code:'): # this is a code chunk
            bmp = '(%s)' % bmp_file[5:].strip()
        else:
            bmp = '(wxBitmap_CreateLoad %s, wxBITMAP_TYPE_ANY)' % \
                  plgen.quote_path(bmp_file)
        init = []
        if id_name: init.append(id_name)

        init.append('(setf (slot-%s obj) (wxBitmapButton_Create %s %s %s -1 -1 -1 -1 0))\n' % 
                    ( obj.name, parent, id, bmp))

        props_buf = plgen.generate_common_properties(obj)

        disabled_bmp = prop.get('disabled_bitmap')
        if disabled_bmp:
            if disabled_bmp.startswith('var:'):
                var = disabled_bmp[4:].strip()
                if var[0] != "$":
                    var = "$" + var
                props_buf.append(
                    '(wxBitmapButton_SetBitmapDisabled (slot-%s obj) %s)'
                    %(obj.name, var))
            elif disabled_bmp.startswith('code:'):
                var = disabled_bmp[5:].strip()
                props_buf.append(
                    '(wxBitmapButton_SetBitmapDisabled (slot-%s obj) %s)'
                    % (obj.name, var))
            else:
                props_buf.append(
                    '(wxBitmapButton_SetBitmapDisabled (slot-%s obj) %s)'
                    % (obj.name, plgen.quote_path(disabled_bmp)))
                wxButton_SetDefault
        if not prop.has_key('size'):
            props_buf.append('(wxButton_SetDefault (slot-%s obj))'
                             %(obj.name))

        if prop.get('default', False):
            props_buf.append('(wxButton_SetDefault (slot-%s obj))'
                             %(obj.name))
        return init, props_buf, []

# end of class LispCodeGenerator



def initialize():
    common.class_names['EditBitmapButton'] = 'wxBitmapButton'
    plgen = common.code_writers.get('lisp')

    if plgen:
        plgen.add_widget_handler('wxBitmapButton', LispCodeGenerator())
