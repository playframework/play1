# codegen.py: code generator functions for spacers
# $Id: codegen.py,v 1.9 2007/03/27 07:01:54 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import common


class PythonCodeGenerator:
    def get_code(self, spacer):
        prop = spacer.properties
        width = prop.get('width', '0')
        height = prop.get('height', '0')
        # we must use the hack in pygen.add_sizeritem (see py_codegen.py)
        spacer.name = '%s, %s' % (width, height)
        return [], [], []

# end of class PythonCodeGenerator


class CppCodeGenerator:
    def get_code(self, spacer):
        """\
        generates the C++ code for a spacer
        """
        prop = spacer.properties
        width = prop.get('width', '0')
        height = prop.get('height', '0')
        # we must use the hack in cppgen.add_sizeritem (see cpp_codegen.py)
        spacer.name = '%s, %s' % (width, height)
        return [], [], [], []

# end of class CppCodeGenerator


def initialize():
    common.class_names['EditSpacer'] = 'spacer'

    # python code generation functions
    pygen = common.code_writers.get('python')
    if pygen:
        pygen.add_widget_handler('spacer', PythonCodeGenerator())
    cppgen = common.code_writers.get('C++')
    if cppgen:
        cppgen.add_widget_handler('spacer', CppCodeGenerator())
