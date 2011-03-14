# perl_codegen.py : perl generator functions for wxMenuBar objects
# $Id: perl_codegen.py,v 1.11 2007/06/23 10:57:58 agriggio Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common
from MenuTree import *
from codegen import MenuHandler

class PerlCodeGenerator:
    def get_properties_code(self, obj):
        return []
        
    def get_init_code(self, obj):
        prop = obj.properties
        plgen = common.code_writers['perl']
        out = []
        append = out.append
        menus = obj.properties['menubar']
        ids = []
        # We need to keep track of tmpnames used.
        tmpsused = {}

        def append_items(menu, items):
            for item in items:
                if item.name == '---': # item is a separator
                    append('%s->AppendSeparator();\n' % menu)
                    continue
                name, val = plgen.generate_code_id(None, item.id)
                if not name and ( not val or val == '-1'):
                    id = 'Wx::NewId()'
                else:
                    if name: ids.append(name)
                    id = val


                if item.children:
                    if item.name:
                        name = item.name
                    else:
                        name = '%s_sub' % menu
                        if not tmpsused.has_key(name):
                            tmpsused[name] = 1
                            append('my %s;\n' % name)

                    append('%s = Wx::Menu->new();\n' % name)
                    append_items(name, item.children)
                    append('%s->Append(%s, %s, %s, %s);\n' %
                           (menu, id, plgen.quote_str(item.label),
                            name, plgen.quote_str(item.help_str)))
                else:
                    item_type = 0
                    if item.checkable == '1':
                        item_type = 1
                    elif item.radio == '1':
                        item_type = 2
                        
                    if item.name: itemname = '$self->{%s} = ' % item.name
                    else: itemname = ''
                    
                    if item_type:
                        append('%s%s->Append(%s, %s, %s, %s);\n' %
                               (itemname, menu, id, plgen.quote_str(item.label),
                                plgen.quote_str(item.help_str), item_type))
                    else:

                        append('%s%s->Append(%s, %s, %s);\n' %
                               (itemname, menu, id, plgen.quote_str(item.label),
                                plgen.quote_str(item.help_str)))
        #print 'menus = %s' % menus

        if obj.is_toplevel: obj_name = '$self'
        else: obj_name = '$self->{%s}' % obj.name

        append('my $wxglade_tmp_menu;\n') # NOTE below name =
        for m in menus:
            menu = m.root
            if menu.name: name = '$self->{%s}' % menu.name
            else: name = '$wxglade_tmp_menu'
            append('%s = Wx::Menu->new();\n' % name)
            if menu.children:
                append_items(name, menu.children)
            append('%s->Append(%s, %s);\n' %
                   (obj_name, name, plgen.quote_str(menu.label)))

        return ids + out

    def get_code(self, obj):
        """\
        function that generates Perl code for the menubar of a wxFrame.
        """
        klass = obj.base;
        if klass != obj.klass : klass = obj.klass; 
        else: klass = klass.replace('wx','Wx::',1);

        plgen = common.code_writers['perl']
        init = [ '\n\n', '# Menu Bar\n\n', '$self->{%s} = %s->new();\n' %
                 (obj.name, klass) ]
##                  '$self->SetMenuBar($self->{%s});\n' % obj.name ]
        init.extend(self.get_init_code(obj))
        init.append('$self->SetMenuBar($self->{%s});\n' % obj.name)
        init.append('\n# Menu Bar end\n\n')
        return init, [], []

    # 2004-12-05
    def get_events(self, obj):
        pygen = common.code_writers['perl']
        cn = pygen.cn
        out = []

        #print 'get_events', obj.properties['menubar']

        def do_get(item):
            ret = []
            if item.name:
                #val = '#self.%s' % item.name # see py_codegen.py, ~480
                val = item.name
            else:
                name, val = pygen.generate_code_id(None, item.id)
                if not val: val = '-1' # but this is wrong anyway...
            if item.handler:
                ret.append((val, 'EVT_MENU', item.handler))
            if item.children:
                for c in item.children:
                    ret.extend(do_get(c))
            return ret
        
        for menu in obj.properties['menubar']:
            out.extend(do_get(menu.root))
        return out

# end of class PerlCodeGenerator


def initialize():
    common.class_names['EditMenuBar'] = 'wxMenuBar'
    common.toplevels['EditMenuBar'] = 1

    plgen = common.code_writers.get('perl')
    if plgen:
        plgen.add_widget_handler('wxMenuBar', PerlCodeGenerator())
        plgen.add_property_handler('menus', MenuHandler)
