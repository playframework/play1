# codegen.py: code generator functions for wxFrame objects
# $Id: lisp_codegen.py,v 1.3 2007/03/27 07:02:00 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common
from MenuTree import *
from codegen import StatusFieldsHandler


class LispStatusBarCodeGenerator:
    def get_code(self, obj):
        """\
        function that generates code for the statusbar of a wxFrame.
        """
        plgen = common.code_writers['lisp']
        labels, widths = obj.properties['statusbar']
        style = obj.properties.get("style")
        if not style: style = '0'
        init = [ '(setf (slot-%s obj) (wxFrame_CreateStatusBar (slot-top-window obj) %s %s))\n'
                 % (obj.name, len(labels), style) ]
        props = []

        append = props.append
        append('(wxStatusBar_SetStatusWidths (slot-%s obj) %s (vector %s))\n'
            %  (obj.name, len(widths),' '.join(map(str, widths))))

        i = 0
        for l in labels:
            append('\t (wxStatusBar_SetStatusText (slot-%s obj) %s %s)\n'
                   % (obj.name, plgen.quote_str(l),i) )
            i=i+1
        return init, props, []

# end of class LispStatusBarCodeGenerator


class LispFrameCodeGenerator:
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
        plgen = common.code_writers['lisp']
        out = []
        title = prop.get('title')
        if title:
            out.append('(wxFrame_SetTitle (slot-top-window %s) %s)\n' % plgen.quote_str(title))

        icon = prop.get('icon')
        if icon:
            out.append('my $icon = Wx::Icon->new();\n')
            out.append('$icon->CopyFromBitmap(Wx::Bitmap->new(%s, '
                       'wxBITMAP_TYPE_ANY));\n' % plgen.quote_str(icon))
            out.append('(wxFrame_SetIcon (slot-top-window obj) $icon)\n')
            
        out.extend(plgen.generate_common_properties(frame))
        return out

    def get_layout_code(self, frame):
        ret = ['$self->Layout();\n']
        try:
            if int(frame.properties['centered']):
                ret.append('(wxFrame_Centre (slot-top-window obj) 0)\n')
        except (KeyError, ValueError):
            pass
        return ret

# end of class LispFrameCodeGenerator


class LispMDIChildFrameCodeGenerator(LispFrameCodeGenerator):
    extra_headers = ['Wx::MDI']
#wxMDIChildFrame(parent, id, title, pos, size, style, name )

# end of class LispMDIChildFrameCodeGenerator


def initialize():
    cn = common.class_names
    cn['EditFrame'] = 'wxFrame'
    cn['EditMDIChildFrame'] = 'wxMDIChildFrame'
    cn['EditStatusBar'] = 'wxStatusBar'
    common.toplevels['EditFrame'] = 1
    common.toplevels['EditMDIChildFrame'] = 1

    plgen = common.code_writers.get('lisp')
    if plgen:
        plgen.add_widget_handler('wxFrame', LispFrameCodeGenerator())
        plgen.add_widget_handler('wxMDIChildFrame',
                                  LispMDIChildFrameCodeGenerator())
        
        plgen.add_widget_handler('wxStatusBar', LispStatusBarCodeGenerator())
        
        plgen.add_property_handler('fields', StatusFieldsHandler)
        plgen.add_property_handler('menubar', plgen.DummyPropertyHandler)
        plgen.add_property_handler('statusbar', plgen.DummyPropertyHandler)

