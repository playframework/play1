#(c)www.stani.be                                                                
import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""General spe dialog, showing the logo on top and displaying the text in a 
customized wxHtmlwindow, so that the links are opened in an external webbrowser."""

__doc__=INFO['doc']%INFO
#_______________________________________________________________________________

import os,webbrowser
import wx
import sm.wxp

def create(parent,message,path):
    dlg= wxDialog1(parent,message,path)
    dlg.ShowModal()

[wxID_WXDIALOG1] = map(lambda _init_ctrls: wx.NewId(), range(1))

class wxDialog1(wx.Dialog):
    def _init_utils(self):
        # generated method, don't edit
        pass

    def _init_ctrls(self, prnt):
        # generated method, don't edit
        wx.Dialog.__init__(self, id=wxID_WXDIALOG1, name='', parent=prnt,
              pos=wx.Point(25, 31), size=wx.Size(488, 588),
              style=wx.DEFAULT_DIALOG_STYLE|wx.RESIZE_BORDER|wx.MAXIMIZE_BOX,
              title='Spe')
        self._init_utils()
        self.SetClientSize(wx.Size(480, 554))

    def __init__(self, parent,message,path=''):
        self._init_ctrls(parent)
        self.SetBackgroundColour(wx.Colour(0, 0, 0))
        self.staticBitmap1 = wx.StaticBitmap(
              bitmap=wx.Bitmap(os.path.join(path,'images','spe_about.jpg'),
              wx.BITMAP_TYPE_JPEG), id=-1,
              name='staticBitmap1', parent=self, pos=wx.Point(0, 0),
              size=wx.Size(480, 100), style=0)
        self.text = sm.wxp.HtmlWindow(id=-1, name='text',
              parent=self, pos=wx.Point(0, 0), size=wx.Size(480, 339))
        self.text.SetPage('<font face=arial>%s</font>'%message)
        self.init_sizers()

    def init_sizers(self):
        """Stretch the listbox."""
        self.box1=wx.BoxSizer(wx.VERTICAL)
        self.box1.Add(self.staticBitmap1,0,0,0)
        self.box1.Add(self.text, 1, wx.ALL|wx.EXPAND, 0)
        self.SetAutoLayout(1)
        self.SetSizer(self.box1)

