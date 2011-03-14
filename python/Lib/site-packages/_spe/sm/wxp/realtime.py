"""realtime.py | GPL - license | (c)2005 www.stani.be

This module provides two classes which enable to update only selectively parts
of a wx.TreeCtrl or a wx.ListCtrl. This makes fast/realtime updating possible
of only changed items instead of the whole tree or list.

These classes are used in SPE's sidebar:
- TreeCtrl for Explore
- ListCtrl for Todo & Index"""

#todo: Maybe self.deleted of Item is not necessary

import wx

WARNING = 'Warning: %s: please contact spe.stani.be at gmail.com'

class Item:
    backgroundColour    = (255,255,255)
    data                = None
    deleted             = False
    textColour          = (0,0,0)
    wx                  = None
    def reset(self):
        self._update    = [] #lists should be created for each instance separately
        self._updateAll = []
        
class Ctrl:
    _base                   = None
    def __init__(self):
        self.items  = {}

    def _createUniqueId(self,base,others=[],data=None):
        """Create unique id with data & index."""
        if data:
            try:
                dataId      = '|'+repr(data).replace('%','%%')
            except:
                dataId      = ''
        else:
            dataId          = ''
        id                  = ('%s%s|%%d'%(base.replace('%','%%'),dataId)).encode('ascii','replace')
        nr                  = 0
        othersId            = [other.id for other in others]
        while id%nr in othersId:
            nr              += 1
        return id%nr
        
    def _deleteItem(self,item,fromItems=True):
        """Delete safely (not wx) item from self._items"""
        if item.wx != None and not item.deleted:
            self._wxDeleteItem(self,item.wx)
            item.wx = None
        if fromItems:
            item._delete()
            if self.items.has_key(item.id):
                del self.items[item.id]
            
    def _renewItem(self,item):
        """DeleteWx component and refresh update.
        This is used for an item which previously came earlier."""
        self._deleteItem(item, fromItems = False)
        item._update    = item._updateAll

    def _DeleteItem(self,item):
        """Indepedent delete wx item method (to be overwritten)."""
        
    def _insertItem(self,parent,children,index,text):
        """Inserts an item after a given one, used by self._update()
        (to be overwritten)"""
            
    def _prependItem(self,parent,text):
        """Inserts an item as the first one, used by self._update().
        (to be overwritten)"""
        
    def _update(self,parent,recursively=False):
        """Update (recursively) all children (Item) of parent(Item or List)."""
        children                = parent.children
        previousChildren        = parent.previousChildren
        for index, child in enumerate(children[:]):
            if child in previousChildren:
                #child exists already
                prevIndex       = previousChildren.index(child)
                abandoneds      = previousChildren[:prevIndex]
                abandoneds.reverse()
                for abandoned in abandoneds:
                    if abandoned in children:
                        #only remove wx attribute
                        self._renewItem(abandoned)
                    else:
                        #remove everything
                        self._DeleteItem(abandoned)
                previousChildren= previousChildren[prevIndex+1:]
            elif previousChildren and previousChildren[0] not in children:
                #child can be copied in existing, abandoned item
                empty_slot      = previousChildren[0]
                child           = children[index]\
                                = self._copyItemTo(child,empty_slot)
                previousChildren= previousChildren[1:]
            else:
                #child must be created
                if index>0:
                    child.wx        = self._insertItem(parent,children,index,child.text)
                else:
                    child.wx        = self._prependItem(parent,child.text)
            self._updateItem(child,index)
            #recursive on its children
            if recursively:
                self._update(child,True)
        for abandoned in previousChildren:
            self._DeleteItem(abandoned)
        parent.previousChildren = children
        
    def _updateItem(self,item,index=None):
        """Execute pending update actions of TreeItem"""
        for action in item._update:
            arguments = [self,item.wx]
            arguments.extend(action[1:])
            action[0](*arguments)

    def SetItemTextColour(self,item,color):
        """Sets the text colour of a TreeItem"""
        if item.textColour != color:
            item.textColour = color
            item._update.append((self._base.SetItemTextColour,color))
        item._updateAll.append((self._base.SetItemTextColour,color))
            
    def SetItemBackgroundColour(self,item,color):
        """Sets the background colour of a TreeItem"""
        if item.backgroundColour != color:
            item.backgroundColour = color
            item._update.append((self._base.SetItemBackgroundColour,color))
        item._updateAll.append((self._base.SetItemBackgroundColour,color))
            
class TreeItem(Item):
    """All the wx actions are handled by the Tree class."""
    bold = False
    
    def __init__(self,text,id):
        """self.wx holds the wx.TreeItemData"""
        self.id                 = id
        self.text               = text
        #
        self.previousChildren   = []
        self.image              = {}
        self.reset()
        
    def _delete(self):
        """When an item is removed its children are also removed recursively."""
        for child in self.children:
            if child.children:
                child._delete();
            child.deleted = True
        self.deleted    = True
        
    def reset(self):
        Item.reset(self)
        self.children           = []
        
class TreeCtrl(Ctrl,wx.TreeCtrl):
    
    _base   = wx.TreeCtrl
    
    def __init__(self,*args,**kwargs):
        Ctrl.__init__(self)
        wx.TreeCtrl.__init__(self,*args, **kwargs)
        self._DeleteItem    = self.Delete
        self._wxDeleteItem  = wx.TreeCtrl.Delete
        self._style         = kwargs['style']
        self._hideRoot      = self._style & wx.TR_HIDE_ROOT
        
    def _copyItemTo(self,frm,to):
        """Copy/steal wx control from an abandoned TreeItem to avoid creating a new wx control."""
        frm.wx  = to.wx
        if frm.data != to.data:
            frm._update.append((wx.TreeCtrl.SetPyData,frm.data))
        frm._updateAll.append((wx.TreeCtrl.SetPyData,frm.data))
        frm.previousChildren = to.previousChildren
        if frm.text != to.text:
            frm._update.append((wx.TreeCtrl.SetItemText,frm.text))
        frm._updateAll.append((wx.TreeCtrl.SetItemText,frm.text))
        return frm
        
    def _insertItem(self,parent,children,index,text):
        """Inserts an item after a given one."""
        return wx.TreeCtrl.InsertItem(self,parent.wx,children[index-1].wx,text)
    
    def _prependItem(self,parent,text):
        """Inserts an item as the first one."""
        return wx.TreeCtrl.PrependItem(self,parent.wx,text)

    def AddRoot(self,text):
        self.root           = TreeItem(text=text,id=text)
        self.items[text]    = self.root
        self.root.wx        = wx.TreeCtrl.AddRoot(self,text)
        if not self._hideRoot: 
            self.root._update.append((wx.TreeCtrl.Expand,))
        return self.root
        
    def AppendItem(self,parent,text,data=None):
        """Add data immediately, if the label is not unique. Be aware that that
        is a devation from wx.ListCtrl, if data is not wxTreeItemData.
        
        There are two possibilities that an item is appended to its parent
        - if already present, pick it up and update the text
        - if not, create one item"""
        if hasattr(data,'GetData'): data = data.GetData()
        pChildren           = parent.children
        id                  = self._createUniqueId(
                                base    = '%s|%s'%(parent.id,text),
                                others  = pChildren,
                                data    = data
                            )
        #get item
        if self.items.has_key(id):
            item            = self.items[id]
            item.reset()
        else:
            item            = self.items[id] = TreeItem(text=text,id=id)
        pChildren.append(item)
        #data
        if data != None:
            self.SetPyData(item,data)
        return item
        
    def Delete(self,item):
        """Delete item and all its children from the tree."""
        self._deleteItem(item)
        
    def Update(self):
        """Update only differences between current and previous state.
        This method MUST be called in the end otherwise there will be no visual
        change."""
        self._update(self.root,recursively=True)
        self._updateItem(self.root)
        
    def Collapse(self,item):
        """Collapse a TreeItem."""
        if wx.TreeCtrl.IsExpanded(self,item.wx):
            wx.TreeCtrl.Collapse(self,item.wx)
            
    def CollapseAndReset(self,item):
        """Remove children of a TreeItem, mostly used for self.root."""
        item.children = []
           
    def Expand(self,item):
        """Expands a TreeItem."""
        if not ((self._hideRoot and item == self.root) or wx.TreeCtrl.IsExpanded(self,item.wx)):
            wx.TreeCtrl.Expand(self,item.wx)
            
    def SetItemBold(self,item,bold=True):
        """Sets the background colour of a TreeItem"""
        if item.bold != bold:
            item.bold = bold
            item._update.append((wx.TreeCtrl.SetItemBold,bold))
        item._updateAll.append((wx.TreeCtrl.SetItemBold,bold))
        
    def SetItemImage(self,item,image,which=wx.TreeItemIcon_Normal):
        """Sets the image for a certain state (which) of a TreeItem"""
        if (not item.image.has_key(which)) or item.image[which] != image:
            item.image[which] = image
            item._update.append((wx.TreeCtrl.SetItemImage,image,which))
        item._updateAll.append((wx.TreeCtrl.SetItemImage,image,which))
        
    def SetItemText(self,item,text):
        """Sets the text of a TreeItem"""
        if item.text != text:
            item.text = text
            item._update.append((wx.TreeCtrl.SetItemText,text))
        item._updateAll.append((wx.TreeCtrl.SetItemText,text))
            
    def SetPyData(self,item,data):
        """Sets the py data of a TreeItem."""
        if item.data != data:
            item.data = data
            item._update.append((wx.TreeCtrl.SetPyData,data))
        item._updateAll.append((wx.TreeCtrl.SetPyData,data))

class ListItem(Item):
    def __init__(self,index,text,id):
        self.index              = index
        self.text               = text
        self.id                 = id
        #
        self.image              = None
        self.reset()
        
    def _delete(self):
        """Flag as deleted."""
        self.deleted    = True
        
class ListCtrl(Ctrl,wx.ListCtrl):

    _base = wx.ListCtrl
    
    def __init__(self,*args,**kwds):
        Ctrl.__init__(self)
        wx.ListCtrl.__init__(self,*args,**kwds)
        self._DeleteItem          = self.DeleteItem
        self._wxDeleteItem        = wx.ListCtrl.DeleteItem
        self.children           = []
        self.previousChildren   = []
        
    def _copyItemTo(self,frm,to):
        """Copy/steal wx control from an abandoned TreeItem to avoid creating a new wx control."""
        frm.wx  = to.wx
        if frm.data != to.data:
            frm._update.append((wx.ListCtrl.SetItemData,frm.data))
        frm._updateAll.append((wx.ListCtrl.SetItemData,frm.data))
        toText  = to.text
        for column, label in frm.text.items():
            if toText.has_key(column) and label != toText[column]:
                frm._update.append((wx.ListCtrl.SetStringItem,column,label))
            frm._updateAll.append((wx.ListCtrl.SetStringItem,column,label))
        return frm
        
    def _insertItem(self,parent,children,index,text):
        """Inserts an item after a given one."""
        return wx.ListCtrl.InsertStringItem(self,index,text[0])
        
    def _prependItem(self,parent,text):
        """Inserts an item as the first one."""
        return wx.ListCtrl.InsertStringItem(self,0,text[0])
        
    def _updateItem(self,item,index):
        """Same as Ctrl, but update also index."""
        item.wx = index
        Ctrl._updateItem(self,item,index)
        
    def DeleteAllItems(self):
        self.children = []
            
    def DeleteItem(self,item):
        """Delete item safely."""
        if item in self.children:
            self.children.remove(item)
        self._deleteItem(item)
    
    def InsertStringItem(self,index,label,data=None):
        """Add data immediately, if the label is not unique. Be aware that that
        is a devation from wx.ListCtrl
        
        There are two possibilities that an item is appended to its parent
        - if already present, pick it up and update the text
        - if not, create one item"""
        id                  = self._createUniqueId(
                                base    = label,
                                others  = self.children,
                                data    = data
                            )
        #get item
        if self.items.has_key(id):
            item            = self.items[id]
            item.reset()
        else:
            item            = self.items[id] = ListItem(index=index,text={0:label},id=id)
        self.children.append(item)
        #data
        if data != None:
            self.SetItemData(item,data)
        return item
        
    def InsertImageStringItem(self,index,label,imageIndex):
        item = self.InsertStringItem(index,label)
        self.SetItemImage(item,imageIndex)
        return item
        
    def SetItemImage(self,item,imageIndex):
        if item.image != imageIndex:
            item.image = imageIndex
            item._update.append((wx.ListCtrl.SetItemImage,imageIndex))
        item._updateAll.append((wx.ListCtrl.SetItemImage,imageIndex))
        
    def SetItemData(self,item,data):
        if item.data != data:
            item.data = data
            item._update.append((wx.ListCtrl.SetItemData,data))
        item._updateAll.append((wx.ListCtrl.SetItemData,data))
        
    def SetItemText(self,item,text):
        """Sets the text of a TreeItem"""
        if item.text != text:
            item.text = text
            item._update.append((wx.TreeCtrl.SetItemText,text))
        item._updateAll.append((wx.TreeCtrl.SetItemText,text))
            
    def SetStringItem(self,item,column,label):
        text                = item.text
        if not text.has_key(column) or text[column] != label:
            text[column]    = label
            item._update.append((wx.ListCtrl.SetStringItem,column,label))
        item._updateAll.append((wx.ListCtrl.SetStringItem,column,label))
                    
    def Update(self):
        """Update only differences between current and previous state.
        This method MUST be called in the end otherwise there will be no visual
        change."""
        self._update(self,recursively=False)
