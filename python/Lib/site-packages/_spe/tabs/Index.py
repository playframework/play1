####(c)www.stani.be-------------------------------------------------------------

import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Index as tab."""

__doc__=INFO['doc']%INFO

####Panel class-----------------------------------------------------------------

import wx

import sys
import _spe.help

if sys.platform == 'win32':
    LIST_STYLE  = wx.LC_LIST|wx.LC_SMALL_ICON
else:
    LIST_STYLE  = wx.LC_LIST

class Panel(wx.ListCtrl):
    def __init__(self,panel,*args,**kwds):
        self.panel  = panel
        wx.ListCtrl.__init__(self,parent=panel,style=panel.LIST_STYLE)
        self.SetImageList(panel.iconsList,wx.IMAGE_LIST_SMALL)
        self.SetHelpText(_spe.help.INDEX)
        #---events
        wx.EVT_LIST_ITEM_SELECTED(self,-1,self.onSelect)
        
    #---events
    def onSelect(self,event):
        stripped,entry,line,colour,icon,file=self.list[event.GetData()]
        self.panel.openList(file,line-1)
        
