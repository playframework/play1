# Name:         undo.py
# Purpose:      XRC editor, undo/redo module
# Author:       Roman Rolinsky <rolinsky@mema.ucl.ac.be>
# Created:      01.12.2002
# RCS-ID:       $Id: undo.py 49125 2007-10-10 23:15:03Z ROL $

from globals import *
import view
from component import Manager
from model import Model

undo_depth = 10                 # max number of undo remembered

# Undo/redo classes
class UndoManager:
    # Undo/redo stacks
    undo = []
    redo = []
    def RegisterUndo(self, undoObj):
        TRACE('RegisterUndo: %s', undoObj.label)
        self.undo.append(undoObj)
        while len(self.undo) > undo_depth: self.undo.pop(0)
        map(Undo.destroy, self.redo)
        self.redo = []
        self.UpdateToolHelp()
    def GetUndoLabel(self):
        return self.undo[-1].label
    def GetRedoLabel(self):
        return self.redo[-1].label
    def Undo(self):
        undoObj = self.undo.pop()
        undoObj.undo()
        self.redo.append(undoObj)
        view.frame.SetStatusText('Undone')
        Presenter.undoSaved = True
        self.UpdateToolHelp()
    def Redo(self):
        undoObj = self.redo.pop()
        undoObj.redo()
        self.undo.append(undoObj)
        view.frame.SetStatusText('Redone')
        Presenter.undoSaved = True
        self.UpdateToolHelp()
    def Clear(self):
        map(Undo.destroy, self.undo)
        self.undo = []
        map(Undo.destroy, self.redo)
        self.redo = []
        self.UpdateToolHelp()
    def CanUndo(self):
        return bool(self.undo)
    def CanRedo(self):
        return bool(self.redo)
    def UpdateToolHelp(self):
        if g.undoMan.CanUndo(): 
            msg = 'Undo ' + self.GetUndoLabel()
            view.frame.tb.SetToolShortHelp(wx.ID_UNDO, msg)
            view.frame.tb.SetToolLongHelp(wx.ID_UNDO, msg)
        if g.undoMan.CanRedo(): 
            msg = 'Redo ' + self.GetRedoLabel()
            view.frame.tb.SetToolShortHelp(wx.ID_REDO, msg)
            view.frame.tb.SetToolLongHelp(wx.ID_REDO, msg)

class Undo:
    '''ABC for Undo*.'''
    def redo(self):             # usually redo is same as undo
        self.undo()
    def destroy(self):
        pass

class UndoCutDelete(Undo):
    label = 'cut/delete'
    def __init__(self, itemIndex, node):
        self.itemIndex = itemIndex
        self.node = node
        self.states = view.tree.GetFullState()
    def destroy(self):
        if self.node: self.node.unlink()
        self.states = None
    def undo(self):
        # Updating DOM. Find parent node first
        parentItem = view.tree.ItemAtFullIndex(self.itemIndex[:-1])
        parentNode = view.tree.GetPyData(parentItem)
        parentComp = Manager.getNodeComp(parentNode)
        nextItem = view.tree.ItemAtFullIndex(self.itemIndex)
        if nextItem:
            nextNode = parentComp.getTreeOrImplicitNode(view.tree.GetPyData(nextItem))
        else:
            nextNode = None
        # Insert before next
        parentNode.insertBefore(self.node, nextNode)
        # Update tree and presenter
        view.tree.Flush()
        view.tree.SetFullState(self.states)
        item = view.tree.ItemAtFullIndex(self.itemIndex)
        view.tree.EnsureVisible(item)
        # This will generate events
        view.tree.SelectItem(item)
    def redo(self):
        item = view.tree.ItemAtFullIndex(self.itemIndex)
        view.tree.SelectItem(item)
        self.node = Presenter.delete(item)

class UndoPasteCreate(Undo):
    label = 'paste/create'
    def __init__(self, itemParent, parent, item, selected):
        self.itemParentIndex = g.tree.ItemFullIndex(itemParent)
        self.parent = parent
        self.itemIndex = g.tree.ItemFullIndex(item) # pasted item
        self.selectedIndex = g.tree.ItemFullIndex(selected) # maybe different from item
        self.elem = None
    def destroy(self):
        if self.elem: self.elem.unlink()
    def undo(self):
        self.elem = g.tree.RemoveLeaf(g.tree.ItemAtFullIndex(self.itemIndex))
        # Restore old selection
        selected = g.tree.ItemAtFullIndex(self.selectedIndex)
        g.tree.EnsureVisible(selected)
        g.tree.SelectItem(selected)
        # Delete testWin?
        if g.testWin:
            # If deleting top-level item, delete testWin
            if selected == g.testWin.item:
                g.testWin.Destroy()
                g.testWin = None
            else:
                # Remove highlight, update testWin
                if g.testWin.highLight:
                    g.testWin.highLight.Remove()
                g.tree.needUpdate = True
    def redo(self):
        item = g.tree.InsertNode(g.tree.ItemAtFullIndex(self.itemParentIndex),
                                 self.parent, self.elem,
                                 g.tree.ItemAtFullIndex(self.itemIndex))
        # Scroll to show new item
        g.tree.EnsureVisible(item)
        g.tree.SelectItem(item)
        self.elem = None
        # Update testWin if needed
        if g.testWin and g.tree.IsHighlatable(item):
            if g.conf.autoRefresh:
                g.tree.needUpdate = True
                g.tree.pendingHighLight = item
            else:
                g.tree.pendingHighLight = None

class UndoReplace(Undo):
    label = 'replace'
    def __init__(self, item):
        self.itemIndex = g.tree.ItemFullIndex(item)
        #self.xxx = g.tree.GetPyData(item)
        self.elem = None
    def destroy(self):
        if self.elem: self.elem.unlink()
    def undo(self):
        print 'Sorry, UndoReplace is not yet implemented.'
        return
        item = g.tree.ItemAtFullIndex(self.itemIndex)
        xxx = g.tree.GetPyData(item)
        # Replace with old element
        parent = xxx.parent.node
        if xxx is self.xxx:   # sizeritem or notebookpage - replace child
            parent.replaceChild(self.xxx.child.node, xxx.child.node)
        else:
            parent.replaceChild(self.xxx.node, xxx.node)
        self.xxx.parent = xxx.parent
        xxx = self.xxx
        g.tree.SetPyData(item, xxx)
        g.tree.SetItemText(item, xxx.treeName())
        g.tree.SetItemImage(item, xxx.treeImage())

        # Update panel
        g.panel.SetData(xxx)
        # Update tools
        g.tools.UpdateUI()
        g.tree.EnsureVisible(item)
        g.tree.SelectItem(item)
        # Delete testWin?
        if g.testWin:
            # If deleting top-level item, delete testWin
            if selected == g.testWin.item:
                g.testWin.Destroy()
                g.testWin = None
            else:
                # Remove highlight, update testWin
                if g.testWin.highLight:
                    g.testWin.highLight.Remove()
                g.tree.needUpdate = True
    def redo(self):
        return

class UndoMove(Undo):
    label = 'move'
    def __init__(self, oldParent, oldIndex, newParent, newIndex):
        # Store indexes because items can be invalid already
        self.oldParentIndex = g.tree.ItemFullIndex(oldParent)
        self.oldIndex = oldIndex
        self.newParentIndex = g.tree.ItemFullIndex(newParent)
        self.newIndex = newIndex
    def destroy(self):
        pass
    def undo(self):
        oldParent = g.tree.ItemAtFullIndex(self.oldParentIndex)
        newParent = g.tree.ItemAtFullIndex(self.newParentIndex)
        item = g.tree.GetFirstChild(newParent)[0]
        for i in range(self.newIndex): item = g.tree.GetNextSibling(item)
        elem = g.tree.RemoveLeaf(item)
        nextItem = g.tree.GetFirstChild(oldParent)[0]
        for i in range(self.oldIndex): nextItem = g.tree.GetNextSibling(nextItem) 

        parent = g.tree.GetPyData(oldParent).treeObject()

        # Check parent and child relationships.
        # If parent is sizer or notebook, child is of wrong class or
        # parent is normal window, child is child container then detach child.
        xxx = MakeXXXFromDOM(parent, elem)
        isChildContainer = isinstance(xxx, xxxChildContainer)
        if isChildContainer and \
           ((parent.isSizer and not isinstance(xxx, xxxSizerItem)) or \
            (isinstance(parent, xxxNotebook) and not isinstance(xxx, xxxNotebookPage)) or \
           not (parent.isSizer or isinstance(parent, xxxNotebook))):
            elem.removeChild(xxx.child.node) # detach child
            elem.unlink()           # delete child container
            elem = xxx.child.node # replace
            # This may help garbage collection
            xxx.child.parent = None
            isChildContainer = False
        # Parent is sizer or notebook, child is not child container
        if parent.isSizer and not isChildContainer and not isinstance(xxx, xxxSpacer):
            # Create sizer item element
            sizerItemElem = MakeEmptyDOM('sizeritem')
            sizerItemElem.appendChild(elem)
            elem = sizerItemElem
        elif isinstance(parent, xxxNotebook) and not isChildContainer:
            pageElem = MakeEmptyDOM('notebookpage')
            pageElem.appendChild(elem)
            elem = pageElem

        selected = g.tree.InsertNode(oldParent, parent, elem, nextItem)
        g.tree.EnsureVisible(selected)
        # Highlight is outdated
        if g.testWin and g.testWin.highLight:
            g.testWin.highLight.Remove()
            g.tree.needUpdate = True
        g.tree.SelectItem(selected)
    def redo(self):
        oldParent = g.tree.ItemAtFullIndex(self.oldParentIndex)
        newParent = g.tree.ItemAtFullIndex(self.newParentIndex)
        item = g.tree.GetFirstChild(oldParent)[0]
        for i in range(self.oldIndex): item = g.tree.GetNextSibling(item)
        elem = g.tree.RemoveLeaf(item)

        parent = g.tree.GetPyData(newParent).treeObject()

        # Check parent and child relationships.
        # If parent is sizer or notebook, child is of wrong class or
        # parent is normal window, child is child container then detach child.
        xxx = MakeXXXFromDOM(parent, elem)
        isChildContainer = isinstance(xxx, xxxChildContainer)
        if isChildContainer and \
           ((parent.isSizer and not isinstance(xxx, xxxSizerItem)) or \
            (isinstance(parent, xxxNotebook) and not isinstance(xxx, xxxNotebookPage)) or \
           not (parent.isSizer or isinstance(parent, xxxNotebook))):
            elem.removeChild(xxx.child.node) # detach child
            elem.unlink()           # delete child container
            elem = xxx.child.node # replace
            # This may help garbage collection
            xxx.child.parent = None
            isChildContainer = False
        # Parent is sizer or notebook, child is not child container
        if parent.isSizer and not isChildContainer and not isinstance(xxx, xxxSpacer):
            # Create sizer item element
            sizerItemElem = MakeEmptyDOM('sizeritem')
            sizerItemElem.appendChild(elem)
            elem = sizerItemElem
        elif isinstance(parent, xxxNotebook) and not isChildContainer:
            pageElem = MakeEmptyDOM('notebookpage')
            pageElem.appendChild(elem)
            elem = pageElem

        nextItem = g.tree.GetFirstChild(newParent)[0]
        for i in range(self.newIndex): nextItem = g.tree.GetNextSibling(nextItem) 
        selected = g.tree.InsertNode(newParent, parent, elem, nextItem)
        g.tree.EnsureVisible(selected)
        # Highlight is outdated
        if g.testWin and g.testWin.highLight:
            g.testWin.highLight.Remove()
            g.tree.needUpdate = True
        g.tree.SelectItem(selected)


class UndoEdit(Undo):
    '''Undo class for using in AttributePanel.'''
    label = 'edit'
    def __init__(self, item, page):
        self.index = view.tree.ItemFullIndex(item)
        self.page = page
        panel = view.panel.nb.GetPage(page).panel
        self.values = panel.GetValues()
    def undo(self):
#        import pdb;pdb.set_trace()
        # Go back to the item if needed
        item = view.tree.GetSelection()
        if not item or self.index != view.tree.ItemFullIndex(item):
            Presenter.unselect()
            undoItem = view.tree.ItemAtFullIndex(self.index)
            if item != undoItem:
                view.tree.SelectItem(view.tree.ItemAtFullIndex(self.index))
                wx.Yield()          # Refresh panel
        panel = view.panel.nb.GetPage(self.page).panel
        values = panel.GetValues()
        panel.SetValues(self.values)
        self.values = values
        # This will not generate events so we have to update the undo object
        view.panel.nb.ChangeSelection(self.page)
        Presenter.createUndoEdit(item, self.page)

class UndoGlobal(Undo):
    label = 'global'
    def __init__(self):
        self.mainNode = Model.mainNode.cloneNode(True)
        self.states = view.tree.GetFullState()
    def destroy(self):
        self.mainNode.unlink()
    def undo(self):
        # Exchange
        Model.mainNode,self.mainNode = \
            self.mainNode,Model.dom.replaceChild(self.mainNode, Model.mainNode)
        # Replace testElem
        Model.testElem = Model.mainNode.childNodes[0]
        states = view.tree.GetFullState()
        Presenter.unselect()
        view.tree.Flush()
        view.tree.SetFullState(self.states)
        self.states = states
    def redo(self):
        self.undo()
        Presenter.unselect()

