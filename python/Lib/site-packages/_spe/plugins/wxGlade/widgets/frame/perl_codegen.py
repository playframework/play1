# codegen.py: code generator functions for wxFrame objects
# $Id: perl_codegen.py,v 1.10 2007/03/27 07:01:59 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common
from MenuTree import *
from codegen import StatusFieldsHandler


class PerlStatusBarCodeGenerator:
    def get_code(self, obj):
        """\
        function that generates code for the statusbar of a wxFrame.
        """
        plgen = common.code_writers['perl']
        labels, widths = obj.properties['statusbar']
        style = obj.properties.get("style")
        if not style: style = '0'
        init = [ '$self->{%s} = $self->CreateStatusBar(%s, %s);\n'
                 % (obj.name, len(labels), style) ]
        props = []
        append = props.append
        append('$self->{%s}->SetStatusWidths(%s);\n'
            %  (obj.name, ','.join(map(str, widths))))
        labels = ',\n\t\t'.join([plgen.quote_str(l) for l in labels])
        append('\n\tmy( @%s_fields ) = (\n\t\t%s\n\t);\n\n' %
               (obj.name, labels))
        append('if( @%s_fields ) {\n' % obj.name)
        append('\t$self->{%s}->SetStatusText($%s_fields[$_], $_) '
            % (obj.name, obj.name) )
        append('\n\t\tfor 0 .. $#%s_fields ;\n\t}\n' % obj.name)
        return init, props, []

# end of class PerlStatusBarCodeGenerator


class PerlFrameCodeGenerator:
#wxFrame(  parent, id, title, pos , size , style , name )
    new_signature = [
        '$parent', '$id', '$title', '$pos', '$size', '$style', '$name'
    ]

    def get_code(self, obj):
        return [], [], [], [] # the frame can't be a children

    def get_properties_code(self, frame):
        """\
        generates the code for the various wxFrame specific properties.
        Returns a list of strings containing the generated code
        """
        prop = frame.properties
        plgen = common.code_writers['perl']
        out = []
        title = prop.get('title')
        if title:
            out.append('$self->SetTitle(%s);\n' % plgen.quote_str(title))

        icon = prop.get('icon')
        if icon:
            out.append('my $icon = Wx::Icon->new();\n')
            out.append('$icon->CopyFromBitmap(Wx::Bitmap->new(%s, '
                       'wxBITMAP_TYPE_ANY));\n' % plgen.quote_str(icon))
            out.append('$self->SetIcon($icon);\n')
            
        out.extend(plgen.generate_common_properties(frame))
        return out

    def get_layout_code(self, frame):
        ret = ['$self->Layout();\n']
        try:
            if int(frame.properties['centered']):
                ret.append('$self->Centre();\n')
        except (KeyError, ValueError):
            pass
        plgen = common.code_writers['perl']
        if frame.properties.get('size', '').strip() and \
               plgen.for_version < (2, 8):
            ret.append(plgen.generate_code_size(frame))
        return ret

# end of class PerlFrameCodeGenerator


class PerlMDIChildFrameCodeGenerator(PerlFrameCodeGenerator):
    extra_headers = ['Wx::MDI']
#wxMDIChildFrame(parent, id, title, pos, size, style, name )

# end of class PerlMDIChildFrameCodeGenerator


def initialize():
    cn = common.class_names
    cn['EditFrame'] = 'wxFrame'
    cn['EditMDIChildFrame'] = 'wxMDIChildFrame'
    cn['EditStatusBar'] = 'wxStatusBar'
    common.toplevels['EditFrame'] = 1
    common.toplevels['EditMDIChildFrame'] = 1

    plgen = common.code_writers.get('perl')
    if plgen:
        plgen.add_widget_handler('wxFrame', PerlFrameCodeGenerator())
        plgen.add_widget_handler('wxMDIChildFrame',
                                  PerlMDIChildFrameCodeGenerator())
        
        plgen.add_widget_handler('wxStatusBar', PerlStatusBarCodeGenerator())
        
        plgen.add_property_handler('fields', StatusFieldsHandler)
        plgen.add_property_handler('menubar', plgen.DummyPropertyHandler)
        plgen.add_property_handler('statusbar', plgen.DummyPropertyHandler)

