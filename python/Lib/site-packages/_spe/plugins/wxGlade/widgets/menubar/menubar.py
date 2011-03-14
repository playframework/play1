# menubar.py: wxMenuBar objects
# $Id: menubar.py,v 1.28 2007/08/07 12:18:34 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import common, math, misc
from tree import Tree
from MenuTree import *
from widget_properties import *
from edit_windows import EditBase, TopLevelBase, PreviewMixin


class MenuItemDialog(wx.Dialog):
    def __init__(self, parent, owner, items=None):
        wx.Dialog.__init__(self, parent, -1, _("Menu editor"),
                          style=wx.DEFAULT_DIALOG_STYLE|wx.RESIZE_BORDER)
        ADD_ID, REMOVE_ID, NAME_ID, LABEL_ID, ID_ID, CHECK_RADIO_ID, LIST_ID, \
                ADD_SEP_ID, MOVE_LEFT_ID, MOVE_RIGHT_ID, MOVE_UP_ID, \
                MOVE_DOWN_ID, HELP_STR_ID = [wx.NewId() for i in range(13)]

        self._staticbox = wx.StaticBox(self, -1, _("Menu item:"))

        self.owner = owner
        self.menu_items = wx.ListCtrl(self, LIST_ID, style=wx.LC_REPORT | \
                                     wx.LC_SINGLE_SEL|wx.SUNKEN_BORDER)
        # ALB 2004-09-26: workaround to make the scroll wheel work...
        wx.EVT_MOUSEWHEEL(self.menu_items, lambda e: e.Skip())
        
        self.menu_items.InsertColumn(0, _("Label"))
        self.menu_items.InsertColumn(1, _("Id"))
        self.menu_items.InsertColumn(2, _("Name"))
        self.menu_items.InsertColumn(3, _("Help String"))
        self.menu_items.InsertColumn(4, _("Type"))
        # ALB 2004-12-05
        self.menu_items.InsertColumn(5, _("Event Handler"))

        self.menu_items.SetColumnWidth(0, 250)
        self.menu_items.SetColumnWidth(2, 250)
        self.menu_items.SetColumnWidth(3, 250)
        self.menu_items.SetColumnWidth(5, 250)

        # menu item fields
        self.id = wx.TextCtrl(self, ID_ID)
        self.label = wx.TextCtrl(self, LABEL_ID)
        self.name = wx.TextCtrl(self, NAME_ID)
        self.help_str = wx.TextCtrl(self, HELP_STR_ID)

        # ALB 2004-12-05
        self.event_handler = wx.TextCtrl(self, -1)
        import re
        self.handler_re = re.compile(r'^\s*\w*\s*$')

        #self.checkable = wx.CheckBox(self, CHECK_ID, "") #Checkable")
        self.check_radio = wx.RadioBox(
            self, CHECK_RADIO_ID, _("Type"),
            choices=['Normal', 'Checkable', 'Radio'], majorDimension=3)

        self.add = wx.Button(self, ADD_ID, _("Add"))
        self.remove = wx.Button(self, REMOVE_ID, _("Remove"))
        self.add_sep = wx.Button(self, ADD_SEP_ID, _("Add separator"))

        # menu items navigation
        self.move_up = wx.Button(self, MOVE_UP_ID, _("Up"))
        self.move_down = wx.Button(self, MOVE_DOWN_ID, _("Down"))
        self.move_left = wx.Button(self, MOVE_LEFT_ID, " < ")
        self.move_right = wx.Button(self, MOVE_RIGHT_ID, " > ")

        self.ok = wx.Button(self, wx.ID_OK, _("OK"))
        self.apply = wx.Button(self, wx.ID_APPLY, _("Apply"))
        self.cancel = wx.Button(self, wx.ID_CANCEL, _("Cancel"))

        self.do_layout()
        self.selected_index = -1 # index of the selected element in the 
                                 # wx.ListCtrl menu_items
        # event handlers
        wx.EVT_BUTTON(self, ADD_ID, self.add_menu_item)
        wx.EVT_BUTTON(self, REMOVE_ID, self.remove_menu_item)
        wx.EVT_BUTTON(self, ADD_SEP_ID, self.add_separator)
        wx.EVT_BUTTON(self, MOVE_LEFT_ID, self.move_item_left)
        wx.EVT_BUTTON(self, MOVE_RIGHT_ID, self.move_item_right)
        wx.EVT_BUTTON(self, MOVE_UP_ID, self.move_item_up)
        wx.EVT_BUTTON(self, MOVE_DOWN_ID, self.move_item_down)
        wx.EVT_BUTTON(self, wx.ID_APPLY, self.on_apply)
        wx.EVT_KILL_FOCUS(self.name, self.update_menu_item)
        wx.EVT_KILL_FOCUS(self.label, self.update_menu_item)
        wx.EVT_KILL_FOCUS(self.id, self.update_menu_item)
        wx.EVT_KILL_FOCUS(self.help_str, self.update_menu_item)
        # ALB 2004-12-05
        wx.EVT_KILL_FOCUS(self.event_handler, self.update_menu_item)
        #wx.EVT_CHECKBOX(self, CHECK_ID, self.update_menu_item)
        wx.EVT_RADIOBOX(self, CHECK_RADIO_ID, self.update_menu_item)
        wx.EVT_LIST_ITEM_SELECTED(self, LIST_ID, self.show_menu_item)
        if items:
            self.add_items(items)

    def do_layout(self):
        self.label.Enable(False)
        self.id.Enable(False)
        self.name.Enable(False)
        self.help_str.Enable(False)
        self.event_handler.Enable(False)
        self.check_radio.Enable(False)
        
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer2 = wx.StaticBoxSizer(self._staticbox, wx.VERTICAL)
        self.label.SetSize((150, -1))
        self.id.SetSize((150, -1))
        self.name.SetSize((150, -1))
        self.help_str.SetSize((150, -1))
        self.event_handler.SetSize((150, -1))
        szr = wx.FlexGridSizer(0, 2)
        if misc.check_wx_version(2, 5, 2):
            flag = wx.FIXED_MINSIZE
        else:
            flag = 0
        label_flag = wx.ALIGN_CENTER_VERTICAL
        szr.Add(wx.StaticText(self, -1, _("Id   ")), flag=label_flag)
        szr.Add(self.id, flag=flag)
        szr.Add(wx.StaticText(self, -1, _("Label  ")), flag=label_flag)
        szr.Add(self.label, flag=flag)
        szr.Add(wx.StaticText(self, -1, _("Name  ")), flag=label_flag)
        szr.Add(self.name, flag=flag)
        szr.Add(wx.StaticText(self, -1, _("Help String  ")), flag=label_flag)
        szr.Add(self.help_str, flag=flag)
        szr.Add(wx.StaticText(self, -1, _("Event Handler  ")), flag=label_flag)
        szr.Add(self.event_handler, flag=flag)
        sizer2.Add(szr, 1, wx.ALL|wx.EXPAND, 5)
        sizer2.Add(self.check_radio, 0, wx.LEFT|wx.RIGHT|wx.BOTTOM, 4)
        szr = wx.GridSizer(0, 2, 3, 3)
        szr.Add(self.add, 0, wx.EXPAND); szr.Add(self.remove, 0, wx.EXPAND)
        sizer2.Add(szr, 0, wx.EXPAND)
        sizer2.Add(self.add_sep, 0, wx.TOP|wx.EXPAND, 3)

        sizer3 = wx.BoxSizer(wx.VERTICAL)
        sizer3.Add(self.menu_items, 1, wx.ALL|wx.EXPAND, 5)
        sizer4 = wx.BoxSizer(wx.HORIZONTAL)

        sizer4.Add(self.move_up, 0, wx.LEFT|wx.RIGHT, 3)
        sizer4.Add(self.move_down, 0, wx.LEFT|wx.RIGHT, 5)
        sizer4.Add(self.move_left, 0, wx.LEFT|wx.RIGHT, 5)
        sizer4.Add(self.move_right, 0, wx.LEFT|wx.RIGHT, 5)
        sizer3.Add(sizer4, 0, wx.ALIGN_CENTER|wx.ALL, 5)
        szr = wx.BoxSizer(wx.HORIZONTAL)
        szr.Add(sizer3, 1, wx.ALL|wx.EXPAND, 5) 
        szr.Add(sizer2, 0, wx.TOP|wx.BOTTOM|wx.RIGHT, 5)
        sizer.Add(szr, 1, wx.EXPAND)
        sizer2 = wx.BoxSizer(wx.HORIZONTAL)
        sizer2.Add(self.ok, 0, wx.ALL, 5)
        sizer2.Add(self.apply, 0, wx.ALL, 5)
        sizer2.Add(self.cancel, 0, wx.ALL, 5)
        sizer.Add(sizer2, 0, wx.ALL|wx.ALIGN_CENTER, 3)
        self.SetAutoLayout(1)
        self.SetSizer(sizer)
        sizer.Fit(self)
        self.SetSize((-1, 350))
        self.CenterOnScreen()

    def _enable_fields(self, enable=True):
        for s in (self.label, self.id, self.name, self.help_str,
                  self.check_radio, self.event_handler):
            s.Enable(enable)

    def add_menu_item(self, event):
        """\
        Event handler called when the Add button is clicked
        """
        index = self.selected_index = self.selected_index+1
        if not self.menu_items.GetItemCount():
            self._enable_fields()
##             for s in (self.label, self.id, self.name, self.help_str,
##                       self.check_radio, self.event_handler):
##                 s.Enable(True)
        if index < 0: index = self.menu_items.GetItemCount()
        elif index > 0: indent = "    " * self.item_level(index-1)
        else: indent = ""
        name, label, id, check_radio = "", "item", "", "0"
        self.menu_items.InsertStringItem(index, indent + label)
        self.menu_items.SetStringItem(index, 1, id)
        self.menu_items.SetStringItem(index, 2, name)
        self.menu_items.SetStringItem(index, 4, check_radio)
        # fix bug 698074
        self.menu_items.SetItemState(index, wx.LIST_STATE_SELECTED,
                                     wx.LIST_STATE_SELECTED)
        self.name.SetValue(name)
        self.label.SetValue(label)
        self.id.SetValue(id)
        self.check_radio.SetSelection(int(check_radio))
        self.event_handler.SetValue("")

    def add_separator(self, event):
        """\
        Event handler called when the Add Separator button is clicked
        """
        index = self.selected_index+1
        if not self.menu_items.GetItemCount():
            self._enable_fields()
##             for s in (self.label, self.id, self.name, self.help_str,
##                       self.check_radio, self.event_handler):
##                 s.Enable(True)
        if index < 0: index = self.menu_items.GetItemCount() 
        elif index > 0: label = "    " * self.item_level(index-1) + '---'
        else: label = '---'
        self.menu_items.InsertStringItem(index, label)
        self.menu_items.SetStringItem(index, 1, '---')
        self.menu_items.SetStringItem(index, 2, '---')
        # fix bug 698074
        self.menu_items.SetItemState(index, wx.LIST_STATE_SELECTED,
                                     wx.LIST_STATE_SELECTED)

    def show_menu_item(self, event):
        """\
        Event handler called when a menu item in the list is selected
        """        
        self.selected_index = index = event.GetIndex()
        if not misc.streq(self.menu_items.GetItem(index, 2).m_text, '---'):
            # skip if the selected item is a separator
            for (s, i) in ((self.label, 0), (self.id, 1), (self.name, 2),
                           (self.help_str, 3), (self.event_handler, 5)):
                s.SetValue(self.menu_items.GetItem(index, i).m_text)
            self.label.SetValue(self.label.GetValue().lstrip())
            try:
                self.check_radio.SetSelection(
                    int(self.menu_items.GetItem(index, 4).m_text))
            except:
                self.check_radio.SetSelection(0)
        event.Skip()

    def update_menu_item(self, event):
        """\
        Event handler called when some of the properties of the current menu
        item changes
        """        
        set_item = self.menu_items.SetStringItem
        index = self.selected_index
        val = self.event_handler.GetValue()
        if not self.handler_re.match(val):
            event.GetEventObject().SetFocus()
            return
        if index < 0:
            return event.Skip()
        set_item(index, 0, "    " * self.item_level(index) + \
                 self.label.GetValue().lstrip())
        set_item(index, 1, self.id.GetValue())
        set_item(index, 2, self.name.GetValue())
        set_item(index, 3, self.help_str.GetValue())
        set_item(index, 4, str(self.check_radio.GetSelection()))
        set_item(index, 5, self.event_handler.GetValue())
        event.Skip()

    def item_level(self, index, label=None):
        """\
        returns the indentation level of the menu item at the given index
        """
        label = self.menu_items.GetItem(index, 0).m_text
        return (len(label) - len(label.lstrip())) / 4
    
    def remove_menu_item(self, event):
        """\
        Event handler called when the Remove button is clicked
        """        
        if self.selected_index >= 0:
            index = self.selected_index+1
            if index < self.menu_items.GetItemCount() and \
               (self.item_level(self.selected_index) < self.item_level(index)):
                self._move_item_left(index)
                self.selected_index = index-1
            for s in (self.name, self.id, self.label, self.help_str,
                      self.event_handler):
                s.SetValue("")
            self.check_radio.SetSelection(0)
            self.menu_items.DeleteItem(self.selected_index)
            if not self.menu_items.GetItemCount():
                self._enable_fields(False)
##                 for s in (self.name, self.id, self.label, \
##                           self.help_str, self.check_radio, self.event_handler):
##                     s.Enable(False)

    def add_items(self, menus):
        """\
        adds the content of 'menus' to self.menu_items. menus is a sequence of
        trees which describes the structure of the menus
        """
        indent = " " * 4
        set_item = self.menu_items.SetStringItem
        add_item = self.menu_items.InsertStringItem
        index = [0]
        def add(node, level):
            i = index[0]
            add_item(i, misc.wxstr(indent * level + node.label.lstrip()))
            set_item(i, 1, misc.wxstr(node.id))
            set_item(i, 2, misc.wxstr(node.name))
            set_item(i, 3, misc.wxstr(node.help_str))
            # ALB 2004-12-05
            set_item(i, 5, misc.wxstr(node.handler))
            
            item_type = 0
            try:
                if node.checkable and int(node.checkable):
                    item_type = 1
                elif int(node.radio):
                    item_type = 2
            except ValueError:
                pass
            set_item(i, 4, misc.wxstr(item_type))
            index[0] += 1
            for item in node.children:
                add(item, level+1)
        for tree in menus:
            add(tree.root, 0)
        if self.menu_items.GetItemCount():
            self._enable_fields()
##             for s in (self.name, self.id, self.label, \
##                       self.help_str, self.check_radio, self.event_handler):
##                 s.Enable(True)
            

    def get_menus(self):
        """\
        returns the contents of self.menu_items as a list of trees which
        describe the structure of the menus in the format used by EditMenuBar
        """
        def get(i, j): return self.menu_items.GetItem(i, j).m_text
        trees = []
        def add(node, index):
            label = get(index, 0).lstrip()
            id = get(index, 1)
            name = get(index, 2)
            help_str = get(index, 3)
            event_handler = get(index, 5)
            try:
                item_type = int(get(index, 4))
            except ValueError:
                item_type = 0
            checkable = item_type == 1 and misc.wxstr("1") or misc.wxstr("")
            radio = item_type == 2 and misc.wxstr("1") or misc.wxstr("")
            n = MenuTree.Node(label, id, name, help_str, checkable, radio,
                              handler=event_handler)
            node.children.append(n)
            n.parent = node
            return n
        level = 0
        curr_item = None
        for index in range(self.menu_items.GetItemCount()):
            label = get(index, 0)
            lvl = self.item_level(index) # get the indentation level
            if not lvl:
                t = MenuTree(get(index, 2), label, id=get(index, 1),
                             handler=get(index, 5))
                curr_item = t.root
                level = 1
                trees.append(t)
                continue
            elif lvl < level:
                for i in range(level-lvl):
                    curr_item = curr_item.parent
                level = lvl
            elif lvl > level:
                curr_item = curr_item.children[-1]
                level = lvl
            add(curr_item, index)

        return trees

    def _move_item_left(self, index):
        if index > 0:
            if (index+1 < self.menu_items.GetItemCount() and \
                (self.item_level(index) < self.item_level(index+1))):
                return
            label = self.menu_items.GetItem(index, 0).m_text
            if misc.streq(label[:4], " " * 4):
                self.menu_items.SetStringItem(index, 0, label[4:])
                self.menu_items.SetItemState(index, wx.LIST_STATE_SELECTED, 
                                             wx.LIST_STATE_SELECTED)
                
    def move_item_left(self, event):
        """\
        moves the selected menu item one level up in the hierarchy, i.e.
        shifts its label 4 spaces left in self.menu_items
        """
        self.menu_items.SetFocus()
        self._move_item_left(self.selected_index)

    def _move_item_right(self, index):
        if index > 0 and (self.item_level(index) <= self.item_level(index-1)): 
            label = self.menu_items.GetItem(index, 0).m_text
            self.menu_items.SetStringItem(index, 0, misc.wxstr(" " * 4)
                                          + label)
            self.menu_items.SetItemState(index, wx.LIST_STATE_SELECTED, \
                                         wx.LIST_STATE_SELECTED)

    def move_item_right(self, event):
        """\
        moves the selected menu item one level down in the hierarchy, i.e.
        shifts its label 4 spaces right in self.menu_items
        """
        self.menu_items.SetFocus()
        self._move_item_right(self.selected_index)


    def move_item_up(self, event):
        """\
        moves the selected menu item before the previous one at the same level
        in self.menu_items
        """
        self.menu_items.SetFocus()
        index = self._do_move_item(event, self.selected_index, False)
        if index is not None:
            state = wx.LIST_STATE_SELECTED | wx.LIST_STATE_FOCUSED
            self.menu_items.SetItemState(index, state, state)

    def _do_move_item(self, event, index, is_down):
        """\
        internal function used by move_item_up and move_item_down.
        Returns the new index of the moved item, or None if no change occurred
        """
        #index = self.selected_index
        if index <= 0: return None
        def get(i, j): return self.menu_items.GetItem(i, j).m_text
        def getall(i): return [get(i, j) for j in range(6)]
        level = self.item_level(index)
        items_to_move = [ getall(index) ]
        i = index+1
        while i < self.menu_items.GetItemCount():
            # collect the items to move up
            if level < self.item_level(i):
                items_to_move.append(getall(i))
                i += 1
            else: break
        i = index-1
        while i >= 0:
            lvl = self.item_level(i)
            if level == lvl: break
            elif level > lvl: return None
            i -= 1
        delete = self.menu_items.DeleteItem
        insert = self.menu_items.InsertStringItem
        set = self.menu_items.SetStringItem
        for j in range(len(items_to_move)-1, -1, -1):
            delete(index+j)
        items_to_move.reverse()
        for label, id, name, help_str, check_radio, event_handler in \
                items_to_move:
            i = insert(i, label)
            set(i, 1, id)
            set(i, 2, name)
            set(i, 3, help_str)
            set(i, 4, check_radio)
            set(i, 5, event_handler)
        ret_idx = i
        if is_down: ret_idx += len(items_to_move)
        return ret_idx
        
    def move_item_down(self, event):
        """\
        moves the selected menu item after the next one at the same level
        in self.menu_items
        """
        self.menu_items.SetFocus()
        index = self.selected_index
        self.selected_index = -1
        if index < 0: return
        def get(i, j): return self.menu_items.GetItem(i, j).m_text
        def getall(i): return [get(i, j) for j in range(6)]
        level = self.item_level(index)
        i = index+1
        while i < self.menu_items.GetItemCount():
            # collect the items to move down
            if level < self.item_level(i):
                i += 1
            else: break
        if i < self.menu_items.GetItemCount():
            # _do_move_item works with selected_index, so we must assing to
            # it the rigth value before the call
            #self.selected_index = i
            self.selected_index = self._do_move_item(event, i, True)
            # fix bug 698071
            state = wx.LIST_STATE_SELECTED | wx.LIST_STATE_FOCUSED
            self.menu_items.SetItemState(self.selected_index, state, state)
        else:
            # restore the selected index
            self.selected_index = index

    def on_apply(self, event):
        self.owner.set_menus(self.get_menus())
        common.app_tree.app.saved = False
                                                  
#end of class MenuItemDialog


class MenuProperty(Property):
    """\
    Property to edit the menus of an EditMenuBar instance.
    """
    def __init__(self, owner, name, parent):
        Property.__init__(self, owner, name, parent)
        self.panel = None
        self.menu_items = {}
        if parent is not None: self.display(parent)

    def display(self, parent):
        self.panel = wx.Panel(parent, -1)
        edit_btn_id = wx.NewId()
        self.edit_btn = wx.Button(self.panel, edit_btn_id, _("Edit menus..."))
        sizer = wx.BoxSizer(wx.HORIZONTAL)
        sizer.Add(self.edit_btn, 1, wx.EXPAND|wx.ALIGN_CENTER|wx.TOP|wx.BOTTOM, 4)
        self.panel.SetAutoLayout(1)
        self.panel.SetSizer(sizer)
        self.panel.SetSize(sizer.GetMinSize())
        wx.EVT_BUTTON(self.panel, edit_btn_id, self.edit_menus)

    def bind_event(*args): pass

    def edit_menus(self, event):
        dialog = MenuItemDialog(self.panel, self.owner,
                                items=self.owner.get_menus())
        if dialog.ShowModal() == wx.ID_OK:
            self.owner.set_menus(dialog.get_menus())
            common.app_tree.app.saved = False # update the status of the app

    def write(self, outfile, tabs):
        fwrite = outfile.write
        fwrite('    ' * tabs + '<menus>\n')
        for menu in self.owner[self.name][0]():
            menu.write(outfile, tabs+1)
        fwrite('    ' * tabs + '</menus>\n')

# end of class MenuProperty


class EditMenuBar(EditBase, PreviewMixin):
    __hidden_frame = None # used on GTK to reparent a menubar before deletion
    
    def __init__(self, name, klass, parent, property_window):
        custom_class = parent is None
        EditBase.__init__(self, name, klass,
                          parent, wx.NewId(), property_window,
                          custom_class=custom_class, show=False)
        self.base = 'wxMenuBar'
        
        def nil(*args): return ()
        self.menus = [] # list of MenuTree objects
        self._mb = None # the real menubar
        self.access_functions['menus'] = (self.get_menus, self.set_menus)
        prop = self.properties['menus'] = MenuProperty(self, 'menus', None) 
##         self.node = Tree.Node(self)
##         common.app_tree.add(self.node, parent.node)
        PreviewMixin.__init__(self)

    def create_widget(self):
        if wx.Platform == '__WXGTK__' and not EditMenuBar.__hidden_frame:
            EditMenuBar.__hidden_frame = wx.Frame(common.palette, -1, "")
            EditMenuBar.__hidden_frame.Hide()
        if self.parent:
            self.widget = self._mb = wx.MenuBar()
            if self.parent.widget: self.parent.widget.SetMenuBar(self.widget)
            if wx.Platform == '__WXMSW__' or wx.Platform == '__WXMAC__':
                self.widget.SetFocus = lambda : None
        else:
            # "top-level" menubar
            self.widget = wx.Frame(None, -1, misc.design_title(self.name))
            self.widget.SetClientSize((400, 30))
            self._mb = wx.MenuBar()
            self.widget.SetMenuBar(self._mb)
            self.widget.SetBackgroundColour(self._mb.GetBackgroundColour())
            import os
            icon = wx.EmptyIcon()
            xpm = os.path.join(common.wxglade_path, 'icons', 'menubar.xpm')
            icon.CopyFromBitmap(misc.get_xpm_bitmap(xpm))
            self.widget.SetIcon(icon)
            wx.EVT_CLOSE(self.widget, lambda e: self.hide_widget())
        wx.EVT_LEFT_DOWN(self.widget, self.on_set_focus)
        self.set_menus(self.menus) # show the menus

    def create_properties(self):
        EditBase.create_properties(self)
        page = self._common_panel
        sizer = page.GetSizer()
        self.properties['menus'].display(page)
        if not sizer:
            sizer = wx.BoxSizer(wx.VERTICAL)
            sizer.Add(self.name_prop.panel, 0, wx.EXPAND)
            sizer.Add(self.klass_prop.panel, 0, wx.EXPAND)
            page.SetAutoLayout(1)
            page.SetSizer(sizer)
        sizer.Add(self.properties['menus'].panel, 0, wx.ALL|wx.EXPAND, 3)
        sizer.Fit(page)
        page.SetSize(self.notebook.GetClientSize())
        sizer.Layout()
        self.notebook.AddPage(page, _("Common"))
        if self.parent is not None:
            self.property_window.Layout()
        else:
            PreviewMixin.create_properties(self)
        
    def __getitem__(self, key):
        return self.access_functions[key]

    def get_menus(self):
        return self.menus

    def set_menus(self, menus):
        self.menus = menus
        if not self._mb: return # nothing left to do
        for i in range(self._mb.GetMenuCount()):
            self._mb.Remove(0)
        def append(menu, items):
            for item in items:
                if misc.streq(item.name, '---'): # item is a separator
                    menu.AppendSeparator()
                elif item.children:
                    m = wx.Menu()
                    append(m, item.children)
                    menu.AppendMenu(wx.NewId(), misc.wxstr(item.label), m,
                                    misc.wxstr(item.help_str))
                else:
                    check_radio = 0
                    try:
                        if int(item.checkable):
                            check_radio = 1
                    except:
                        check_radio = 0
                    if not check_radio:
                        try:
                            if int(item.radio):
                                check_radio = 2
                        except:
                            check_radio = 0
                    menu.Append(wx.NewId(), misc.wxstr(item.label),
                                misc.wxstr(item.help_str), check_radio)
        first = self._mb.GetMenuCount()
        for menu in self.menus:
            m = wx.Menu()
            append(m, menu.root.children)
            if first:
                self._mb.Replace(0, m, misc.wxstr(menu.root.label))
                first = 0
            else: self._mb.Append(m, misc.wxstr(menu.root.label))
        self._mb.Refresh()
      
    def remove(self, *args, **kwds):
        if self.parent is not None:
            self.parent.properties['menubar'].set_value(0)
            if kwds.get('gtk_do_nothing', False) and wx.Platform == '__WXGTK__':
                # workaround to prevent some segfaults on GTK: unfortunately,
                # I'm not sure that this works in all cases, and moreover it
                # could probably leak some memory (but I'm not sure)
                self.widget = None
            else:
                if self.parent.widget:
                    if wx.Platform == '__WXGTK__' and \
                           not misc.check_wx_version(2, 5):
                        self.widget.Reparent(EditMenuBar.__hidden_frame)
                        self.widget.Hide()
                    self.parent.widget.SetMenuBar(None)
        else:
            if self.widget:
                self.widget.Destroy()
                self.widget = None
        EditBase.remove(self)

    def popup_menu(self, event):
        if self.parent is not None:
            return # do nothing in this case
        if self.widget:
            if not self._rmenu:
                REMOVE_ID, HIDE_ID = [wx.NewId() for i in range(2)]
                self._rmenu = misc.wxGladePopupMenu(self.name)
                misc.append_item(self._rmenu, REMOVE_ID, _('Remove\tDel'),
                                 wx.ART_DELETE)
                misc.append_item(self._rmenu, HIDE_ID, _('Hide'))
                def bind(method):
                    return lambda e: misc.wxCallAfter(method)
                wx.EVT_MENU(self.widget, REMOVE_ID, bind(self.remove))
                wx.EVT_MENU(self.widget, HIDE_ID, bind(self.hide_widget))
                
            self.widget.PopupMenu(self._rmenu, event.GetPosition())

    def hide_widget(self, *args):
        if self.widget and self.widget is not self._mb:
            self.widget.Hide()
            common.app_tree.expand(self.node, False)
            common.app_tree.select_item(self.node.parent)
            common.app_tree.app.show_properties()

##     def show_widget(self, yes):
##         EditBase.show_widget(self, yes)
##         if self._frame:
##             self._frame.Show(yes)

    def set_name(self, name):
        EditBase.set_name(self, name)
        if self.widget is not self._mb:
            self.widget.SetTitle(misc.design_title(misc.wxstr(self.name)))

    def get_property_handler(self, name):
        class MenuHandler:
            itemattrs = ['label', 'id', 'name', 'help_str',
                         'checkable', 'radio', 'handler']
            def __init__(self, owner):
                self.owner = owner
                self.menu_items = []
                self.curr_menu = []
                self.curr_item = None
                self.curr_index = 0
                self.menu_depth = 0
            def start_elem(self, name, attrs):
                if name == 'menus': return
                if name == 'menu':
                    self.menu_depth += 1
                    label = misc._encode(attrs['label'])
                    if self.menu_depth == 1:
                        t = MenuTree(attrs['name'], label,
                                     attrs.get('itemid', ''),
                                     attrs.get('help_str', ''),
                                     handler=attrs.get('handler', ''))
                        self.curr_menu.append( (t.root,) )
                        self.owner.menus.append(t)
                        return
                    node = MenuTree.Node(label=label, name=attrs['name'],
                                         id=attrs.get('itemid', ''),
                                         help_str=attrs.get('help_str', ''),
                                         handler=attrs.get('handler', ''))
                    cm = self.curr_menu[-1]
                    cm[0].children.append(node)
                    node.parent = cm[0]
                    menu = wx.Menu()
                    self.curr_menu.append( (node, menu) )
                elif name == 'item':
                    self.curr_item = MenuTree.Node()
                else:
                    try: self.curr_index = self.itemattrs.index(name)
                    except ValueError:
                        # ignore unknown attributes...
                        self.curr_index = -1
                        pass
##                         from xml_parse import XmlParsingError
##                         raise XmlParsingError, _("invalid menu item attribute")
            def end_elem(self, name):
                if name == 'item':
                    try: cm = self.curr_menu[-1]
                    except IndexError:
                        from xml_parse import XmlParsingError
                        raise XmlParsingError, _("menu item outside a menu")
                    cm[0].children.append(self.curr_item)
                    self.curr_item.parent = cm[0]
                elif name == 'menu':
                    self.menu_depth -= 1
                    self.curr_menu.pop()
                elif name == 'menus':
                    self.owner.set_menus(self.owner.menus)
                    return True
            def char_data(self, data):
                setattr(self.curr_item, self.itemattrs[self.curr_index], data)
                
        if name == 'menus':
            return MenuHandler(self)
        return None

# end of class EditMenuBar


def builder(parent, sizer, pos, number=[0]):
    """\
    factory function for EditMenuBar objects.
    """
    class Dialog(wx.Dialog):
        def __init__(self):
            wx.Dialog.__init__(self, None, -1, _('Select menubar class'))
            if common.app_tree.app.get_language().lower() == 'xrc':
                self.klass = 'wxMenuBar'
            else:
                if not number[0]: self.klass = 'MyMenuBar'
                else: self.klass = 'MyMenuBar%s' % number[0]
                number[0] += 1
            klass_prop = TextProperty(self, 'class', self, label=_('class'))
            szr = wx.BoxSizer(wx.VERTICAL)
            szr.Add(klass_prop.panel, 0, wx.EXPAND)
            sz2 = wx.BoxSizer(wx.HORIZONTAL)
            sz2.Add(wx.Button(self, wx.ID_OK, _('OK')), 0, wx.ALL, 3)
            sz2.Add(wx.Button(self, wx.ID_CANCEL, _('Cancel')), 0, wx.ALL, 3)
            szr.Add(sz2, 0, wx.ALL|wx.ALIGN_CENTER, 3)
            self.SetAutoLayout(True)
            self.SetSizer(szr)
            szr.Fit(self)
            if self.GetBestSize()[0] < 150:
                self.SetSize((150, -1))
            self.CenterOnScreen()

        def undo(self):
            if number[0] > 0:
                number[0] -= 1

        def __getitem__(self, value):
            if value == 'class':
                def set_klass(c): self.klass = c
                return (lambda : self.klass, set_klass)
    # end of inner class

    dialog = Dialog()
    if dialog.ShowModal() == wx.ID_CANCEL:
        # cancel the operation
        dialog.undo()
        dialog.Destroy()
        return
    
    name = 'menubar_%d' % (number[0] or 1)
    while common.app_tree.has_name(name):
        number[0] += 1
        name = 'menubar_%d' % number[0]

    mb = EditMenuBar(name, dialog.klass, parent, common.property_panel)
    mb.node = Tree.Node(mb)
    common.app_tree.add(mb.node)
    mb.show_widget(True)
    mb.show_properties()
    

def xml_builder(attrs, parent, sizer, sizeritem, pos=None):
    """\
    factory to build EditMenuBar objects from an xml file
    """
    name = attrs.get('name')
    if parent is not None:
        if name:
            parent.menubar.set_name(name)
            parent.menubar.name_prop.set_value(name)
        return parent.menubar
    else:
        mb = EditMenuBar(name, attrs.get('class', 'wxMenuBar'), None,
                         common.property_panel)
        mb.node = Tree.Node(mb)
        common.app_tree.add(mb.node)
        return mb


def initialize():
    """\
    initialization function for the module: returns a wx.BitmapButton to be
    added to the main palette.
    """
    cwx = common.widgets_from_xml
    cwx['EditMenuBar'] = xml_builder
    common.widgets['EditMenuBar'] = builder
    
    return common.make_object_button('EditMenuBar', 'icons/menubar.xpm', 1)
