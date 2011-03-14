# perl_codegen.py : perl generator functions for wxDialog objects
# $Id: perl_codegen.py,v 1.3 2004/09/17 13:09:53 agriggio Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY


import common


class PerlCodeGenerator:
#wxDialog( parent, id, title, pos, size, style, name )
    new_signature = [
        '$parent', '$id', '$title', '$pos', '$size', '$style', '$name'
    ]

    def get_code(self, obj):
        return [], [], []

    def get_properties_code(self, dialog):
        prop = dialog.properties
        plgen = common.code_writers['perl']
        out = []
        title = prop.get('title')
        if title: out.append('$self->SetTitle(%s);\n' % plgen.quote_str(title))

        icon = prop.get('icon')
        if icon: 
            out.append('my $icon = &Wx::wxNullIcon();\n')
            out.append('$icon->CopyFromBitmap(Wx::Bitmap->new(%s, '
                       'wxBITMAP_TYPE_ANY));\n' % plgen.quote_str(icon))
            out.append('$self->SetIcon($icon);\n')

        out.extend(plgen.generate_common_properties(dialog))

        return out


    def get_layout_code(self, dialog):
        ret = ['$self->Layout();\n']
        try:
            if int(dialog.properties['centered']):
                ret.append('$self->Centre();\n')
        except (KeyError, ValueError):
            pass
        return ret

# end of class PerlCodeGenerator

def initialize():
    cn = common.class_names
    cn['EditDialog'] = 'wxDialog'
    common.toplevels['EditDialog'] = 1

    plgen = common.code_writers.get('perl')
    if plgen:
        plgen.add_widget_handler('wxDialog', PerlCodeGenerator())

