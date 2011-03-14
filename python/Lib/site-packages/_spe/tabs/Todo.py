####(c)www.stani.be-------------------------------------------------------------

import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Todo as tab."""

__doc__=INFO['doc']%INFO

####Panel class-----------------------------------------------------------------

import wx
import _spe.help

class Panel(wx.ListCtrl):
    def __init__(self,panel,*args,**kwds):
        wx.ListCtrl.__init__(self,parent=panel,style=wx.LC_REPORT)
        self.panel  = panel
        self.InsertColumn(col=0, format=wx.LIST_FORMAT_LEFT, 
                heading='File',width=120)
        self.InsertColumn(col=1, format=wx.LIST_FORMAT_LEFT, 
                heading='Line',width=40)
        self.InsertColumn(col=2, format=wx.LIST_FORMAT_LEFT, 
                heading='!',width=20)
        self.InsertColumn(col=3, format=wx.LIST_FORMAT_LEFT, 
                heading='Task',width=500)
        self.SetHelpText(_spe.help.TODO)
        #events
        wx.EVT_LIST_ITEM_SELECTED(self,-1,self.onSelect)
        
    #---events
    def onSelect(self,event):
        file,line=self.list[event.GetData()]
        self.panel.openList(file,line-1)
        
