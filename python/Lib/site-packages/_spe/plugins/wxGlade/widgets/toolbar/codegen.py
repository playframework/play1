# codegen.py: code generator functions for wxToolBar objects
# $Id: codegen.py,v 1.23 2007/03/27 07:01:51 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common, os
from tool import *

class PythonCodeGenerator:
    def get_properties_code(self, obj):
        prop = obj.properties
        pygen = common.code_writers['python']
        out = []
        append = out.append
        
        if obj.is_toplevel: obj_name = 'self'
        else: obj_name = 'self.' + obj.name
        
        bitmapsize = prop.get('bitmapsize')
        if bitmapsize:
            try:
                w, h = [int(i) for i in bitmapsize.split(',')]
                append('%s.SetToolBitmapSize((%s, %s))\n' % (obj_name, w, h))
            except:
                pass
        margins = prop.get('margins')
        if margins:
            try:
                w, h = [int(i) for i in margins.split(',')]
                append('%s.SetMargins((%s, %s))\n' % (obj_name, w, h))
            except:
                pass
        packing = prop.get('packing')
        if packing:
            append('%s.SetToolPacking(%s)\n' % (obj_name, packing))
        separation = prop.get('separation')
        if separation:
            append('%s.SetToolSeparation(%s)\n' % (obj_name, separation))
        append('%s.Realize()\n' % obj_name)

        return out

    def get_init_code(self, obj):
        prop = obj.properties
        pygen = common.code_writers['python']
        cn = pygen.cn
        out = []
        append = out.append
        tools = obj.properties['toolbar']
        ids = []
       
        if obj.is_toplevel: obj_name = 'self'
        else: obj_name = 'self.' + obj.name

        def _get_bitmap(bitmap):
            bmp_preview_path = os.path.join(common.wxglade_path, "icons",
                                            "icon.xpm")
            if not bitmap:
                return cn('wxNullBitmap')
            elif bitmap.startswith('var:'):
                if obj.preview:
                    return "%s('%s', %s)" % (cn('wxBitmap'), bmp_preview_path,
                                             cn('wxBITMAP_TYPE_XPM') )
                else:
                    return (cn('wxBitmap') + '(%s,' + cn('wxBITMAP_TYPE_ANY') +
                            ')') % (bitmap[4:].strip())
            elif bitmap.startswith('code:'):
                if obj.preview:
                    return "%s('%s', %s)" % (cn('wxBitmap'), bmp_preview_path,
                                             cn('wxBITMAP_TYPE_XPM') )
                else:
                    return '(%s)' % bitmap[5:].strip()
            else:
                if obj.preview:
                    import misc
                    bitmap = misc.get_relative_path(bitmap, True)
                return cn('wxBitmap') + \
                       ('(%s, ' + cn('wxBITMAP_TYPE_ANY') + ')') % \
                       pygen.quote_str(bitmap, False, False)
                
        for tool in tools:
            if tool.id == '---': # item is a separator
                append('%s.AddSeparator()\n' % obj_name)
            else:
                name, val = pygen.generate_code_id(None, tool.id)
                if obj.preview or (not name and (not val or val == '-1')):
                    id = cn('wxNewId()')
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
                append('%s.AddLabelTool(%s, %s, %s, %s, %s, %s, %s)\n' %
                       (obj_name, id, pygen.quote_str(tool.label),
                        bmp1, bmp2, cn(kind), pygen.quote_str(tool.short_help),
                        pygen.quote_str(tool.long_help)))
        
        return ids + out

    def get_code(self, obj):
        """\
        function that generates Python code for the menubar of a wxFrame.
        """
        pygen = common.code_writers['python']
        style = obj.properties.get('style')
        if style:
            style = ', style=' + pygen.cn_f('wxTB_HORIZONTAL|' + style)
        else:
            style = ''
        klass = obj.klass
        if klass == obj.base: klass = pygen.cn(klass)
        init = [ '\n', '# Tool Bar\n', 'self.%s = %s(self, -1%s)\n' %
                 (obj.name, klass, style),
                 'self.SetToolBar(self.%s)\n' % obj.name ]
        init.extend(self.get_init_code(obj))
        init.append('# Tool Bar end\n')
        return init, self.get_properties_code(obj), []

    def get_events(self, obj):
        pygen = common.code_writers['python']
        cn = pygen.cn
        out = []

        def do_get(tool):
            ret = []
            name, val = pygen.generate_code_id(None, tool.id)
            if not val: val = '-1' # but this is wrong anyway...
            if tool.handler:
                ret.append((val, 'EVT_TOOL', tool.handler))
            return ret
        
        for tool in obj.properties['toolbar']:
            out.extend(do_get(tool))
        return out

# end of class PythonCodeGenerator


class ToolsHandler:
    """Handler for tools of a toolbar"""
    item_attrs = ('label', 'id', 'short_help', 'type', 'long_help',
                  'bitmap1', 'bitmap2', 'handler')
    def __init__(self):
        self.tools = []
        self.curr_tool = None
        self.attr_val = []

    def start_elem(self, name, attrs):
        if name == 'tool':
            self.curr_tool = Tool()

    def end_elem(self, name, code_obj):
        if name == 'tools':
            code_obj.properties['toolbar'] = self.tools
            return True
        if name == 'tool' and self.curr_tool:
            self.tools.append(self.curr_tool)
        elif name in self.item_attrs:
            setattr(self.curr_tool, name, "".join(self.attr_val))
            self.attr_val = []

    def char_data(self, data):
        self.attr_val.append(data)

# end of class ToolsHandler


def xrc_code_generator(obj):
    """\
    function that generates XRC code for a toolbar
    """
    from xml.sax.saxutils import escape, quoteattr
    xrcgen = common.code_writers['XRC']
    
    class ToolBarXrcObject(xrcgen.DefaultXrcObject):
        def append_item(self, item, outfile, tabs):
            write = outfile.write
            if item.id == '---': # item is a separator
                write('    '*tabs + '<object class="separator"/>\n')
            else:
                if item.id:
                    name = item.id.split('=', 1)[0]
                    if name:
                        write('    '*tabs + '<object class="tool" ' \
                              'name=%s>\n' % quoteattr(name))
                    else:
                        write('    '*tabs + '<object class="tool">\n')
                else:
                    write('    '*tabs + '<object class="tool">\n')
                # why XRC seems to ignore label??
                # this has been fixed on CVS, so add it (it shouldn't hurt...)
                if item.label:
                    write('    '*(tabs+1) + '<label>%s</label>\n' %
                          escape(item.label))
                if item.short_help:
                    write('    '*(tabs+1) + '<tooltip>%s</tooltip>\n' % \
                          escape(item.short_help))
                if item.long_help:
                    write('    '*(tabs+1) + '<longhelp>%s</longhelp>\n' % \
                          escape(item.long_help))
                if item.bitmap1:
                    write('    '*(tabs+1) + '<bitmap>%s</bitmap>\n' % \
                          escape(item.bitmap1))
                if item.bitmap2:
                    write('    '*(tabs+1) + '<bitmap2>%s</bitmap2>\n' % \
                          escape(item.bitmap2))
                try:
                    # again, it seems that XRC doesn't support "radio" tools
                    if int(item.type) == 1:
                        write('    '*(tabs+1) + '<toggle>1</toggle>\n')
                    # the above has been fixed on CVS, so add a radio if
                    # it's there
                    elif int(item.type) == 2:
                        write('    '*(tabs+1) + '<radio>1</radio>\n')
                except ValueError:
                    pass
                write('    '*tabs + '</object>\n')
        
        def write(self, outfile, tabs):
            tools = self.code_obj.properties['toolbar']
            write = outfile.write
            write('    '*tabs + '<object class="wxToolBar" name=%s>\n' % \
                  quoteattr(self.name))
            for prop_name in 'bitmapsize', 'margins':
                prop = self.code_obj.properties.get(prop_name)
                if prop:
                    try:
                        w, h = [int(i) for i in prop.split(',')]
                        write('    ' * (tabs+1) + '<%s>%s, %s</%s>\n' \
                              % (prop_name, w, h, prop_name))
                    except:
                        pass
            for prop_name in 'packing', 'separation':
                prop = self.code_obj.properties.get(prop_name)
                if prop:
                    write('    ' * (tabs+1) + '<%s>%s</%s>\n' % \
                          (prop_name, escape(prop), prop_name))
            style = self.code_obj.properties.get('style')
            if style:
                style = style.split('|')
                style.append('wxTB_HORIZONTAL')
                write('    '*(tabs+1) + '<style>%s</style>\n' % \
                      escape('|'.join(style)))
            for t in tools:
                self.append_item(t, outfile, tabs+1)
            write('    '*tabs + '</object>\n')

    # end of class ToolBarXrcObject
    
    return ToolBarXrcObject(obj)


class CppCodeGenerator:
    constructor = [('wxWindow*', 'parent'), ('int', 'id'),
                   ('const wxPoint&', 'pos', 'wxDefaultPosition'),
                   ('const wxSize&', 'size', 'wxDefaultSize'),
                   ('long', 'style', 'wxTB_HORIZONTAL|wxTB_NOBORDER')]

    def get_code(self, obj):
        """\
        generates C++ code for the toolbar of a wxFrame.
        """
        cppgen = common.code_writers['C++']
        style = obj.properties.get('style')
        if style:
            style = ', wxDefaultPosition, wxDefaultSize, wxTB_HORIZONTAL|' + \
                    style
        else:
            style = ''
        init = [ '%s = new %s(this, -1%s);\n' % (obj.name, obj.klass, style),
                 'SetToolBar(%s);\n' % obj.name ]
        init.extend(self.get_properties_code(obj))
        ids = self.get_ids_code(obj)
        return init, ids, [], []

    def get_properties_code(self, obj):
        cppgen = common.code_writers['C++']
        tools = obj.properties['toolbar']
        out = []
        append = out.append
        prop = obj.properties

        if obj.is_toplevel: obj_name = ''
        else: obj_name = obj.name + '->'

        bitmapsize = obj.properties.get('bitmapsize')
        if bitmapsize:
            try:
                w, h = [int(i) for i in bitmapsize.split(',')]
                append('%sSetToolBitmapSize(wxSize(%s, %s));\n' % \
                       (obj_name, w, h))
            except:
                pass
        margins = obj.properties.get('margins')
        if margins:
            try:
                w, h = [int(i) for i in margins.split(',')]
                append('%sSetMargins(wxSize(%s, %s));\n' % \
                       (obj_name, w, h))
            except:
                pass
        packing = prop.get('packing')
        if packing:
            append('%sSetToolPacking(%s);\n' % (obj_name, packing))
        separation = prop.get('separation')
        if separation:
            append('%sSetToolSeparation(%s);\n' % (obj_name, separation))

        def _get_bitmap(bitmap):
            if not bitmap:
                return 'wxNullBitmap'
            elif bitmap.startswith('var:'):
                return 'wxBitmap(%s, wxBITMAP_TYPE_ANY)' % bitmap[4:].strip()
            elif bitmap.startswith('code:'):
                return '(%s)' % bitmap[5:].strip()
            else:
                return 'wxBitmap(%s, wxBITMAP_TYPE_ANY)' % \
                       cppgen.quote_str(bitmap, False, False)
                
        for tool in tools:
            if tool.id == '---': # item is a separator
                append('%sAddSeparator();\n' % obj_name)
            else:
                name, val = cppgen.generate_code_id(None, tool.id)
                if not name and (not val or val == '-1'):
                    id = 'wxNewId()'
                else:
                    id = val
                kinds = ['wxITEM_NORMAL', 'wxITEM_CHECK', 'wxITEM_RADIO']
                try:
                    kind = kinds[int(tool.type)]
                except (IndexError, ValueError):
                    kind = 'wxITEM_NORMAL'
                bmp1 = _get_bitmap(tool.bitmap1)
                bmp2 = _get_bitmap(tool.bitmap2)
                append('%sAddTool(%s, %s, %s, %s, %s, %s, %s);\n' %
                       (obj_name, id, cppgen.quote_str(tool.label),
                        bmp1, bmp2, kind, cppgen.quote_str(tool.short_help),
                        cppgen.quote_str(tool.long_help)))

        append('%sRealize();\n' % obj_name)

        return out

    def get_ids_code(self, obj):
        cppgen = common.code_writers['C++']
        ids = []
        tools = obj.properties['toolbar']
        
        for item in tools:
            if item.id == '---': # item is a separator
                pass # do nothing
            else:
                name, val = cppgen.generate_code_id(None, item.id)
                if name.find('=') != -1:
                    ids.append(name)
##                 if item.id:
##                     tokens = item.id.split('=')
##                     if len(tokens) > 1:
##                         id = tokens[0]
##                         ids.append(' = '.join(tokens))
        return ids

    def get_events(self,obj):
        cppgen = common.code_writers['C++']
        out = []

        def do_get(tool):
            ret = []
            name, val = cppgen.generate_code_id(None, tool.id)
            if not val: val = '-1' # but this is wrong anyway...
            if tool.handler:
                ret.append((val, 'EVT_TOOL', tool.handler, 'wxCommandEvent'))
            return ret

        for tool in obj.properties['toolbar']:
            out.extend(do_get(tool))
        return out

# end of class CppCodeGenerator


def initialize():
    common.class_names['EditToolBar'] = 'wxToolBar'
    common.toplevels['EditToolBar'] = 1

    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('wxToolBar', PythonCodeGenerator())
        pygen.add_property_handler('tools', ToolsHandler)
    xrcgen = common.code_writers.get('XRC')
    if xrcgen:
        xrcgen.add_widget_handler('wxToolBar', xrc_code_generator)
        xrcgen.add_property_handler('tools', ToolsHandler)
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxToolBar', CppCodeGenerator())
        cppgen.add_property_handler('tools', ToolsHandler)
