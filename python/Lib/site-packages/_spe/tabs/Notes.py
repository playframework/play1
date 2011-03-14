####(c)www.stani.be-------------------------------------------------------------

import _spe.info as info
INFO    = info.copy()

INFO['description']=\
"""Notes as tab."""

__doc__=INFO['doc']%INFO

####Panel class-----------------------------------------------------------------

import wx
import _spe.help

class Panel(wx.TextCtrl):
    def __init__(self,panel,*args,**kwds):
        style       = wx.TE_MULTILINE
        if not info.DARWIN:
            style   |= wx.TE_DONTWRAP
        wx.TextCtrl.__init__(self,parent=panel,id=-1,style=style)
        self.SetHelpText(_spe.help.NOTES)
