#Boa:Dialog:wxDialog1

#(c)www.stani.be                                                                
import _spe.info,sm
INFO=_spe.info.copy()

INFO['description']=\
"""Dialog to insert a coloured separator for the class explorer in the sidebar."""

__doc__=INFO['doc']%INFO
#_______________________________________________________________________________

import wx
import string

def create(parent):
    return wxDialog1(parent)

[wxID_WXDIALOG1, wxID_WXDIALOG1BUTTON1, wxID_WXDIALOG1BUTTON2, 
 wxID_WXDIALOG1BUTTON3, wxID_WXDIALOG1BUTTON4, wxID_WXDIALOG1CHECKBOX1, 
 wxID_WXDIALOG1TEXTCTRL1, 
] = map(lambda _init_ctrls: wx.NewId(), range(7))

class wxDialog1(wx.Dialog):
    def _init_utils(self):
        # generated method, don't edit
        pass

    def _init_ctrls(self, prnt):
        # generated method, don't edit
        wx.Dialog.__init__(self, id=wxID_WXDIALOG1, name='', parent=prnt,
              pos=wx.Point(639, 463), size=wx.Size(360, 136),
              style=wx.DEFAULT_DIALOG_STYLE, title='Spe - Insert separator')
        self._init_utils()
        self.SetClientSize(wx.Size(352, 109))

        self.textCtrl1 = wx.TextCtrl(id=wxID_WXDIALOG1TEXTCTRL1, name='label',
              parent=self, pos=wx.Point(8, 9), size=wx.Size(256, 21), style=0,
              value='Label')

        self.button1 = wx.Button(id=wxID_WXDIALOG1BUTTON1, label='Background',
              name='button1', parent=self, pos=wx.Point(272, 35), size=wx.Size(75,
              23), style=0)
        wx.EVT_BUTTON(self.button1, wxID_WXDIALOG1BUTTON1, self.OnBackgroundChoose)

        self.button2 = wx.Button(id=wxID_WXDIALOG1BUTTON2, label='Foreground',
              name='button2', parent=self, pos=wx.Point(272, 8), size=wx.Size(75,
              23), style=0)
        wx.EVT_BUTTON(self.button2, wxID_WXDIALOG1BUTTON2, self.OnForegroundChoose)

        self.button3 = wx.Button(id=wxID_WXDIALOG1BUTTON3, label='Insert',
              name='button3', parent=self, pos=wx.Point(192, 80), size=wx.Size(75,
              23), style=0)
        wx.EVT_BUTTON(self.button3, wxID_WXDIALOG1BUTTON3, self.OnInsert)

        self.button4 = wx.Button(id=wxID_WXDIALOG1BUTTON4, label='Cancel',
              name='button4', parent=self, pos=wx.Point(272, 80), size=wx.Size(75,
              23), style=0)
        wx.EVT_BUTTON(self.button4, wxID_WXDIALOG1BUTTON4, self.OnCancel)

        self.checkBox1 = wx.CheckBox(id=wxID_WXDIALOG1CHECKBOX1,
              label='Use background color', name='checkBox1', parent=self,
              pos=wx.Point(8, 40), size=wx.Size(256, 13), style=0)
        self.checkBox1.SetValue(0)
        wx.EVT_CHECKBOX(self.checkBox1, wxID_WXDIALOG1CHECKBOX1,
              self.OnBackgroundUse)

    def __init__(self, parent):
        self._init_ctrls(parent)
        self.foregroundColour=wx.Colour(0,0,0)
        self.backgroundColour=wx.Colour(255,255,255)
        self.button1.Disable()

    def OnCancel(self,event):
        self.Close()
        
    def OnBackgroundChoose(self, event):
        backgroundColour=self.selectColour(self.backgroundColour)
        if backgroundColour:
            self.backgroundColour=backgroundColour
            self.textCtrl1.SetBackgroundColour(self.backgroundColour)

    def OnForegroundChoose(self, event):
        foregroundColour=self.selectColour(self.foregroundColour)
        if foregroundColour:
            self.foregroundColour=foregroundColour
            self.textCtrl1.SetForegroundColour(self.foregroundColour)

    def selectColour(self,colour):
        dlg = wx.ColourDialog(self)
        dlg.GetColourData().SetChooseFull(1)
        dlg.GetColourData().SetColour(colour)
        if dlg.ShowModal() == wx.ID_OK:
            data = dlg.GetColourData().GetColour()
        else:return None
        dlg.Destroy()
        return data

    def OnBackgroundUse(self, event):
        checked=event.IsChecked()
        if checked:colour=self.backgroundColour
        else:colour=wx.Colour(255,255,255)
        self.textCtrl1.SetBackgroundColour(colour)
        self.button1.Enable(checked)

    def OnInsert(self, event):
        if not self.checkBox1.GetValue():self.backgroundColour=wx.Colour(255,255,255)
        label=self.textCtrl1.GetValue()
        separator='#---%s---%s%s%s'%(\
            label,
            wxColour2html(self.foregroundColour),
            wxColour2html(self.backgroundColour),
            sm.zfill('-',(59-len(label))).replace('0','-'))
        self.GetParent().source.ReplaceSelection(separator)
        self.Close()


def wxColour2html(c):
    return ('#%s%s%s'%tuple(map(lambda x:sm.zfill(hex(x)[2:],2),c.Get()))).upper()
