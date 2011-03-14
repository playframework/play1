# lisp_codegen.py : lisp generator functions for wxDialog objects
# $Id: lisp_codegen.py,v 1.1 2005/09/22 06:59:14 efuzzyone Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY


import common


class LispCodeGenerator:
#wxDialog( parent, id, title, pos, size, style, name )
    new_signature = [
        '$parent', '$id', '$title', '$pos', '$size', '$style', '$name'
    ]

    def get_code(self, obj):
        return [], [], []

    def get_properties_code(self, dialog):
        prop = dialog.properties
        plgen = common.code_writers['lisp']
        out = []
        title = prop.get('title')
        if title: out.append('(wxWindow_SetTitle (slot-%s self) %s)\n'
                             % (dialog.name, plgen.quote_str(title)))

        icon = prop.get('icon')
        if icon: 
            out.append('my $icon = &Wx::wxNullIcon();\n')
            out.append('$icon->CopyFromBitmap(Wx::Bitmap->new(%s, '
                       'wxBITMAP_TYPE_ANY));\n' % plgen.quote_str(icon))
            out.append('$self->SetIcon($icon);\n')

        out.extend(plgen.generate_common_properties(dialog))

        return out


    def get_layout_code(self, dialog):
        ret = ['(wxWindow_layout (slot-%s slef))\n' % dialog.name]
        try:
            if int(dialog.properties['centered']):
                ret.append('(wxWindow_Centre (slot-%s slef) wxBOTH)\n' % dialog.name)
        except (KeyError, ValueError):
            pass
        return ret

# end of class LispCodeGenerator

def initialize():
    cn = common.class_names
    cn['EditDialog'] = 'wxDialog'
    common.toplevels['EditDialog'] = 1

    plgen = common.code_writers.get('lisp')
    if plgen:
        plgen.add_widget_handler('wxDialog', LispCodeGenerator())

