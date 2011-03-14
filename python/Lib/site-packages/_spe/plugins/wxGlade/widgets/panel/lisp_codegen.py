# lisp_codegen.py : lisp generator functions for wxPanel objects
# $Id: lisp_codegen.py,v 1.2 2007/08/07 12:15:21 agriggio Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common

class LispCodeGenerator:
#wxScrolledWindow(parent, id, pos, size, style, name )
    new_signature = [
        '$parent', '$id', '$pos', '$size', '$style', '$name'
    ]

    def get_code(self, panel):
        plgen = common.code_writers['lisp']
        prop = panel.properties
        try:
            scrollable = int(prop['scrollable'])
        except:
            scrollable = False

        id_name, id = plgen.generate_code_id(panel)
        if not panel.parent.is_toplevel:
            parent = '(slot-%s obj)' % panel.parent.name
        else:
            parent = '(slot-top-window obj)'

        if panel.is_toplevel:
            l = []
            if id_name: l.append(id_name)

            l.append('(setf (slot-%s obj) (wxPanel_Create %s %s -1 -1 -1 -1))\n'
                     % (panel.name, parent, id))
            return l, [], []

        init = []
        if id_name: init.append(id_name)
        style = prop.get("style", 'wxTAB_TRAVERSAL')
        if not( scrollable or style != 'wxTAB_TRAVERSAL' ):
            style = 'wxTAB_TRAVERSAL'
        else:
            style = style.strip().replace('|',' ')
            if style.find(' ') != -1:
                style = '(logior %s)' % style


        init.append('(setf (slot-%s obj) '
                    '(wxPanel_Create %s %s -1 -1 -1 -1 %s))\n'
                    % (panel.name, parent, id, style))

        props_buf = plgen.generate_common_properties(panel)
        if scrollable:
            sr = prop.get('scroll_rate', '0 0')
            sr = sr.replace(',',' ')
            props_buf.append('(wxScrolledWindow:wxScrolledWindow_SetScrollRate'
                             ' (slot-%s obj) %s)\n' % (panel.name, sr))
        return init, props_buf, []

    def get_properties_code(self, obj):
        plgen = common.code_writers['lisp']
        prop = obj.properties
        try:
            scrollable = int(prop['scrollable'])
        except:
            scrollable = False

        props_buf = plgen.generate_common_properties(obj)
        if scrollable:
            sr = prop.get('scroll_rate', '0 0')
            props_buf.append('(wxScrolledWindow:wxScrolledWindow_SetScrollRate '
                             '(slot-%s obj))\n' % sr)
        return props_buf

# end of class LispCodeGenerator


def initialize():
    common.class_names['EditPanel'] = 'wxPanel'
    common.class_names['EditTopLevelPanel'] = 'wxPanel'
    common.toplevels['EditPanel'] = 1
    common.toplevels['EditTopLevelPanel'] = 1

    plgen = common.code_writers.get('lisp')
    if plgen:
        plgen.add_widget_handler('wxPanel', LispCodeGenerator())
        plgen.add_widget_handler('wxScrolledWindow', LispCodeGenerator())
