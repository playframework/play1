# lisp_sizers_codegen.py : lisp generator functions for the various wxSizerS
# $Id: lisp_sizers_codegen.py,v 1.2 2006/12/02 11:20:29 agriggio Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY
#

import common

class LispBoxSizerBuilder:
    def get_code(self, obj):
        orient = obj.properties.get('orient', 'wxHORIZONTAL')
        init = [
            '(setf (slot-%s obj) (wxBoxSizer_Create  %s))\n' % (obj.name, orient)
            ]
        layout = []

        if obj.is_toplevel:
            if not obj.parent.is_toplevel:
                parent = '(slot-%s obj)' % obj.parent.name
            else:
                parent = '(slot-top-window obj)'

            #layout.append('(wxWindow_SetAutoLayout %s 1)\n' % parent)
            layout.append('(wxWindow_SetSizer %s (slot-%s obj))\n' % (parent, obj.name))

            if not obj.parent.properties.has_key('size') and \
                   obj.parent.is_toplevel:
                layout.append('(wxSizer_Fit (slot-%s obj) %s)\n' % (obj.name, parent))
            if obj.parent.properties.get('sizehints', False):
                layout.append('(wxSizer_SetSizeHints (slot-%s obj) %s)\n'
                              % (obj.name, parent))
        return init, [], layout

# end of class LispBoxSizerBuilder


class LispStaticBoxSizerBuilder:
    def get_code(self, obj):
        plgen = common.code_writers['lisp']
        orient = obj.properties.get('orient', 'wxHORIZONTAL')
        label = obj.properties.get('label', '')
        if not obj.parent.is_toplevel: parent = '(slot-%s obj)' % obj.parent.name
        else: parent = '(slot-frame obj)'
        init = [
          '(setf (slot-%s obj) (StaticBoxSizer_Create (wxStaticBox:wxStaticBox_Create %s %s) %s))\n'
          % (obj.name, parent, plgen.quote_str(label), orient)
        ]
        layout = []
        if obj.is_toplevel:
            #layout.append('(wxWindow_SetAutoLayout %s 1)\n' % parent)
            layout.append('(wxWindow_SetSizer %s (slot-%s obj))\n' % (parent, obj.name))
            if not obj.parent.properties.has_key('size') and \
                   obj.parent.is_toplevel:
                layout.append('(wxSizer_Fit (slot-%s obj) %s)\n' % (obj.name, parent))
            if obj.parent.properties.get('sizehints', False):
                layout.append('(wxSizer_SetSizeHints (slot-%s obj) %s)\n'
                              % (obj.name, parent))
        return init, [], layout

# end of class LispStaticBoxSizerBuilder


class LispGridSizerBuilder:
    klass = 'Wx::GridSizer'

    def get_code(self, obj):
        props = obj.properties
        if not obj.parent.is_toplevel:
            parent = '(slot-%s obj)' % obj.parent.name
        else:
                parent = '(slot-frame obj)'
        rows = props.get('rows', '0')
        cols = props.get('cols', '0')
        vgap = props.get('vgap', '0')
        hgap = props.get('hgap', '0')
        init = [
            '(setf (slot-%s obj) (wxGridSizer_Create %s %s %s %s))\n' %
                (obj.name, rows, cols, vgap, hgap)
            ]
        layout = []
        if obj.is_toplevel:
            #layout.append('(wxWindow_SetAutoLayout %s 1)\n' % parent)
            layout.append('(wxWindow_SetSizer %s (slot-%s obj))\n' % (parent, obj.name))
            if not obj.parent.properties.has_key('size') and \
                   obj.parent.is_toplevel:
                layout.append('(wxSizer_Fit (slot-%s obj) %s)\n' % (obj.name, parent))
            if obj.parent.properties.get('sizehints', False):
                layout.append('(wxSizer_SetSizeHints (slot-%s obj) %s)\n'
                              % (obj.name, parent))
        return init, [], layout   

# end of class LispGridSizerBuilder


class LispFlexGridSizerBuilder(LispGridSizerBuilder):
    klass = 'Wx::FlexGridSizer'

    def get_code(self, obj):
        init, p, layout = LispGridSizerBuilder.get_code(self, obj)
        props = obj.properties
        if props.has_key('growable_rows'):
            for r in props['growable_rows'].split(','):
                layout.append('(wxFlexGridSizer_AddGrowableRow (slot-%s obj) %s)\n' %
                              (obj.name, r.strip()))
        if props.has_key('growable_cols'):
            for r in props['growable_cols'].split(','):
                layout.append('(wxFlexGridSizer_AddGrowableCol (slot-%s obj) %s)\n' %
                              (obj.name, r.strip()))
        return init, p, layout

# end of class LispFlexGridSizerBuilder


def initialize():
    cn = common.class_names
    cn['EditBoxSizer'] = 'wxBoxSizer'
    cn['EditStaticBoxSizer'] = 'wxStaticBoxSizer'
    cn['EditGridSizer'] = 'wxGridSizer'
    cn['EditFlexGridSizer'] = 'wxFlexGridSizer'

    plgen  = common.code_writers.get("lisp")
    if plgen:
        awh = plgen.add_widget_handler
        awh('wxBoxSizer', LispBoxSizerBuilder())
        awh('wxStaticBoxSizer', LispStaticBoxSizerBuilder())
        awh('wxGridSizer', LispGridSizerBuilder())
        awh('wxFlexGridSizer', LispFlexGridSizerBuilder())
