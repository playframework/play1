# perl_sizers_codegen.py : perl generator functions for the various wxSizerS
# $Id: perl_sizers_codegen.py,v 1.7 2006/12/02 11:20:29 agriggio Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY
#

import common

class PerlBoxSizerBuilder:
    def get_code(self, obj):
        orient = obj.properties.get('orient', 'wxHORIZONTAL')
        init = [
            '$self->{%s} = Wx::BoxSizer->new(%s);\n' % (obj.name, orient)
            ]
        layout = []

        if obj.is_toplevel:
            if not obj.parent.is_toplevel:
                parent = '$self->{%s}' % obj.parent.name
            else:
                parent = '$self'

            #layout.append('%s->SetAutoLayout(1);\n' % parent)
            layout.append('%s->SetSizer($self->{%s});\n' % (parent, obj.name))

            if not obj.parent.properties.has_key('size') and \
                   obj.parent.is_toplevel:
                layout.append('$self->{%s}->Fit(%s);\n' % (obj.name, parent))
            if obj.parent.properties.get('sizehints', False):
                layout.append('$self->{%s}->SetSizeHints(%s);\n'
                              % (obj.name, parent))
        return init, [], layout

# end of class PerlBoxSizerBuilder


class PerlStaticBoxSizerBuilder:
    def get_code(self, obj):
        plgen = common.code_writers['perl']
        orient = obj.properties.get('orient', 'wxHORIZONTAL')
        label = obj.properties.get('label', '')
        if not obj.parent.is_toplevel: parent = '$self->{%s}' % obj.parent.name
        else: parent = '$self'
        init = [
          '$self->{%s_staticbox} = Wx::StaticBox->new(%s, -1, %s );\n'
          % (obj.name, parent, plgen.quote_str(label)), # this get
          '$self->{%s}= Wx::StaticBoxSizer->new($self->{%s_staticbox}, %s);\n'
          % (obj.name,obj.name, orient)
        ]
        layout = []
        if obj.is_toplevel:
            #layout.append('%s->SetAutoLayout(1);\n' % parent)
            layout.append('%s->SetSizer($self->{%s});\n' % (parent, obj.name))
            if not obj.parent.properties.has_key('size') and \
                   obj.parent.is_toplevel:
                layout.append('$self->{%s}->Fit(%s);\n' % (obj.name, parent))
            if obj.parent.properties.get('sizehints', False):
                layout.append('$self->{%s}->SetSizeHints(%s);\n'
                              % (obj.name, parent))
        return init, [], layout

# end of class PerlStaticBoxSizerBuilder


class PerlGridSizerBuilder:
    klass = 'Wx::GridSizer'

    def get_code(self, obj):
        props = obj.properties
        if not obj.parent.is_toplevel:
            parent = '$self->{%s}' % obj.parent.name
        else:
            parent = '$self'
        rows = props.get('rows', '0')
        cols = props.get('cols', '0')
        vgap = props.get('vgap', '0')
        hgap = props.get('hgap', '0')
        init = [
            '$self->{%s} = %s->new(%s, %s, %s, %s);\n' %
                (obj.name, self.klass.replace('wx','Wx::',1), rows,
                    cols, vgap, hgap)
            ]
        layout = []
        if obj.is_toplevel:
            #layout.append('%s->SetAutoLayout(1);\n' % parent)
            layout.append('%s->SetSizer($self->{%s});\n'
                % (parent, obj.name))
            if not obj.parent.properties.has_key('size') and \
                   obj.parent.is_toplevel:
                layout.append('$self->{%s}->Fit(%s);\n' % (obj.name, parent))
            if obj.parent.properties.get('sizehints', False):
                layout.append('$self->{%s}->SetSizeHints(%s);\n'
                              % (obj.name, parent))
        return init, [], layout   

# end of class PerlGridSizerBuilder


class PerlFlexGridSizerBuilder(PerlGridSizerBuilder):
    klass = 'Wx::FlexGridSizer'

    def get_code(self, obj):
        init, p, layout = PerlGridSizerBuilder.get_code(self, obj)
        props = obj.properties
        if props.has_key('growable_rows'):
            for r in props['growable_rows'].split(','):
                layout.append('$self->{%s}->AddGrowableRow(%s);\n' %
                              (obj.name, r.strip()))
        if props.has_key('growable_cols'):
            for r in props['growable_cols'].split(','):
                layout.append('$self->{%s}->AddGrowableCol(%s);\n' %
                              (obj.name, r.strip()))
        return init, p, layout

# end of class PerlFlexGridSizerBuilder


def initialize():
    cn = common.class_names
    cn['EditBoxSizer'] = 'wxBoxSizer'
    cn['EditStaticBoxSizer'] = 'wxStaticBoxSizer'
    cn['EditGridSizer'] = 'wxGridSizer'
    cn['EditFlexGridSizer'] = 'wxFlexGridSizer'

    plgen  = common.code_writers.get("perl")
    if plgen:
        awh = plgen.add_widget_handler
        awh('wxBoxSizer', PerlBoxSizerBuilder())
        awh('wxStaticBoxSizer', PerlStaticBoxSizerBuilder())
        awh('wxGridSizer', PerlGridSizerBuilder())
        awh('wxFlexGridSizer', PerlFlexGridSizerBuilder())
