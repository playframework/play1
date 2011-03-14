import wx
import os
import _spe.info as info

class Browser(wx.GenericDirCtrl) :
    def __init__ (self, parent, id, init_path=''):
        wx.GenericDirCtrl.__init__(self,parent,id,
            dir     = init_path,
            #size=(200,225),
            filter  = info.WILDCARD_EXTENDED,
            style   = wx.DIRCTRL_SHOW_FILTERS)
        self.dir    = init_path
        self.tree   = self.GetTreeCtrl()
        self.home   = os.path.dirname(info.INFO['userPath'])
        self.tree.Bind(wx.EVT_LEFT_DCLICK, self.onClick)
        self.tree.Bind(wx.EVT_RIGHT_DOWN, self.onClick)
        
    def update(self):
        self.SetPath(self.dir)
        
    #onClick
    def onClick (self, event) :
        event.Skip()
        path    = self.GetFilePath()
        if os.path.isfile(path): 
            self.open(path)
            
    def open(self, fname) : 
        pass
        
