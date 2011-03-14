# cpp_codegen.py: C++ code generator
# $Id: cpp_codegen.py,v 1.49 2007/03/30 06:37:53 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import sys, os, os.path
import common, config
import cStringIO, re
from xml_parse import XmlParsingError

# these two globals must be defined for every code generator module
language = 'C++'
writer = sys.modules[__name__] # the writer is the module itself

# default extensions for generated files: a list of file extensions
default_extensions = ['h', 'cpp']

"""\
dictionary that maps the lines of code of a class to the name of such class:
the lines are divided in 3 categories: lines in the constructor,
'set_properties' and 'do_layout'
"""
classes = None

"""\
dictionary of ``writers'' for the various objects. These are objects that must
implement the WidgetHandler interface (see below)
"""
obj_builders = {}

# random number used to be sure that the replaced tags in the sources are
# the right ones (see SourceFileContent and add_class)
nonce = None

# lines common to all the generated files (include of <wx/wx.h>, ...)
header_lines = []

# if True, generate a file for each custom class
multiple_files = False

# if not None, they are the header and source file to write into
output_header, output_source = None, None
# if not None, name (without extension) of the file to write into
output_name = None

# if not None, it is the directory inside which the output files are saved
out_dir = None


# ALB 2004-12-05: wx version we are generating code for
for_version = (2, 4)


class ClassLines:
    """\
    Stores the lines of python code for a custom class
    """
    def __init__(self):
        self.init = [] # lines of code to insert in the constructor
        self.parents_init = [] # lines of code to insert in the constructor for
                               # container widgets (panels, splitters, ...)
        self.ids = [] # ids declared in the source (to use for Event handling):
                      # these are grouped together into a public enum in
                      # the custom class
        self.sizers_init = [] # lines related to sizer objects declarations
        self.props = [] # lines to insert in the __set_properties method
        self.layout = [] # lines to insert in the __do_layout method
        self.sub_objs = [] # list of 2-tuples (type, name) of the
                           # sub-objects which are attributes of the
                           # toplevel object
        
        self.dependencies = [] # names of the modules this class depends on
        self.done = False # if True, the code for this class has already
                          # been generated

        # ALB 2004-12-08
        self.event_handlers = [] # lines to bind events

        self.extra_code_h = [] # extra code to output
        self.extra_code_cpp = []

# end of class ClassLines


class SourceFileContent:
    """\
    Keeps info about an existing file that has to be updated, to replace only
    the lines inside a wxGlade block, an to keep the rest of the file as it was
    """
    def __init__(self, name):
        self.name = name # name of the file without extension
                         # (both header and .cpp)
        self.header_content = None # content of the header file
        self.source_content = None
        self.classes = {} # classes declared in the file
        self.new_classes = [] # new classes to add to the file (they are
                              # inserted BEFORE the old ones)

        # ALB 2004-12-08
        self.event_handlers = {} # list of event handlers for each class
        self.event_table_decl = {}
        self.event_table_def = {}
        self.end_class_re = re.compile('^\s*};\s*//\s+wxGlade:\s+end class\s*$')

        if classes is None: self.classes = {}
        self.build_untouched_content()

    def build_untouched_content(self):
        """\
        Builds a string with the contents of the file that must be left as is,
        and replaces the wxGlade blocks with tags that in turn will be replaced
        by the new wxGlade blocks
        """
        self._build_untouched(self.name + '.h', True)
        self._build_untouched(self.name + '.cpp', False)

    def _build_untouched(self, filename, is_header):
        class_name = None
        new_classes_inserted = False
        # regexp to match class declarations (this isn't very accurate -
        # doesn't match template classes, nor virtual inheritance, but
        # should be enough for most cases)
        class_decl = re.compile(r'^\s*class\s+([a-zA-Z_]\w*)\s*')
##                                 '(:\s*(public|protected|private)?\s+[\w:]+'
##                                 '(,\s*(public|protected|private)?\s+[\w:]+)*'
##                                 ')?')
        # regexps to match wxGlade blocks
        block_start = re.compile(r'^\s*//\s*begin\s+wxGlade:\s*'
                                 '(\w*)::(\w+)\s*$')
        block_end = re.compile(r'^\s*//\s*end\s+wxGlade\s*$')
        # regexp to match event handlers
        # ALB 2004-12-08
        event_handler = re.compile(r'^\s*(?:virtual\s+)?'
                                   'void\s+([A-Za-z_]+\w*)\s*'
                                   '\([A-Za-z_:0-9]+\s*&\s*\w*\)\s*;\s*'
                                   '//\s*wxGlade:\s*<event_handler>\s*$')
        decl_event_table = re.compile(r'^\s*DECLARE_EVENT_TABLE\s*\(\s*\)'
                                      '\s*;?\s*$')
        def_event_table = re.compile(r'^\s*BEGIN_EVENT_TABLE\s*\(\s*(\w+)\s*,'
                                     '\s*(\w+)\s*\)\s*$')
        event_handlers_marker = re.compile(r'^\s*//\s*wxGlade:\s*add\s+'
                                           '((?:\w|:)+)\s+event handlers\s*$')
        prev_was_handler = False
        events_tag_added = False

        inside_block = False
        inside_comment = False
        tmp_in = open(filename)
        out_lines = []
        for line in tmp_in:
            comment_index = line.find('/*')
            if not inside_comment and comment_index != -1 \
                   and comment_index > line.find('//'):
                inside_comment = True
            if inside_comment:
                end_index = line.find('*/')
                if end_index > comment_index: inside_comment = False
            if not is_header: result = None
            else: result = class_decl.match(line)
            if not inside_comment and not inside_block and result is not None:
                if class_name is None:
                    # this is the first class declared in the file: insert the
                    # new ones before this
                    out_lines.append('<%swxGlade insert new_classes>' %
                                     nonce)
                    new_classes_inserted = True
                class_name = result.group(1)
##                 print 'OK:', class_name
                self.classes[class_name] = 1 # add the found class to the list
                                             # of classes of this module
                out_lines.append(line)
            elif not inside_block:
                result = block_start.match(line)
                if not inside_comment and result is not None:
                    # replace the lines inside a wxGlade block with a tag that
                    # will be used later by add_class
                    inside_block = True
                    out_lines.append('<%swxGlade replace %s %s>' % \
                                     (nonce, result.group(1), result.group(2)))
                else:
                    dont_append = False
                    
                    # ALB 2004-12-08 event handling support...
                    if is_header and not inside_comment:
                        result = event_handler.match(line)
                        if result is not None:
                            prev_was_handler = True
                            which_handler = result.group(1)
                            which_class = class_name #result.group(2)
                            self.event_handlers.setdefault(
                                which_class, {})[which_handler] = 1
                        else:
                            if prev_was_handler:
                                # add extra event handlers here...
                                out_lines.append('<%swxGlade event_handlers %s>'
                                                 % (nonce, class_name))
                                prev_was_handler = False
                                events_tag_added = True
                            elif not events_tag_added and \
                                     self.is_end_of_class(line):
                                out_lines.append('<%swxGlade event_handlers %s>'
                                                 % (nonce, class_name))
                            # now try to see if we already have a
                            # DECLARE_EVENT_TABLE
                            result = decl_event_table.match(line)
                            if result is not None:
                                self.event_table_decl[class_name] = True
                    elif not inside_comment:
                        result = event_handlers_marker.match(line)
                        if result is not None:
                            out_lines.append('<%swxGlade add %s event '
                                             'handlers>' % \
                                             (nonce, result.group(1)))
                            dont_append = True
                        result = def_event_table.match(line)
                        if result is not None:
                            which_class = result.group(1)
                            self.event_table_def[which_class] = True
                    # ----------------------------------------
                    
                    if not dont_append:
                        out_lines.append(line)
            else:
                # ignore all the lines inside a wxGlade block
                if block_end.match(line) is not None:
                    inside_block = False
        if is_header and not new_classes_inserted:
            # if we are here, the previous ``version'' of the file did not
            # contain any class, so we must add the new_classes tag at the
            # end of the file
            out_lines.append('<%swxGlade insert new_classes>' % nonce)
        tmp_in.close()
        # set the ``persistent'' content of the file
        if is_header: self.header_content = "".join(out_lines)
        else: self.source_content = "".join(out_lines)
        
    def is_end_of_class(self, line):
        # not really, but for wxglade-generated code it should work...
        return self.end_class_re.match(line) is not None #[:2] == '};'

# end of class SourceFileContent

# if not None, it is an instance of SourceFileContent that keeps info about
# the previous version of the source to generate
previous_source = None 


def tabs(number):
    return '    ' * number


# if True, overwrite any previous version of the source file instead of
# updating only the wxGlade blocks
_overwrite = False

# if True, enable gettext support
_use_gettext = False

_quote_str_pattern = re.compile(r'\\[natbv"]?')
def _do_replace(match):
    if match.group(0) == '\\': return '\\\\'
    else: return match.group(0)

def quote_str(s, translate=True, escape_chars=True):
    """\
    returns a quoted version of 's', suitable to insert in a C++ source file
    as a string object. Takes care also of gettext support
    """
    if not s: return 'wxEmptyString'
    s = s.replace('"', r'\"')
    if escape_chars: s = _quote_str_pattern.sub(_do_replace, s)
    else: s = s.replace('\\', r'\\')
    if _use_gettext and translate: return '_("' + s + '")'
    else: return 'wxT("' + s + '")'


def initialize(app_attrs):
    """\
    Writer initialization function.
    See py_codegen.initialize for a description of the parameter.
    """
    out_path = app_attrs['path']
    multi_files = app_attrs['option']
    
    global classes, header_lines, multiple_files, previous_source, nonce, \
           _use_gettext, _overwrite, _last_generated_id, \
           _current_extra_code_h, _current_extra_code_cpp
    import time, random

    _last_generated_id = 1000

    try: _use_gettext = int(app_attrs['use_gettext'])
    except (KeyError, ValueError): _use_gettext = False

    # overwrite added 2003-07-15
    try: _overwrite = int(app_attrs['overwrite'])
    except (KeyError, ValueError): _overwrite = False

    # ALB 2004-12-05
    global for_version
    try:
        for_version = tuple([int(t) for t in
                             app_attrs['for_version'].split('.')[:2]])
    except (KeyError, ValueError):
        if common.app_tree is not None:
            for_version = common.app_tree.app.for_version
        else:
            for_version = (2, 4) # default...

    # this is to be more sure to replace the right tags
    nonce = '%s%s' % (str(time.time()).replace('.', ''),
                      random.randrange(10**6, 10**7))

    classes = {}
    header_lines = ['// -*- C++ -*- generated by wxGlade %s on %s%s\n\n' % \
                    (common.version, time.asctime(), common.generated_from()),
                    '#include <wx/wx.h>\n', '#include <wx/image.h>\n']
    if not config.preferences.write_timestamp:
        header_lines[0] = '// -*- C++ -*- generated by wxGlade %s%s\n\n' % \
                          (common.version, common.generated_from())

    # extra lines to generate (see the 'extracode' property of top-level
    # widgets)
    _current_extra_code_h = []
    _current_extra_code_cpp = []
    
    multiple_files = multi_files
    if not multiple_files:
        global output_header, output_source, output_name
        name, ext = os.path.splitext(out_path)
        output_name = name
        if not _overwrite and os.path.isfile(name + '.h'):
            # the file exists, we must keep all the lines not inside a wxGlade
            # block. NOTE: this may cause troubles if out_path is not a valid
            # C++ file, so be careful!
            previous_source = SourceFileContent(name)
        else:
            previous_source = None
            output_header = cStringIO.StringIO() 
            output_source = cStringIO.StringIO() 
            for line in header_lines:
                output_header.write(line)
                #output_source.write(line)
            # isolation directives
            oh = os.path.basename(name + '.h').upper().replace('.', '_')
            # extra headers
            #for val in _obj_headers.itervalues():
##             for handler in obj_builders.itervalues():
##                 for header in getattr(handler, 'extra_headers', []):
##                     output_header.write('#include %s\n' % header)
            # now, write the tag to store dependencies
            output_header.write('<%swxGlade replace  dependencies>\n' % nonce)

            output_header.write('\n#ifndef %s\n#define %s\n' % (oh, oh))
            output_header.write('\n')
            # write the tag to store extra code
            output_header.write('\n<%swxGlade replace  extracode>\n' % nonce)

            output_source.write(header_lines[0])
            output_source.write('#include "%s%s"\n\n' % \
                                (os.path.basename(name), '.h'))
            output_source.write('<%swxGlade replace  extracode>\n\n' % nonce)
    else:
        previous_source = None
        global out_dir
        if not os.path.isdir(out_path):
            raise IOError("'path' must be a directory when generating"\
                          " multiple output files")
        out_dir = out_path


def finalize():
    """\
    Writer ``finalization'' function: flushes buffers, closes open files, ...
    """
    if previous_source is not None:
        # insert all the new custom classes inside the old file
        tag = '<%swxGlade insert new_classes>' % nonce
        if previous_source.new_classes:
            code = "".join([ c[0] for c in previous_source.new_classes])
        else:
            code = ""
        header_content = previous_source.header_content.replace(tag, code)
        extra_source = "".join([ c[1] for c in previous_source.new_classes])
        source_content = previous_source.source_content

        # extra code (see the 'extracode' property of top-level widgets)
        tag = '<%swxGlade replace  extracode>' % nonce
        code = "\n".join(['// begin wxGlade: ::extracode'] +
                         _current_extra_code_h +
                         ['// end wxGlade\n'])
        header_content = header_content.replace(tag, code)
        code = "\n".join(['// begin wxGlade: ::extracode'] +
                         _current_extra_code_cpp +
                         ['// end wxGlade\n'])
        source_content = source_content.replace(tag, code)
        # --------------------------------------------------------------
        
        # now remove all the remaining <123415wxGlade ...> tags from the
        # source: this may happen if we're not generating multiple files,
        # and one of the container class names is changed
        tags = re.findall('(<%swxGlade replace ([a-zA-Z_]*\w*) (\w+)>)' %
                          nonce, header_content)
        for tag in tags:
            if tag[2] == 'dependencies':
                #print 'writing dependencies'
                deps = []
                for code in classes.itervalues():
                    deps.extend(code.dependencies)
                tmp = ["// begin wxGlade: ::dependencies\n"]
                for dep in _unique(deps):
                    if dep and ('"' != dep[0] != '<'):
                        tmp.append('#include "%s.h"\n' % dep)
                    else:
                        tmp.append('#include %s\n' % dep)
                tmp.append("// end wxGlade\n")
                lines = "".join(tmp)
            elif tag[2] == 'methods':
                lines = '%svoid set_properties();\n%svoid do_layout();\n' \
                        % (tabs(1), tabs(1))
            else:
                lines = '// content of this block (%s) not found: ' \
                        'did you rename this class?\n' % tag[2]
            header_content = header_content.replace(tag[0], lines)
            
        tags = re.findall('(<%swxGlade replace ([a-zA-Z_]\w*) +(\w+)>)' %
                          nonce, source_content)
        for tag in tags:
            comment = '// content of this block not found: ' \
                      'did you rename this class?\n'
            source_content = source_content.replace(tag[0], comment)

        # ALB 2004-12-08
        tags = re.findall('<%swxGlade event_handlers \w+>' % nonce,
                          header_content)
        for tag in tags:
            header_content = header_content.replace(tag, "")
        tags = re.findall('<%swxGlade add \w+ event_handlers>' % nonce,
                          source_content)
        for tag in tags:
            source_content = source_content.replace(tag, "")

        # write the new file contents to disk
        common.save_file(previous_source.name + '.h', header_content,
                         'codegen')
        common.save_file(previous_source.name + '.cpp',
                         source_content + '\n\n' + extra_source,
                         'codegen')
        
    elif not multiple_files:
        oh = os.path.basename(output_name).upper() + '_H'
        output_header.write('\n#endif // %s\n' % oh)
        # write the list of include files
        header_content = output_header.getvalue()
        source_content = output_source.getvalue()
        tags = re.findall('<%swxGlade replace  dependencies>' %
                          nonce, header_content)
        deps = []
        for code in classes.itervalues():
            deps.extend(code.dependencies)
        tmp = ["// begin wxGlade: ::dependencies\n"]
        for dep in _unique(deps):
            if dep and ('"' != dep[0] != '<'):
                tmp.append('#include "%s.h"\n' % dep)
            else:
                tmp.append('#include %s\n' % dep)
        tmp.append("// end wxGlade\n")
        header_content = header_content.replace(
            '<%swxGlade replace  dependencies>' % nonce, "".join(tmp))

        # extra code (see the 'extracode' property of top-level widgets)
        tag = '<%swxGlade replace  extracode>' % nonce
        code = "\n".join(['// begin wxGlade: ::extracode'] +
                         _current_extra_code_h +
                         ['// end wxGlade\n'])
        header_content = header_content.replace(tag, code)
        code = "\n".join(['// begin wxGlade: ::extracode'] +
                         _current_extra_code_cpp +
                         ['// end wxGlade\n'])
        source_content = source_content.replace(tag, code)
        # --------------------------------------------------------------        
            
        common.save_file(output_name + '.h', header_content, 'codegen')
        common.save_file(output_name + '.cpp', source_content, 'codegen')


def test_attribute(obj):
    """\
    Returns True if 'obj' should be added as an attribute of its parent's
    class, False if it should be created as a local variable of __do_layout.
    To do so, tests for the presence of the special property 'attribute'
    """
    try: return int(obj.properties['attribute'])
    except (KeyError, ValueError): return True # this is the default


def add_object(top_obj, sub_obj):
    """\
    adds the code to build 'sub_obj' to the class body of 'top_obj'.
    """
    try: klass = classes[top_obj.klass]
    except KeyError: klass = classes[top_obj.klass] = ClassLines()
    try: builder = obj_builders[sub_obj.base]
    except KeyError:
        # no code generator found: write a comment about it
        klass.init.extend(['\n', '// code for %s (type %s) not generated: '
                           'no suitable writer found' % (sub_obj.name,
                                                         sub_obj.klass),'\n'])
    else:
        try:
            init, ids, props, layout = builder.get_code(sub_obj)
            #builder(sub_obj)
        except:
            print sub_obj
            raise
        if sub_obj.in_windows: # the object is a wxWindow instance
            # --- patch 2002-08-26 ------------------------------------------
            if sub_obj.is_container and not sub_obj.is_toplevel:
                init.reverse()
                klass.parents_init.extend(init)
            else: klass.init.extend(init)
            # ---------------------------------------------------------------
            # -- ALB 2004-12-08 ---------------------------------------------
            if hasattr(builder, 'get_events'):
                klass.event_handlers.extend(builder.get_events(sub_obj))
            elif 'events' in sub_obj.properties:
                id_name, id = generate_code_id(sub_obj)
                #if id == '-1': id = 'self.%s.GetId()' % sub_obj.name
                for event, handler in sub_obj.properties['events'].iteritems():
                    klass.event_handlers.append((id, event, handler))
            # ---------------------------------------------------------------
            # try to see if there's some extra code to add to this class
            extra_code = getattr(builder, 'extracode',
                                 sub_obj.properties.get('extracode', ""))
            if extra_code:
                extra_code = re.sub(r'\\n', '\n', extra_code)
                extra_code = re.split(re.compile(r'^###\s*$', re.M),
                                      extra_code, 1)
                klass.extra_code_h.append(extra_code[0])
                if len(extra_code) > 1:
                    klass.extra_code_cpp.append(extra_code[1])
                # if we are not overwriting existing source, warn the user
                # about the presence of extra code
                if multiple_files:
                    warn = False
                else:
                    warn = previous_source is not None
                if warn:
                    common.message(
                        'WARNING',
                        '%s has extra code, but you are '
                        'not overwriting existing sources: please check '
                        'that the resulting code is correct!' % \
                        sub_obj.name)
            # -----------------------------------------------------------
            

            klass.ids.extend(ids)
            if sub_obj.klass != 'spacer':
                # attribute is a special property which control whether
                # sub_obj must be accessible as an attribute of top_obj,
                # or as a local variable in the do_layout method
                if test_attribute(sub_obj):
                    klass.sub_objs.append( (sub_obj.klass, sub_obj.name) )
        else: # the object is a sizer
            # ALB 2004-09-17: workaround (hack) for static box sizers...
            if sub_obj.base == 'wxStaticBoxSizer':
                klass.sub_objs.insert(0, ('wxStaticBox',
                                          '%s_staticbox' % sub_obj.name))
                klass.parents_init.insert(1, init.pop(0))
            klass.sizers_init.extend(init)
        klass.props.extend(props)
        klass.layout.extend(layout)
        if multiple_files and \
               (sub_obj.is_toplevel and sub_obj.base != sub_obj.klass):
            #print top_obj.name, sub_obj.name
            klass.dependencies.append(sub_obj.klass)
        else:
##             headers = _obj_headers.get(sub_obj.base, [])
            if sub_obj.base in obj_builders:
                headers = getattr(obj_builders[sub_obj.base],
                                  'extra_headers', [])
                klass.dependencies.extend(headers)


def add_sizeritem(toplevel, sizer, obj, option, flag, border):
    """\
    writes the code to add the object 'obj' to the sizer 'sizer'
    in the 'toplevel' object.
    """
    try: klass = classes[toplevel.klass]
    except KeyError: klass = classes[toplevel.klass] = ClassLines()
    name = obj.name
    if obj.base == 'wxNotebook' and for_version < (2, 5):
        name = 'new wxNotebookSizer(%s)' % obj.name
    buffer = '%s->Add(%s, %s, %s, %s);\n' % \
             (sizer.name, name, option, flag, border)
    klass.layout.append(buffer)


def add_class(code_obj):
    """\
    Generates the code for a custom class.
    """
    if classes.has_key(code_obj.klass) and classes[code_obj.klass].done:
        return # the code has already been generated

    if not multiple_files:
        # in this case, previous_source is the SourceFileContent instance
        # that keeps info about the single file to generate
        prev_src = previous_source
    else:
        # let's see if the file to generate exists, and in this case
        # create a SourceFileContent instance
        filename = os.path.join(out_dir,
                                code_obj.klass.replace('::', '_') + '.h')
        if _overwrite or not os.path.exists(filename):
            prev_src = None
        else:
            prev_src = SourceFileContent(os.path.join(out_dir, code_obj.klass))

    if prev_src is not None and prev_src.classes.has_key(code_obj.klass):
        # this class wasn't in the previous version of the source (if any)
        is_new = False 
    else:
        is_new = True

    header_buffer = []
    source_buffer = []
    hwrite = header_buffer.append
    swrite = source_buffer.append

    if not classes.has_key(code_obj.klass):
        # if the class body was empty, create an empty ClassLines
        classes[code_obj.klass] = ClassLines()


##     # first thing to do, call the property writer: we do this here because it
##     # is admissible for the property code generator to have side effects (i.e.
##     # to operate on the ClassLines instance): this is actually done in the
##     # toplevel menubar
##     props_builder = obj_properties.get(code_obj.base)
##     write_body = len(classes[code_obj.klass].props)
##     if props_builder:
##         obj_p = props_builder(code_obj)#obj_properties[code_obj.base](code_obj)
##         if not write_body: write_body = len(obj_p)
##     else: obj_p = []

    try:
        builder = obj_builders[code_obj.base]
    except KeyError:
        print code_obj
        raise # this shouldn't happen

    # try to see if there's some extra code to add to this class
    extra_code = getattr(builder, 'extracode',
                         code_obj.properties.get('extracode', ""))
    if extra_code:
        extra_code = re.sub(r'\\n', '\n', extra_code)
        extra_code = re.split(re.compile(r'^###\s*$', re.M), extra_code, 1)
        classes[code_obj.klass].extra_code_h.append(extra_code[0])
        if len(extra_code) > 1:
            classes[code_obj.klass].extra_code_cpp.append(extra_code[1])
        if not is_new:
            common.message('WARNING', '%s has extra code, but you are '
                           'not overwriting existing sources: please check '
                           'that the resulting code is correct!' % \
                           code_obj.name)

    if not multiple_files and extra_code:
        _current_extra_code_h.append("".join(
            classes[code_obj.klass].extra_code_h[::-1]))
        _current_extra_code_cpp.append("".join(
            classes[code_obj.klass].extra_code_cpp[::-1]))
    #------------------------------------------------------------

    default_sign = [('wxWindow*', 'parent'), ('int', 'id')]
##     sign = obj_constructors.get(code_obj.base, default_sign)
    sign = getattr(builder, 'constructor', default_sign)
    
    defaults = []
    for t in sign:
        if len(t) == 3: defaults.append(t[2])
        else: defaults.append(None)
    tmp_sign = [ t[0] + ' ' + t[1] for t in sign ]
    sign_decl2 = ', '.join(tmp_sign)
    for i in range(len(tmp_sign)):
        if defaults[i] is not None:
            tmp_sign[i] += '=%s' % defaults[i]
    sign_decl1 = ', '.join(tmp_sign)
    sign_inst = ', '.join([ t[1] for t in sign])


    # ALB 2004-12-08 event handling
    event_handlers = classes[code_obj.klass].event_handlers
    if hasattr(builder, 'get_events'):
        event_handlers.extend(builder.get_events(code_obj))

    # ALB 2007-08-31 custom base classes support
    custom_base = getattr(code_obj, 'custom_base',
                          code_obj.properties.get('custom_base', None))
    if custom_base and not custom_base.strip():
        custom_base = None

    if not is_new and custom_base is not None:
        # custom base classes set, but "overwrite existing sources" not
        # set. Issue a warning about this
        common.message('WARNING', '%s has custom base classes, but you are '
                       'not overwriting existing sources: please check that '
                       'the resulting code is correct!' % code_obj.name)        

    if is_new:
        # header file
        base = code_obj.base
        if custom_base is not None:
            base = ", public ".join([b.strip() for b in custom_base.split(',')])
        hwrite('\nclass %s: public %s {\n' % (code_obj.klass, base))
        hwrite('public:\n')
        # the first thing to add it the enum of the various ids
        hwrite(tabs(1) + '// begin wxGlade: %s::ids\n' % code_obj.klass)
        ids = classes[code_obj.klass].ids

        # let's try to see if there are extra ids to add to the enum
        if hasattr(builder, 'get_ids_code'):
            ids.extend(builder.get_ids_code(code_obj))
        
        if ids:
            hwrite(tabs(1) + 'enum {\n')
            ids = (',\n' + tabs(2)).join(ids)
            hwrite(tabs(2) + ids)
            hwrite('\n' + tabs(1) + '};\n')
        hwrite(tabs(1) + '// end wxGlade\n\n')
        # constructor prototype
        hwrite(tabs(1) + '%s(%s);\n' % (code_obj.klass, sign_decl1))
        hwrite('\nprivate:\n')
        # set_properties and do_layout prototypes
        hwrite(tabs(1) + '// begin wxGlade: %s::methods\n' % code_obj.klass)
        hwrite(tabs(1) + 'void set_properties();\n')
        hwrite(tabs(1) + 'void do_layout();\n')
        hwrite(tabs(1) + '// end wxGlade\n')
        # declarations of the attributes
        hwrite('\n')
        hwrite('protected:\n')
        hwrite(tabs(1) + '// begin wxGlade: %s::attributes\n' % code_obj.klass)
        for o_type, o_name in classes[code_obj.klass].sub_objs:
            hwrite(tabs(1) + '%s* %s;\n' % (o_type, o_name))
        hwrite(tabs(1) + '// end wxGlade\n')

        # ALB 2004-12-08 event handling
        if event_handlers:
            t = tabs(1)
            hwrite('\n' + t + 'DECLARE_EVENT_TABLE();\n')
            hwrite('\npublic:\n')
            already_there = {}
            for tpl in event_handlers:
                if len(tpl) == 4:
                    win_id, event, handler, evt_type = tpl
                else:
                    win_id, event, handler = tpl
                    evt_type = 'wxCommandEvent'
                if handler not in already_there:
                    # Sebastien JEFFROY & Steve MULLER contribution
                    # Adding virtual attribute permits to derivate from the
                    # class generated by wxGlade
                    hwrite(t + 'virtual void %s(%s &event); '
                           '// wxGlade: <event_handler>\n' %
                           (handler, evt_type))
                    already_there[handler] = 1
        
        hwrite('}; // wxGlade: end class\n\n')
        
    elif prev_src is not None:
        hwrite(tabs(1) + '// begin wxGlade: %s::ids\n' % code_obj.klass)
        ids = classes[code_obj.klass].ids

        # let's try to see if there are extra ids to add to the enum
        if hasattr(builder, 'get_ids_code'):
            ids.extend(builder.get_ids_code(code_obj))

        if ids:
            hwrite(tabs(1) + 'enum {\n')
            ids = (',\n' + tabs(2)).join(ids)
            hwrite(tabs(2) + ids)
            hwrite('\n' + tabs(1) + '};\n')
        hwrite(tabs(1) + '// end wxGlade\n')
        tag = '<%swxGlade replace %s ids>' % (nonce, code_obj.klass)
        if prev_src.header_content.find(tag) < 0:
            # no ids tag found, issue a warning and do nothing
            common.message("WARNING", "wxGlade ids block not found for %s," \
                           " ids declarations code NOT generated" % \
                           code_obj.name)
        else:
            prev_src.header_content = prev_src.header_content.\
                                      replace(tag, "".join(header_buffer))
        header_buffer = [ tabs(1) + '// begin wxGlade: %s::methods\n' % \
                          code_obj.klass,
                          tabs(1) + 'void set_properties();\n',
                          tabs(1) + 'void do_layout();\n',
                          tabs(1) + '// end wxGlade\n' ]
        tag = '<%swxGlade replace %s methods>' % (nonce, code_obj.klass)
        if prev_src.header_content.find(tag) < 0:
            # no methods tag found, issue a warning and do nothing
            common.message("WARNING",
                           "wxGlade methods block not found for %s," \
                           " methods declarations code NOT generated" % \
                           code_obj.name)
        else:
            prev_src.header_content = prev_src.header_content.\
                                      replace(tag, "".join(header_buffer))
        header_buffer = []
        hwrite = header_buffer.append
        hwrite(tabs(1) + '// begin wxGlade: %s::attributes\n' % code_obj.klass)
        for o_type, o_name in classes[code_obj.klass].sub_objs:
            hwrite(tabs(1) + '%s* %s;\n' % (o_type, o_name))
        hwrite(tabs(1) + '// end wxGlade\n')
        tag = '<%swxGlade replace %s attributes>' % (nonce, code_obj.klass)
        if prev_src.header_content.find(tag) < 0:
            # no attributes tag found, issue a warning and do nothing
            common.message("WARNING",
                           "wxGlade attributes block " \
                           "not found for %s, attributes declarations code " \
                           "NOT generated" % code_obj.name)
        else:
            prev_src.header_content = prev_src.header_content.\
                                      replace(tag, "".join(header_buffer))

        header_buffer = []
        hwrite = header_buffer.append
        # ALB 2004-12-08 event handling
        if event_handlers:
            already_there = prev_src.event_handlers.get(code_obj.klass, {})
            t = tabs(1)
            for tpl in event_handlers:
                if len(tpl) == 4:
                    win_id, event, handler, evt_type = tpl
                else:
                    win_id, event, handler = tpl
                    evt_type = 'wxCommandEvent'
                if handler not in already_there:
                    # Sebastien JEFFROY & Steve MULLER contribution :
                    # Adding virtual attribute permits to derivate from the
                    # class generated by wxGlade
                    hwrite(t + 'virtual void %s(%s &event); // wxGlade: '
                           '<event_handler>\n' % (handler, evt_type))
                    already_there[handler] = 1
            if code_obj.klass not in prev_src.event_table_def:
                hwrite('\nprotected:\n')
                hwrite(tabs(1) + 'DECLARE_EVENT_TABLE()\n')
        tag = '<%swxGlade event_handlers %s>' % (nonce, code_obj.klass)
        if prev_src.header_content.find(tag) < 0:
            # no attributes tag found, issue a warning and do nothing
            common.message("WARNING", "wxGlade events block " \
                           "not found for %s, event table code NOT generated" %
                           code_obj.name)
        else:
            prev_src.header_content = prev_src.header_content.\
                                      replace(tag, "".join(header_buffer))
        
    # source file
    # set the window's style
    prop = code_obj.properties
    style = prop.get("style", None)
    if style is not None:
        sign_inst = sign_inst.replace('style', '%s' % style)
    
    # constructor
    if is_new:
        base = "%s(%s)" % (code_obj.base, sign_inst)
        if custom_base:
            bases = [b.strip() for b  in custom_base.split(',')]
            if bases:
                base = "%s(%s)" % (bases[0], sign_inst)
                rest = ", ".join([b + "()" for b in bases[1:]])
                if rest: base += ", " + rest
            
        swrite('\n%s::%s(%s):\n%s%s\n{\n' % (code_obj.klass,
                                             code_obj.klass,
                                             sign_decl2, tabs(1), base))
##                                                  code_obj.base, sign_inst))
    swrite(tabs(1) + '// begin wxGlade: %s::%s\n' % (code_obj.klass,
                                                     code_obj.klass))
    tab = tabs(1)
    init_lines = classes[code_obj.klass].init
    # --- patch 2002-08-26 ---------------------------------------------------
    parents_init = classes[code_obj.klass].parents_init
    parents_init.reverse()    
    for l in parents_init: swrite(tab + l)
    # ------------------------------------------------------------------------
    for l in init_lines: swrite(tab + l)

    # now see if there are extra init lines to add
    if hasattr(builder, 'get_init_code'):
        for l in builder.get_init_code(code_obj):
            swrite(tab + l)
    
    swrite('\n' + tab + 'set_properties();\n')
    swrite(tab + 'do_layout();\n')
    # end tag
    swrite(tab + '// end wxGlade\n')
    if is_new: swrite('}\n\n')

    if prev_src is not None and not is_new:
        # replace the lines inside the constructor wxGlade block
        # with the new ones
        tag = '<%swxGlade replace %s %s>' % (nonce, code_obj.klass,
                                             code_obj.klass)
        if prev_src.source_content.find(tag) < 0:
            # no constructor tag found, issue a warning and do nothing
            common.message("WARNING", "wxGlade %s::%s block not found," \
                           " relative code NOT generated" % (code_obj.klass,
                                                             code_obj.klass))
        else:
            prev_src.source_content = prev_src.source_content.\
                                      replace(tag, "".join(source_buffer))
        source_buffer = []
        swrite = source_buffer.append

    # ALB 2004-12-08 event handling code
    if event_handlers:
        # 1) event table declaration/definition...
        if prev_src is not None and \
               code_obj.klass in prev_src.event_table_decl:
            has_event_table = True
        else:
            has_event_table = False
        if is_new or not has_event_table:
            swrite('\nBEGIN_EVENT_TABLE(%s, %s)\n' % \
                   (code_obj.klass, code_obj.base))
            
        swrite(tab + '// begin wxGlade: %s::event_table\n' % code_obj.klass)
        for tpl in event_handlers:
            win_id, event, handler = tpl[:3]
            swrite(tab + '%s(%s, %s::%s)\n' % \
                   (event, win_id, code_obj.klass, handler))
        swrite(tab + '// end wxGlade\n')
        
        if is_new or not has_event_table:
            swrite('END_EVENT_TABLE();\n')

        if prev_src is not None and not is_new:
            tag = '<%swxGlade replace %s event_table>' % (nonce, code_obj.klass)
            if prev_src.source_content.find(tag) < 0:
                # no constructor tag found, issue a warning and do nothing
                common.message("WARNING", "wxGlade %s::event_table block " \
                               "not found, relative code NOT generated" % \
                               (code_obj.klass))
            else:
                prev_src.source_content = prev_src.source_content.\
                                          replace(tag, "".join(source_buffer))
            source_buffer = []
            swrite = source_buffer.append

        # 2) event handler stubs...
        if prev_src is not None:
            already_there = prev_src.event_handlers.get(code_obj.klass, {})
        else:
            already_there = {}
        for tpl in event_handlers:
            if len(tpl) == 4:
                win_id, event, handler, evt_type = tpl
            else:
                win_id, event, handler = tpl
                evt_type = 'wxCommandEvent'
            if handler not in already_there:
                swrite('\n\nvoid %s::%s(%s &event)\n{\n' % \
                       (code_obj.klass, handler, evt_type))
                swrite(tab + 'event.Skip();\n')
                swrite(tab + 'wxLogDebug(wxT("Event handler (%s::%s) not '
                       'implemented yet")); //notify the user '
                       'that he hasn\'t implemented the event handler yet\n' % \
                       (code_obj.klass, handler))
                swrite('}\n')
                already_there[handler] = 1
        if is_new or prev_src is None:
            swrite('\n\n')
        swrite('// wxGlade: add %s event handlers\n' % code_obj.klass)
        if is_new or prev_src is None:
            swrite('\n')

        if prev_src is not None and not is_new:
            tag = '<%swxGlade add %s event handlers>' % \
                  (nonce, code_obj.klass)
            if prev_src.source_content.find(tag) < 0:
                # no constructor tag found, issue a warning and do nothing
                common.message("WARNING", "wxGlade %s event handlers " \
                               "marker not found, relative code NOT generated" \
                               % (code_obj.klass))
            else:
                prev_src.source_content = prev_src.source_content.\
                                          replace(tag, "".join(source_buffer))
            source_buffer = []
            swrite = source_buffer.append
    

    # set_properties
##     props_builder = obj_properties.get(code_obj.base)
##     #write_body = len(classes[code_obj.klass].props)
##     if props_builder:
##         obj_p = props_builder(code_obj)#obj_properties[code_obj.base](code_obj)
##         #if not write_body: write_body = len(obj_p)
##     else: obj_p = []
    if hasattr(builder, 'get_properties_code'):
        obj_p = builder.get_properties_code(code_obj)
    else:
        obj_p = generate_common_properties(code_obj)

    # set_properties
    if is_new:
        swrite('\nvoid %s::set_properties()\n{\n' % code_obj.klass)
    swrite(tab + '// begin wxGlade: %s::set_properties\n' % code_obj.klass)
    for l in obj_p: swrite(tab + l)
    for l in classes[code_obj.klass].props:
        swrite(tab + l)
    swrite(tab + '// end wxGlade\n')
    if is_new:
        swrite('}\n\n')
    
    if prev_src is not None and not is_new:
        # replace the lines inside the constructor wxGlade block
        # with the new ones
        tag = '<%swxGlade replace %s set_properties>' % (nonce, code_obj.klass)
        if prev_src.source_content.find(tag) < 0:
            # no set_properties tag found, issue a warning and do nothing
            common.message("WARNING", "wxGlade %s::set_properties block "\
                           "not found, relative code NOT generated" % \
                           (code_obj.klass))
        else:
            prev_src.source_content = prev_src.source_content.\
                                      replace(tag, "".join(source_buffer))
        source_buffer = []
        swrite = source_buffer.append


    # do_layout
    if is_new:
        swrite('\nvoid %s::do_layout()\n{\n' % code_obj.klass)
    layout_lines = classes[code_obj.klass].layout
    sizers_init_lines = classes[code_obj.klass].sizers_init
    swrite(tab + '// begin wxGlade: %s::do_layout\n' % code_obj.klass)
    sizers_init_lines.reverse()
    for l in sizers_init_lines: swrite(tab + l)
    for l in layout_lines: swrite(tab + l)

    #if sizers_init_lines or layout_lines: swrite(tab + 'Layout();\n')

    # now, check if there are extra layout lines to add
    if hasattr(builder, 'get_layout_code'):
        for l in builder.get_layout_code(code_obj):
            swrite(tab + l)
    
    swrite(tab + '// end wxGlade\n')
    if is_new:
        swrite('}\n\n')

    if prev_src is not None and not is_new:
        # replace the lines inside the constructor wxGlade block
        # with the new ones
        tag = '<%swxGlade replace %s do_layout>' % (nonce, code_obj.klass)
        if prev_src.source_content.find(tag) < 0:
            # no do_layout tag found, issue a warning and do nothing
            common.message("WARNING", "wxGlade %s::do_layout block "\
                           "not found, relative code NOT generated" %
                           (code_obj.klass))
        else:
            prev_src.source_content = prev_src.source_content.\
                                      replace(tag, "".join(source_buffer))
        source_buffer = []
        swrite = source_buffer.append


    # the code has been generated
    classes[code_obj.klass].done = True

    if not multiple_files and prev_src is not None:
        # if this is a new class, add its code to the new_classes list of the
        # SourceFileContent instance
        if is_new: prev_src.new_classes.append( ("".join(header_buffer),
                                                "".join(source_buffer)) )
        return

    if multiple_files:
        if code_obj.base in obj_builders:
            classes[code_obj.klass].dependencies.extend(
                getattr(obj_builders[code_obj.base], 'extra_headers', []))
        if prev_src is not None:
            tag = '<%swxGlade insert new_classes>' % nonce
            prev_src.header_content = prev_src.header_content.replace(tag, "")
            
            # insert the module dependencies of this class
            extra_modules = classes[code_obj.klass].dependencies
            #print 'extra_modules:', extra_modules, code_obj.base
            deps = ['// begin wxGlade: ::dependencies\n']
            for module in _unique(extra_modules):
                if module and ('"' != module[0] != '<'):
                    deps.append('#include "%s.h"\n' % module)
                else:
                    deps.append('#include %s\n' % module)
            deps.append('// end wxGlade\n')
            # WARNING: there's a double space '  ' between 'replace' and
            # 'dependencies' in the tag below, because there is no class name
            # (see SourceFileContent, line ~147)
            tag = '<%swxGlade replace  dependencies>' % nonce
            prev_src.header_content = prev_src.header_content.\
                                      replace(tag, "".join(deps))

            # insert the extra code of this class
            extra_code_h = "".join(classes[code_obj.klass].extra_code_h[::-1])
            extra_code_cpp = \
                           "".join(classes[code_obj.klass].extra_code_cpp[::-1])
            # if there's extra code but we are not overwriting existing
            # sources, warn the user
            if extra_code_h or extra_code_cpp:
                common.message('WARNING', '%s (or one of its chilren) has '
                               'extra code classes, but you are '
                               'not overwriting existing sources: please check '
                               'that the resulting code is correct!' % \
                               code_obj.name)
            
            extra_code_h = '// begin wxGlade: ::extracode\n%s\n' \
                           '// end wxGlade\n' % extra_code_h
            extra_code_cpp = '// begin wxGlade: ::extracode\n%s\n' \
                             '// end wxGlade\n' % extra_code_cpp
            tag = '<%swxGlade replace  extracode>' % nonce
            prev_src.header_content = prev_src.header_content.replace(
                tag, extra_code_h)
            prev_src.source_content = prev_src.source_content.replace(
                tag, extra_code_cpp)
            
            # store the new file contents to disk
            name = os.path.join(out_dir, code_obj.klass)
            common.save_file(name + '.h', prev_src.header_content, 'codegen')
            common.save_file(name + '.cpp', prev_src.source_content, 'codegen')
            return

        # create the new source file
        header_file = os.path.join(out_dir, code_obj.klass + '.h')
        source_file = os.path.join(out_dir, code_obj.klass + '.cpp')
        hout = cStringIO.StringIO()
        sout = cStringIO.StringIO()
        # header file
        hwrite = hout.write
        # write the common lines
        for line in header_lines: hwrite(line)
        # isolation directives
        hn = os.path.basename(header_file).upper().replace('.', '_')
        hwrite('\n#ifndef %s\n#define %s\n' % (hn, hn))
        # write the module dependecies for this class
        #extra_headers = classes[code_obj.klass].dependencies
        hwrite('\n// begin wxGlade: ::dependencies\n')
        extra_modules = classes[code_obj.klass].dependencies
        for module in _unique(extra_modules):
            if module and ('"' != module[0] != '<'):
                hwrite('#include "%s.h"\n' % module)
            else:
                hwrite('#include %s\n' % module)
        hwrite('// end wxGlade\n')
        hwrite('\n')

        # insert the extra code of this class
        extra_code_h = "".join(classes[code_obj.klass].extra_code_h[::-1])
        extra_code_h = '// begin wxGlade: ::extracode\n%s\n// end wxGlade\n' % \
                       extra_code_h
        hwrite(extra_code_h)
        hwrite('\n')
        
        # write the class body
        for line in header_buffer: hwrite(line)
        hwrite('\n#endif // %s\n' % hn)

        # source file
        swrite = sout.write
        # write the common lines
        #for line in header_lines: swrite(line)
        swrite(header_lines[0])
        swrite('#include "%s"\n\n' % os.path.basename(header_file))

        # insert the extra code of this class
        extra_code_cpp = "".join(classes[code_obj.klass].extra_code_cpp[::-1])
        extra_code_cpp = '// begin wxGlade: ::extracode\n%s\n' \
                         '// end wxGlade\n' % extra_code_cpp
        swrite(extra_code_cpp)
        swrite('\n')
        
        # write the class implementation
        for line in source_buffer: swrite(line)

        # store source to disk
        common.save_file(header_file, hout.getvalue(), 'codegen')
        common.save_file(source_file, sout.getvalue(), 'codegen')

        hout.close()
        sout.close()

    else: # not multiple_files
        # write the class body onto the single source file 
        hwrite = output_header.write
        for line in header_buffer: hwrite(line)
        swrite = output_source.write
        for line in source_buffer: swrite(line)
        # extra code
        if classes[code_obj.klass].extra_code_h:
            _current_extra_code_h.extend(
                classes[code_obj.klass].extra_code_h[::-1])
        if classes[code_obj.klass].extra_code_cpp:
            _current_extra_code_cpp.extend(
                classes[code_obj.klass].extra_code_h[::-1])


def add_app(app_attrs, top_win_class):
    """\
    Generates the code for a wxApp instance.
    if the 'class' property has no value, the function does nothing
    """
    if not multiple_files: prev_src = previous_source
    else:
        filename = os.path.join(out_dir, 'main.cpp')
        if not os.path.exists(filename): prev_src = None
        elif _overwrite: prev_src = None
        else:
            # prev_src doesn't need to be a SourceFileContent instance in this
            # case, as we do nothing if it is not None
            prev_src = 1
    if prev_src is not None:
        return # do nothing if the file existed

    klass = app_attrs.get('class')
    top_win = app_attrs.get('top_window')
    if not klass or not top_win: return # do nothing in these cases

    lines = []
    append = lines.append
    tab = tabs(1)
    append('\n\nclass %s: public wxApp {\n' % klass)
    append('public:\n')
    append(tab + 'bool OnInit();\n')
    append('};\n\n')
    append('IMPLEMENT_APP(%s)\n\n' % klass)
    append('bool %s::OnInit()\n{\n' % klass)
    append(tab + 'wxInitAllImageHandlers();\n') # we add this to avoid troubles
    append(tab + '%s* %s = new %s(NULL, wxID_ANY, wxEmptyString);\n' % \
           (top_win_class, top_win, top_win_class))
    append(tab + 'SetTopWindow(%s);\n' % top_win)
    append(tab + '%s->Show();\n' % top_win)
    append(tab + 'return true;\n}\n')

    if multiple_files:
        filename = os.path.join(out_dir, 'main.cpp')
        out = cStringIO.StringIO()
        write = out.write
        # write the common lines
        for line in header_lines: write(line)
        # import the top window module
        write('#include "%s.h"\n\n' % top_win_class)
        # write the wxApp code
        for line in lines: write(line)
        common.save_file(filename, out.getvalue(), 'codegen')
    else:
        write = output_source.write
        for line in lines: write(line)


def generate_code_size(obj):
    """\
    returns the code fragment that sets the size of the given object.
    """
    if obj.is_toplevel: name1 = ''; name2 = 'this'
    else: name1 = '%s->' % obj.name; name2 = obj.name
    size = obj.properties.get('size', '').strip()
    use_dialog_units = (size[-1] == 'd')
    if for_version < (2, 5) or obj.parent is None:
        method = 'SetSize'
    else:
        method = 'SetMinSize'
    if use_dialog_units:
        return name1 + method + '(wxDLG_UNIT(%s, wxSize(%s)));\n' % \
               (name2, size[:-1])
    else:
        return name1 + method + '(wxSize(%s));\n' % size


def _string_to_colour(s):
    return '%d, %d, %d' % (int(s[1:3], 16), int(s[3:5], 16), int(s[5:], 16))


def generate_code_foreground(obj): 
    """\
    returns the code fragment that sets the foreground colour of
    the given object.
    """
    if not obj.is_toplevel: intro = '%s->' % obj.name
    else: intro = ''
    try:
        color = 'wxColour(%s)' % \
                _string_to_colour(obj.properties['foreground'])
    except (IndexError, ValueError): # the color is from system settings
        color = 'wxSystemSettings::GetColour(%s)' % \
                obj.properties['foreground']
    return intro + 'SetForegroundColour(%s);\n' % color


def generate_code_background(obj):
    """\
    returns the code fragment that sets the background colour of
    the given object.
    """
    if not obj.is_toplevel: intro = '%s->' % obj.name
    else: intro = ''
    try:
        color = 'wxColour(%s)' % \
                _string_to_colour(obj.properties['background'])
    except (IndexError, ValueError): # the color is from system settings
        color = 'wxSystemSettings::GetColour(%s)' % \
                obj.properties['background']
    return intro + 'SetBackgroundColour(%s);\n' % color


def generate_code_font(obj):
    """\
    returns the code fragment that sets the font the given object.
    """
    font = obj.properties['font'] 
    size = font['size']; family = font['family']
    underlined = font['underlined']
    style = font['style']; weight = font['weight']
    face = '"%s"' % font['face'].replace('"', r'\"')
    if obj.is_toplevel: intro = ''
    else: intro = '%s->' % obj.name
    return intro + 'SetFont(wxFont(%s, %s, %s, %s, %s, wxT(%s)));\n' % \
           (size, family, style, weight, underlined, face)


_last_generated_id = 1000

def generate_code_id(obj, id=None):
    """\
    returns a 2-tuple of strings representing the LOC that sets the id of the
    given object: the first line is the declaration of the variable, and is
    empty if the object's id is a constant, and the second line is the value
    of the id
    """
    global _last_generated_id

    if id is None:
        id = obj.properties.get('id')

    if not id: return '', 'wxID_ANY'
    tokens = id.split('=')
    if len(tokens) > 1: name, val = tokens[:2]
    else: return '', tokens[0] # we assume name is declared elsewhere
    if not name: return '', val
    if val.strip() == '?':
        val = 'wxID_HIGHEST + ' + str(_last_generated_id)
        _last_generated_id += 1        
    return '%s = %s' % (name, val), name


def generate_code_tooltip(obj):
    """\
    returns the code fragment that sets the tooltip of
    the given object.
    """
    if not obj.is_toplevel: intro = '%s->' % obj.name
    else: intro = ''
    return intro + 'SetToolTip(%s);\n' % quote_str(obj.properties['tooltip'])


def _get_code_name(obj):
    if not obj.is_toplevel: return '%s->' % obj.name
    else: return ''  


def generate_code_disabled(obj):
    self = _get_code_name(obj)
    try: disabled = int(obj.properties['disabled'])
    except: disabled = False
    if disabled:
        return self + 'Enable(false);\n'


def generate_code_focused(obj):
    self = _get_code_name(obj)
    try: focused = int(obj.properties['focused'])
    except: focused = False
    if focused:
        return self + 'SetFocus();\n'


def generate_code_hidden(obj):
    self = _get_code_name(obj)
    try: hidden = int(obj.properties['hidden'])
    except: hidden = False
    if hidden:
        return self + 'Hide();\n'


def generate_code_extraproperties(obj):
    self = _get_code_name(obj)
    prop = obj.properties['extraproperties']
    ret = []
    for name in sorted(prop):
        ret.append(self + 'Set%s(%s);\n' % (name, prop[name]))
    return ret


def generate_common_properties(widget):
    """\
    generates the code for various properties common to all widgets (background
    and foreground colors, font, ...)
    Returns a list of strings containing the generated code
    """
    prop = widget.properties
    out = []
    if prop.get('size', '').strip(): out.append(generate_code_size(widget))
    if prop.get('background'): out.append(generate_code_background(widget))
    if prop.get('foreground'): out.append(generate_code_foreground(widget))
    if prop.get('font'): out.append(generate_code_font(widget))
    if prop.get('tooltip'): out.append(generate_code_tooltip(widget))
    # trivial boolean properties
    if prop.get('disabled'): out.append(generate_code_disabled(widget))
    if prop.get('focused'): out.append(generate_code_focused(widget))
    if prop.get('hidden'): out.append(generate_code_hidden(widget))
    # ALB 2007-09-01 extra properties
    if prop.get('extraproperties') and not widget.preview:
        out.extend(generate_code_extraproperties(widget))
    return out


# custom property handlers
class FontPropertyHandler:
    """Handler for font properties"""
    font_families = { 'default': 'wxDEFAULT', 'decorative': 'wxDECORATIVE',
                      'roman': 'wxROMAN', 'swiss': 'wxSWISS',
                      'script': 'wxSCRIPT', 'modern': 'wxMODERN',
                      'teletype': 'wxTELETYPE' }
    font_styles = { 'normal': 'wxNORMAL', 'slant': 'wxSLANT',
                    'italic': 'wxITALIC' }
    font_weights = { 'normal': 'wxNORMAL', 'light': 'wxLIGHT',
                     'bold': 'wxBOLD' }
    def __init__(self):
        self.dicts = { 'family': self.font_families, 'style': self.font_styles,
                       'weight': self.font_weights }
        self.attrs = { 'size': '0', 'style': '0', 'weight': '0', 'family': '0',
                       'underlined': '0', 'face': '' }
        self.current = None 
        self.curr_data = []
        
    def start_elem(self, name, attrs):
        self.curr_data = []
        if name != 'font' and name in self.attrs:
            self.current = name
        else: self.current = None
            
    def end_elem(self, name, code_obj):
        if name == 'font':
            code_obj.properties['font'] = self.attrs
            return True
        elif self.current is not None:
            decode = self.dicts.get(self.current)
            if decode: val = decode.get("".join(self.curr_data), '0')
            else: val = "".join(self.curr_data)
            self.attrs[self.current] = val
        
    def char_data(self, data):
        self.curr_data.append(data)

# end of class FontPropertyHandler


class DummyPropertyHandler:
    """Empty handler for properties that do not need code"""
    def start_elem(self, name, attrs): pass
    def end_elem(self, name, code_obj): return True
    def char_data(self, data): pass

# end of class DummyPropertyHandler


class EventsPropertyHandler(object):
    def __init__(self):
        self.handlers = {}
        self.event_name = None
        self.curr_handler = []
        
    def start_elem(self, name, attrs):
        if name == 'handler':
            self.event_name = attrs['event']

    def end_elem(self, name, code_obj):
        if name == 'handler':
            if self.event_name and self.curr_handler:
                self.handlers[self.event_name] = ''.join(self.curr_handler)
            self.event_name = None
            self.curr_handler = []
        elif name == 'events':
            code_obj.properties['events'] = self.handlers
            return True

    def char_data(self, data):
        data = data.strip()
        if data:
            self.curr_handler.append(data)

# end of class EventsPropertyHandler
    

class ExtraPropertiesPropertyHandler(object):
    def __init__(self):
        self.props = {}
        self.prop_name = None
        self.curr_prop = []
        
    def start_elem(self, name, attrs):
        if name == 'property':
            name = attrs['name']
            if name and name[0].islower():
                name = name[0].upper() + name[1:]
            self.prop_name = name

    def end_elem(self, name, code_obj):
        if name == 'property':
            if self.prop_name and self.curr_prop:
                self.props[self.prop_name] = ''.join(self.curr_prop)
            self.prop_name = None
            self.curr_prop = []
        elif name == 'extraproperties':
            code_obj.properties['extraproperties'] = self.props
            return True # to remove this handler

    def char_data(self, data):
        data = data.strip()
        if data:
            self.curr_prop.append(data)

# end of class ExtraPropertiesPropertyHandler


# dictionary whose items are custom handlers for widget properties
_global_property_writers = { 'font': FontPropertyHandler,
                             'events': EventsPropertyHandler,
                             'extraproperties': ExtraPropertiesPropertyHandler,
                             }

# dictionary of dictionaries of property handlers specific for a widget
# the keys are the class names of the widgets
# Ex: _property_writers['wxRadioBox'] = {'choices', choices_handler}
_property_writers = {}

# dictionary of additional headers for objects
_obj_headers = {}

def get_property_handler(property_name, widget_name):
    try: cls = _property_writers[widget_name][property_name]
    except KeyError: cls = _global_property_writers.get(property_name, None)
    if cls: return cls()
    return None


def add_property_handler(property_name, handler, widget_name=None):
    """\
    sets a function to parse a portion of XML to get the value of the property
    property_name. If widget_name is not None, the function is called only if
    the property in inside a widget whose class is widget_name
    """
    if widget_name is None: _global_property_writers[property_name] = handler
    else:
        try: _property_writers[widget_name][property_name] = handler
        except KeyError:
            _property_writers[widget_name] = { property_name: handler }


class WidgetHandler:
    """\
    Interface the various code generators for the widgets must implement
    """

    """``signature'' of the widget's constructor"""
    constructor = []
    
    """
    if not None, list of extra header file, in the form
    <header.h> or "header.h"
    """
    extra_headers = []
    
    def get_code(self, obj):
        """\
        Handler for normal widgets (non-toplevel): returns 4 lists of strings,
        init, ids, properties and layout, that contain the code for the
        corresponding parts/methods of the class to generate
        """
        return [], [], [], []

    def get_properties_code(self, obj):
        """\
        Handler for the code of the set_properties method of toplevel objects.
        Returns a list of strings containing the code to generate
        """
        return []

    def get_init_code(self, obj):
        """\
        Handler for the code of the constructor of toplevel objects.  Returns a
        list of strings containing the code to generate.  Usually the default
        implementation is ok (i.e. there are no extra lines to add). The
        generated lines are appended at the end of the constructor
        """
        return []
        
    def get_ids_code(self, obj):
        """\
        Handler for the code of the ids enum of toplevel objects.
        Returns a list of strings containing the code to generate.
        Usually the default implementation is ok (i.e. there are no
        extra lines to add)
        """
        return []

    def get_layout_code(self, obj):
        """\
        Handler for the code of the do_layout method of toplevel objects.
        Returns a list of strings containing the code to generate.
        Usually the default implementation is ok (i.e. there are no
        extra lines to add)
        """
        return []

# end of class WidgetHandler


def add_widget_handler(widget_name, handler):
    obj_builders[widget_name] = handler


def _unique(sequence):
    """\
    Strips all duplicates from sequence. Works only if items of sequence
    are hashable
    """
    tmp = {}
    for item in sequence: tmp[item] = 1
    return tmp.keys()


def get_events_with_type(obj, evt_type):
    """\
    Returns the list of event handlers defined for `obj', setting the type of
    the argument of the handlers (i.e. the event parameter) to `evt_type'
    """
    ret = []
    if 'events' not in obj.properties:
        return ret
    id_name, id = generate_code_id(obj)
    for event, handler in obj.properties['events'].iteritems():
        ret.append((id, event, handler, evt_type))
    return ret
