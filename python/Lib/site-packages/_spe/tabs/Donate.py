####(c)www.stani.be-------------------------------------------------------------

import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Index as tab."""

__doc__=INFO['doc']%INFO

####Panel class-----------------------------------------------------------------

import sm.wxp

import os, webbrowser
#import _spe.help

class Panel(sm.wxp.HtmlWindow):
    def __init__(self,panel,*args,**kwds):
        self.panel  = panel
        sm.wxp.HtmlWindow.__init__(self,parent=panel,id=-1)
        self.LoadPage(os.path.join(panel.path,'doc','donate.html'))
        
