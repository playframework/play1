####(c)www.stani.be-------------------------------------------------------------

import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Session as tab."""

__doc__=INFO['doc']%INFO

####Panel class-----------------------------------------------------------------

import wx
import _spe.help
from wx.py.crust import SessionListing

class Panel(SessionListing):
    def __init__(self,panel,*args,**kwds):
        SessionListing.__init__(self,parent=panel)
        self.SetHelpText(_spe.help.SESSION)
