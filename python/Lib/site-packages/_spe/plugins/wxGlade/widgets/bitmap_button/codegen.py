# codegen.py: code generator functions for wxBitmapButton objects
# $Id: codegen.py,v 1.26 2007/08/07 12:18:34 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common, os

class PythonCodeGenerator:
    def get_code(self, obj):
        pygen = common.code_writers['python']
        cn = pygen.cn
        prop = obj.properties
        id_name, id = pygen.generate_code_id(obj) 
        bmp_file = prop.get('bitmap', '')
        bmp_preview_path = os.path.join(common.wxglade_path, "icons",
                                        "icon.xpm")
        if not obj.parent.is_toplevel: parent = 'self.%s' % obj.parent.name
        else: parent = 'self'
        style = prop.get("style")
        if style: style = ", style=%s" % pygen.cn_f(style)
        else: style = ''
        if not bmp_file:
            bmp = cn('wxNullBitmap')
        elif bmp_file.startswith('var:'):
            if obj.preview:
                bmp = "%s('%s', %s)" % (cn('wxBitmap'), bmp_preview_path,
                                        cn('wxBITMAP_TYPE_XPM'))
            else:
                bmp = (cn('wxBitmap') + '(%s,' + cn('wxBITMAP_TYPE_ANY)')) % \
                      bmp_file[4:].strip()
        elif bmp_file.startswith('code:'):
            if obj.preview:
                bmp = "%s('%s', %s)" % (cn('wxBitmap'), bmp_preview_path,
                                        cn('wxBITMAP_TYPE_XPM'))
            else:
                bmp = '(%s)' % \
                      bmp_file[5:].strip()
        else:
            if obj.preview:
                import misc
                bmp_file = misc.get_relative_path(bmp_file, True)
            bmp = (cn('wxBitmap') + '(%s, ' + cn('wxBITMAP_TYPE_ANY') +
                   ')') % pygen.quote_str(bmp_file, False, False)
        init = []
        if id_name: init.append(id_name)
        klass = obj.klass
        if klass == obj.base: klass = cn(klass)
        init.append('self.%s = %s(%s, %s, %s%s)\n' % 
                    (obj.name, klass, parent, id, bmp,style))
        props_buf = pygen.generate_common_properties(obj)

        disabled_bmp = prop.get('disabled_bitmap')
        if disabled_bmp:
            if disabled_bmp.startswith('var:'):
                if not obj.preview:
                    var = disabled_bmp[4:].strip()
                    props_buf.append(
                        ('self.%s.SetBitmapDisabled(' +
                         cn('wxBitmap') +'(%s,' + cn('wxBITMAP_TYPE_ANY') +
                         '))\n') % (obj.name, var))
            elif disabled_bmp.startswith('code:'):
                if not obj.preview:
                    var = disabled_bmp[5:].strip()
                    props_buf.append(
                        ('self.%s.SetBitmapDisabled(' +
                        '(%s))\n') % \
                        (obj.name, var))
            else:
                props_buf.append(('self.%s.SetBitmapDisabled(' +
                                  cn('wxBitmap') + '(%s, ' +
                                  cn('wxBITMAP_TYPE_ANY') + '))\n') % \
                                 (obj.name,
                                  pygen.quote_str(disabled_bmp, False, False)))
                
        if not prop.has_key('size'):
            props_buf.append('self.%s.SetSize(self.%s.GetBestSize())\n' % \
                             (obj.name, obj.name))
        if prop.get('default', False):
            props_buf.append('self.%s.SetDefault()\n' % obj.name)
        return init, props_buf, []

# end of class PythonCodeGenerator


class CppCodeGenerator:
    def get_code(self, obj):
        """\
        fuction that generates C++ code for wxBitmapButton objects.
        """
        cppgen = common.code_writers['C++']
        prop = obj.properties
        id_name, id = cppgen.generate_code_id(obj) 
        if id_name: ids = [ id_name ]
        else: ids = []
        bmp_file = prop.get('bitmap', '')
        if not obj.parent.is_toplevel: parent = '%s' % obj.parent.name
        else: parent = 'this'
        
        extra = ''
        style = prop.get("style")
        if style: extra = ', wxDefaultPosition, wxDefaultSize, %s' % style
        
        if not bmp_file:
            bmp = 'wxNullBitmap'
        elif bmp_file.startswith('var:'):
            bmp = 'wxBitmap(%s, wxBITMAP_TYPE_ANY)' % bmp_file[4:].strip()
        elif bmp_file.startswith('code:'):
            bmp = '(%s)' % bmp_file[5:].strip()
        else:
            bmp = 'wxBitmap(%s, wxBITMAP_TYPE_ANY)' % \
                  cppgen.quote_str(bmp_file, False, False)
        init = [ '%s = new %s(%s, %s, %s%s);\n' % 
                 (obj.name, obj.klass, parent, id, bmp,extra) ]
        props_buf = cppgen.generate_common_properties(obj)

        disabled_bmp = prop.get('disabled_bitmap')
        if disabled_bmp:
            if disabled_bmp.startswith('var:'):
                var = disabled_bmp[4:].strip()
                props_buf.append('%s->SetBitmapDisabled('
                                 'wxBitmap(%s,wxBITMAP_TYPE_ANY));\n' %
                                 (obj.name, var))
            elif disabled_bmp.startswith('code:'):
                var = disabled_bmp[5:].strip()
                props_buf.append('%s->SetBitmapDisabled('
                                 '(%s));\n' % (obj.name, var))
            else:
                props_buf.append(
                    '%s->SetBitmapDisabled('
                    'wxBitmap(%s, wxBITMAP_TYPE_ANY));\n' % \
                    (obj.name, cppgen.quote_str(disabled_bmp, False, False)))
                
        if not prop.has_key('size'):
            props_buf.append('%s->SetSize(%s->GetBestSize());\n' % \
                             (obj.name, obj.name))
        if prop.get('default', False):
            props_buf.append('%s->SetDefault();\n' % obj.name)
        return init, ids, props_buf, []

# end of class CppCodeGenerator


def xrc_code_generator(obj):
    xrcgen = common.code_writers['XRC']
    class BitmapButtonXrcObject(xrcgen.DefaultXrcObject):
        def write_property(self, name, val, outfile, tabs):
            if name == 'disabled_bitmap':
                name = 'disabled'
            xrcgen.DefaultXrcObject.write_property(
                self, name, val, outfile, tabs)

    # end of class BitmapButtonXrcObject

    return BitmapButtonXrcObject(obj)


def initialize():
    common.class_names['EditBitmapButton'] = 'wxBitmapButton'
    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('wxBitmapButton', PythonCodeGenerator())
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('wxBitmapButton', CppCodeGenerator())
    xrcgen = common.code_writers.get("XRC")
    if xrcgen:
        xrcgen.add_widget_handler('wxBitmapButton', xrc_code_generator)
