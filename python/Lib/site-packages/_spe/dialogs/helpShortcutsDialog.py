#Boa:Dialog:wxDialog1

#(c)www.stani.be                                                                
import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Help dialog showing all the shortcuts."""

__doc__=INFO['doc']%INFO
#_______________________________________________________________________________

import os

#---Boa generated
from wxPython.wx import *

def create(parent,path=''):
    return wxDialog1(parent,path)

[wxID_WXDIALOG1, wxID_WXDIALOG1LISTCTRL1, 
] = map(lambda _init_ctrls: wxNewId(), range(2))

class wxDialog1(wxDialog):
    def _init_coll_listCtrl1_Columns(self, parent):
        # generated method, don't edit

        parent.InsertColumn(col=0, format=wxLIST_FORMAT_LEFT, heading='Key',
              width=170)
        parent.InsertColumn(col=1, format=wxLIST_FORMAT_LEFT,
              heading='Description', width=500)

    def _init_utils(self):
        # generated method, don't edit
        pass

    def _init_ctrls(self, prnt):
        # generated method, don't edit
        wxDialog.__init__(self, id=wxID_WXDIALOG1, name='', parent=prnt,
              pos=wxPoint(119, 108), size=wxSize(486, 376),
              style=wxDEFAULT_DIALOG_STYLE|wxRESIZE_BORDER|wxMAXIMIZE_BOX,
              title='Spe - Shorcuts - www.stani.be')
        self._init_utils()
        self.SetClientSize(wxSize(478, 349))

        self.listCtrl1 = wxListCtrl(id=wxID_WXDIALOG1LISTCTRL1,
              name='listCtrl1', parent=self, pos=wxPoint(0, 0), size=wxSize(478,
              349), style=wxLC_REPORT, validator=wxDefaultValidator)
        self._init_coll_listCtrl1_Columns(self.listCtrl1)

    def __init__(self, parent, path):
        #wxImage_AddHandler(wxPNGHandler())
        self._init_ctrls(parent)
        self.init_listCtrl1(path)

    def init_listCtrl1(self,path):
        f=open(os.path.join(path,'dialogs','shortcuts.txt'))
        lines=f.read().split('\n')
        f.close()
        ln=0
        self.il=wxImageList(16, 16)
        self.key = self.il.Add(wxBitmap(os.path.join(path,'images','keyboard.png'),wxBITMAP_TYPE_PNG))
        self.keys = self.il.Add(wxBitmap(os.path.join(path,'images','key_bindings.png'),wxBITMAP_TYPE_PNG))
        self.listCtrl1.SetImageList(self.il, wxIMAGE_LIST_SMALL)

        for line in lines:
            data=line.split('\t')
            if len(data)>2:
                if data[0].replace('SHIFT','').replace('CTRL','').replace('ALT','')==data[0]:
                    self.listCtrl1.InsertImageStringItem(ln,data[0],self.key)
                else:
                    self.listCtrl1.InsertImageStringItem(ln,data[0],self.keys)
                self.listCtrl1.SetStringItem(ln, 1, data[2])
                ln+=1
            
