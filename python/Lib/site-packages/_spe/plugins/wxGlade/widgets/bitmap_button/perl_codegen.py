# perl_codegen.py : perl generator functions for wxBitmapButton objects
# $Id: perl_codegen.py,v 1.10 2005/08/15 07:35:15 crazyinsomniac Exp $
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

class PerlCodeGenerator:
    def get_code(self, obj):
        plgen = common.code_writers['perl']
        prop = obj.properties
        id_name, id = plgen.generate_code_id(obj) 
        bmp_file = prop.get('bitmap', '')

        if not obj.parent.is_toplevel:
            parent = '$self->{%s}' % obj.parent.name
        else:
            parent = '$self'

        if not bmp_file:
            bmp = 'wxNullBitmap'
        elif bmp_file.startswith('var:'):
            # this is a variable holding pathname of bitmap
            var = bmp_file[4:].strip()
            if var[0] != "$":
                var = "$" + var
            bmp = 'Wx::Bitmap->new(%s, wxBITMAP_TYPE_ANY)' % var
        elif bmp_file.startswith('code:'): # this is a code chunk
            bmp = '(%s)' % bmp_file[5:].strip()
        else:
            bmp = 'Wx::Bitmap->new(%s, wxBITMAP_TYPE_ANY)' % \
                  plgen.quote_path(bmp_file)
        init = []
        if id_name: init.append(id_name)

        klass = obj.base;
        if klass != obj.klass : klass = obj.klass; 
        else: klass = klass.replace('wx','Wx::',1);

        init.append('$self->{%s} = %s->new(%s, %s, %s);\n' % 
                    ( obj.name, klass, parent, id, bmp) )

        props_buf = plgen.generate_common_properties(obj)

        disabled_bmp = prop.get('disabled_bitmap')
        if disabled_bmp:
            if disabled_bmp.startswith('var:'):
                var = disabled_bmp[4:].strip()
                if var[0] != "$":
                    var = "$" + var
                props_buf.append(
                    '$self->{%s}->SetBitmapDisabled('
                    'Wx::Bitmap->new(%s, wxBITMAP_TYPE_ANY));\n' %
                    (obj.name, var))
            elif disabled_bmp.startswith('code:'):
                var = disabled_bmp[5:].strip()
                props_buf.append(
                    '$self->{%s}->SetBitmapDisabled('
                    '%s);\n' % (obj.name, var))
            else:
                props_buf.append(
                    '$self->{%s}->SetBitmapDisabled('
                    'Wx::Bitmap->new(%s, wxBITMAP_TYPE_ANY));\n' % \
                    (obj.name, plgen.quote_path(disabled_bmp)))
                
        if not prop.has_key('size'):
            props_buf.append(
                '$self->{%s}->SetSize($self->{%s}->GetBestSize());\n' %
                (obj.name, obj.name)
                )

        if prop.get('default', False):
            props_buf.append('$self->{%s}->SetDefault();\n' % obj.name)
        return init, props_buf, []

# end of class PerlCodeGenerator



def initialize():
    common.class_names['EditBitmapButton'] = 'wxBitmapButton'
    plgen = common.code_writers.get('perl')

    if plgen:
        plgen.add_widget_handler('wxBitmapButton', PerlCodeGenerator())
