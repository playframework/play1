#(c)www.stani.be                                                                
import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Help dialog."""

__doc__=INFO['doc']%INFO

import os

def create(self,path,fileName,replacements=None):
    text=open(os.path.join(path,'doc',fileName)).read()
    if replacements: text=text % replacements
    if os.path.splitext(fileName)[1].lower()=='.txt':text=text.replace('\n','<br>')
    self.messageScrolled(text)
