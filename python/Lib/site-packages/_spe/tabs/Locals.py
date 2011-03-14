####(c)www.stani.be-------------------------------------------------------------

import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Locals as tab."""

__doc__=INFO['doc']%INFO

####Panel class-----------------------------------------------------------------

import sm.wxp
import _spe.help
import sys

class Panel(sm.wxp.SmFilling):
    def __init__(self,panel,*args,**options):
        sm.wxp.SmFilling.__init__(self,parent=panel,
            rootObject=panel.shell.interp.locals, 
            rootLabel='locals()', 
            rootIsNamespace=1)
        self.tree.setStatusText=panel.SetStatusText
        self.SetHelpText(_spe.help.LOCALS)
