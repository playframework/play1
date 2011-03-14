#!/usr/bin/env python
# wxglade.py: entry point of wxGlade
# $Id: wxglade.py,v 1.27 2007/08/07 12:18:34 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import os, sys, gettext

t = gettext.translation(domain="wxglade", localedir="locale", fallback=True)
t.install("wxglade")

# check to see if the Python release supports boolean identifiers
# and bool built-in function (>= Python 2.2.1).
try:
    True, False, bool
except NameError:
    setattr(__builtins__, 'True', 1)
    setattr(__builtins__, 'False', not True)
    def bool(value): return not not value
    setattr(__builtins__, 'bool', bool)
# and this is for Python <= 2.3
try:
    sorted
except NameError:
    def sorted(l):
        l = list(l)[:]
        l.sort()
        return l
    setattr(__builtins__, 'sorted', sorted)    


def _fix_path(path):
    """\
    Returns an absolute version of path, accroding to the invoking dir of
    wxglade (which can be different from '.' if it is invoked from a shell
    script)
    """
    if not os.path.isabs(path):
        return os.path.join(os.getcwd(), path)
        #getenv('WXGLADE_INVOKING_DIR', '.'), path)
    return path


def parse_command_line():
    import getopt, common
    try: options, args = getopt.getopt(sys.argv[1:], "g:o:",
                                       ['generate-code=', 'output='])
    except getopt.GetoptError:
        #import traceback; traceback.print_exc()
        usage()
    return options, args


def command_line_code_generation(options, args):
    """\
    starts a code generator without starting the GUI.
    """
    import common
    if not options: usage()
    if not options[0]:
        usage() # a language for code generation must be provided
    if len(args) != 1: usage() # an input file name must be provided
    
    common.use_gui = False # don't import wxPython.wx
    # use_gui has to be set before importing config
    import config
    config.init_preferences()
    common.load_code_writers()
    common.load_widgets()
    common.load_sizers()
    try:
        from xml_parse import CodeWriter
        out_path = None
        language = ''
        for option, arg in options:
            if option == '-g' or option == '--generate-code':
                language = arg
            elif option == '-o' or option == '--output':
                out_path = _fix_path(arg)
        writer = common.code_writers[language]
        CodeWriter(writer, _fix_path(args[0]), out_path=out_path)
    except KeyError:
        print >> sys.stderr, \
              _('Error: no writer for language "%s" available') % language
        sys.exit(1)
    except Exception, e:
        print >> sys.stderr, _("Error: %s") % e
        import traceback; traceback.print_exc()
        sys.exit(1)
    sys.exit(0)


def usage():
    """\
    Prints a help message about the usage of wxGlade from the command line.
    """
    msg = _("""\
wxGlade usage:
- to start the GUI: python wxglade.py [WXG_FILE]
- to generate code from the command line: python wxglade.py OPTIONS... FILE
  OPTIONS are the following:
  -g, --generate-code=LANGUAGE  (required) give the output language
  -o, --output=PATH             (optional) name of the output file (in
                                single-file mode) or directory (in
                                multi-file mode)
    """)
    print msg
    print _('Valid LANGUAGE values:'),
    import common
    common.use_gui = False
    common.load_code_writers()
    for value in common.code_writers: print value,
    print '\n'
    sys.exit(1)


def determine_wxglade_path():
    try:
        root = __file__
        if os.path.islink(root):
            root = os.path.realpath(root)
        return os.path.dirname(os.path.abspath(root))
    except:
        # __file__ is not defined when building an .exe with McMillan
        return os.path.dirname(sys.argv[0])


def run_main():
    """\
    This main procedure is started by calling either wxglade.py or
    wxglade.pyw on windows
    """
    # prepend the widgets dir to the
    # app's search path
    wxglade_path = determine_wxglade_path()
    #sys.path = [os.getcwd(), os.path.join(os.getcwd(), 'widgets')] + sys.path
    sys.path = [wxglade_path, os.path.join(wxglade_path, 'widgets')] + sys.path
    # set the program's path
    import common
    common.wxglade_path = wxglade_path #os.getcwd()
    # before running the GUI, let's see if there are command line options for
    # code generation
    if len(sys.argv) == 1:
        # if there was no option, start the app in GUI mode
        import main
        main.main()
    else:
        options, args = parse_command_line()
        if not options:
            # start the app in GUI mode, opening the given file
            filename = _fix_path(args[0])
            import main
            main.main(filename)
        else:
            command_line_code_generation(options, args)

if __name__ == "__main__":
    run_main()
