# pl_codegen.py: perl code generator
# $Id: pl_codegen.py,v 1.41 2007/08/07 12:26:20 agriggio Exp $
#
# Copyright (c) 2002-2004 D.H. aka crazyinsomniac on sourceforge.net
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY
#
# like all other perl parts, based on the pre-existing python generators
#

"""\
How the code is generated: every time the end of an object is reached during
the parsing of the xml tree, either the function 'add_object' or the function
'add_class' is called: the latter when the object is a toplevel one, the former
when it is not. In the last case, 'add_object' calls the appropriate ``writer''
function for the specific object, found in the 'obj_builders' dict. Such
function accepts one argument, the CodeObject representing the object for
which the code has to be written, and returns 3 lists of strings, representing
the lines to add to the '__init__', '__set_properties' and '__do_layout'
methods of the parent object.
"""

import sys, os, os.path
import common, config
import cStringIO
from xml_parse import XmlParsingError


# these two globals must be defined for every code generator module
language = 'perl'
writer = sys.modules[__name__] # the writer is the module itself

# default extensions for generated files: a list of file extensions
default_extensions = ['pl','pm']

"""\
dictionary that maps the lines of code of a class to the name of such class:
the lines are divided in 3 categories: '__init__', '__set_properties' and
'__do_layout'
"""
classes = None

"""dictionary of ``writers'' for the various objects"""
obj_builders = {}

"""\
dictionary of ``property writer'' functions, used to set the properties of a
toplevel object
"""
obj_properties = {}

# random number used to be sure that the replaced tags in the sources are
# the right ones (see SourceFileContent and add_class)
nonce = None

# lines common to all the generated files (use Wx [:everything], ...)
header_lines = []

# if True, generate a file for each custom class
multiple_files = False

# if not None, it is the single source file to write into
output_file = None
# if not None, it is the directory inside which the output files are saved
out_dir = None

def cn(class_name):
    if class_name[:2] == 'wx': 
        return 'Wx::' + class_name[2:]
    elif class_name[:4] == 'EVT_':
        return 'Wx::' + class_name
    else:
        return class_name

class ClassLines:
    """\
    Stores the lines of perl code for a custom class
    """
    def __init__(self):
        self.init = [] # lines of code to insert in the __init__ method
                       # (for children widgets)
        self.parents_init = [] # lines of code to insert in the __init__ for
                               # container widgets (panels, splitters, ...)
        self.sizers_init = [] # lines related to sizer objects declarations
        self.props = [] # lines to insert in the __set_properties method
        self.layout = [] # lines to insert in the __do_layout method
        
        self.dependencies = {} # names of the modules this class depends on
        self.done = False # if True, the code for this class has already
                          # been generated
        self.event_handlers = [] # lines to bind events 

# end of class ClassLines


class SourceFileContent:
    """\
    Keeps info about an existing file that has to be updated, to replace only
    the lines inside a wxGlade block, an to keep the rest of the file as it was
    
    WARNING: NOT YET COMPLETE
    (always overwrites destination file, ALWAYS) -- crazyinsomniac

    alb - the warning above shouldn't be true anymore... but we need testing...
    """
    def __init__(self, name=None, content=None, classes=None):
        self.name = name # name of the file
        self.content = content # content of the source file, if it existed
                               # before this session of code generation
        self.classes = classes # classes declared in the file
        self.new_classes = [] # new classes to add to the file (they are
                              # inserted BEFORE the old ones)
        if classes is None: self.classes = {}
        self.spaces = {} # indentation level for each class
        self.event_handlers = {} # list of event handlers for each class
        if self.content is None:
            self.build_untouched_content()

    def build_untouched_content(self):
        """\
        Builds a string with the contents of the file that must be left as is,
        and replaces the wxGlade blocks with tags that in turn will be replaced
        by the new wxGlade blocks
        
        WARNING: NOT YET COMPLETE -- crazyinsomniac

        alb - almost done :)
        WARNING: There is *NO* support for here documents: if you put wxGlade
        blocks inside a here document, you're likely going into troubles...
        """
        import re
        class_name = None
        new_classes_inserted = False
        # regexp to match class declarations
        #  package Foo; or package Foo::bar::baz   ;
        #class_decl = re.compile(r'^\s*package\s+[a-zA-Z]\w*(::\w+)*\s*;\s*$')
        # less precise regex, but working :-P
        class_decl = re.compile(r'^\s*package\s+([a-zA-Z_][\w:]*)\s*;.*$')
        # regexps to match wxGlade blocks
        block_start = re.compile(r'^(\s*)#\s*begin\s+wxGlade:\s*'
                                 '([a-zA-Z_][\w:]*?)::(\w+)\s*$')
        block_end = re.compile(r'^\s*#\s*end\s+wxGlade\s*$')
        pod_re = re.compile(r'^\s*=[A-Za-z_]+\w*.*$')
        event_handler = re.compile(
            r'#\s*wxGlade:\s*([\w:]+)::(\w+) <event_handler>\s*$')
        inside_block = False
        inside_pod = False
        tmp_in = open(self.name)
        out_lines = []
        for line in tmp_in:
            result = pod_re.match(line)
            if result is not None:
                inside_pod = True
            if inside_pod:
                out_lines.append(line)
                if line.startswith('=cut'):
                    inside_pod = False
                continue
            
            result = class_decl.match(line)
            if result is not None:
                #print ">> found class %s" % result.group(1)
                if class_name is None:
                    # this is the first class declared in the file: insert the
                    # new ones before this
                    out_lines.append('#<%swxGlade insert new_classes>' %
                                     nonce)
                    new_classes_inserted = True
                class_name = result.group(1)
                self.classes[class_name] = 1 # add the found class to the list
                                             # of classes of this module
                out_lines.append(line)
            elif not inside_block:
                result = block_start.match(line)
                if result is not None:
                    # replace the lines inside a wxGlade block with a tag that
                    # will be used later by add_class
                    spaces = result.group(1)
                    which_class = result.group(2)
                    which_block = result.group(3)
                    if which_class is None: which_class = class_name
                    self.spaces[which_class] = spaces
                    inside_block = True
                    if class_name is None:
                        out_lines.append('#<%swxGlade replace %s>' % \
                                         (nonce, which_block))
                    else:
                        out_lines.append('#<%swxGlade replace %s %s>' % \
                                         (nonce, which_class, which_block))
                else:
                    #- ALB 2004-12-05 ----------
                    result = event_handler.match(line)
                    if result is not None:
                        which_handler = result.group(2)
                        which_class = result.group(1)
                        self.event_handlers.setdefault(
                            which_class, {})[which_handler] = 1
                    if class_name is not None and self.is_end_of_class(line):
                        # add extra event handlers here...
                        out_lines.append('#<%swxGlade event_handlers %s>'
                                         % (nonce, class_name))
                    #---------------------------
                    out_lines.append(line)
                    if line.lstrip().startswith('use Wx'):
                        # add a tag to allow extra modules
                        out_lines.append('#<%swxGlade extra_modules>\n'
                                         % nonce)
            else:
                # ignore all the lines inside a wxGlade block
                if block_end.match(line) is not None:
                    inside_block = False
        if not new_classes_inserted:
            # if we are here, the previous ``version'' of the file did not
            # contain any class, so we must add the new_classes tag at the
            # end of the file
            out_lines.append('#<%swxGlade insert new_classes>' % nonce)
        tmp_in.close()
        # set the ``persistent'' content of the file
        self.content = "".join(out_lines)
        
    def is_end_of_class(self, line):
        return line.strip().startswith('# end of class ')

# end of class SourceFileContent

# if not None, it is an instance of SourceFileContent that keeps info about
# the previous version of the source to generate
previous_source = None 

# if True, overwrite any previous version of the source file instead of
# updating only the wxGlade blocks
_overwrite = False

# if True, enable gettext support
_use_gettext = False

import re
_quote_str_re = re.compile( r'\\(?![nrt])' )

def quote_str(s):
    """\
    returns a quoted version of 's', suitable to insert in a perl source file
    as a string object. Takes care also of gettext support
    """
    if not s: return '""'
    s = _quote_str_re.sub(r'\\\\', s )
    s = s.replace('"', r'\"')
    s = s.replace('$', r'\$')
    s = s.replace('@', r'\@')
    if _use_gettext:
        return '_T("' + s + '")'
    else:
        return '"' + s + '"'

def quote_path(s):
    """\
    escapes all " and \ , thus making a path suitable to
    insert in a perl source file
    """ # " alb: comment needed to avoid emacs going insane with colorization..
    s = s.replace('\\', '\\\\')
    s = s.replace('"', r'\"')
    s = s.replace('$', r'\$') # sigh
    s = s.replace('@', r'\@')
    return '"' + s + '"'

def initialize(app_attrs): 
    """\
    Writer initialization function.
    - app_attrs: dict of attributes of the application. The following two
                 are always present:
           path: output path for the generated code (a file if multi_files is
                 False, a dir otherwise)
         option: if True, generate a separate file for each custom class
    """

    out_path = app_attrs['path']
    multi_files = app_attrs['option']

    global classes, header_lines, multiple_files, previous_source, nonce, \
           _current_extra_modules, _use_gettext, _overwrite
    import time, random

##     # scan widgets.txt for widgets, load perl_codegen's
##     _widgets_dir = os.path.join(common.wxglade_path, 'widgets')
##     widgets_file = os.path.join(_widgets_dir, 'widgets.txt')
##     if not os.path.isfile(widgets_file):
##         print >> sys.stderr, "widgets file (%s) doesn't exist" % widgets_file
##         return
##     import sys
##     sys.path.append(_widgets_dir)
##     modules = open(widgets_file)
##     for line in modules:
##         module_name = line.strip()
##         if not module_name or module_name.startswith('#'): continue
##         module_name = module_name.split('#')[0].strip()
##         try:
##             m = __import__(
##                 module_name + '.perl_codegen', {}, {}, ['initialize'])
##             m.initialize()
##         except (ImportError, AttributeError):
##             print 'ERROR loading "%s"' % module_name
##             import traceback;
##             traceback.print_exc()
## ##        else:
## ##            print 'initialized perl generator for ', module_name
##     modules.close()

##     # ...then, the sizers
##     import edit_sizers.perl_sizers_codegen
##     edit_sizers.perl_sizers_codegen.initialize()  

    try:
        _use_gettext = int(app_attrs['use_gettext'])
    except (KeyError, ValueError):
        _use_gettext = False

    try: _overwrite = int(app_attrs['overwrite'])
    except (KeyError, ValueError): _overwrite = False

    # this is to be more sure to replace the right tags
    nonce = '%s%s' % (str(time.time()).replace('.', ''),
                      random.randrange(10**6, 10**7))

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

    classes = {}
    _current_extra_modules = {}
    header_lines = [
        '# generated by wxGlade %s on %s%s\n' % \
        (common.version, time.asctime(), common.generated_from()), 
        '# To get wxPerl visit http://wxPerl.sourceforge.net/\n\n',
        'use Wx 0.15 qw[:allclasses];\nuse strict;\n'
    ]
    if not config.preferences.write_timestamp:
        header_lines[0] = '# generated by wxGlade %s%s\n' % \
                          (common.version, common.generated_from())
    

    multiple_files = multi_files
    if not multiple_files:
        global output_file, output_file_name
        if not _overwrite and os.path.isfile(out_path):
            # the file exists, we must keep all the lines not inside a wxGlade
            # block. NOTE: this may cause troubles if out_path is not a valid
            # perl file, so be careful!
            previous_source = SourceFileContent(out_path)
        else:
            # if the file doesn't exist, create it and write the ``intro''
            previous_source = None
            output_file = cStringIO.StringIO()
            output_file_name = out_path
            output_file.write('#!/usr/bin/perl -w -- \n')
            for line in header_lines:
                output_file.write(line)
            output_file.write('#<%swxGlade extra_modules>\n' % nonce)
            output_file.write('\n')
    else:
        previous_source = None
        global out_dir
        if not os.path.isdir(out_path):
            raise XmlParsingError("'path' must be a directory when generating"\
                                  " multiple output files")
        out_dir = out_path


def finalize():
    """\
    Writer ``finalization'' function: flushes buffers, closes open files, ...
    """
    if previous_source is not None:
        # insert all the new custom classes inside the old file
        tag = '#<%swxGlade insert new_classes>' % nonce
        if previous_source.new_classes:
            code = "".join(previous_source.new_classes)
        else:
            code = ""
        previous_source.content = previous_source.content.replace(tag, code)
        tag = '#<%swxGlade extra_modules>\n' % nonce
        code = "".join(_current_extra_modules.keys())
        previous_source.content = previous_source.content.replace(tag, code)
        # now remove all the remaining <123415wxGlade ...> tags from the
        # source: this may happen if we're not generating multiple files,
        # and one of the container class names is changed
        import re
        tags = re.findall('(#<%swxGlade replace ([a-zA-Z_]\w*) +\w+>)' % nonce,
                          previous_source.content)
        for tag in tags:
            indent = previous_source.spaces.get(tag[1], '\t')
            comment = '%s# content of this block not found: ' \
                      'did you rename this class?\n' % indent
            previous_source.content = previous_source.content.replace(tag[0],
                                                                      comment)
        tags = re.findall('#<%swxGlade event_handlers \w+>' % nonce,
                          previous_source.content)
        for tag in tags:
            previous_source.content = previous_source.content.replace(tag, "")

        # write the new file contents to disk
        common.save_file(previous_source.name, previous_source.content,
                         'codegen')
        
    elif not multiple_files:
        global output_file
        em = "".join(_current_extra_modules.keys())
        content = output_file.getvalue().replace(
            '#<%swxGlade extra_modules>\n' % nonce, em)
        output_file.close()
        try:
            common.save_file(output_file_name, content, 'codegen')
            # make the file executable
            if _app_added:
                os.chmod(output_file_name, 0755)
        except IOError, e:
            raise XmlParsingError(str(e))
        except OSError: pass # this isn't necessary a bad error
        del output_file


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
        klass.init.extend(['\n', '# code for %s (type %s) not generated: '
                           'no suitable writer found' % (sub_obj.name,
                                                         sub_obj.klass),'\n'])
    else:
        try:
            init, props, layout = builder.get_code(sub_obj)
        except:
            print sub_obj
            raise # this shouldn't happen
        if sub_obj.in_windows: # the object is a wxWindow instance
            # --- patch 2002-08-26 ------------------------------------------
            if sub_obj.is_container and not sub_obj.is_toplevel:
                init.reverse()
                klass.parents_init.extend(init)
            else: klass.init.extend(init)
            # ---------------------------------------------------------------
            mycn = getattr(builder, 'cn', cn)
            if hasattr(builder, 'get_events'):
                evts = builder.get_events(sub_obj)
                for id, event, handler in evts:
                    klass.event_handlers.append((id, event, handler))
            elif 'events' in sub_obj.properties:
                id_name, id = generate_code_id(sub_obj)
                #if id == '-1': id = 'self.%s.GetId()' % sub_obj.name
                if id == '-1': id = '#$self->%s' % sub_obj.name
                for event, handler in sub_obj.properties['events'].iteritems():
                    klass.event_handlers.append((id, event, handler))

        else: # the object is a sizer
            if sub_obj.base == 'wxStaticBoxSizer':
                klass.parents_init.insert(1, init.pop(0))
                # ${staticboxsizername}_staticbox
            klass.sizers_init.extend(init)
        klass.props.extend(props)
        klass.layout.extend(layout)
        if multiple_files and \
               (sub_obj.is_toplevel and sub_obj.base != sub_obj.klass):
            key = 'use %s;\n' % sub_obj.klass
            klass.dependencies[key] = 1
##         for dep in _widget_extra_modules.get(sub_obj.base, []):
        for dep in getattr(obj_builders.get(sub_obj.base),
                           'import_modules', []):
            klass.dependencies[dep] = 1


def add_sizeritem(toplevel, sizer, obj, option, flag, border):
    """\
    writes the code to add the object 'obj' to the sizer 'sizer'
    in the 'toplevel' object.
    """
    # an ugly hack to allow the addition of spacers: if obj_name can be parsed
    # as a couple of integers, it is the size of the spacer to add

    obj_name = obj.name
    try:
        w, h = [ int(s) for s in obj_name.split(',') ]
    except ValueError:
        if obj.in_windows:
            # attribute is a special property, which tells us if the object
            # is a local variable or an attribute of its paren
            if test_attribute(obj):
                obj_name = '$self->{%s}' % obj_name

        if obj.base == 'wxNotebook' and for_version < (2, 6): # deprecated since wxWidgets 2.5.3
            obj_name = 'Wx::NotebookSizer->new(%s)' % obj_name
        elif obj_name[0:1] != '$':
            obj_name = '$self->{%s}' % obj_name

    try:
        klass = classes[toplevel.klass]
    except KeyError:
        klass = classes[toplevel.klass] = ClassLines()

    buffer = '$self->{%s}->Add(%s, %s, %s, %s);\n' % \
             (sizer.name, obj_name, option, flag, border)
    klass.layout.append(buffer)


new_defaults = {
    '$parent' : '\t$parent = undef              unless defined $parent;\n' ,
    '$id'     : '\t$id     = -1                 unless defined $id;\n' ,
    '$title'  : '\t$title  = ""                 unless defined $title;\n' ,
    '$pos'    : '\t$pos    = wxDefaultPosition  unless defined $pos;\n' ,
    '$size'   : '\t$size   = wxDefaultSize      unless defined $size;\n' ,
    '$name'   : '\t$name   = ""                 unless defined $name;\n\n',
#   '$style' is a special case
}

def add_class(code_obj):
    """\
    Generates the code for a custom class.
    """
    global _current_extra_modules
    if not multiple_files:
        # in this case, previous_source is the SourceFileContent instance
        # that keeps info about the single file to generate
        prev_src = previous_source
    else:
        # let's see if the file to generate exists, and in this case
        # create a SourceFileContent instance
        filename = os.path.join(out_dir, code_obj.klass + '.pm') # MODULE!!
        if not os.path.exists(filename): prev_src = None
        else: prev_src = SourceFileContent(filename)
        _current_extra_modules = {}
    
    if classes.has_key(code_obj.klass) and classes[code_obj.klass].done:
        return # the code has already been generated

    try:
        builder = obj_builders[code_obj.base]
    except KeyError:
        print code_obj
        raise # this is an error, let the exception be raised

    if prev_src is not None and prev_src.classes.has_key(code_obj.klass):
        is_new = False
    else:
        # this class wasn't in the previous version of the source (if any)
        is_new = True
        mods = getattr(builder, 'extra_modules', [])
        if mods:
            for m in mods: _current_extra_modules[m] = 1

    buffer = []
    write = buffer.append

    if not classes.has_key(code_obj.klass):
        # if the class body was empty, create an empty ClassLines
        classes[code_obj.klass] = ClassLines()

    new_signature = getattr(builder, 'new_signature', [] )

    if _use_gettext and not is_new:
        classes[code_obj.klass].dependencies[
            "use Wx::Locale gettext => '_T';\n"]=1

    if is_new:
        write('package %s;\n\n' % code_obj.klass )
        write('use Wx qw[:everything];\nuse base qw(%s);\nuse strict;\n\n'
                % code_obj.base.replace('wx','Wx::',1) )

        if _use_gettext:
            if multiple_files:
                classes[code_obj.klass].dependencies[
                    "use Wx::Locale gettext => '_T';\n"]=1
            else:
                write("use Wx::Locale gettext => '_T';\n")

        if multiple_files:
            # write the module dependecies for this class (package)
            write('# begin wxGlade: ::dependencies\n')
            for module in classes[code_obj.klass].dependencies:
                write(module)
            write('# end wxGlade\n')
            write('\n')

        write('sub new {\n')

        write("\tmy( $self, %s ) = @_;\n"
                % ", ".join( new_signature  ) )

        if new_signature:
            for k in new_signature :
                if new_defaults.has_key(k) :
                    write( new_defaults[k] )
        else:
            new_signature = [ '@_[1 .. $#_]' ] # shift(@_)->SUPER::new(@_);
            print code_obj.klass + " did not declare new_defaults "


    # constructor (new) begin tag
    write('# begin wxGlade: %s::new\n\n' % code_obj.klass)
    prop = code_obj.properties
    style = prop.get("style", None)
    if style:
        write('\t$style = %s \n\t\tunless defined $style;\n\n' % style)

    # constructor (new)
    write('\t$self = $self->SUPER::new( %s );\n'
          % ", ".join( new_signature ))

    init_lines = classes[code_obj.klass].init

    parents_init = classes[code_obj.klass].parents_init
    parents_init.reverse()

    for l in parents_init: write('\t' +l)
    for l in init_lines: write('\t' +l)

    # now check if there are extra lines to add to the init method
    if hasattr(builder, 'get_init_code'):
        for l in builder.get_init_code(code_obj): write('\t' + l)
    
    write('\n\t$self->__set_properties();\n')
    write('\t$self->__do_layout();\n\n')

    event_handlers = classes[code_obj.klass].event_handlers
    if hasattr(builder, 'get_events'):
        for id, event, handler in builder.get_events(code_obj):
            event_handlers.append((id, event, handler))
    for win_id, event, handler in event_handlers:
        if win_id.startswith('#'):
            win_id = '$self->{' + win_id[8:] + '}->GetId'
        write('\tWx::Event::%s($self, %s, \\&%s);\n' % \
              (event, win_id, handler))
    if event_handlers: write('\n')
   
    write('# end wxGlade\n')

    if is_new:
        write('\treturn $self;\n\n')
        write('}\n\n')

    if prev_src is not None and not is_new:
        # replace the lines inside the ::new wxGlade block with the new ones
        tag = '#<%swxGlade replace %s %s>' % (nonce, code_obj.klass, 'new')
        if prev_src.content.find(tag) < 0:
            # no __init__ tag found, issue a warning and do nothing
            print >> sys.stderr, "WARNING: wxGlade ::new block not found," \
                  " constructor code NOT generated"
        else:
            prev_src.content = prev_src.content.replace(tag, "".join(buffer))
        buffer = []
        write = buffer.append

    # __set_properties
    obj_p = getattr(builder, 'get_properties_code',
                    generate_common_properties)(code_obj)
    obj_p.extend(classes[code_obj.klass].props)
    write_body = len(obj_p)

    if is_new: write('\nsub __set_properties {\n\tmy $self = shift;\n\n')

    write('# begin wxGlade: %s::__set_properties\n\n' % code_obj.klass)

    if not write_body: write('\treturn;\n')
    else:
        for l in obj_p: write('\t' +l)

    write('\n# end wxGlade\n')

    if is_new: write('}\n')

    if prev_src is not None and not is_new:
        # replace the lines inside the __set_properties wxGlade block
        # with the new ones
        tag = '#<%swxGlade replace %s %s>' % (nonce, code_obj.klass,
                                             '__set_properties')
        if prev_src.content.find(tag) < 0:
            # no __set_properties tag found, issue a warning and do nothing
            print >> sys.stderr, "WARNING: wxGlade __set_properties block " \
                  "not found, __set_properties code NOT generated"
        else:
            prev_src.content = prev_src.content.replace(tag, "".join(buffer))
        buffer = []
        write = buffer.append

    # __do_layout
    if is_new: write('\nsub __do_layout {\n\tmy $self = shift;\n\n')
    layout_lines = classes[code_obj.klass].layout
    sizers_init_lines = classes[code_obj.klass].sizers_init

    # check if there are extra layout lines to add
    if hasattr(builder, 'get_layout_code'):
        extra_layout_lines = builder.get_layout_code(code_obj)
    else:
        extra_layout_lines = []

    write('# begin wxGlade: %s::__do_layout\n\n' % code_obj.klass)

    if layout_lines or sizers_init_lines or extra_layout_lines:
        sizers_init_lines.reverse()
        for l in sizers_init_lines  : write('\t' +l)
        for l in layout_lines       : write('\t' +l)
        for l in extra_layout_lines : write('\t' +l)
    else: write('\treturn;\n')

    write('\n# end wxGlade\n')

    if is_new: write('}\n')

    if prev_src is not None and not is_new:
        # replace the lines inside the __do_layout wxGlade block
        # with the new ones
        tag = '#<%swxGlade replace %s %s>' % (nonce, code_obj.klass,
                                             '__do_layout')
        if prev_src.content.find(tag) < 0:
            # no __do_layout tag found, issue a warning and do nothing
            print >> sys.stderr, "WARNING: wxGlade __do_layout block " \
                  "not found, __do_layout code NOT generated"
        else:
            prev_src.content = prev_src.content.replace(tag, "".join(buffer))

    if prev_src is not None and not is_new:
        already_there = prev_src.event_handlers.get(code_obj.klass, {})
        buf = []
        for name, event, handler in event_handlers:
            if handler not in already_there:
                buf.append('\nsub %s {\n'
                           '\tmy ($self, $event) = @_;\n'
                           '# wxGlade: %s::%s <event_handler>\n\n'
                           '\twarn "Event handler (%s) not implemented";\n'
                           '\t$event->Skip;\n\n# end wxGlade\n}\n\n'
                       % (handler, code_obj.klass,handler, handler))
                already_there[handler] = 1

        tag = '#<%swxGlade event_handlers %s>' % (nonce, code_obj.klass)
        if prev_src.content.find(tag) < 0:
            # no event_handlers tag found, issue a warning and do nothing
            print >> sys.stderr, "WARNING: wxGlade event_handlers block " \
                  "not found, event_handlers code NOT generated"
        else:
            prev_src.content = prev_src.content.replace(tag, "".join(buf))
        del buf
    else:
        already_there = {}
        for name, event, handler in event_handlers:
            if handler not in already_there:
                write('\nsub %s {\n'
                      '\tmy ($self, $event) = @_;\n'
                      '# wxGlade: %s::%s <event_handler>\n\n'
                      '\twarn "Event handler (%s) not implemented";\n'
                      '\t$event->Skip;\n\n# end wxGlade\n}\n\n'
                      % (handler, code_obj.klass,handler, handler))
                already_there[handler] = 1

    # the code has been generated
    classes[code_obj.klass].done = True

    write('\n# end of class %s\n\n1;\n\n' % code_obj.klass)

    if not multiple_files and prev_src is not None:
        # if this is a new class, add its code to the new_classes list of the
        # SourceFileContent instance
        if is_new: prev_src.new_classes.append("".join(buffer))
        return

    if multiple_files:
        #return # not implemented yet -- crazyinsomniac
        if prev_src is not None:
            tag = '#<%swxGlade insert new_classes>' % nonce
            prev_src.content = prev_src.content.replace(tag, "")

            # insert the extra modules
            tag = '#<%swxGlade extra_modules>\n' % nonce
            code = "".join(_current_extra_modules.keys())
            prev_src.content = prev_src.content.replace(tag, code)
            
            # insert the module dependencies of this class
            extra_modules = classes[code_obj.klass].dependencies.keys()
            deps = ['# begin wxGlade: ::dependencies\n'] + extra_modules + \
                   ['# end wxGlade\n']
            tag = '#<%swxGlade replace %s dependencies>' % \
                    (nonce, code_obj.klass)
            prev_src.content = prev_src.content.replace(tag, "".join(deps))
            
            try:
                # store the new file contents to disk
                common.save_file(filename, prev_src.content, 'codegen')
            except:
                raise IOError("pl_codegen.add_class: %s, %s, %s" % \
                              (out_dir, prev_src.name, code_obj.klass))
            return

        # create the new source file
        filename = code_obj.klass.replace('::', os.sep ) + '.pm'  # MODULE!!
        filename = os.path.join(out_dir, filename ) 
        out = cStringIO.StringIO()
        write = out.write
        # write the common lines
        for line in header_lines: write(line)
        
        # write the class body
        for line in buffer: write(line)
        try:
            dirname = os.path.dirname(filename) # create Foo in Foo::Bar,
                                                # Foo/Bar.pm
            if not os.path.exists(dirname):
                os.makedirs(dirname)
            # store the contents to filename
            common.save_file(filename, out.getvalue(), 'codegen')
        except:
            import traceback; traceback.print_exc()
        out.close()
    else: # not multiple_files
        # write the class body onto the single source file
        for dep in classes[code_obj.klass].dependencies:
            _current_extra_modules[dep] = 1
        write = output_file.write
        for line in buffer: write(line)
        

_app_added = False


def add_app(app_attrs, top_win_class):
    """\
    Generates the code for a wxApp instance.
    If the file to write into already exists, this function does nothing.
    """
    global _app_added
    _app_added = True
    
    name = app_attrs.get('name')
    if not name: name = 'app'

    if not multiple_files: prev_src = previous_source
    else:
        filename = os.path.join(out_dir, name + '.pl')
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
    if not top_win: return # do nothing if there is no top window
    lines = []
    append = lines.append

    if klass:
        append('package %s;\n' % klass)
        append('\nuse base qw(Wx::App);\nuse strict;\n\n')
        if multiple_files:
            # import the top window module
            append('use %s;\n\n' % top_win_class)
        append('sub OnInit {\n\tmy( $self ) = shift;\n\n')
    else:
        append('1;\n\npackage main;\n')
        if multiple_files:
            # import the top window module
            append('\nuse %s;\n\n' % top_win_class )
        append('\nunless(caller){\n')
        if _use_gettext:
            append('\tmy $local = Wx::Locale->new("English", "en", "en");'
                + ' # replace with ??\n')
            append('\t$local->AddCatalog("%s");' % name
                + ' # replace with the appropriate catalog name\n\n' )
# and now,  basically fake wxPySimpleApp
        append('\tlocal *Wx::App::OnInit = sub{1};\n')
        append('\tmy $%s = Wx::App->new();\n'% name)
        
    append('\tWx::InitAllImageHandlers();\n\n') # we add this to avoid troubles
    append('\tmy $%s = %s->new();\n\n' % (top_win, top_win_class))

    if klass:
        append('\t$self->SetTopWindow($%s);\n' % top_win)
        append('\t$%s->Show(1);\n\n' % top_win)
        append('\treturn 1;\n}\n')
        append('# end of class %s\n\n' % klass)
        append('package main;\n\nunless(caller){\n')

        if _use_gettext:
            append('\tmy $local = Wx::Locale->new("English", "en", "en");'
                + ' # replace with ??\n')
            append('\t$local->AddCatalog("%s");'  % name
                + ' # replace with the appropriate catalog name\n\n')
        append('\tmy $%s = %s->new();\n' % (name, klass))
    else:
        append('\t$%s->SetTopWindow($%s);\n' % (name, top_win))
        append('\t$%s->Show(1);\n' % top_win)
    append('\t$%s->MainLoop();\n}\n' % name)

    if multiple_files: # not read yet
        filename = os.path.join(out_dir, name + '.pl')
        out = cStringIO.StringIO()
        write = out.write
        write('#!/usr/bin/perl -w -- \n')
        # write the common lines
        for line in header_lines: write(line)
        # write the wxApp code
        for line in lines: write(line)
        try:
            common.save_file(filename, out.getvalue(), 'codegen')
        except:
            import traceback; traceback.print_wexc()
        # make the file executable
        try: os.chmod(filename, 0755)
        except OSError: pass # this is not a bad error
        out.close()
    else:
        write = output_file.write
        for line in lines: write(line)


def _get_code_name(obj):
    """\
    returns the name of the variable ( either $self, $foo, or $self->{foo} )
    """
    if obj.is_toplevel:
        return '$self'
    else:
        if test_attribute(obj):
            return '$self->{%s}' % obj.name
        else:  # it's an already declared lexical (my $foo)
            if obj.name[0] == '$':
                return obj.name
            else:
                return '$' + obj.name 


def generate_code_size(obj):
    """\
    returns the code fragment that sets the size of the given object.
    """
    name = _get_code_name(obj)
    size = obj.properties.get('size', '').strip()
    use_dialog_units = (size[-1] == 'd')
    if for_version < (2, 5) or obj.parent is None:
        method = 'SetSize'
    else:
        method = 'SetMinSize'
    if use_dialog_units:
        return name + '->' + method + \
               '(%s->ConvertDialogSizeToPixels(Wx::Size->new(%s)));\n' % \
               (name, size[:-1])

    return name + '->' + method + '(Wx::Size->new(%s));\n' % size


def _string_to_colour(s):
    return '%d, %d, %d' % (int(s[1:3], 16), int(s[3:5], 16), int(s[5:], 16))


def generate_code_foreground(obj): 
    """\
    returns the code fragment that sets the foreground colour of
    the given object.
    """
    self = _get_code_name(obj)
    try:
        color = 'Wx::Colour->new(%s)' % \
                _string_to_colour(obj.properties['foreground'])
    except (IndexError, ValueError): # the color is from system settings
        color = 'Wx::SystemSettings::GetColour(%s)' % \
                obj.properties['foreground']
    return self + '->SetForegroundColour(%s);\n' % color


def generate_code_background(obj):
    """\
    returns the code fragment that sets the background colour of
    the given object.
    """
    self = _get_code_name(obj)
    try:
        color = 'Wx::Colour->new(%s)' % \
                _string_to_colour(obj.properties['background'])
    except (IndexError, ValueError): # the color is from system settings
        color = 'Wx::SystemSettings::GetColour(%s)' % \
                obj.properties['background']
    return self + '->SetBackgroundColour(%s);\n' % color


def generate_code_font(obj):
    """\
    returns the code fragment that sets the font of the given object.
    """
    font = obj.properties['font'] 
    size = font['size']; family = font['family']
    underlined = font['underlined']
    style = font['style']; weight = font['weight']
    face = '"%s"' % font['face'].replace('"', r'\"')
    self = _get_code_name(obj)
    return self + \
        '->SetFont(Wx::Font->new(%s, %s, %s, %s, %s, %s));\n' % \
            (size, family, style, weight, underlined, face)


def generate_code_id(obj, id=None):
    """\
    returns a 2-tuple of strings representing the LOC that sets the id of the
    given object: the first line is the declaration of the variable, and is
    empty if the object's id is a constant, and the second line is the value
    of the id
    """
    if obj and id is None:
        id = obj.properties.get('id')
        
    if id is None:
        return '', '-1'
    tokens = id.split('=')
    if len(tokens) > 1:
        name, val = tokens[:2]
    else:
        return '', tokens[0] # we assume name is declared elsewhere
    if not name:
        return '', val
    if val.strip() == '?':
        val = 'Wx::NewId()'
    return ('use constant %s => %s;\n' % (name, val), name)


def generate_code_tooltip(obj):
    """\
    returns the code fragment that sets the tooltip of the given object.
    """
    self = _get_code_name(obj)
    return self + \
        '->SetToolTipString(%s);\n' % quote_str(obj.properties['tooltip'])


def generate_code_disabled(obj):
    self = _get_code_name(obj)
    try:
        disabled = int(obj.properties['disabled'])
    except:
        disabled = False

    if disabled:
        return self + '->Enable(0);\n' # Disable() is not available


def generate_code_focused(obj):
    self = _get_code_name(obj)
    try:
        focused = int(obj.properties['focused'])
    except:
        focused = False

    if focused:
        return self + '->SetFocus();\n'


def generate_code_hidden(obj):
    self = _get_code_name(obj)
    try:
        hidden = int(obj.properties['hidden'])
    except:
        hidden = False

    if hidden:
        return  self + '->Show(0);\n' # Hide() is not available
    

def generate_code_extraproperties(obj):
    self = _get_code_name(obj)
    prop = obj.properties['extraproperties']
    ret = []
    for name in sorted(prop):
        ret.append(self + '->Set%s(%s);\n' % (name, prop[name]))
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
    # tooltip
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
        else:
            self.current = None
            
    def end_elem(self, name, code_obj):
        if name == 'font':
            code_obj.properties['font'] = self.attrs
            return True
        elif self.current is not None:
            decode = self.dicts.get(self.current)
            if decode:
                val = decode.get("".join(self.curr_data), '0')
            else:
                val = "".join(self.curr_data)

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


# map of widget class names to a list of extra modules needed for the
# widget. Example: 'wxGrid': 'use Wx::Grid;\n'
_widget_extra_modules = {}

# set of lines of extra modules to add to the current file
_current_extra_modules = {} 

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
    
    """list of modules to import (eg. ['use Wx::Grid;\n'])"""
    import_modules = []
    
    """constructor signature ($self->SUPER::new(@stuff), see new_defaults )"""
    new_signature = []

    def get_code(self, obj):
        """\
        Handler for normal widgets (non-toplevel): returns 3 lists of strings,
        init, properties and layout, that contain the code for the
        corresponding methods of the class to generate
        """
        return [], [], []

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



def setup():
    """\
    Code generator setup function. This is called once, when the code
    generator is loaded in wxglade.
    """
    # scan widgets.txt for widgets, load perl_codegen's
    _widgets_dir = os.path.join(common.wxglade_path, 'widgets')
    widgets_file = os.path.join(_widgets_dir, 'widgets.txt')
    if not os.path.isfile(widgets_file):
        print >> sys.stderr, "widgets file (%s) doesn't exist" % widgets_file
        return
    import sys
    sys.path.append(_widgets_dir)
    modules = open(widgets_file)
    for line in modules:
        module_name = line.strip()
        if not module_name or module_name.startswith('#'): continue
        module_name = module_name.split('#')[0].strip()
        try:
            m = __import__(
                module_name + '.perl_codegen', {}, {}, ['initialize'])
            m.initialize()
        except (ImportError, AttributeError):
            pass
##             print 'ERROR loading "%s"' % module_name
##             import traceback;
##             traceback.print_exc()
##         else:
##             print 'initialized perl generator for ', module_name
    modules.close()

    # ...then, the sizers
    import edit_sizers.perl_sizers_codegen
    edit_sizers.perl_sizers_codegen.initialize()  
