# codegen.py: code generator functions for CustomWidget objects
# $Id: codegen.py,v 1.13 2007/08/07 12:13:43 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common

class ArgumentsCodeHandler:
    def __init__(self):
        self.arguments = []
        self.curr_arg = []

    def start_elem(self, name, attrs):
        pass

    def end_elem(self, name, code_obj):
        if name == 'arguments':
            code_obj.properties['arguments'] = self.arguments
            return True
        elif name == 'argument':
            tab_name = "".join(self.curr_arg)
            self.arguments.append(tab_name)
            self.curr_arg = []
        return False

    def char_data(self, data):
        self.curr_arg.append(data)

# end of class ArgumentsCodeHandler


def _fix_arguments(arguments, parent, id, size):
    # Dinogen, 29 oct 2003
    # adding $width e $height:
    vSize = size.split(',')
    for i in range(len(arguments)):
        if arguments[i] == '$parent': arguments[i] = parent
        elif arguments[i] == '$id': arguments[i] = id
        elif arguments[i] == '$width': arguments[i] = vSize[0]
        elif arguments[i] == '$height': arguments[i] = vSize[1]
    return arguments


class PythonCodeGenerator:
    def get_code(self, widget):
        if widget.preview and widget.klass not in widget.parser.class_names:
            # if this CustomWidget refers to another class in the same wxg
            # file, use that for the preview
            #print "PREVIEW:", widget.klass, widget.parser.class_names
            return self.get_code_preview(widget)
        pygen = common.code_writers['python']
        prop = widget.properties
        id_name, id = pygen.generate_code_id(widget)
        if not widget.parent.is_toplevel:
            parent = 'self.%s' % widget.parent.name
        else: parent = 'self'
        init = []
        if id_name: init.append(id_name)
        arguments = _fix_arguments(
            prop.get('arguments', []), parent, id,
            prop.get('size', '-1, -1').strip())
        init.append('self.%s = %s(%s)\n' % (widget.name, widget.klass,
                                            ", ".join(arguments)))
        props_buf = pygen.generate_common_properties(widget)
        return init, props_buf, []

    def get_code_preview(self, widget):
        pygen = common.code_writers['python']
        if not widget.parent.is_toplevel:
            parent = 'self.%s' % widget.parent.name
        else: parent = 'self'
        init = []
        append = init.append
        append('self.%s = wx.Window(%s, -1)\n' % (widget.name, parent))
        on_paint_code = """\
def self_%s_on_paint(event):
    widget = self.%s
    dc = wx.PaintDC(widget)
    dc.BeginDrawing()
    dc.SetBrush(wx.WHITE_BRUSH)
    dc.SetPen(wx.BLACK_PEN)
    dc.SetBackground(wx.WHITE_BRUSH)
    dc.Clear()
    w, h = widget.GetClientSize()
    dc.DrawLine(0, 0, w, h)
    dc.DrawLine(w, 0, 0, h)
    text = 'Custom Widget: %s'
    tw, th = dc.GetTextExtent(text)
    x = (w - tw)/2
    y = (h - th)/2
    dc.SetPen(wx.ThePenList.FindOrCreatePen(wx.BLACK, 0, wx.TRANSPARENT))
    dc.DrawRectangle(x-1, y-1, tw+2, th+2)
    dc.DrawText(text, x, y)
    dc.EndDrawing()    
""" % ((widget.name,) * 3)
        for line in on_paint_code.splitlines():
            append(line + '\n')        
        append('wx.EVT_PAINT(self.%s, self_%s_on_paint)\n' %
               (widget.name, widget.name))
        return init, [], []

# end of class PythonCodeGenerator


class CppCodeGenerator:
    def get_code(self, widget):
        cppgen = common.code_writers['C++']
        prop = widget.properties
        id_name, id = cppgen.generate_code_id(widget)
        if id_name: ids = [ id_name ]
        else: ids = []
        if not widget.parent.is_toplevel: parent = '%s' % widget.parent.name
        else: parent = 'this'
        arguments = _fix_arguments(
            prop.get('arguments', []), parent, id,
            prop.get('size', '-1, -1').strip())
        init = ['%s = new %s(%s);\n' % (widget.name, widget.klass,
                                        ", ".join(arguments)) ]
        props_buf = cppgen.generate_common_properties(widget)
        return init, ids, props_buf, []

# end of class CppCodeGenerator


        
def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']

    class CustomXrcObject(xrcgen.DefaultXrcObject):
        from xml.sax.saxutils import escape

        def write(self, outfile, ntabs):
            # first, fix the class:
            self.klass = obj.klass
            # then, the attributes:
            if 'arguments' in self.properties:
                args = self.properties['arguments']
                del self.properties['arguments']
                for arg in args:           
                    try:
                        name, val = [s.strip() for s in arg.split(':', 1)]
                    except Exception, e:
                        print 'Exception:', e
                        continue # silently ignore malformed arguments
                    self.properties[name] = val
            xrcgen.DefaultXrcObject.write(self, outfile, ntabs)

    return CustomXrcObject(obj)


def initialize():
    common.class_names['CustomWidget'] = 'CustomWidget'

    # python code generation functions
    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('CustomWidget', PythonCodeGenerator())
        pygen.add_property_handler('arguments', ArgumentsCodeHandler,
                                   'CustomWidget')
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('CustomWidget', CppCodeGenerator())
        cppgen.add_property_handler('arguments', ArgumentsCodeHandler,
                                    'CustomWidget')
    xrcgen = common.code_writers.get('XRC')
    if xrcgen:
        xrcgen.add_widget_handler('CustomWidget', xrc_code_generator)
        xrcgen.add_property_handler('arguments', ArgumentsCodeHandler,
                                    'CustomWidget')
