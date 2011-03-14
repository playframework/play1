# xml_parse.py: parsers used to load an app and to generate the code
# from an xml file.
# $Id: xml_parse.py,v 1.46 2007/08/07 12:16:44 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY
#
# NOTE: custom tag handler interface (called by XmlWidgetBuilder)
# class CustomTagHandler:
#     def start_elem(self, name, attrs):
#         pass
#     def end_elem(self, name):
#         return True -> the handler must be removed from the Stack
#     def char_data(self, data):
#         return False -> no further processing needed

import os
import common, edit_sizers
from xml.sax import SAXException, make_parser
from xml.sax.handler import ContentHandler
import traceback

# ALB 2005-03-10: importing the module here prevents a segfault with python 2.4
# hmmm... need to investigate this more (it seems that import of
# xml.sax.expatreader should happen before something else... but what?)
import xml.sax.expatreader


if common.use_gui:
    #from wxPython import wx
    import wx

class XmlParsingError(SAXException):
    """\
    Custom exception to report problems during parsing
    """
    locator = None
    def __init__(self, msg):
        if self.locator:
            l = self.locator
            msg += ' _((line: %s, column:  %s))' % (l.getLineNumber(),
                                                 l.getColumnNumber())
        SAXException.__init__(self, msg)

# end of class XmlParsingError


class XmlParser(ContentHandler):
    """\
    'abstract' base class of the parsers used to load an app and to generate
    the code
    """
    def __init__(self):
        self._objects = Stack() # stack of 'alive' objects
        self._windows = Stack() # stack of window objects (derived by wxWindow)
        self._sizers = Stack()  # stack of sizer objects
        self._sizer_item = Stack() # stack of sizer items
        self._curr_prop = None # name of the current property
        self._curr_prop_val = [] # value of the current property (list into
                                 # which the various pieces of char data
                                 # collected are inserted)
        self._appl_started = False
        self.top = self._objects.top
        self.parser = make_parser()
        self.parser.setContentHandler(self)
        self.locator = None # document locator
        
    def parse(self, source):
        self.parser.parse(source)

    def parse_string(self, source):
        from cStringIO import StringIO
        source = StringIO(source)
        self.parser.parse(source)
        source.close()

    def setDocumentLocator(self, locator):
        self.locator = locator
        XmlParsingError.locator = locator
    
    def startElement(self, name, attrs):
        raise NotImplementedError
    
    def endElement(self, name, attrs):
        raise NotImplementedError

    def characters(self, data):
        raise NotImplementedError

    def pop(self):
        try: return self._objects.pop().pop()
        except AttributeError: return None

# end of class XmlParser


class XmlWidgetBuilder(XmlParser):
    """\
    parser used to build the tree of widgets from an xml file
    """
    def startElement(self, name, attrs):
        if name == 'application':
            # get properties of the app
            self._appl_started = True
            app = common.app_tree.app
            encoding = attrs.get("encoding")
            if encoding:
                try: unicode('a', encoding)
                except LookupError: pass
                else:
                    app.encoding = encoding
                    app.encoding_prop.set_value(encoding)
            path = attrs.get("path")
            if path:
                app.output_path = path
                app.outpath_prop.set_value(path)
            name = attrs.get("name")
            if name:
                app.name = name
                app.name_prop.toggle_active(True)
                app.name_prop.set_value(name)
            klass = attrs.get("class")
            if klass:
                app.klass = klass
                app.klass_prop.toggle_active(True)
                app.klass_prop.set_value(klass)
            option = attrs.get("option")
            if option:
                try: option = int(option)
                except ValueError: option = 0
                app.codegen_opt = option
                app.codegen_prop.set_value(option)
            language = attrs.get('language')
            if language:
                app.codewriters_prop.set_str_value(language)
                app.set_language(language)
            top_win = attrs.get("top_window")
            if top_win: self.top_window = top_win
            try: use_gettext = int(attrs["use_gettext"])
            except (KeyError, ValueError): use_gettext = False
            if use_gettext:
                app.use_gettext = True
                app.use_gettext_prop.set_value(True)
                
            try: is_template = int(attrs["is_template"])
            except (KeyError, ValueError): is_template = False
            app.is_template = is_template
            
            try: overwrite = int(attrs['overwrite'])
            except (KeyError, ValueError): overwrite = False
            if overwrite:
                app.overwrite = True
                app.overwrite_prop.set_value(True)
            # ALB 2004-01-18
            try: use_new_namespace = int(attrs['use_new_namespace'])
            except (KeyError, ValueError): use_new_namespace = False
##             app.set_use_new_namespace(use_new_namespace)
##             app.use_new_namespace_prop.set_value(use_new_namespace)
            app.set_use_old_namespace(not use_new_namespace)
            app.use_old_namespace_prop.set_value(not use_new_namespace)
            # ALB 2004-12-05
            try:
                for_version = attrs['for_version']
                app.for_version = for_version
                app.for_version_prop.set_str_value(for_version)
            except KeyError:
                pass
            return
        if not self._appl_started:
            raise XmlParsingError(_("the root of the tree must be <application>"))
        if name == 'object':
            # create the object and push it on the appropriate stacks
            XmlWidgetObject(attrs, self)
        else:
            # handling of the various properties
            try:
                # look for a custom handler to push on the stack
                handler = self.top().obj.get_property_handler(name)
                if handler: self.top().prop_handlers.push(handler)
                # get the top custom handler and use it if there's one
                handler = self.top().prop_handlers.top()
                if handler: handler.start_elem(name, attrs)
            except AttributeError: pass
            self._curr_prop = name

    def endElement(self, name):
        if name == 'application':
            self._appl_started = False
            if hasattr(self, 'top_window'):
                common.app_tree.app.top_window = self.top_window
                common.app_tree.app.top_win_prop.SetStringSelection(
                    self.top_window)
            return
        if name == 'object':
            # remove last object from the stack
            obj = self.pop()
            if obj.klass in ('sizeritem', 'sizerslot'): return
            si = self._sizer_item.top()
            if si is not None and si.parent == obj.parent:
                sprop = obj.obj.sizer_properties
                # update the values
                sprop['option'].set_value(si.obj.option)
                sprop['flag'].set_value(si.obj.flag_str())
                sprop['border'].set_value(si.obj.border)
                # call the setter functions
                obj.obj['option'][1](si.obj.option)
                obj.obj['flag'][1](si.obj.flag_str())
                obj.obj['border'][1](si.obj.border)
        else:
            # end of a property or error
            # 1: set _curr_prop value
            data = common._encode_from_xml("".join(self._curr_prop_val))
            if data:
                try:
                    handler = self.top().prop_handlers.top()
                    if not handler or handler.char_data(data):
                        # if char_data returned False,
                        # we don't have to call add_property
                        self.top().add_property(self._curr_prop, data)
                except AttributeError: pass
            # 2: call custom end_elem handler
            try:
                # if there is a custom handler installed for this property,
                # call its end_elem function: if this returns True, remove
                # the handler from the Stack
                handler = self.top().prop_handlers.top()
                if handler.end_elem(name):
                    self.top().prop_handlers.pop()
            except AttributeError: pass
            self._curr_prop = None
            self._curr_prop_val = []

    def characters(self, data):
        if not data or data.isspace():
            return
        if self._curr_prop is None:
            raise XmlParsingError(_("character data can be present only " \
                                  "inside properties"))
        self._curr_prop_val.append(data)

# end of class XmlWidgetBuilder


class ProgressXmlWidgetBuilder(XmlWidgetBuilder):
    """\
    Adds support for a progress dialog to the widget builder parser 
    """
    def __init__(self, *args, **kwds):
        self.input_file = kwds.get('input_file')
        if self.input_file:
            del kwds['input_file']
            self.size = len(self.input_file.readlines())
            self.input_file.seek(0)
            self.progress = wx.ProgressDialog(_("Loading..."), _("Please wait "
                                              "while loading the app"), 20)
            self.step = 4
            self.i = 1
        else:
            self.size = 0
            self.progress = None
        XmlWidgetBuilder.__init__(self, *args, **kwds)

    def endElement(self, name):
        if self.progress:
            if name == 'application': self.progress.Destroy()
            else:
                if self.locator:
                    where = self.locator.getLineNumber()
                    value = int(round(where*20.0/self.size))
                else:
                    # we don't have any information, so we update the progress
                    # bar ``randomly''
                    value = (self.step*self.i) % 20
                    self.i += 1
                self.progress.Update(value)
        XmlWidgetBuilder.endElement(self, name)

    def parse(self, *args):
        try: XmlWidgetBuilder.parse(self, *args)
        finally:
            if self.progress: self.progress.Destroy()

    def parse_string(self, *args):
        try: XmlWidgetBuilder.parse_string(self, *args)
        finally:
            if self.progress: self.progress.Destroy()

# end of class ProgressXmlWidgetBuilder


class ClipboardXmlWidgetBuilder(XmlWidgetBuilder):
    """\
    Parser used to cut&paste widgets. The differences with XmlWidgetBuilder
    are:
      - No <application> tag in the piece of xml to parse
      - Fake parent, sizer and sizeritem objects to push on the three stacks:
        they keep info about the destination of the hierarchy of widgets (i.e.
        the target of the 'paste' command)
      - The first widget built must be hidden and shown again at the end of
        the operation
    """
    def __init__(self, parent, sizer, pos, option, flag, border):
        XmlWidgetBuilder.__init__(self)
        class XmlClipboardObject:
            def __init__(self, **kwds):
                self.__dict__.update(kwds)
        par = XmlClipboardObject(obj=parent, parent=parent) # fake window obj
        if sizer is not None:
            # fake sizer object
            szr = XmlClipboardObject(obj=sizer, parent=parent)
            sizeritem = Sizeritem()
            sizeritem.option = option
            sizeritem.flag = flag
            sizeritem.border = border
            sizeritem.pos = pos
            # fake sizer item            
            si = XmlClipboardObject(obj=sizeritem, parent=parent) 
        # push the fake objects on the stacks
        self._objects.push(par); self._windows.push(par)
        if sizer is not None:
            self._objects.push(szr); self._sizers.push(szr)
            self._objects.push(si); self._sizer_item.push(si)
        self.depth_level = 0
        self._appl_started = True # no application tag when parsing from the
                                  # clipboard

    def startElement(self, name, attrs):
        if name == 'object' and attrs.has_key('name'):
            # generate a unique name for the copy
            oldname = str(attrs['name'])
            newname = oldname
            i = 0
            while common.app_tree.has_name(newname):
                if not i: newname = '%s_copy' % oldname
                else: newname = '%s_copy_%s' % (oldname, i)
                i += 1
            attrs = dict(attrs)
            attrs['name'] = newname
        XmlWidgetBuilder.startElement(self, name, attrs)
        if name == 'object':
            if not self.depth_level:
                common.app_tree.auto_expand = False
                try:
                    self.top_obj = self.top().obj
                except AttributeError:
                    print _('Exception! obj: %s') % self.top_obj
                    traceback.print_exc()
            self.depth_level += 1

    def endElement(self, name):
        if name == 'object':
            obj = self.top()
            self.depth_level -= 1
            if not self.depth_level:
                common.app_tree.auto_expand = True
                try:
                    # show the first object and update its layout
                    common.app_tree.show_widget(self.top_obj.node)
                    self.top_obj.show_properties()
                    common.app_tree.select_item(self.top_obj.node)
                except AttributeError:
                    print _('Exception! obj: %s') % self.top_obj
                    traceback.print_exc()
        XmlWidgetBuilder.endElement(self, name)

# end of class ClipboardXmlWidgetBuilder


class XmlWidgetObject:
    """\
    A class to encapsulate a widget read from an xml file: its purpose is to
    store various widget attributes until the widget can be created 
    """
    def __init__(self, attrs, parser):
        # prop_handlers is a stack of custom handler functions to set
        # properties of this object
        self.prop_handlers = Stack()
        self.parser = parser
        self.in_windows, self.in_sizers = False, False
        try:
            base = attrs.get('base', None)
            self.klass = attrs['class']
        except KeyError:
            raise XmlParsingError(_("'object' items must have a 'class' " \
                                  "attribute"))
                  
        if base is not None:
            # if base is not None, the object is a widget (or sizer), and not a
            # sizeritem
            sizer = self.parser._sizers.top()
            parent = self.parser._windows.top()
            if parent is not None: parent = self.parent = parent.obj
            else: self.parent = None
            sizeritem = self.parser._sizer_item.top()
            if sizeritem is not None: sizeritem = sizeritem.obj
            if sizer is not None:
                # we must check if the sizer on the top of the stack is
                # really the one we are looking for: to check this
                if sizer.parent != parent:
                    sizer = None
                else: sizer = sizer.obj
            if hasattr(sizeritem, 'pos'):
                pos = sizeritem.pos
            else: pos = None
            
            if parent and hasattr(parent, 'virtual_sizer') and \
                parent.virtual_sizer:
                sizer = parent.virtual_sizer
                sizer.node = parent.node
                sizeritem = Sizeritem()
                if pos is None:
                    pos = sizer.get_itempos(attrs)
            
            # build the widget
            if pos is not None:
                pos = int(pos)
            self.obj = common.widgets_from_xml[base](attrs, parent, sizer,
                                                     sizeritem, pos)
            try:
                #self.obj.klass = self.klass
                self.obj.set_klass(self.klass)
                self.obj.klass_prop.set_value(self.klass)
            except AttributeError: pass
            
            # push the object on the appropriate stack
            if isinstance(self.obj, edit_sizers.SizerBase):
                self.parser._sizers.push(self)
                self.in_sizers = True
            else:
                self.parser._windows.push(self)
                self.in_windows = True

        elif self.klass == 'sizeritem':
            self.obj = Sizeritem()
            self.parent = self.parser._windows.top().obj
            self.parser._sizer_item.push(self)

        elif self.klass == 'sizerslot':
            sizer = self.parser._sizers.top().obj
            assert sizer is not None, \
                   _("malformed wxg file: slots can only be inside sizers!")
            sizer.add_slot()
            self.parser._sizer_item.push(self)
                        
        # push the object on the _objects stack
        self.parser._objects.push(self)

    def pop(self):
        if self.in_windows: return self.parser._windows.pop()
        elif self.in_sizers: return self.parser._sizers.pop()
        else: return self.parser._sizer_item.pop()

    def add_property(self, name, val):
        """\
        adds a property to this widget. This method is not called if there
        was a custom handler for this property, and its char_data method
        returned False
        """
        if name == 'pos': # sanity check, this shouldn't happen...
            print 'add_property pos'
            return 
        try:
            self.obj[name][1](val) # call the setter for this property
            try:
                prop = self.obj.properties[name]
                prop.set_value(val)
                prop.toggle_active(True)
            except AttributeError: pass
        except KeyError:
            # unknown property for this object
            # issue a warning and ignore the property
            import sys
            print >> sys.stderr, _("Warning: property '%s' not supported by " \
                  "this object ('%s') ") % (name, self.obj)

#end of class XmlWidgetObject


class CodeWriter(XmlParser):
    """parser used to produce the source from a given xml file"""
    def __init__(self, writer, input, from_string=False, out_path=None,
                 preview=False, class_names=None):
        # writer: object that actually writes the code
        XmlParser.__init__(self)
        self._toplevels = Stack() # toplevel objects, i.e. instances of a
                                  # custom class
        self.app_attrs = {} # attributes of the app (name, class, top_window)
        self.top_win = ''   # class name of the top window of the app (if any)
        self.out_path = out_path # this allows to override the output path
                                 # specified in the xml file
        
        self.code_writer = writer

        self.preview = preview # if True, we are generating the code for the
                               # preview
                               
        # used in the CustomWidget preview code, to generate better previews
        # (see widgets/custom_widget/codegen.py)
        self.class_names = class_names
        if self.class_names is None: self.class_names = set()

        if from_string: self.parse_string(input)
        else:
            inputfile = None
            try:
                inputfile = open(input)
                self.parse(inputfile)
            finally:
                if inputfile: inputfile.close()

    def startElement(self, name, attrs_impl):
        attrs = {}
        try:
            encoding = self.app_attrs['encoding']
            unicode('a', encoding)
        except (KeyError, LookupError):
            if name == 'application':
                encoding = str(attrs_impl.get('encoding', 'ISO-8859-1'))
            else:
                encoding = 'ISO-8859-1'
        # turn all the attribute values from unicode to str objects
        for attr, val in attrs_impl.items():
            attrs[attr] = common._encode_from_xml(val, encoding)
        if name == 'application':
            # get the code generation options
            self._appl_started = True
            self.app_attrs = attrs
            try:
                attrs['option'] = bool(int(attrs['option']))
                use_multiple_files = attrs['option']
            except (KeyError, ValueError):
                use_multiple_files = attrs['option'] = False
            if self.out_path is None:
                try: self.out_path = attrs['path']
                except KeyError:
                    raise XmlParsingError(_("'path' attribute missing: could "
                                          "not generate code"))
            else: attrs['path'] = self.out_path

            # ALB 2004-11-01: check if the values of
            # use_multiple_files and out_path agree
            if use_multiple_files:
                if not os.path.isdir(self.out_path):
                    raise IOError(_("Output path must be an existing directory"
                                  " when generating multiple files"))
            else:
                if os.path.isdir(self.out_path):
                    raise IOError(_("Output path can't be a directory when "
                                  "generating a single file"))
            
            # initialize the writer
            self.code_writer.initialize(attrs)
            return
        if not self._appl_started:
            raise XmlParsingError(_("the root of the tree must be <application>"))
        if name == 'object':
            # create the CodeObject which stores info about the current widget
            CodeObject(attrs, self, preview=self.preview)
            if attrs.has_key('name') and \
                   attrs['name'] == self.app_attrs.get('top_window', ''):
                self.top_win = attrs['class']
        else:
            # handling of the various properties
            try:
                # look for a custom handler to push on the stack
                w = self.top()
                handler = self.code_writer.get_property_handler(name, w.base)
                if handler: w.prop_handlers.push(handler)
                # get the top custom handler and use it if there's one
                handler = w.prop_handlers.top()
                if handler: handler.start_elem(name, attrs)
            except AttributeError:
                print 'ATTRIBUTE ERROR!!'
                traceback.print_exc()
            self._curr_prop = name

    def endElement(self, name):
        if name == 'application':
            self._appl_started = False
            if self.app_attrs:
                self.code_writer.add_app(self.app_attrs, self.top_win)
            # call the finalization function of the code writer
            self.code_writer.finalize()
            return
        if name == 'object':
            obj = self.pop()
            if obj.klass in ('sizeritem', 'sizerslot'): return
            # at the end of the object, we have all the information to add it
            # to its toplevel parent, or to generate the code for the custom
            # class
            if obj.is_toplevel and not obj.in_sizers:
                self.code_writer.add_class(obj)
            topl = self._toplevels.top()
            if topl:
                self.code_writer.add_object(topl, obj)
                # if the object is not a sizeritem, check whether it
                # belongs to some sizer (in this case,
                # self._sizer_item.top() doesn't return None): if so,
                # write the code to add it to the sizer at the top of
                # the stack
                si = self._sizer_item.top()
                if si is not None and si.parent == obj.parent:
                    szr = self._sizers.top()
                    if not szr: return
                    self.code_writer.add_sizeritem(topl, szr, obj,
                                                   si.obj.option,
                                                   si.obj.flag_str(),
                                                   si.obj.border)
        else:
            # end of a property or error
            # 1: set _curr_prop value
            try:
                encoding = self.app_attrs['encoding']
                unicode('a', encoding)
            except (KeyError, LookupError):
                encoding = 'ISO-8859-1'
            data = common._encode_from_xml(u"".join(self._curr_prop_val),
                                           encoding)
            if data:
                handler = self.top().prop_handlers.top()
                if not handler or handler.char_data(data):
                    # if char_data returned False,
                    # we don't have to call add_property
                    self.top().add_property(self._curr_prop, data)
            # 2: call custom end_elem handler
            try:
                # if there is a custom handler installed for this property,
                # call its end_elem function: if this returns True, remove
                # the handler from the stack
                obj = self.top()
                handler = obj.prop_handlers.top()
                if handler.end_elem(name, obj):
                    obj.prop_handlers.pop()
            except AttributeError: pass
            self._curr_prop = None
            self._curr_prop_val = []

    def characters(self, data):
        if not data or data.isspace(): return
        if self._curr_prop is None:
            raise XmlParsingError(_("character data can only appear inside " \
                                  "properties"))
        self._curr_prop_val.append(data)

# end of class CodeWriter


class CodeObject:
    """\
    A class to store information needed to generate the code for a given object
    """
    def __init__(self, attrs, parser, preview=False):
        self.parser = parser
        self.in_windows = self.in_sizers = False
        self.is_toplevel = False # if True, the object is a toplevel one:
                             # for window objects, this means that they are
                             # instances of a custom class, for sizers, that
                             # they are at the top of the hierarchy
        self.is_container = False # if True, the widget is a container
                                  # (frame, dialog, panel, ...)
        self.properties = {} # properties of the widget/sizer
        # prop_handlers is a stack of custom handler functions to set
        # properties of this object
        self.prop_handlers = Stack()
        self.preview = preview
        try:
            base = attrs.get('base', None)
            self.klass = attrs['class']
        except KeyError:
            raise XmlParsingError(_("'object' items must have a 'class' " \
                                  "attribute"))
        self.parser._objects.push(self)
        self.parent = self.parser._windows.top()
        # -------- added 2002-08-26 to detect container widgets --------------
        if self.parent is not None:
            self.parent.is_container = True
        # -------- end added 2002-08-26 --------------------------------------
        self.base = None
        if base is not None: # this is a ``real'' object, not a sizeritem
            self.name = attrs['name']
            self.base = common.class_names[base]
            can_be_toplevel = common.toplevels.has_key(base)
            if (self.parent is None or self.klass != self.base) and \
                   can_be_toplevel:
                #self.base != 'CustomWidget':
                self.is_toplevel = True
                # ALB 2005-11-19: for panel objects, if the user sets a
                # custom class but (s)he doesn't want the code
                # to be generated...
                if int(attrs.get('no_custom_class', False)) and \
                       not self.preview:
                    self.is_toplevel = False
                    #print 'OK:', str(self)
                    #self.in_windows = True
                    #self.parser._windows.push(self)
                else:
                    self.parser._toplevels.push(self)
            #------------- 2003-05-07: preview --------------------------------
            elif self.preview and not can_be_toplevel and \
                     self.base != 'CustomWidget':
                # if this is a custom class, but not a toplevel one,
                # for the preview we have to use the "real" class
                #
                # ALB 2007-08-04: CustomWidgets handle this in a special way
                # (see widgets/custom_widget/codegen.py)
                self.klass = self.base
            #------------------------------------------------------------------
            
            # temporary hack: to detect a sizer, check whether the name
            # of its class contains the string 'Sizer': TODO: find a
            # better way!!
            if base.find('Sizer') != -1:
                self.in_sizers = True
                if not self.parser._sizers.count(): self.is_toplevel = True
                else:
                    # the sizer is a toplevel one if its parent has not a
                    # sizer yet
                    sz = self.parser._sizers.top()
                    if sz.parent != self.parent: self.is_toplevel = True
                self.parser._sizers.push(self)
            else:
                self.parser._windows.push(self) 
                self.in_windows = True
        else: # the object is a sizeritem
            self.obj = Sizeritem()
            self.obj.flag_s = '0'
            self.parser._sizer_item.push(self)

    def __str__(self):
        return "<xml_code_object: %s, %s, %s>" % (self.name, self.base,
                                                  self.klass)

    def add_property(self, name, value):
        if hasattr(self, 'obj'): # self is a sizeritem
            try:
                if name == 'flag':
##                     flag = 0
##                     for f in value.split('|'):
##                         flag |= Sizeritem.flags[f.strip()]
##                     setattr(self.obj, name, flag)
                    self.obj.flag_s = value.strip()
                else: setattr(self.obj, name, int(value))
            except: raise XmlParsingError(_("property '%s' not supported by " \
                                          "'%s' objects") % (name, self.klass))
        self.properties[name] = value

    def pop(self):
        if self.is_toplevel and not self.in_sizers:
            self.parser._toplevels.pop()
        if self.in_windows: return self.parser._windows.pop()
        elif self.in_sizers: return self.parser._sizers.pop()
        else: return self.parser._sizer_item.pop()

# end of class CodeObject


class Stack:
    def __init__(self):
        self._repr = []

    def push(self, elem):
        self._repr.append(elem)

    def pop(self):
        try: return self._repr.pop()
        except IndexError: return None

    def top(self):
        try: return self._repr[-1]
        except IndexError: return None

    def count(self):
        return len(self._repr)

# end of class Stack


class Sizeritem:
    if common.use_gui:
        flags = { 'wxALL': wx.ALL,
                  'wxEXPAND': wx.EXPAND, 'wxALIGN_RIGHT': wx.ALIGN_RIGHT,
                  'wxALIGN_BOTTOM': wx.ALIGN_BOTTOM,
                  'wxALIGN_CENTER_HORIZONTAL': wx.ALIGN_CENTER_HORIZONTAL,
                  'wxALIGN_CENTER_VERTICAL': wx.ALIGN_CENTER_VERTICAL,
                  'wxLEFT': wx.LEFT, 'wxRIGHT': wx.RIGHT,
                  'wxTOP': wx.TOP,
                  'wxBOTTOM': wx.BOTTOM,
                  'wxSHAPED': wx.SHAPED,
                  'wxADJUST_MINSIZE': wx.ADJUST_MINSIZE, }
        import misc
        if misc.check_wx_version(2, 5, 2):
            flags['wxFIXED_MINSIZE'] = wx.FIXED_MINSIZE
        else:
            flags['wxFIXED_MINSIZE'] = 0

    def __init__(self):
        self.option = self.border = 0
        self.flag = 0

    def __getitem__(self, name):
        if name != 'flag':
            return (None, lambda v: setattr(self, name, v))

        def get_flag(v):
            val = reduce(lambda a, b: a|b,
                         [Sizeritem.flags[t] for t in v.split("|")])
            setattr(self, name, val)
        return (None, get_flag)
##                 lambda v: setattr(self, name,
##                                   reduce(lambda a,b: a|b,
##                                          [Sizeritem.flags[t] for t in
##                                           v.split("|")])))

    def flag_str(self):
        # returns the flag attribute as a string of tokens separated
        # by a '|' (used during the code generation)
        if hasattr(self, 'flag_s'): return self.flag_s
        else:
            try:
                tmp = {}
                for k in self.flags:
                    if self.flags[k] & self.flag:
                        tmp[k] = 1
                # patch to make wxALL work
                remove_wxall = 4
                for k in ('wxLEFT', 'wxRIGHT', 'wxTOP', 'wxBOTTOM'):
                    if k in tmp: remove_wxall -= 1
                if remove_wxall:
                    try: del tmp['wxALL']
                    except KeyError: pass
                else:
                    for k in ('wxLEFT', 'wxRIGHT', 'wxTOP', 'wxBOTTOM'):
                        try: del tmp[k]
                        except KeyError: pass
                    tmp['wxALL'] = 1
                tmp = '|'.join(tmp.keys())
            except:
                print 'EXCEPTION: self.flags = %s, self.flag = %s' % \
                      (self.flags, repr(self.flag))
                raise
            if tmp: return tmp
            else: return '0'

# end of class Sizeritem

