# tree.py: classes to handle and display the structure of a wxGlade app
# $Id: tree.py,v 1.56 2007/08/07 12:13:44 agriggio Exp $
# 
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

#from wxPython.wx import *
import wx
from xml.sax.saxutils import quoteattr
import misc, common, os.path

try: set
except NameError: from sets import Set as set


class Tree:
    """\
    A class to represent a hierarchy of widgets.
    """
    class Node:
        __empty_win = None
        def __init__(self, widget=None, children=None):
            self.widget = widget
            self.children = children
            self.parent = None

        def remove(self):
            def remove_rec(node):
                if node.children is not None:
                    map(remove_rec, node.children)
                node.children = None
                if node.widget is not None:
                    # replace the just destroyed notebook with an empty window
                    pw = node.widget.property_window
                    pw.SetTitle(_('Properties - <>'))
                    if Tree.Node.__empty_win is None:
                        Tree.Node.__empty_win = wx.Window(pw, -1)
                    pw.GetSizer().GetChildren()[0].SetWindow(
                        Tree.Node.__empty_win)
                    #wxNotebook(node.widget.property_window, -1))
                    # call the widget's ``destructor''
                    node.widget.delete()
                node.widget = None
            remove_rec(self)
            try: self.parent.children.remove(self)
            except: pass
                    
        def __repr__(self):
            try: return self.widget.name
            except AttributeError: return repr(self.widget)

        def write(self, outfile, tabs, class_names=None):
            """\
            Writes the xml code for the widget to the given output file
            """
            import edit_sizers
            fwrite = outfile.write
            assert self.widget is not None
            w = self.widget
            classname = getattr(w, '_classname', w.__class__.__name__)
            # ALB 2005-11-19: to disable custom class code generation
            # (for panels...)
            no_custom = ""
            if getattr(w, 'no_custom_class', False):
                no_custom = ' no_custom_class="1"'
            fwrite('    ' * tabs + '<object class=%s name=%s base=%s%s>\n'
                   % (quoteattr(w.klass), quoteattr(w.name),
                      quoteattr(classname), no_custom))
            for p in w.properties:
                w.properties[p].write(outfile, tabs+1)

            if class_names is not None:
                class_names.add(w.klass)

            if isinstance(w, edit_sizers.SizerBase):
                maxpos = len(w.children)
                children = self.children is not None and self.children or []
                tmp = {}
                for c in children:
                    tmp[c.widget.pos] = c
                children = []
                class SlotNode:
                    def write(self, outfile, tabs):
                        fwrite('    ' * tabs + '<object class="sizerslot" />\n')
                for i in range(1, maxpos):
                    children.append(tmp.get(i, SlotNode()))
                #if self.children is not None:
                #    for c in self.children:
                for c in children:
                    if hasattr(c, 'widget'):
                        fwrite('    ' * (tabs+1) +
                               '<object class="sizeritem">\n')
                        sp = c.widget.sizer_properties
                        for p in sp: sp[p].write(outfile, tabs+2)
                        c.write(outfile, tabs+2, class_names)
                        fwrite('    ' * (tabs+1) + '</object>\n')
                    else:
                        c.write(outfile, tabs+1)
            elif self.children is not None:
                for c in self.children: c.write(outfile, tabs+1, class_names)
            fwrite('    ' * tabs + '</object>\n')

    # end of class Node

    def __init__(self, root=None, app=None):
        self.root = root
        if self.root is None: self.root = Tree.Node()
        self.current = self.root
        self.app = app # reference to the app properties
        self.names = {} # dictionary of names of the widgets: each entry is
                        # itself a dictionary, one for each toplevel widget...

    def _find_toplevel(self, node):
        assert node is not None, _("None node in _find_toplevel")
        if node.parent is self.root:
            return node.parent
        while node.parent is not self.root:
            node = node.parent
        return node

    def has_name(self, name, node=None):
        if node is None:
            #print '\nname to check:', name
            for n in self.names:
                #print 'names of %s: %s' % (n.widget.name, self.names[n])
                if name in self.names[n]:
                    return True
            return False
        else:
            node = self._find_toplevel(node)
            #print '\nname to check:', name
            #print 'names of node %s: %s' % (node.widget.name, self.names[node])
            return name in self.names[node]
        #return self.names.has_key(name)

    def add(self, child, parent=None):
        if parent is None: parent = self.root 
        if parent.children is None: parent.children = []
        parent.children.append(child)
        child.parent = parent
        self.current = child
        #self.names[str(child.widget.name)] = 1
        #self.names.setdefault(parent, {})[str(child.widget.name)] = 1
        self.names.setdefault(self._find_toplevel(child), {})[
            str(child.widget.name)] = 1
        if parent is self.root and \
               getattr(child.widget.__class__, '_is_toplevel', False):
            self.app.add_top_window(child.widget.name)

    def insert(self, child, parent, index):
        if parent is None: parent = self.root
        if parent.children is None:
            parent.children = []
        parent.children.insert(index, child)
        child.parent = parent
        self.current = child
        #self.names[str(child.widget.name)] = 1
        #self.names.setdefault(parent, {})[str(child.widget.name)] = 1
        self.names.setdefault(self._find_toplevel(child), {})[
            str(child.widget.name)] = 1
        if parent is self.root:
            self.app.add_top_window(child.widget.name)

    def remove(self, node=None):
        if node is not None:
            def clear_name(n):
                try:
                    #del self.names[str(n.widget.name)]
                    del self.names[self._find_toplevel(n)][str(n.widget.name)]
                except (KeyError, AttributeError):
                    pass
                if n.children:
                    for c in n.children: clear_name(c)
            clear_name(node)
            if node.parent is self.root:
                self.app.remove_top_window(node.widget.name)
            node.remove()
        elif self.root.children:
            for n in self.root.children:
                n.remove()
            self.root.children = None
            self.names = {}

    def write(self, outfile=None, tabs=0):
        """\
        Writes the xml equivalent of this tree to the given output file
        """
        if outfile is None:
            import sys
            outfile = sys.stdout
        from time import asctime
        import common
        outfile.write('<?xml version="1.0"?>\n<!-- generated by wxGlade %s ' \
                      'on %s -->\n\n' % (common.version, asctime()))
        outpath = os.path.expanduser(self.app.output_path.strip())
        name = self.app.get_name()
        klass = self.app.get_class()
        option = str(self.app.codegen_opt)
        top_window = self.app.get_top_window()
        language = self.app.get_language()
        encoding = self.app.get_encoding()
        use_gettext = str(int(self.app.use_gettext))
        is_template = str(int(self.app.is_template))
        overwrite = str(int(self.app.overwrite))
        # ALB 2004-01-18
        #use_new_namespace = str(int(self.app.get_use_new_namespace()))
        use_new_namespace = str(int(not self.app.get_use_old_namespace()))
        # ALB 2004-12-05
        for_version = str(self.app.for_version)
        outfile.write('<application path=%s name=%s class=%s option=%s ' \
                      'language=%s top_window=%s encoding=%s ' \
                      'use_gettext=%s overwrite=%s '
                      'use_new_namespace=%s for_version=%s is_template=%s>\n' \
                      % tuple([quoteattr(common._encode_to_xml(i)) for i in 
                               (outpath, name, klass, option, language,
                                top_window, encoding, use_gettext,
                                overwrite, use_new_namespace, for_version,
                                is_template)]))
        if self.app.is_template and getattr(self.app, 'template_data', None):
            self.app.template_data.write(outfile, tabs+1)
        class_names = set()
        if self.root.children is not None:
            for c in self.root.children:
                c.write(outfile, tabs+1, class_names)
        outfile.write('</application>\n')
        return class_names

    def change_node(self, node, widget):
        """\
        Changes the node 'node' so that it refers to 'widget'
        """
        try:
            #del self.names[node.widget.name]
            del self.names[self._find_toplevel(node)][node.widget.name]
        except KeyError:
            pass
        node.widget = widget
        #self.names[widget.name] = 1
        self.names.setdefault(self._find_toplevel(node), {})[widget.name] = 1

    def change_node_pos(self, node, new_pos, index=None):
        if index is None: index = node.parent.children.index(node)
        if index >= new_pos:
            node.parent.children.insert(new_pos, node)
            del node.parent.children[index+1]
        else:
            del node.parent.children[index]
            node.parent.children.insert(new_pos+1, node)

# end of class Tree


class WidgetTree(wx.TreeCtrl, Tree):
    """\
    Tree with the ability to display the hierarchy of widgets
    """
    images = {} # dictionary of icons of the widgets displayed
    def __init__(self, parent, application):
        id = wx.NewId()
        style = wx.TR_DEFAULT_STYLE|wx.TR_HAS_VARIABLE_ROW_HEIGHT
        if wx.Platform == '__WXGTK__':
            style |= wx.TR_NO_LINES|wx.TR_FULL_ROW_HIGHLIGHT
        elif wx.Platform == '__WXMAC__':
            style &= ~wx.TR_ROW_LINES
        wx.TreeCtrl.__init__(self, parent, id, style=style)
        root_node = Tree.Node(application)
        self.cur_widget = None # reference to the selected widget
        Tree.__init__(self, root_node, application)
        image_list = wx.ImageList(21, 21)
        image_list.Add(wx.Bitmap(os.path.join(common.wxglade_path,
                                             'icons/application.xpm'),
                                wx.BITMAP_TYPE_XPM))
        for w in WidgetTree.images:
##             WidgetTree.images[w] = image_list.Add(wx.Bitmap(
##                 WidgetTree.images[w], wx.BITMAP_TYPE_XPM))
            WidgetTree.images[w] = image_list.Add(
                misc.get_xpm_bitmap(WidgetTree.images[w]))
        self.AssignImageList(image_list)
        root_node.item = self.AddRoot(_('Application'), 0)
        self.SetPyData(root_node.item, root_node)
        self.skip_select = 0 # necessary to avoid an infinite loop on win32, as
                             # SelectItem fires an EVT_TREE_SEL_CHANGED event
        self.title = ' '
        self.set_title(self.title)
        
        self.auto_expand = True # this control the automatic expansion of
                                # nodes: it is set to False during xml loading
        self._show_menu = misc.wxGladePopupMenu('widget') # popup menu to
                                                          # show toplevel
                                                          # widgets
        SHOW_ID = wx.NewId()
        self._show_menu.Append(SHOW_ID, _('Show'))
        wx.EVT_TREE_SEL_CHANGED(self, id, self.on_change_selection)
        wx.EVT_RIGHT_DOWN(self, self.popup_menu)
        wx.EVT_LEFT_DCLICK(self, self.show_toplevel)
        wx.EVT_MENU(self, SHOW_ID, self.show_toplevel)
        def on_key_down(event):
            evt_flags = 0
            if event.ControlDown(): evt_flags = wx.ACCEL_CTRL
            evt_key = event.GetKeyCode()
            for flags, key, function in misc.accel_table:
                if evt_flags == flags and evt_key == key:
                    wx.CallAfter(function)
                    break
            event.Skip()
        wx.EVT_KEY_DOWN(self, on_key_down)

    def _build_label(self, node):
        s = node.widget.name
        if node.widget.klass != node.widget.base and \
               node.widget.klass != 'wxScrolledWindow': # special case...
            s += ' (%s)' % node.widget.klass
        return s
        
    def add(self, child, parent=None, image=None): # is image still used?
        """\
        appends child to the list of parent's children
        """
        Tree.add(self, child, parent)
        import common
        name = child.widget.__class__.__name__
        index = WidgetTree.images.get(name, -1)
        if parent is None: parent = parent.item = self.GetRootItem()
        child.item = self.AppendItem(parent.item, self._build_label(child),
                                     index)
        self.SetPyData(child.item, child)
        if self.auto_expand:
            self.Expand(parent.item)
            self.select_item(child)
        child.widget.show_properties()
        self.app.check_codegen(child.widget)

    def insert(self, child, parent, pos, image=None):
        """\
        inserts child to the list of parent's children, before index
        """
        if parent.children is None:
            self.add(child, parent, image)
            return
        import common
        name = child.widget.__class__.__name__
        image_index = WidgetTree.images.get(name, -1)
        if parent is None: parent = parent.item = self.GetRootItem()

        index = 0
        if misc.check_wx_version(2, 5):
            item, cookie = self.GetFirstChild(parent.item)
        else:
            item, cookie = self.GetFirstChild(parent.item, 1)
        while item.IsOk():
            item_pos = self.GetPyData(item).widget.pos
            if pos < item_pos: break
            index += 1
            item, cookie = self.GetNextChild(parent.item, cookie)

        Tree.insert(self, child, parent, index)
        child.item = self.InsertItemBefore(parent.item, index,
                                           self._build_label(child),
                                           image_index)
        self.SetPyData(child.item, child)
        if self.auto_expand:
            self.Expand(parent.item)
            self.select_item(child)
        child.widget.show_properties()
        self.app.check_codegen(child.widget)

    def remove(self, node=None):
        self.app.saved = False # update the status of the app
        Tree.remove(self, node)
        if node is not None:
            try:
                self.cur_widget = None
                self.SelectItem(node.parent.item)
            except: self.SelectItem(self.GetRootItem())
            self.Delete(node.item)
        else:
            wx.TreeCtrl.Destroy(self)

    def clear(self):
        self.app.reset()
        self.skip_select = True
        if self.root.children:
            while self.root.children:
                c = self.root.children[-1]
                if c.widget: c.widget.remove()
            self.root.children = None
        self.skip_select = False
        app = self.GetPyData(self.GetRootItem())
        app.widget.show_properties()

    def refresh_name(self, node, oldname=None): #, name=None):
        if oldname is not None:
            try:
                #del self.names[self.GetItemText(node.item)]
                del self.names[self._find_toplevel(node)][oldname]
            except KeyError:
                pass
        #self.names[name] = 1
        #self.SetItemText(node.item, name)
        #self.names[node.widget.name] = 1
        self.names.setdefault(self._find_toplevel(node), {})[
            node.widget.name] = 1
        self.SetItemText(node.item, self._build_label(node))

    def select_item(self, node):
        self.skip_select = True
        self.SelectItem(node.item)
        self.skip_select = False
        if self.cur_widget: self.cur_widget.update_view(False)
        self.cur_widget = node.widget
        self.cur_widget.update_view(True)
        self.cur_widget.show_properties()
        misc.focused_widget = self.cur_widget

    def on_change_selection(self, event):
        if not self.skip_select:
            item = event.GetItem()
            try:
                if self.cur_widget: self.cur_widget.update_view(False)
                self.cur_widget = self.GetPyData(item).widget
                misc.focused_widget = self.cur_widget
                self.cur_widget.show_properties(None)
                self.cur_widget.update_view(True)
            except AttributeError:
                pass
            except Exception:
                import traceback
                traceback.print_exc()

    def popup_menu(self, event):
        node = self._find_item_by_pos(*event.GetPosition())
        if not node: return
        else:
            self.select_item(node)
            item = node.widget
        if not item.widget or not item.is_visible():
            import edit_windows
            #if isinstance(item, edit_windows.TopLevelBase):
            if node.parent is self.root:
                self._show_menu.SetTitle(item.name)
                self.PopupMenu(self._show_menu, event.GetPosition())
            return
        try:
            x, y = self.ClientToScreen(event.GetPosition())
            x, y = item.widget.ScreenToClient((x, y))
            event.m_x, event.m_y = x, y
            item.popup_menu(event)
        except AttributeError:
            import traceback; traceback.print_exc()

    def expand(self, node=None, yes=True):
        """\
        expands or collapses the given node
        """
        if node is None: node = self.root
        if yes: self.Expand(node.item)
        else: self.Collapse(node.item)

    def set_title(self, value):
        if value is None: value = ""
        self.title = value
        try: self.GetParent().SetTitle(_('wxGlade: Tree %s') % value)
        except: pass

    def get_title(self):
        if not self.title: self.title = ' '
        return self.title

    def show_widget(self, node, toplevel=False):
        """\
        Shows the widget of the given node and all its children
        """
        if toplevel:
            if not wx.IsBusy():
                wx.BeginBusyCursor()
            if not node.widget.widget:
                node.widget.create_widget()
                node.widget.finish_widget_creation()
            if node.children:
                for c in node.children: self.show_widget(c)
            node.widget.post_load()
            node.widget.show_widget(True)
            node.widget.show_properties()
            node.widget.widget.Raise()
            # set the best size for the widget (if no one is given)
            props = node.widget.properties
            if 'size' in props and not props['size'].is_active() and \
                   node.widget.sizer:
                node.widget.sizer.fit_parent()
            if wx.IsBusy():
                wx.EndBusyCursor()
        else:
            import edit_sizers
            def show_rec(node):
                node.widget.show_widget(True)
                self.expand(node, True)
                if node.children:
                    for c in node.children: show_rec(c)
                node.widget.post_load()
##                 w = node.widget
##                 if isinstance(w, edit_sizers.SizerBase): return
##                 elif not w.properties['size'].is_active() and \
##                          w.sizer and w.sizer.toplevel:
##                     w.sizer.fit_parent()                    
            show_rec(node)

    def show_toplevel(self, event):
        """\
        Event handler for left double-clicks: if the click is above a toplevel
        widget and this is hidden, shows it
        """
        try: x, y = event.GetPosition()
        except AttributeError:
            # if we are here, event is a CommandEvent and not a MouseEvent
            node = self.GetPyData(self.GetSelection())
            self.expand(node) # if we are here, the widget must be shown
        else:
            node = self._find_item_by_pos(x, y, True)
        if node is not None:
            if not node.widget.is_visible():
                # added by rlawson to expand node on showing top level widget
                self.expand(node)
                self.show_widget(node, True)
            else:
                node.widget.show_widget(False)
                #self.select_item(self.root)
                # added by rlawson to collapse only the toplevel node,
                # not collapse back to root node
                self.select_item(node)
                self.app.show_properties()
                event.Skip()
        #event.Skip()

    def _find_item_by_pos(self, x, y, toplevels_only=False):
        """\
        Finds the node which is displayed at the given coordinates.
        Returns None if there's no match.
        If toplevels_only is True, scans only root's children
        """
        item, flags = self.HitTest((x, y))
        if item and flags & (wx.TREE_HITTEST_ONITEMLABEL |
                             wx.TREE_HITTEST_ONITEMICON):
            node = self.GetPyData(item)
            if not toplevels_only or node.parent is self.root:
                return node
        return None

    def change_node(self, node, widget):
        Tree.change_node(self, node, widget)
        self.SetItemImage(node.item, self.images.get(
            widget.__class__.__name__, -1))
        self.SetItemText(node.item, self._build_label(node)) #widget.name)

    def change_node_pos(self, node, new_pos):
        if new_pos >= self.GetChildrenCount(node.parent.item, False):
            return
        index = node.parent.children.index(node)
        Tree.change_node_pos(self, node, new_pos, index)
        old_item = node.item
        image = self.GetItemImage(node.item)
        self.Freeze()
        #print self._build_label(node), index, new_pos
        if index >= new_pos:
            node.item = self.InsertItemBefore(
                node.parent.item, new_pos, self._build_label(node), image)
        else:
            node.item = self.InsertItemBefore(
                node.parent.item, new_pos+1, self._build_label(node), image)
        self.SetPyData(node.item, node)
        def append(parent, node):
            idx = WidgetTree.images.get(node.widget.__class__.__name__, -1)
            node.item = self.AppendItem(parent.item,
                                        self._build_label(node), idx)
            self.SetPyData(node.item, node)
            if node.children:
                for c in node.children:
                    append(node, c)
            self.Expand(node.item)
        if node.children:
            for c in node.children:
                append(node, c)
        self.Expand(node.item)
        self.Delete(old_item)
        self.Thaw()

    def get_selected_path(self):
        """\
        returns a list of widget names, from the toplevel to the selected one
        Example:
        ['frame_1', 'sizer_1', 'panel_1', 'sizer_2', 'button_1']
        if button_1 is the currently selected widget.
        """
        from edit_sizers import SizerBase
        ret = []
        w = self.cur_widget
        oldw = None
        while w is not None:
            oldw = w
            ret.append(w.name)
            sizer = getattr(w, 'sizer', None)
            if getattr(w, 'parent', "") is None:
                w = w.parent
            elif sizer is not None and not sizer.is_virtual():
                w = sizer
            else:
                if isinstance(w, SizerBase):
                    w = w.window
                else:
                    w = w.parent
        ret.reverse()
        # ALB 2007-08-28: remember also the position of the toplevel window in
        # the selected path
        if oldw is not None:
            assert oldw.widget
            pos = misc.get_toplevel_parent(oldw.widget).GetPosition()
            ret[0] = (ret[0], pos)
        return ret

    def select_path(self, path):
        """\
        sets the selected widget from a path_list, which should be in the
        form returned by get_selected_path
        """
        index = 0
        item, cookie = self._get_first_child(self.GetRootItem())
        itemok = None
        parent = None
        pos = None
        while item.Ok() and index < len(path):
            widget = self.GetPyData(item).widget
            name = path[index]
            if index == 0 and type(name) == type(()):
                name, pos = name
            if misc.streq(widget.name, name):
                #print 'OK:', widget.name
                #self.EnsureVisible(item)
                itemok = item
                if parent is None:
                    parent = self.GetPyData(itemok)
                self.cur_widget = widget
                item, cookie = self._get_first_child(item)
                index += 1
            else:
                #print 'NO:', widget.name
                item = self.GetNextSibling(item)
        if itemok is not None:
            node = self.GetPyData(itemok)
            if parent is not None:
                self.show_widget(parent, True)
                if pos is not None:
                    misc.get_toplevel_parent(parent.widget).SetPosition(pos)
            self.select_item(node)
        
    def _get_first_child(self, item):
        if misc.check_wx_version(2, 5):
            return self.GetFirstChild(item)
        else:
            return self.GetFirstChild(item, 1)
        
# end of class WidgetTree

