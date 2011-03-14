#(c)www.stani.be

import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Recent files as tab."""

__doc__=INFO['doc']%INFO
#_______________________________________________________________________________

import os
import wx
import sm.scriptutils as scriptutils
import _spe.help as help

class DropAdd(wx.FileDropTarget):
    """Adds a file to recent files when dropped on recent tab."""
    def __init__(self,add):
        wx.FileDropTarget.__init__(self)
        self.add=add
    def OnDropFiles(self,x,y,fileNames):
        fileNames=[script for script in fileNames 
            if os.path.splitext(script)[-1].lower() in ['.py','.pyw']]
        if fileNames:
            self.add(fileNames)
            return 1
        else:return 0
        
class Panel(wx.ListCtrl):
    def __init__(self,panel,**options):
        wx.ListCtrl.__init__(self,parent=panel,style=panel.LIST_STYLE)
        self.imageList  = wx.ImageList(16,16)
        self.pyIcon     = self.imageList.Add(panel.icons['source_py.png'])
        self.files      = []
        self.panel      = panel
        self.SetImageList(self.imageList,wx.IMAGE_LIST_SMALL)
        self.SetDropTarget(DropAdd(self.add))
        self.SetHelpText(help.RECENT)
        #events
        wx.EVT_LIST_ITEM_SELECTED(self, -1, self.onLeftClick)
        wx.EVT_LIST_ITEM_ACTIVATED(self, -1, self.onRightClick)
        wx.EVT_LIST_ITEM_MIDDLE_CLICK(self, -1, self.onMiddleClick)
        
    def add(self,fileList):
        """Register file list as recent."""
        files   = [file for file in self.files \
            if file not in fileList and os.path.exists(file)]
        if len(self.files)-len(files)!=len(fileList):
            fileList.extend(files)
            self.files=fileList
            self.update()
        
    def update(self):
        self.DeleteAllItems()
        i=0
        self.files = [(os.path.basename(str(file)),file) for file in self.files]
        try:
            self.files.sort(key=lambda (name,path): name.lower())
        except:
            #python2.3
            self.files.sort()
        self.files = [file[1] for file in self.files]
        for file in self.files:
            self.InsertImageStringItem(i, os.path.basename(str(file)),self.pyIcon)
            i+=1
        try:
            self.SetColumnWidth(-1,300)
        except:
            pass

    #---events
    def onLeftClick(self,event):
        """Open file on left click."""
        file=self.files[event.GetIndex()]
        f=self.panel.openList(file)
        if not f:
            self.panel.toolBar.SetFocus()
            self.files.remove(file)
            self.update()
       
    def onRightClick(self,event):
        """Run file on right click."""
        file=self.files[event.GetIndex()]
        scriptutils.run(file)
        self.panel.shell.prompt()
    
    def onMiddleClick(self,event):
        """Import file on middle click."""
        file=self.files[event.GetIndex()]
        scriptutils.importMod(file)
        self.panel.shell.prompt()

    
