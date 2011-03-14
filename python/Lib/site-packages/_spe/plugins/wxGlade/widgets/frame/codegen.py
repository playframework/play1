# codegen.py: code generator functions for wxFrame objects
# $Id: codegen.py,v 1.24 2007/03/27 07:02:00 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common
from MenuTree import *


class PythonStatusbarCodeGenerator:
    def get_code(self, obj):
        pygen = common.code_writers['python']
        labels, widths = obj.properties['statusbar']
        style = obj.properties.get("style")
        if style: style = pygen.cn_f(style)
        else: style = '0'
        init = [ 'self.%s = self.CreateStatusBar(%s, %s)\n' % \
                 (obj.name, len(labels), style) ]
        props = []
        append = props.append
        append('self.%s.SetStatusWidths(%s)\n' % (obj.name, repr(widths)))
        append('# statusbar fields\n')
        append('%s_fields = [%s]\n' % \
               (obj.name, ', '.join([pygen.quote_str(l) for l in labels])))
        append('for i in range(len(%s_fields)):\n' % obj.name)
        append('    self.%s.SetStatusText(%s_fields[i], i)\n' % \
               (obj.name, obj.name))
        return init, props, []

# end of class PythonStatusbarCodeGenerator


class PythonFrameCodeGenerator:
    def get_code(self, obj):
        return [], [], []
    
    def get_properties_code(self, frame):
        prop = frame.properties
        pygen = common.code_writers['python']
        cn = pygen.cn
        out = []
        title = prop.get('title')
        if title: out.append('self.SetTitle(%s)\n' % pygen.quote_str(title))
        icon = prop.get('icon')
        if icon: 
            if icon.startswith('var:'):
                if not frame.preview:
                    out.append('_icon = ' + cn('wxEmptyIcon') + '()\n')
                    out.append(('_icon.CopyFromBitmap(' + cn('wxBitmap') +
                                '(%s, ' + cn('wxBITMAP_TYPE_ANY') + '))\n') % \
                               icon[4:].strip())
                    out.append('self.SetIcon(_icon)\n')
            elif icon.startswith('code:'):
                if not frame.preview:
                    out.append('_icon = ' + cn('wxEmptyIcon') + '()\n')
                    out.append(('_icon.CopyFromBitmap(%s)\n') % \
                               icon[5:].strip())
                    out.append('self.SetIcon(_icon)\n')
            else:
                if frame.preview:
                    import misc
                    icon = misc.get_relative_path(icon, True)
                out.append('_icon = ' + cn('wxEmptyIcon') + '()\n')
                out.append(('_icon.CopyFromBitmap(' + cn('wxBitmap') +
                            '(%s, ' + cn('wxBITMAP_TYPE_ANY') + '))\n') % \
                           pygen.quote_str(icon, False, False))
                out.append('self.SetIcon(_icon)\n')

        out.extend(pygen.generate_common_properties(frame))
        return out

    def get_layout_code(self, frame):
        ret = ['self.Layout()\n']
        try:
            if int(frame.properties['centered']):
                ret.append('self.Centre()\n')
        except (KeyError, ValueError):
            pass
        pygen = common.code_writers['python']
        if frame.properties.get('size', '').strip() and \
               pygen.for_version < (2, 8):
            ret.append(pygen.generate_code_size(frame))
        return ret
    
# end of class PythonFrameCodeGenerator


# property handlers for code generation

class StatusFieldsHandler:
    """Handler for statusbar fields"""
    def __init__(self):
        self.labels = []
        self.widths = []
        self.curr_label = []
        
    def start_elem(self, name, attrs):
        if name == 'field':
            self.widths.append(int(attrs.get('width', -1)))
            
    def end_elem(self, name, code_obj):
        if name == 'fields':
            code_obj.properties['statusbar'] = (self.labels, self.widths)
            return True
        self.labels.append("".join(self.curr_label))
        self.curr_label = []
        
    def char_data(self, data):
        self.curr_label.append(data)

# end of class StatusFieldsHandler


def xrc_frame_code_generator(obj):
    xrcgen = common.code_writers['XRC']
    class FrameXrcObject(xrcgen.DefaultXrcObject):
        def write(self, outfile, tabs):
            if 'menubar' in self.properties:
                del self.properties['menubar']
            if 'statusbar' in self.properties:
                del self.properties['statusbar']
            if 'toolbar' in self.properties:
                del self.properties['toolbar']
            xrcgen.DefaultXrcObject.write(self, outfile, tabs)

        def write_property(self, name, val, outfile, ntabs):
            if name != 'sizehints':
                xrcgen.DefaultXrcObject.write_property(
                    self, name, val, outfile, ntabs)

    # end of class FrameXrcObject
    
    return FrameXrcObject(obj)
                

def xrc_statusbar_code_generator(obj):
    xrcgen = common.code_writers['XRC']
    class StatusbarXrcObject(xrcgen.DefaultXrcObject):
        def write(self, outfile, tabs):
            if 'statusbar' in self.properties:
                fields, widths = self.properties['statusbar']
                self.properties['fields'] = str(len(fields))
                self.properties['widths'] = ', '.join([str(w) for w in widths])
                del self.properties['statusbar']
            xrcgen.DefaultXrcObject.write(self, outfile, tabs)

    # end of class StatusbarXrcObject
    
    return StatusbarXrcObject(obj)


class CppStatusBarCodeGenerator:
    def get_code(self, obj):
        """\
        function that generates code for the statusbar of a wxFrame.
        """
        cppgen = common.code_writers['C++']
        labels, widths = obj.properties['statusbar']
        style = obj.properties.get("style")
        if not style: style = '0'
        init = [ '%s = CreateStatusBar(%s, %s);\n' %
                 (obj.name, len(labels), style) ]
        props = []
        append = props.append
        append('int %s_widths[] = { %s };\n' % (obj.name,
                                                ', '.join(map(str, widths))))
        append('%s->SetStatusWidths(%s, %s_widths);\n' % \
               (obj.name, len(widths), obj.name))
        labels = ',\n        '.join([cppgen.quote_str(l) for l in labels])
        append('const wxString %s_fields[] = {\n        %s\n    };\n' %
               (obj.name, labels))
        append('for(int i = 0; i < %s->GetFieldsCount(); ++i) {\n' % obj.name)
        append('    %s->SetStatusText(%s_fields[i], i);\n    }\n' % \
               (obj.name, obj.name))
        return init, [], props, []

# end of class CppStatusBarCodeGenerator


class CppFrameCodeGenerator:
    constructor = [('wxWindow*', 'parent'), ('int', 'id'),
                   ('const wxString&', 'title'),
                   ('const wxPoint&', 'pos', 'wxDefaultPosition'),
                   ('const wxSize&', 'size', 'wxDefaultSize'),
                   ('long', 'style', 'wxDEFAULT_FRAME_STYLE')]

    def get_code(self, obj):
        return [], [], [], [] # the frame can't be a children

    def get_properties_code(self, frame):
        """\
        generates the code for the various wxFrame specific properties.
        Returns a list of strings containing the generated code
        """
        prop = frame.properties
        cppgen = common.code_writers['C++']
        out = []
        title = prop.get('title')
        if title: out.append('SetTitle(%s);\n' % cppgen.quote_str(title))
        icon = prop.get('icon')
        if icon:
            out.append('wxIcon _icon;\n')
            if icon.startswith('var:'):
                out.append('_icon.CopyFromBitmap(wxBitmap(' +
                           '%s, wxBITMAP_TYPE_ANY));\n' % \
                           icon[4:].strip())
            elif icon.startswith('code:'):
                out.append('_icon.CopyFromBitmap(%s);\n' % \
                           icon[5:].strip())
            else:
                out.append('_icon.CopyFromBitmap(wxBitmap(%s, '
                           'wxBITMAP_TYPE_ANY));\n' % \
                           cppgen.quote_str(icon, False, False))
            out.append('SetIcon(_icon);\n')
            
        out.extend(cppgen.generate_common_properties(frame))
        return out

    def get_layout_code(self, frame):
        ret = ['Layout();\n']
        try:
            if int(frame.properties['centered']):
                ret.append('Centre();\n')
        except (KeyError, ValueError):
            pass
        cppgen = common.code_writers['C++']
        if frame.properties.get('size', '').strip() and \
               cppgen.for_version < (2, 8):
            ret.append(cppgen.generate_code_size(frame))
        return ret

# end of class CppFrameCodeGenerator


class CppMDIChildFrameCodeGenerator(CppFrameCodeGenerator):
    extra_headers = ['<wx/mdi.h>']

    constructor = [('wxMDIParentFrame*', 'parent'), ('int', 'id'),
                   ('const wxString&', 'title'),
                   ('const wxPoint&', 'pos', 'wxDefaultPosition'),
                   ('const wxSize&', 'size', 'wxDefaultSize'),
                   ('long', 'style', 'wxDEFAULT_FRAME_STYLE')]

# end of class CppMDIChildFrameCodeGenerator


def initialize():
    cn = common.class_names
    cn['EditFrame'] = 'wxFrame'
    cn['EditMDIChildFrame'] = 'wxMDIChildFrame'
    cn['EditStatusBar'] = 'wxStatusBar'
    common.toplevels['EditFrame'] = 1
    common.toplevels['EditMDIChildFrame'] = 1

    pygen = common.code_writers.get('python')
    if pygen:
        awh = pygen.add_widget_handler
        awh('wxFrame', PythonFrameCodeGenerator())
        awh('wxMDIChildFrame', PythonFrameCodeGenerator())
        awh('wxStatusBar', PythonStatusbarCodeGenerator())
        aph = pygen.add_property_handler
        aph('statusbar', pygen.DummyPropertyHandler)
        aph('fields', StatusFieldsHandler)
        aph('menubar', pygen.DummyPropertyHandler)

    xrcgen = common.code_writers.get('XRC')
    if xrcgen:
        xrcgen.add_widget_handler('wxFrame', xrc_frame_code_generator)
        xrcgen.add_widget_handler('wxMDIChildFrame',
                                  xrcgen.NotImplementedXrcObject)
        xrcgen.add_widget_handler('wxStatusBar', xrc_statusbar_code_generator)
        #xrcgen.NotImplementedXrcObject)
        xrcgen.add_property_handler('fields', StatusFieldsHandler)

    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxFrame', CppFrameCodeGenerator())
        cppgen.add_widget_handler('wxMDIChildFrame',
                                  CppMDIChildFrameCodeGenerator())
        
        cppgen.add_widget_handler('wxStatusBar', CppStatusBarCodeGenerator())
        
        cppgen.add_property_handler('fields', StatusFieldsHandler)
        cppgen.add_property_handler('menubar', cppgen.DummyPropertyHandler)
        cppgen.add_property_handler('statusbar', cppgen.DummyPropertyHandler)
