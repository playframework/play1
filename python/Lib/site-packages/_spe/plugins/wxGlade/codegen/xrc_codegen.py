# xrc_codegen.py: wxWidgets resources XRC code generator
# $Id: xrc_codegen.py,v 1.21 2007/03/27 07:02:06 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

"""\
Generates the xml code for the app in XRC format.
Calls the appropriate ``writers'' of the various objects. These functions
return an instance of XrcObject
"""

import common, config
import sys
import cStringIO
from xml_parse import XmlParsingError
from xml.sax.saxutils import escape, quoteattr, unescape

language = "XRC"
writer = sys.modules[__name__]

# default extensions for generated files: a list of file extensions
default_extensions = ['xrc']

# output string buffer for the code 
output_file = None

# name of the output file
output_file_name = None

# dictionary of ``writers'' for the various objects
obj_builders = {}

# current indentation level
curr_tab = 0
def tabs(number): return '    ' * number

# encoding of the application
app_encoding = 'ISO-8859-1' # default, if nothing else found


class XrcObject:
    """\
    Class to produce the XRC code for a given widget. This is a base class
    which does nothing
    """
    def __init__(self):
        self.properties = {}
        self.children = [] # sub-objects

    def write_child_prologue(self, child, out_file, ntabs): pass
    def write_child_epilogue(self, child, out_file, ntabs): pass
    def write_property(self, name, val, outfile, ntabs): pass
    def write(self, out_file, ntabs): pass

# end of class XrcObject


"""\
dictionary of active XrcObject instances: during the code generation it stores
all the non-sizer objects that have children (i.e. frames, dialogs, panels,
notebooks, etc.), while at the end of the code generation, before finalize
is called, it contains only the true toplevel objects (frames and dialogs), and
is used to write their XML code (see finalize). The other objects are deleted
when add_object is called with their corresponding code_object as argument (see
add_object)
"""
xrc_objects = None 


class SizerItemXrcObject(XrcObject):
    """\
    XrcObject to handle sizer items
    """
    def __init__(self, obj, option, flag, border):
        XrcObject.__init__(self)
        self.obj = obj # the XrcObject representing the widget
        self.option = option
        self.flag = flag
        self.border = border
        
    def write(self, out_file, ntabs):
        write = out_file.write
        write(tabs(ntabs) + '<object class="sizeritem">\n')
        if self.option != '0':
            write(tabs(ntabs+1) + '<option>%s</option>\n' % self.option)
        if self.flag and self.flag != '0':
            write(tabs(ntabs+1) + '<flag>%s</flag>\n' % self.flag)
        if self.border != '0':
            write(tabs(ntabs+1) + '<border>%s</border>\n' % self.border)
        # write the widget
        self.obj.write(out_file, ntabs+1)
        write(tabs(ntabs) + '</object>\n')
        
# end of class SizerItemXrcObject


class SpacerXrcObject(XrcObject):
    """\
    XrcObject to handle widgets
    """
    def __init__(self, size_str, option, flag, border):
        self.size_str = size_str
        self.option = option
        self.flag = flag
        self.border = border

    def write(self, out_file, ntabs):
        write = out_file.write
        write(tabs(ntabs) + '<object class="spacer">\n')
        write(tabs(ntabs+1) + '<size>%s</size>\n' % self.size_str.strip())
        if self.option != '0':
            write(tabs(ntabs+1) + '<option>%s</option>\n' % self.option)
        if self.flag and self.flag != '0':
            write(tabs(ntabs+1) + '<flag>%s</flag>\n' % self.flag)
        if self.border != '0':
            write(tabs(ntabs+1) + '<border>%s</border>\n' % self.border)
        write(tabs(ntabs) + '</object>\n')

# end of class SpacerXrcObject


class DefaultXrcObject(XrcObject):
    """\
    Standard XrcObject for every widget, used if no specific XrcObject is
    available
    """
    def __init__(self, code_obj):
        XrcObject.__init__(self)
        self.properties = code_obj.properties
        self.code_obj = code_obj
        self.name = code_obj.name
        self.klass = code_obj.base # custom classes aren't allowed in XRC
        self.subclass = code_obj.klass

    def write_property(self, name, val, outfile, ntabs):
        if val:
            name = escape(name)
            outfile.write(tabs(ntabs) + '<%s>%s</%s>\n' % \
                          (name, escape(val), name))

    def write(self, out_file, ntabs):
        write = out_file.write
        if self.code_obj.in_sizers:
            write(tabs(ntabs) + '<object class=%s>\n' % quoteattr(self.klass))
        else:
            if self.subclass and self.subclass != self.klass:
                write(tabs(ntabs) +
                      '<object class=%s name=%s subclass=%s>\n' % \
                      (quoteattr(self.klass), quoteattr(self.name),
                       quoteattr(self.subclass)))
            else:
                write(tabs(ntabs) + '<object class=%s name=%s>\n' % \
                      (quoteattr(self.klass), quoteattr(self.name)))
        tab_str = tabs(ntabs+1)
        # write the properties
        if self.properties.has_key('foreground'):
            if self.properties['foreground'].startswith('#'):
                # XRC does not support colors from system settings
                self.properties['fg'] = self.properties['foreground']
            del self.properties['foreground']
        if self.properties.has_key('background'):
            if self.properties['background'].startswith('#'):
                # XRC does not support colors from system settings
                self.properties['bg'] = self.properties['background']
            del self.properties['background']
        if self.properties.has_key('font'):
            font = self.properties['font']
            del self.properties['font']
        else: font = None
        style = str(self.properties.get('style', ''))
        if style and style == '0':
            del self.properties['style']

        if 'id' in self.properties:
            del self.properties['id'] # id has no meaning for XRC

        # ALB 2004-12-05
        if 'events' in self.properties:
            #del self.properties['events'] # no event handling in XRC
            for handler, event in self.properties['events'].iteritems():
                write(tab_str + '<handler event=%s>%s</handler>\n' % \
                      (quoteattr(handler), escape(event)))
            del self.properties['events']

        # 'disabled' property is actually 'enabled' for XRC
        if 'disabled' in self.properties:
            try: val = int(self.properties['disabled'])
            except: val = False
            if val:
                self.properties['enabled'] = '0'
            del self.properties['disabled']

        # ALB 2007-08-31 extracode property
        if 'extracode' in self.properties:
            write(self.properties['extracode'].replace('\\n', '\n'))
            del self.properties['extracode']

        # custom base classes are ignored for XRC...
        if 'custom_base' in self.properties:
            del self.properties['custom_base']
            
        if 'extraproperties' in self.properties:
            prop = self.properties['extraproperties']
            del self.properties['extraproperties']
            self.properties.update(prop)
            
        for name, val in self.properties.iteritems():
            self.write_property(str(name), val, out_file, ntabs+1)
        # write the font, if present
        if font:
            write(tab_str + '<font>\n')
            tab_str = tabs(ntabs+2)
            for key, val in font.iteritems():
                if val:
                    write(tab_str + '<%s>%s</%s>\n' % \
                          (escape(key), escape(val), escape(key)))
            write(tabs(ntabs+1) + '</font>\n')
        # write the children
        for c in self.children:
            self.write_child_prologue(c, out_file, ntabs+1)
            c.write(out_file, ntabs+1)
            self.write_child_epilogue(c, out_file, ntabs+1)
        write(tabs(ntabs) + '</object>\n')
        
# end of class DefaultXrcObject


class NotImplementedXrcObject(XrcObject):
    """\
    XrcObject used when no code for the widget can be generated (for example,
    because XRC does not currently handle such widget)
    """
    def __init__(self, code_obj):
        XrcObject.__init__(self)
        self.code_obj = code_obj
        
    def write(self, outfile, ntabs):
        m = 'code generator for %s objects not available' % self.code_obj.base
        print >> sys.stderr, 'WARNING: %s' % m 
        outfile.write(tabs(ntabs) + '<!-- %s -->\n' % m)

# end of class NotImplementedXrcObject


def initialize(app_attrs): #out_path, multi_files):
    """\
    Code generator initialization function.
    """
    out_path = app_attrs['path']
    multi_files = app_attrs['option']
    
    global output_file, curr_tab, xrc_objects, output_file_name, app_encoding
    # first, set the app encoding
    if 'encoding' in app_attrs:
        app_encoding = app_attrs['encoding']
        # wx doesn't like latin-1
        if app_encoding == 'latin-1': app_encoding = 'ISO-8859-1'
    if multi_files:
        # for now we handle only single-file code generation
        raise IOError("XRC code cannot be split into multiple files")
    output_file_name = out_path
    output_file = cStringIO.StringIO() #open(out_path, 'w')
    from time import asctime
    header_lines = ['<?xml version="1.0" encoding="%s"?>' % app_encoding, 
                    '<!-- generated by wxGlade %s on %s%s -->' % \
                    (common.version, asctime(), common.generated_from())]
    if not config.preferences.write_timestamp:
        header_lines[1] = '<!-- generated by wxGlade %s%s -->' % \
                          (common.version, common.generated_from())

    for line in header_lines:
        output_file.write(line + '\n')
    output_file.write('\n<resource version="2.3.0.1">\n')
    curr_tab = 1
    xrc_objects = {}


def finalize():
    """\
    Code generator finalization function.
    """
    # write the code for every toplevel object
    for obj in xrc_objects.itervalues():
        obj.write(output_file, 1)
    output_file.write('</resource>\n')
    #output_file.close()
    # store the contents to file
    common.save_file(output_file_name, output_file.getvalue(), 'codegen')


def add_object(unused, sub_obj):
    """\
    Adds the object sub_obj to the XRC tree. The first argument is unused.
    """
    # what we need in XRC is not top_obj, but sub_obj's true parent
    top_obj = sub_obj.parent
    builder = obj_builders.get(sub_obj.base, DefaultXrcObject)
    try:
        # check whether we already created the xrc_obj
        xrc_obj = sub_obj.xrc
    except AttributeError:
        xrc_obj = builder(sub_obj) # builder functions must return a subclass
                                   # of XrcObject
        sub_obj.xrc = xrc_obj
    else:
        # if we found it, remove it from the xrc_objects dictionary (if it was
        # there, i.e. the object is not a sizer), because this isn't a true
        # toplevel object
        if sub_obj in xrc_objects:
            del xrc_objects[sub_obj]
    # let's see if sub_obj's parent already has an XrcObject: if so, it is
    # temporairly stored in the xrc_objects dict...
    try: top_xrc = xrc_objects[top_obj]
    except KeyError:
        # ...otherwise, create it and store it in the xrc_objects dict
        top_xrc = obj_builders.get(top_obj.base, DefaultXrcObject)(top_obj)
        top_obj.xrc = top_xrc
        xrc_objects[top_obj] = top_xrc
    top_obj.xrc.children.append(xrc_obj)


def add_sizeritem(unused, sizer, obj, option, flag, border):
    """\
    Adds a sizeritem to the XRC tree. The first argument is unused.
    """
    # what we need in XRC is not toplevel, but sub_obj's true parent
    toplevel = obj.parent
    top_xrc = toplevel.xrc
    obj_xrc = obj.xrc
    try: sizer_xrc = sizer.xrc
    except AttributeError:
        # if the sizer has not an XrcObject yet, create it now
        sizer_xrc = obj_builders.get(sizer.base, DefaultXrcObject)(sizer)
        sizer.xrc = sizer_xrc
    # we now have to move the children from 'toplevel' to 'sizer' 
    index = top_xrc.children.index(obj_xrc)
    if obj.klass == 'spacer':
        w = obj.properties.get('width', '0')
        h = obj.properties.get('height', '0')
        obj_xrc = SpacerXrcObject('%s, %s' % (w, h), str(option), str(flag),
                                  str(border))
        sizer.xrc.children.append(obj_xrc)
    else:
        sizeritem_xrc = SizerItemXrcObject(obj_xrc, str(option), str(flag),
                                           str(border))
        sizer.xrc.children.append(sizeritem_xrc)
    del top_xrc.children[index]


def add_class(code_obj):
    """\
    Add class behaves very differently for XRC output than for other lanaguages
    (i.e. pyhton): since custom classes are not supported in XRC, this has
    effect only for true toplevel widgets, i.e. frames and dialogs. For other
    kinds of widgets, this is equivalent to add_object
    """
    if not xrc_objects.has_key(code_obj):
        builder = obj_builders.get(code_obj.base, DefaultXrcObject)
        xrc_obj = builder(code_obj)
        code_obj.xrc = xrc_obj
        # add the xrc_obj to the dict of the toplevel ones
        xrc_objects[code_obj] = xrc_obj


def add_app(app_attrs, top_win_class):
    # in the case of XRC output, there's no wxApp code to generate
    pass


class FontPropertyHandler:
    def __init__(self):
        self.props = {'size': '', 'family': '', 'style': '', 'weight': '',
                      'underlined': '', 'face': ''}
        self.current = None

    def start_elem(self, name, attrs):
        self.current = name

    def end_elem(self, name, code_obj):
        if name == 'font':
            code_obj.properties['font'] = self.props
            return True # to remove this handler

    def char_data(self, data):
        self.props[self.current] = str(data.strip())

# end of class FontHandler


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


class DummyPropertyHandler:
    """Empty handler for properties that do not need code"""
    def start_elem(self, name, attrs): pass
    def end_elem(self, name, code_obj): return True
    def char_data(self, data): pass

# end of class DummyPropertyHandler


class ExtraPropertiesPropertyHandler(object):
    def __init__(self):
        self.props = {}
        self.prop_name = None
        self.curr_prop = []
        
    def start_elem(self, name, attrs):
        if name == 'property':
            self.prop_name = attrs['name']

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

def add_widget_handler(widget_name, handler, *args, **kwds):
    obj_builders[widget_name] = handler

