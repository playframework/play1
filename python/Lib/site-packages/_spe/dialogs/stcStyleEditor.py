#-----------------------------------------------------------------------------
# Name:        STCStyleEditor.py
# Purpose:     Style editor for the wx.StyledTextCtrl adapted for SPE
#
# Author:      Riaan Booysen, Vlad, Stani
#
# Created:     2001/08/20
# RCS-ID:      $Id: STCStyleEditor.py,v 1.5 2004/06/26 18:45:02 RD Exp $
# Copyright:   (c) 2001 - 2002 Riaan Booysen
# Licence:     wx.Windows license
#-----------------------------------------------------------------------------
#Boa:Dialog:STCStyleEditDlg

import os, string, pprint, copy
import ConfigParser

import wx
from wx.lib.anchors import LayoutAnchors
import wx.stc as wx_stc

customStyle = 'stc.style.Custom'

settingsIdNames = {-1: 'Selection', -2: 'Caret', -3: 'Edge'}

commonPropDefs = {}

styleCategoryDescriptions = {
 '----Language----': 'Styles spesific to the language',
 '----Standard----': 'Styles shared by all languages',
 '----Settings----': 'Properties set by STC methods',
 '----Common----': 'User definable values that can be shared between languages'}

[ID_STCSTYLEEDITDLG, ID_STCSTYLEEDITDLGADDCOMMONITEMBTN,
 ID_STCSTYLEEDITDLGBGCOLBTN, ID_STCSTYLEEDITDLGBGCOLCB,
 ID_STCSTYLEEDITDLGBGCOLDEFCB, ID_STCSTYLEEDITDLGBGCOLOKBTN,
 ID_STCSTYLEEDITDLGCANCELBTN, ID_STCSTYLEEDITDLGCONTEXTHELPBUTTON1,
 ID_STCSTYLEEDITDLGELEMENTLB, ID_STCSTYLEEDITDLGFACECB,
 ID_STCSTYLEEDITDLGFACEDEFCB, ID_STCSTYLEEDITDLGFACEOKBTN,
 ID_STCSTYLEEDITDLGFGCOLBTN, ID_STCSTYLEEDITDLGFGCOLCB,
 ID_STCSTYLEEDITDLGFGCOLDEFCB, ID_STCSTYLEEDITDLGFGCOLOKBTN,
 ID_STCSTYLEEDITDLGFIXEDWIDTHCHK, ID_STCSTYLEEDITDLGOKBTN,
 ID_STCSTYLEEDITDLGPANEL1, ID_STCSTYLEEDITDLGPANEL2,
 ID_STCSTYLEEDITDLGPANEL3, ID_STCSTYLEEDITDLGPANEL4,
 ID_STCSTYLEEDITDLGREMOVECOMMONITEMBTN, ID_STCSTYLEEDITDLGSIZECB,
 ID_STCSTYLEEDITDLGSIZEOKBTN, ID_STCSTYLEEDITDLGSPEEDSETTINGCH,
 ID_STCSTYLEEDITDLGSTATICBOX1, ID_STCSTYLEEDITDLGSTATICBOX2,
 ID_STCSTYLEEDITDLGSTATICLINE1, ID_STCSTYLEEDITDLGSTATICTEXT2,
 ID_STCSTYLEEDITDLGSTATICTEXT3, ID_STCSTYLEEDITDLGSTATICTEXT4,
 ID_STCSTYLEEDITDLGSTATICTEXT6, ID_STCSTYLEEDITDLGSTATICTEXT7,
 ID_STCSTYLEEDITDLGSTATICTEXT8, ID_STCSTYLEEDITDLGSTATICTEXT9,
 ID_STCSTYLEEDITDLGSTC, ID_STCSTYLEEDITDLGSTYLEDEFST,
 ID_STCSTYLEEDITDLGTABOLDCB, ID_STCSTYLEEDITDLGTABOLDDEFCB,
 ID_STCSTYLEEDITDLGTAEOLFILLEDCB, ID_STCSTYLEEDITDLGTAEOLFILLEDDEFCB,
 ID_STCSTYLEEDITDLGTAITALICCB, ID_STCSTYLEEDITDLGTAITALICDEFCB,
 ID_STCSTYLEEDITDLGTASIZEDEFCB, ID_STCSTYLEEDITDLGTAUNDERLINEDCB,
 ID_STCSTYLEEDITDLGTAUNDERLINEDDEFCB,
] = map(lambda _init_ctrls: wx.NewId(), range(47))

class STCStyleEditDlg(wx.Dialog):
    """ Style editor for the wx.StyledTextCtrl """
    _custom_classes = {'wx.Window' : ['wx.StyledTextCtrl']}
    def _init_utils(self):
        # generated method, don't edit
        pass

    def _init_ctrls(self, prnt):
        # generated method, don't edit
        wx.Dialog.__init__(self, id=ID_STCSTYLEEDITDLG, name='STCStyleEditDlg',
              parent=prnt, pos=wx.Point(583, 291), size=wx.Size(459, 482),
              style=wx.WANTS_CHARS | wx.DEFAULT_DIALOG_STYLE | wx.RESIZE_BORDER,
              title=self.stc_title)
        self._init_utils()
        self.SetClientSize(wx.Size(451, 455))
        self.SetAutoLayout(True)
        self.SetSizeHints(425, 400, -1, -1)
        self.Center(wx.BOTH)
        wx.EVT_SIZE(self, self.OnStcstyleeditdlgSize)

        self.speedsettingCh = wx.Choice(choices=[],
              id=ID_STCSTYLEEDITDLGSPEEDSETTINGCH, name='speedsettingCh',
              parent=self, pos=wx.Point(96, 28), size=wx.Size(346, 21), style=0,
              validator=wx.DefaultValidator)
        self.speedsettingCh.SetConstraints(LayoutAnchors(self.speedsettingCh,
              True, True, True, False))
        self.speedsettingCh.SetHelpText('The speed setting allows you to revert to one of the predefined style sets. This will overwrite your current settings when tha dialog is posted.')
        wx.EVT_CHOICE(self.speedsettingCh, ID_STCSTYLEEDITDLGSPEEDSETTINGCH,
              self.OnSpeedsettingchChoice)

        self.elementLb = wx.ListBox(choices=[], id=ID_STCSTYLEEDITDLGELEMENTLB,
              name='elementLb', parent=self, pos=wx.Point(8, 72),
              size=wx.Size(160, 128), style=0, validator=wx.DefaultValidator)
        self.elementLb.SetConstraints(LayoutAnchors(self.elementLb, True, True,
              True, False))
        self.elementLb.SetHelpText('Select a style here to edit it. Common definitions can be added and maintained here.  A common definition is a property that can be shared between styles and special cased per platform.')
        wx.EVT_LISTBOX(self.elementLb, ID_STCSTYLEEDITDLGELEMENTLB,
              self.OnElementlbListbox)

        self.styleDefST = wx.StaticText(id=ID_STCSTYLEEDITDLGSTYLEDEFST,
              label='(nothing selected)', name='styleDefST', parent=self,
              pos=wx.Point(96, 8), size=wx.Size(366, 16),
              style=wx.ST_NO_AUTORESIZE)
        self.styleDefST.SetFont(wx.Font(self.style_font_size, wx.SWISS, wx.NORMAL,
              wx.BOLD, False, ''))
        self.styleDefST.SetConstraints(LayoutAnchors(self.styleDefST, True,
              True, True, False))

        self.staticLine1 = wx.StaticLine(id=ID_STCSTYLEEDITDLGSTATICLINE1,
              name='staticLine1', parent=self, pos=wx.Point(48, 62),
              size=wx.Size(120, 2), style=wx.LI_HORIZONTAL)
        self.staticLine1.SetConstraints(LayoutAnchors(self.staticLine1, True,
              True, True, False))

        self.staticText6 = wx.StaticText(id=ID_STCSTYLEEDITDLGSTATICTEXT6,
              label='Style', name='staticText6', parent=self, pos=wx.Point(8,
              56), size=wx.Size(40, 13), style=0)

        self.staticText8 = wx.StaticText(id=ID_STCSTYLEEDITDLGSTATICTEXT8,
              label='Style def:', name='staticText8', parent=self,
              pos=wx.Point(8, 8), size=wx.Size(88, 13), style=0)

        self.staticText9 = wx.StaticText(id=ID_STCSTYLEEDITDLGSTATICTEXT9,
              label='SpeedSetting:', name='staticText9', parent=self,
              pos=wx.Point(8, 32), size=wx.Size(88, 13), style=0)

        self.panel3 = wx.Panel(id=ID_STCSTYLEEDITDLGPANEL3, name='panel3',
              parent=self, pos=wx.Point(176, 56), size=wx.Size(146, 104),
              style=wx.TAB_TRAVERSAL)
        self.panel3.SetConstraints(LayoutAnchors(self.panel3, False, True, True,
              False))

        self.panel4 = wx.Panel(id=ID_STCSTYLEEDITDLGPANEL4, name='panel4',
              parent=self, pos=wx.Point(330, 56), size=wx.Size(114, 104),
              style=wx.TAB_TRAVERSAL)
        self.panel4.SetConstraints(LayoutAnchors(self.panel4, False, True, True,
              False))

        self.panel1 = wx.Panel(id=ID_STCSTYLEEDITDLGPANEL1, name='panel1',
              parent=self, pos=wx.Point(176, 161), size=wx.Size(143, 40),
              style=wx.TAB_TRAVERSAL)
        self.panel1.SetConstraints(LayoutAnchors(self.panel1, False, True, True,
              False))

        self.panel2 = wx.Panel(id=ID_STCSTYLEEDITDLGPANEL2, name='panel2',
              parent=self, pos=wx.Point(330, 162), size=wx.Size(112, 40),
              style=wx.TAB_TRAVERSAL)
        self.panel2.SetConstraints(LayoutAnchors(self.panel2, False, True, True,
              False))

        self.stc = wx_stc.StyledTextCtrl(id=ID_STCSTYLEEDITDLGSTC, name='stc',
              parent=self, pos=wx.Point(8, 208), size=wx.Size(435, 207),
              style=wx.SUNKEN_BORDER)
        self.stc.SetConstraints(LayoutAnchors(self.stc, True, True, True, True))
        self.stc.SetHelpText('The style preview window. Click or move the cursor over a spesific style to select the style for editing in the editors above.')
        wx.EVT_LEFT_UP(self.stc, self.OnUpdateUI)
        wx.EVT_KEY_UP(self.stc, self.OnUpdateUI)

        self.contextHelpButton1 = wx.ContextHelpButton(parent=self,
              pos=wx.Point(8, 423), size=wx.Size(24, 24), style=wx.BU_AUTODRAW)
        self.contextHelpButton1.SetConstraints(LayoutAnchors(self.contextHelpButton1,
              True, False, False, True))

        self.okBtn = wx.Button(id=ID_STCSTYLEEDITDLGOKBTN, label='OK',
              name='okBtn', parent=self, pos=wx.Point(282, 423), size=wx.Size(75,
              23), style=0)
        self.okBtn.SetConstraints(LayoutAnchors(self.okBtn, False, False, True,
              True))
        self.okBtn.SetToolTipString('Save changes to the config file')
        wx.EVT_BUTTON(self.okBtn, ID_STCSTYLEEDITDLGOKBTN, self.OnOkbtnButton)

        self.cancelBtn = wx.Button(id=ID_STCSTYLEEDITDLGCANCELBTN,
              label='Cancel', name='cancelBtn', parent=self, pos=wx.Point(366,
              423), size=wx.Size(75, 23), style=0)
        self.cancelBtn.SetConstraints(LayoutAnchors(self.cancelBtn, False,
              False, True, True))
        self.cancelBtn.SetToolTipString('Close dialog without saving changes')
        wx.EVT_BUTTON(self.cancelBtn, ID_STCSTYLEEDITDLGCANCELBTN,
              self.OnCancelbtnButton)

        self.staticText4 = wx.StaticText(id=ID_STCSTYLEEDITDLGSTATICTEXT4,
              label='Face:', name='staticText4', parent=self.panel1,
              pos=wx.Point(0, 0), size=wx.Size(48, 13), style=0)

        self.fixedWidthChk = wx.CheckBox(id=ID_STCSTYLEEDITDLGFIXEDWIDTHCHK,
              label='', name='fixedWidthChk', parent=self.panel1, pos=wx.Point(0,
              23), size=wx.Size(13, 19), style=0)
        self.fixedWidthChk.SetToolTipString('Check this for Fixed Width fonts')
        wx.EVT_CHECKBOX(self.fixedWidthChk, ID_STCSTYLEEDITDLGFIXEDWIDTHCHK,
              self.OnFixedwidthchkCheckbox)

        self.faceCb = wx.ComboBox(choices=[], id=ID_STCSTYLEEDITDLGFACECB,
              name='faceCb', parent=self.panel1, pos=wx.Point(17, 18),
              size=wx.Size(105, 21), style=0, validator=wx.DefaultValidator,
              value='')

        self.staticText7 = wx.StaticText(id=ID_STCSTYLEEDITDLGSTATICTEXT7,
              label='Size:', name='staticText7', parent=self.panel2,
              pos=wx.Point(0, 0), size=wx.Size(40, 13), style=0)

        self.sizeCb = wx.ComboBox(choices=[], id=ID_STCSTYLEEDITDLGSIZECB,
              name='sizeCb', parent=self.panel2, pos=wx.Point(0, 17),
              size=wx.Size(91, 21), style=0, validator=wx.DefaultValidator,
              value='')

        self.sizeOkBtn = wx.Button(id=ID_STCSTYLEEDITDLGSIZEOKBTN, label='ok',
              name='sizeOkBtn', parent=self.panel2, pos=wx.Point(90, 17),
              size=wx.Size(21, 21), style=0)

        self.faceOkBtn = wx.Button(id=ID_STCSTYLEEDITDLGFACEOKBTN, label='ok',
              name='faceOkBtn', parent=self.panel1, pos=wx.Point(122, 18),
              size=wx.Size(21, 21), style=0)

        self.fgColBtn = wx.Button(id=ID_STCSTYLEEDITDLGFGCOLBTN,
              label='Foreground', name='fgColBtn', parent=self.panel3,
              pos=wx.Point(8, 16), size=wx.Size(72, 16), style=0)
        wx.EVT_BUTTON(self.fgColBtn, ID_STCSTYLEEDITDLGFGCOLBTN,
              self.OnFgcolbtnButton)

        self.fgColCb = wx.ComboBox(choices=[], id=ID_STCSTYLEEDITDLGFGCOLCB,
              name='fgColCb', parent=self.panel3, pos=wx.Point(8, 32),
              size=wx.Size(89, 21), style=0, validator=wx.DefaultValidator,
              value='')

        self.fgColOkBtn = wx.Button(id=ID_STCSTYLEEDITDLGFGCOLOKBTN,
              label='ok', name='fgColOkBtn', parent=self.panel3, pos=wx.Point(96,
              32), size=wx.Size(21, 21), style=0)

        self.staticText3 = wx.StaticText(id=ID_STCSTYLEEDITDLGSTATICTEXT3,
              label='default', name='staticText3', parent=self.panel3,
              pos=wx.Point(100, 16), size=wx.Size(37, 16), style=0)

        self.fgColDefCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGFGCOLDEFCB,
              label='checkBox1', name='fgColDefCb', parent=self.panel3,
              pos=wx.Point(120, 31), size=wx.Size(16, 16), style=0)

        self.bgColBtn = wx.Button(id=ID_STCSTYLEEDITDLGBGCOLBTN,
              label='Background', name='bgColBtn', parent=self.panel3,
              pos=wx.Point(8, 56), size=wx.Size(72, 16), style=0)
        wx.EVT_BUTTON(self.bgColBtn, ID_STCSTYLEEDITDLGBGCOLBTN,
              self.OnBgcolbtnButton)

        self.bgColCb = wx.ComboBox(choices=[], id=ID_STCSTYLEEDITDLGBGCOLCB,
              name='bgColCb', parent=self.panel3, pos=wx.Point(8, 72),
              size=wx.Size(89, 21), style=0, validator=wx.DefaultValidator,
              value='')

        self.bgColOkBtn = wx.Button(id=ID_STCSTYLEEDITDLGBGCOLOKBTN,
              label='ok', name='bgColOkBtn', parent=self.panel3, pos=wx.Point(96,
              72), size=wx.Size(21, 21), style=0)

        self.staticBox2 = wx.StaticBox(id=ID_STCSTYLEEDITDLGSTATICBOX2,
              label='Text attributes', name='staticBox2', parent=self.panel4,
              pos=wx.Point(0, 0), size=wx.Size(112, 99), style=0)
        self.staticBox2.SetConstraints(LayoutAnchors(self.staticBox2, False,
              True, True, False))
        self.staticBox2.SetHelpText('Text attribute flags.')

        self.staticText2 = wx.StaticText(id=ID_STCSTYLEEDITDLGSTATICTEXT2,
              label='default', name='staticText2', parent=self.panel4,
              pos=wx.Point(68, 11), size=wx.Size(37, 16), style=0)

        self.taBoldDefCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGTABOLDDEFCB,
              label='checkBox1', name='taBoldDefCb', parent=self.panel4,
              pos=wx.Point(88, 27), size=wx.Size(16, 16), style=0)

        self.taItalicDefCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGTAITALICDEFCB,
              label='checkBox1', name='taItalicDefCb', parent=self.panel4,
              pos=wx.Point(88, 43), size=wx.Size(16, 16), style=0)

        self.taUnderlinedDefCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGTAUNDERLINEDDEFCB,
              label='checkBox1', name='taUnderlinedDefCb', parent=self.panel4,
              pos=wx.Point(88, 59), size=wx.Size(16, 16), style=0)

        self.taEOLfilledDefCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGTAEOLFILLEDDEFCB,
              label='checkBox1', name='taEOLfilledDefCb', parent=self.panel4,
              pos=wx.Point(88, 75), size=wx.Size(16, 16), style=0)

        self.taEOLfilledCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGTAEOLFILLEDCB,
              label='EOL filled', name='taEOLfilledCb', parent=self.panel4,
              pos=wx.Point(8, 75), size=wx.Size(72, 16), style=0)
        wx.EVT_CHECKBOX(self.taEOLfilledCb, ID_STCSTYLEEDITDLGTAEOLFILLEDCB,
              self.OnTaeoffilledcbCheckbox)

        self.taUnderlinedCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGTAUNDERLINEDCB,
              label='Underlined', name='taUnderlinedCb', parent=self.panel4,
              pos=wx.Point(8, 59), size=wx.Size(72, 16), style=0)
        wx.EVT_CHECKBOX(self.taUnderlinedCb, ID_STCSTYLEEDITDLGTAUNDERLINEDCB,
              self.OnTaunderlinedcbCheckbox)

        self.taItalicCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGTAITALICCB,
              label='Italic', name='taItalicCb', parent=self.panel4,
              pos=wx.Point(8, 43), size=wx.Size(72, 16), style=0)
        wx.EVT_CHECKBOX(self.taItalicCb, ID_STCSTYLEEDITDLGTAITALICCB,
              self.OnTaitaliccbCheckbox)

        self.taBoldCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGTABOLDCB,
              label='Bold', name='taBoldCb', parent=self.panel4, pos=wx.Point(8,
              27), size=wx.Size(72, 16), style=0)
        wx.EVT_CHECKBOX(self.taBoldCb, ID_STCSTYLEEDITDLGTABOLDCB,
              self.OnTaboldcbCheckbox)

        self.bgColDefCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGBGCOLDEFCB,
              label='checkBox1', name='bgColDefCb', parent=self.panel3,
              pos=wx.Point(120, 71), size=wx.Size(16, 16), style=0)

        self.staticBox1 = wx.StaticBox(id=ID_STCSTYLEEDITDLGSTATICBOX1,
              label='Colour', name='staticBox1', parent=self.panel3,
              pos=wx.Point(0, 0), size=wx.Size(142, 99), style=0)
        self.staticBox1.SetConstraints(LayoutAnchors(self.staticBox1, False,
              True, True, False))

        self.faceDefCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGFACEDEFCB,
              label='checkBox1', name='faceDefCb', parent=self.panel1,
              pos=wx.Point(120, 0), size=wx.Size(16, 16), style=0)

        self.taSizeDefCb = wx.CheckBox(id=ID_STCSTYLEEDITDLGTASIZEDEFCB,
              label='checkBox1', name='taSizeDefCb', parent=self.panel2,
              pos=wx.Point(88, 0), size=wx.Size(16, 16), style=0)

    def __init__(self, parent, langTitle, configFile, STC):
        global commonPropDefs
        commonPropDefs = {'fore': '#888888',
                          'size': 10,
                          'face': wx.SystemSettings_GetFont(wx.SYS_DEFAULT_GUI_FONT).GetFaceName()}

        self.stc_title = 'wx.StyledTextCtrl Style Editor'
        self.stc_title = 'wx.StyledTextCtrl Style Editor - %s' % langTitle
        if wx.Platform == '__WXMSW__':
            self.style_font_size = 10
        elif wx.Platform == '__WXMAC__':
            self.style_font_size = 14
            commonPropDefs['size']=14
        else:
            self.style_font_size = 10
        self._init_ctrls(parent)
        self.configFile = configFile
        self.style = ''
        self.styleNum = 0
        self.names = []
        self.values = {}
        self.STC = STC
        self._blockUpdate = False


        for combo, okBtn, evtRet, evtCB, evtRDC in (
         (self.fgColCb, self.fgColOkBtn, self.OnfgColRet, self.OnfgColCombobox, self.OnGotoCommonDef),
         (self.bgColCb, self.bgColOkBtn, self.OnbgColRet, self.OnbgColCombobox, self.OnGotoCommonDef),
         (self.faceCb, self.faceOkBtn, self.OnfaceRet, self.OnfaceCombobox, self.OnGotoCommonDef),
         (self.sizeCb, self.sizeOkBtn, self.OnsizeRet, self.OnsizeCombobox, self.OnGotoCommonDef)):
            self.bindComboEvts(combo, okBtn, evtRet, evtCB, evtRDC)

        (self.config, self.commonDefs, self.styleIdNames, self.styles,
         self.styleGroupNames, self.predefStyleGroups,
         self.displaySrc, self.keywords, self.braceInfo) = \
              initFromConfig(configFile)

        self.currSpeedSetting = customStyle
        for grp in [self.currSpeedSetting]+self.styleGroupNames:
            self.speedsettingCh.Append(grp)
        self.speedsettingCh.SetSelection(0)

        margin = 0
        self.stc.SetMarginType(margin, wx_stc.STC_MARGIN_NUMBER)
        self.stc.SetMarginWidth(margin, 25)
        self.stc.SetMarginSensitive(margin, True)
        wx_stc.EVT_STC_MARGINCLICK(self.stc, ID_STCSTYLEEDITDLGSTC, self.OnMarginClick)

        self.stc.SetUseTabs(False)
        self.stc.SetTabWidth(4)
        self.stc.SetIndentationGuides(True)
        self.stc.SetEdgeMode(wx_stc.STC_EDGE_BACKGROUND)
        self.stc.SetEdgeColumn(44)

        self.setStyles()

        self.populateStyleSelector()

        self.defNames, self.defValues = parseProp(\
              self.styleDict.get(wx_stc.STC_STYLE_DEFAULT, ''))
        self.stc.SetText(self.displaySrc)
        self.stc.EmptyUndoBuffer()
        self.stc.SetCurrentPos(self.stc.GetTextLength())
        self.stc.SetAnchor(self.stc.GetTextLength())

        self.populateCombosWithCommonDefs()

        # Logical grouping of controls and the property they edit
        self.allCtrls = [((self.fgColBtn, self.fgColCb, self.fgColOkBtn), self.fgColDefCb,
                             'fore', ID_STCSTYLEEDITDLGFGCOLDEFCB),
                         ((self.bgColBtn, self.bgColCb, self.bgColOkBtn), self.bgColDefCb,
                             'back', ID_STCSTYLEEDITDLGBGCOLDEFCB),
                         (self.taBoldCb, self.taBoldDefCb,
                             'bold', ID_STCSTYLEEDITDLGTABOLDDEFCB),
                         (self.taItalicCb, self.taItalicDefCb,
                             'italic', ID_STCSTYLEEDITDLGTAITALICDEFCB),
                         (self.taUnderlinedCb, self.taUnderlinedDefCb,
                             'underline', ID_STCSTYLEEDITDLGTAUNDERLINEDDEFCB),
                         (self.taEOLfilledCb, self.taEOLfilledDefCb,
                             'eolfilled', ID_STCSTYLEEDITDLGTAEOLFILLEDDEFCB),
                         ((self.sizeCb, self.sizeOkBtn), self.taSizeDefCb,
                             'size', ID_STCSTYLEEDITDLGTASIZEDEFCB),
                         ((self.faceCb, self.faceOkBtn, self.fixedWidthChk), self.faceDefCb,
                             'face', ID_STCSTYLEEDITDLGFACEDEFCB)]

        self.clearCtrls(disableDefs=True)
        # centralised default checkbox event handler
        self.chbIdMap = {}
        for ctrl, chb, prop, wid in self.allCtrls:
            self.chbIdMap[wid] = ctrl, chb, prop, wid
            wx.EVT_CHECKBOX(chb, wid, self.OnDefaultCheckBox)
            chb.SetToolTipString('Toggle defaults')

        self.Center(wx.BOTH)

#---Property methods------------------------------------------------------------
    def getCtrlForProp(self, findprop):
        for ctrl, chb, prop, wid in self.allCtrls:
            if findprop == prop:
                return ctrl, chb
        raise Exception('PropNotFound', findprop)

    def editProp(self, on, prop, val=''):
        oldstyle = self.rememberStyles()
        if on:
            if not self.names.count(prop):
                self.names.append(prop)
            self.values[prop] = val
        else:
            try: self.names.remove(prop)
            except ValueError: pass
            try: del self.values[prop]
            except KeyError: pass

        try:
            self.updateStyle()
            return True
        except KeyError, errkey:
            wx.LogError('Name not found in Common definition, '\
                'please enter valid reference. (%s)'%errkey)
            self.restoreStyles(oldstyle)
            return False

#---Control population methods--------------------------------------------------
    def setStyles(self):
        if self._blockUpdate: return
        self.styles, self.styleDict, self.styleNumIdxMap = \
              setSTCStyles(self.stc, self.styles, self.styleIdNames,
              self.commonDefs, self.keywords)

    def updateStyle(self):
        # called after a control edited self.names, self.values
        # Special case for saving common defs settings
        if self.styleNum == 'common':
            strVal = self.style[2] = self.values.values()[0]
            if self.style[1] == 'size': self.style[2] = int(strVal)

            self.commonDefs[self.style[0]] = self.style[2]
            self.styleDefST.SetLabel(strVal)
        else:
            self.style = writePropVal(self.names, self.values)
            styleDecl = writeProp(self.styleNum, self.style)
            self.styles[self.styleNumIdxMap[self.styleNum]] = styleDecl
            self.styleDefST.SetLabel(self.style)
        self.setStyles()

    def findInStyles(self, txt, styles):
        for style in styles:
            if string.find(style, txt) != -1:
                return True
        return False

    def rememberStyles(self):
        return self.names[:], copy.copy(self.values)

    def restoreStyles(self, style):
        self.names, self.values = style
        self.updateStyle()

    def clearCtrls(self, isDefault=False, disableDefs=False):
        self._blockUpdate = True
        try:
            for ctrl, chb, prop, wid in self.allCtrls:
                if prop in ('fore', 'back'):
                    cbtn, txt, btn = ctrl
                    cbtn.SetBackgroundColour(\
                          wx.SystemSettings_GetColour(wx.SYS_COLOUR_BTNFACE))
                    cbtn.SetForegroundColour(wx.Colour(255, 255, 255))
                    cbtn.Enable(isDefault)
                    txt.SetValue('')
                    txt.Enable(isDefault)
                    btn.Enable(isDefault)
                elif prop == 'size':
                    cmb, btn = ctrl
                    cmb.SetValue('')
                    cmb.Enable(isDefault)
                    btn.Enable(isDefault)
                elif prop == 'face':
                    cmb, btn, chk = ctrl
                    cmb.SetValue('')
                    cmb.Enable(isDefault)
                    btn.Enable(isDefault)
                    chk.Enable(isDefault)
                    chk.SetValue(False)
                elif prop in ('bold', 'italic', 'underline', 'eolfilled'):
                    ctrl.SetValue(False)
                    ctrl.Enable(isDefault)

                chb.Enable(not isDefault and not disableDefs)
                chb.SetValue(True)
        finally:
            self._blockUpdate = False

    def populateProp(self, items, default, forceDisable=False):
        self._blockUpdate = True
        try:
            for name, val in items:
                if name:
                    ctrl, chb = self.getCtrlForProp(name)

                    if name in ('fore', 'back'):
                        cbtn, txt, btn = ctrl
                        repval = val%self.commonDefs
                        cbtn.SetBackgroundColour(strToCol(repval))
                        cbtn.SetForegroundColour(wx.Colour(0, 0, 0))
                        cbtn.Enable(not forceDisable)
                        txt.SetValue(val)
                        txt.Enable(not forceDisable)
                        btn.Enable(not forceDisable)
                        chb.SetValue(default)
                    elif name  == 'size':
                        cmb, btn = ctrl
                        cmb.SetValue(val)
                        cmb.Enable(not forceDisable)
                        btn.Enable(not forceDisable)
                        chb.SetValue(default)
                    elif name  == 'face':
                        cmb, btn, chk = ctrl
                        cmb.SetValue(val)
                        cmb.Enable(not forceDisable)
                        btn.Enable(not forceDisable)
                        chk.Enable(not forceDisable)
                        chb.SetValue(default)
                    elif name in ('bold', 'italic', 'underline', 'eolfilled'):
                        ctrl.Enable(not forceDisable)
                        ctrl.SetValue(True)
                        chb.SetValue(default)
        finally:
            self._blockUpdate = False

    def valIsCommonDef(self, val):
        return len(val) >= 5 and val[:2] == '%('

    def populateCtrls(self):
        self.clearCtrls(self.styleNum == wx_stc.STC_STYLE_DEFAULT,
            disableDefs=self.styleNum < 0)

        # handle colour controls for settings
        if self.styleNum < 0:
            self.fgColDefCb.Enable(True)
            if self.styleNum == -1:
                self.bgColDefCb.Enable(True)

        # populate with default style
        self.populateProp(self.defValues.items(), True,
            self.styleNum != wx_stc.STC_STYLE_DEFAULT)
        # override with current settings
        self.populateProp(self.values.items(), False)

    def getCommonDefPropType(self, commonDefName):
        val = self.commonDefs[commonDefName]
        if type(val) == type(0): return 'size'
        if len(val) == 7 and val[0] == '#': return 'fore'
        return 'face'

    def bindComboEvts(self, combo, btn, btnEvtMeth, comboEvtMeth, rdclickEvtMeth):
        wx.EVT_COMBOBOX(combo, combo.GetId(), comboEvtMeth)
        wx.EVT_BUTTON(btn, btn.GetId(), btnEvtMeth)
        wx.EVT_RIGHT_DCLICK(combo, rdclickEvtMeth)
        combo.SetToolTipString('Select from list or click "ok" button on the right to change a manual entry, right double-click \n'\
            'the drop down button to select Common definition in the Style Editor (if applicable)')
        btn.SetToolTipString('Accept value')

    def populateCombosWithCommonDefs(self, fixedWidthOnly=None):
        self._blockUpdate = True
        try:
            commonDefs = {'fore': [], 'face': [], 'size': []}

            if self.elementLb.GetSelection() < self.commonDefsStartIdx:
                for common in self.commonDefs.keys():
                    prop = self.getCommonDefPropType(common)
                    commonDefs[prop].append('%%(%s)%s'%(common,
                                                       prop=='size' and 'd' or 's'))

            # Colours
            currFg, currBg = self.fgColCb.GetValue(), self.bgColCb.GetValue()
            self.fgColCb.Clear(); self.bgColCb.Clear()
            for colCommonDef in commonDefs['fore']:
                self.fgColCb.Append(colCommonDef)
                self.bgColCb.Append(colCommonDef)
            self.fgColCb.SetValue(currFg); self.bgColCb.SetValue(currBg)

            # Font
            if fixedWidthOnly is None:
                fixedWidthOnly = self.fixedWidthChk.GetValue()
            fontEnum = wx.FontEnumerator()
            fontEnum.EnumerateFacenames(fixedWidthOnly=fixedWidthOnly)
            fontNameList = fontEnum.GetFacenames()

            currFace = self.faceCb.GetValue()
            self.faceCb.Clear()
            for colCommonDef in ['']+fontNameList+commonDefs['face']:
                self.faceCb.Append(colCommonDef)
            self.faceCb.SetValue(currFace)

            # Size (XXX add std font sizes)
            currSize = self.sizeCb.GetValue()
            self.sizeCb.Clear()
            for colCommonDef in commonDefs['size']:
                self.sizeCb.Append(colCommonDef)
            self.sizeCb.SetValue(currSize)
        finally:
            self._blockUpdate = False

    def populateStyleSelector(self):
        numStyles = self.styleIdNames.items()
        numStyles.sort()
        self.styleNumLookup = {}
        stdStart = -1
        stdOffset = 0
        extrOffset = 0
        # add styles
        for num, name in numStyles:
            if num == wx_stc.STC_STYLE_DEFAULT:
                self.elementLb.InsertItems([name, '----Language----'], 0)
                self.elementLb.Append('----Standard----')
                stdStart = stdPos = self.elementLb.GetCount()
            else:
                # std styles
                if num >= 33 and num < 40:
                    self.elementLb.InsertItems([name], stdStart + stdOffset)
                    stdOffset = stdOffset + 1
                # extra styles
                elif num >= 40:
                    self.elementLb.InsertItems([name], stdStart + extrOffset -1)
                    extrOffset = extrOffset + 1
                # normal lang styles
                else:
                    self.elementLb.Append(name)
            self.styleNumLookup[name] = num

        # add settings
        self.elementLb.Append('----Settings----')
        settings = settingsIdNames.items()
        settings.sort();settings.reverse()
        for num, name in settings:
            self.elementLb.Append(name)
            self.styleNumLookup[name] = num

        # add definitions
        self.elementLb.Append('----Common----')
        self.commonDefsStartIdx = self.elementLb.GetCount()
        for common in self.commonDefs.keys():
            tpe = type(self.commonDefs[common])
            self.elementLb.Append('%('+common+')'+(tpe is type('') and 's' or 'd'))
            self.styleNumLookup[common] = num

#---Colour methods--------------------------------------------------------------
    def getColourDlg(self, colour, title=''):
        data = wx.ColourData()
        data.SetColour(colour)
        data.SetChooseFull(True)
        dlg = wx.ColourDialog(self, data)
        try:
            dlg.SetTitle(title)
            if dlg.ShowModal() == wx.ID_OK:
                data = dlg.GetColourData()
                return data.GetColour()
        finally:
            dlg.Destroy()
        return None

    colDlgTitles = {'fore': 'Foreground', 'back': 'Background'}
    def editColProp(self, colBtn, colCb, prop):
        col = self.getColourDlg(colBtn.GetBackgroundColour(),
              self.colDlgTitles[prop]+ ' colour')
        if col:
            colBtn.SetForegroundColour(wx.Colour(0, 0, 0))
            colBtn.SetBackgroundColour(col)
            colStr = colToStr(col)
            colCb.SetValue(colStr)
            self.editProp(True, prop, colStr)

    def OnFgcolbtnButton(self, event):
        self.editColProp(self.fgColBtn, self.fgColCb, 'fore')

    def OnBgcolbtnButton(self, event):
        self.editColProp(self.bgColBtn, self.bgColCb, 'back')

    def editColTCProp(self, colCb, colBtn, prop, val=None):
        if val is None:
            colStr = colCb.GetValue()
        else:
            colStr = val
        if colStr:
            col = strToCol(colStr%self.commonDefs)
        if self.editProp(colStr!='', prop, colStr):
            if colStr:
                colBtn.SetForegroundColour(wx.Colour(0, 0, 0))
                colBtn.SetBackgroundColour(col)
            else:
                colBtn.SetForegroundColour(wx.Colour(255, 255, 255))
                colBtn.SetBackgroundColour(\
                      wx.SystemSettings_GetColour(wx.SYS_COLOUR_BTNFACE))

    def OnfgColRet(self, event):
        try: self.editColTCProp(self.fgColCb, self.fgColBtn, 'fore')
        except AssertionError: wx.LogError('Not a valid colour value')

    def OnfgColCombobox(self, event):
        if self._blockUpdate: return
        try: self.editColTCProp(self.fgColCb, self.fgColBtn, 'fore', event.GetString())
        except AssertionError: wx.LogError('Not a valid colour value')

    def OnbgColRet(self, event):
        try: self.editColTCProp(self.bgColCb, self.bgColBtn, 'back')
        except AssertionError: wx.LogError('Not a valid colour value')

    def OnbgColCombobox(self, event):
        if self._blockUpdate: return
        try: self.editColTCProp(self.bgColCb, self.bgColBtn, 'back', event.GetString())
        except AssertionError: wx.LogError('Not a valid colour value')

#---Text attribute events-------------------------------------------------------
    def OnTaeoffilledcbCheckbox(self, event):
        self.editProp(event.IsChecked(), 'eolfilled')

    def OnTaitaliccbCheckbox(self, event):
        self.editProp(event.IsChecked(), 'italic')

    def OnTaboldcbCheckbox(self, event):
        self.editProp(event.IsChecked(), 'bold')

    def OnTaunderlinedcbCheckbox(self, event):
        self.editProp(event.IsChecked(), 'underline')

    def OnGotoCommonDef(self, event):
        val = event.GetEventObject().GetValue()
        if self.valIsCommonDef(val):
            idx = self.elementLb.FindString(val)
            if idx != -1:
                self.elementLb.SetSelection(idx, True)
                self.OnElementlbListbox(None)

    def OnfaceRet(self, event):
        self.setFace(self.faceCb.GetValue())

    def OnfaceCombobox(self, event):
        if self._blockUpdate: return
        self.setFace(event.GetString())

    def setFace(self, val):
        try: val%self.commonDefs
        except KeyError: wx.LogError('Invalid common definition')
        else: self.editProp(val!='', 'face', val)

    def OnsizeRet(self, event):
        self.setSize(self.sizeCb.GetValue())

    def OnsizeCombobox(self, event):
        if self._blockUpdate: return
        self.setSize(event.GetString())

    def setSize(self, val):
        try: int(val%self.commonDefs)
        except ValueError: wx.LogError('Not a valid integer size value')
        except KeyError: wx.LogError('Invalid common definition')
        else: self.editProp(val!='', 'size', val)

#---Main GUI events-------------------------------------------------------------
    def OnElementlbListbox(self, event):
        isCommon = self.elementLb.GetSelection() >= self.commonDefsStartIdx

        styleIdent = self.elementLb.GetStringSelection()
        # common definition selected
        if isCommon:
            common = styleIdent[2:-2]
            prop = self.getCommonDefPropType(common)
            self.clearCtrls(disableDefs=True)
            if prop == 'fore':
                self.fgColBtn.Enable(True)
                self.fgColCb.Enable(True)
                self.fgColOkBtn.Enable(True)
            elif prop == 'face':
                self.faceCb.Enable(True)
                self.fixedWidthChk.Enable(True)
                self.faceOkBtn.Enable(True)
            elif prop == 'size':
                self.sizeCb.Enable(True)
                self.sizeOkBtn.Enable(True)

            commonDefVal = str(self.commonDefs[common])
            self.styleDefST.SetLabel(commonDefVal)
            self.populateProp( [(prop, commonDefVal)], True)

            self.styleNum = 'common'
            self.style = [common, prop, commonDefVal]
            self.names, self.values = [prop], {prop: commonDefVal}

        # normal style element selected
        elif len(styleIdent) >=2 and styleIdent[:2] != '--':
            self.styleNum = self.styleNumLookup[styleIdent]
            self.style = self.styleDict[self.styleNum]
            self.names, self.values = parseProp(self.style)
            if self.styleNum == wx_stc.STC_STYLE_DEFAULT:
                self.defNames, self.defValues = \
                      self.names, self.values

            self.checkBraces(self.styleNum)

            self.styleDefST.SetLabel(self.style)

            self.populateCtrls()
        # separator selected
        else:
            self.clearCtrls(disableDefs=True)
            if styleIdent:
                self.styleDefST.SetLabel(styleCategoryDescriptions[styleIdent])

        self.populateCombosWithCommonDefs()

    def OnDefaultCheckBox(self, event):
        if self.chbIdMap.has_key(event.GetId()):
            ctrl, chb, prop, wid = self.chbIdMap[event.GetId()]
            restore = not event.IsChecked()
            if prop in ('fore', 'back'):
                cbtn, cmb, btn = ctrl
                cbtn.Enable(restore)
                cmb.Enable(restore)
                btn.Enable(restore)
                if restore:
                    colStr = cmb.GetValue()
                    #if prop == 'fore': colStr = self.fgColCb.GetValue()
                    #else: colStr = self.bgColCb.GetValue()
                    if colStr: self.editProp(True, prop, colStr)
                else:
                    self.editProp(False, prop)
            elif prop  == 'size':
                cmb, btn = ctrl
                val = cmb.GetValue()
                if val: self.editProp(restore, prop, val)
                cmb.Enable(restore)
                btn.Enable(restore)
            elif prop  == 'face':
                cmb, btn, chk = ctrl
                val = cmb.GetStringSelection()
                if val: self.editProp(restore, prop, val)
                cmb.Enable(restore)
                btn.Enable(restore)
                chk.Enable(restore)
            elif prop in ('bold', 'italic', 'underline', 'eolfilled'):
                ctrl.Enable(restore)
                if ctrl.GetValue(): self.editProp(restore, prop)

    def OnOkbtnButton(self, event):
        # write styles and common defs to the config
        wx.BeginBusyCursor()
        try:
            writeStylesToConfig(self.config, customStyle, self.styles)
            if self.STC is not None:
                setSTCStyles(self.STC, self.styles, self.styleIdNames, 
                    self.commonDefs, self.keywords)
        finally:
            wx.EndBusyCursor()
        self.EndModal(wx.ID_OK)
        if wx.Platform == '__WXMAC__':
            dlg = wx.MessageDialog(self, 'Please restart SPE.',
                                   'Styles reconfigured...',
                                   wx.OK | wx.ICON_INFORMATION
                                   )
            dlg.ShowModal()
            dlg.Destroy()
        return wx.ID_OK


    def OnCancelbtnButton(self, event):
        self.EndModal(wx.ID_CANCEL)

    def OnCommondefsbtnButton(self, event):
        dlg = wx.TextEntryDialog(self, 'Edit common definitions dictionary',
              'Common definitions', pprint.pformat(self.commonDefs),
              style=wx.TE_MULTILINE | wx.OK | wx.CANCEL | wx.CENTRE)
        try:
            if dlg.ShowModal() == wx.ID_OK:
                answer = eval(dlg.GetValue())
                assert type(answer) is type({}), 'Not a valid dictionary'
                oldDefs = self.commonDefs
                self.commonDefs = answer
                try:
                    self.setStyles()
                except KeyError, badkey:
                    wx.LogError(str(badkey)+' not defined but required, \n'\
                          'reverting to previous common definition')
                    self.commonDefs = oldDefs
                    self.setStyles()
                self.populateCombosWithCommonDefs()

        finally:
            dlg.Destroy()

    def OnSpeedsettingchChoice(self, event):
        group = event.GetString()
        if group:
            if self.currSpeedSetting == customStyle:
                self.predefStyleGroups[customStyle] = self.styles
            self.styles = self.predefStyleGroups[group]
            self.setStyles()
            self.defNames, self.defValues = parseProp(\
                  self.styleDict.get(wx_stc.STC_STYLE_DEFAULT, ''))
            self.OnElementlbListbox(None)
            self.currSpeedSetting = group

    def OnFixedwidthchkCheckbox(self, event):
        self.populateCombosWithCommonDefs(event.Checked())

#---STC events------------------------------------------------------------------
    def OnUpdateUI(self, event):
        styleBefore = self.stc.GetStyleAt(self.stc.GetCurrentPos())
        if self.styleIdNames.has_key(styleBefore):
            self.elementLb.SetStringSelection(self.styleIdNames[styleBefore],
                  True)
        else:
            self.elementLb.SetSelection(0, False)
            self.styleDefST.SetLabel('Style %d not defined, sorry.'%styleBefore)
        self.OnElementlbListbox(None)
        event.Skip()

    def checkBraces(self, style):
        if style == wx_stc.STC_STYLE_BRACELIGHT and self.braceInfo.has_key('good'):
            line, col = self.braceInfo['good']
            pos = self.stc.PositionFromLine(line-1) + col
            braceOpposite = self.stc.BraceMatch(pos)
            if braceOpposite != -1:
                self.stc.BraceHighlight(pos, braceOpposite)
        elif style == wx_stc.STC_STYLE_BRACEBAD and self.braceInfo.has_key('bad'):
            line, col = self.braceInfo['bad']
            pos = self.stc.PositionFromLine(line-1) + col
            self.stc.BraceBadLight(pos)
        else:
            self.stc.BraceBadLight(-1)
        return

    def OnStcstyleeditdlgSize(self, event):
        self.Layout()
        # Without this refresh, resizing leaves artifacts
        self.Refresh(1)
        event.Skip()

    def OnMarginClick(self, event):
        self.elementLb.SetStringSelection('Line numbers', True)
        self.OnElementlbListbox(None)

        self.result = ( '', '' )
        self.EndModal(wx.ID_CANCEL)
#---Functions useful outside of the editor----------------------------------

def setSelectionColour(stc, style):
    names, values = parseProp(style)
    if 'fore' in names:
        stc.SetSelForeground(True, strToCol(values['fore']))
    if 'back' in names:
        stc.SetSelBackground(True, strToCol(values['back']))

def setCursorColour(stc, style):
    names, values = parseProp(style)
    if 'fore' in names:
        stc.SetCaretForeground(strToCol(values['fore']))

def setEdgeColour(stc, style):
    names, values = parseProp(style)
    if 'fore' in names:
        stc.SetEdgeColour(strToCol(values['fore']))

def strToCol(strCol):
    assert len(strCol) == 7 and strCol[0] == '#', 'Not a valid colour string'
    return wx.Colour(string.atoi('0x'+strCol[1:3], 16),
                    string.atoi('0x'+strCol[3:5], 16),
                    string.atoi('0x'+strCol[5:7], 16))
def colToStr(col):
    return '#%s%s%s' % (string.zfill(string.upper(hex(col.Red())[2:]), 2),
                        string.zfill(string.upper(hex(col.Green())[2:]), 2),
                        string.zfill(string.upper(hex(col.Blue())[2:]), 2))

def writeProp(num, style):
    if num >= 0:
        return 'style.%s='%(string.zfill(`num`, 3)) + style
    else:
        return 'setting.%d='%(num) + style

def writePropVal(names, values):
    res = []
    for name in names:
        if name:
            res.append(values[name] and name+':'+values[name] or name)
    return string.join(res, ',')

def parseProp(prop):
    items = string.split(prop, ',')
    names = []
    values = {}
    for item in items:
        nameVal = string.split(item, ':')
        names.append(string.strip(nameVal[0]))
        if len(nameVal) == 1:
            values[nameVal[0]] = ''
        else:
            values[nameVal[0]] = string.strip(nameVal[1])
    return names, values

def parsePropLine(prop):
    name, value = string.split(prop, '=')
    return int(string.split(name, '.')[-1]), value

def setSTCStyles(stc, styles, styleIdNames, commonDefs, keywords):
    #wx.LogMessage('Set style')
    styleDict = {}
    styleNumIdxMap = {}

    # build style dict based on given styles
    for numStyle in styles:
        num, style = parsePropLine(numStyle)
        styleDict[num] = style

    # Add blank style entries for undefined styles
    newStyles = []
    styleItems = styleIdNames.items() + settingsIdNames.items()
    styleItems.sort()
    idx = 0
    for num, name in styleItems:
        styleNumIdxMap[num] = idx
        if not styleDict.has_key(num):
            styleDict[num] = ''
        newStyles.append(writeProp(num, styleDict[num]))
        idx = idx + 1

    # Set background colour to reduce flashing effect on refresh or page switch
    bkCol = None
    if styleDict.has_key(0): prop = styleDict[0]
    else: prop = styleDict[wx_stc.STC_STYLE_DEFAULT]
    names, vals = parseProp(prop)
    if 'back' in names:
        bkCol = strToCol(vals['back'])
    if bkCol is None:
        bkCol = wx.WHITE
    stc.SetBackgroundColour(bkCol)

    # Set the styles on the wx.STC
    if wx.Platform != '__WXMAC__': stc.StyleResetDefault()
    stc.ClearDocumentStyle()
    stc.StyleSetSpec(wx_stc.STC_STYLE_DEFAULT,
          styleDict[wx_stc.STC_STYLE_DEFAULT] % commonDefs)
    stc.StyleClearAll()
    stc.SetLexer(wx_stc.STC_LEX_PYTHON)
    stc.SetKeyWords(0, keywords)

    for num, style in styleDict.items():
        if num >= 0:
            stc.StyleSetSpec(num, styleDict[num] % commonDefs)
        elif num == -1:
            setSelectionColour(stc, style % commonDefs)
        elif num == -2:
            setCursorColour(stc, style % commonDefs)
        elif num == -3:
            setEdgeColour(stc, style % commonDefs)

    stc.Colourise(0, stc.GetTextLength())

    return newStyles, styleDict, styleNumIdxMap

#---Config reading and writing -------------------------------------------------
commonDefsFile = 'common.defs.%s'%(wx.Platform == '__wx.MSW__' and 'msw' or 'gtk')

def getDefs():
    if wx.Platform == '__WXMSW__':
        commonDefs = {
            'helv': 'Courier New', 
            'mono': 'Courier New', 
            'lnsize': 10, 
            'backcol': '#FFFFFF', 
            'size': 10}
    elif wx.Platform =='__WXMAC__':
        commonDefs = {
            'helv': 'Helvetica', 
            'mono': 'Courier', 
            'lnsize': 14, 
            'backcol': '#FFFFFF', 
            'size': 14}
    else:    
        commonDefs = {
            'helv': 'Helvetica', 
            'mono': 'Courier', 
            'lnsize': 10, 
            'backcol': '#FFFFFF', 
            'size': 10}
            
    commonStyleIdNames = {
        wx_stc.STC_STYLE_DEFAULT: 'Style default',
        wx_stc.STC_STYLE_LINENUMBER: 'Line numbers', 
        wx_stc.STC_STYLE_BRACELIGHT: 'Matched braces', 
        wx_stc.STC_STYLE_BRACEBAD: 'Unmatched brace', 
        wx_stc.STC_STYLE_CONTROLCHAR: 'Control characters', 
        wx_stc.STC_STYLE_INDENTGUIDE: 'Indent guide'}

    # Lang spesific settings
    styleIdNames = {
        wx_stc.STC_P_DEFAULT: 'Default', 
        wx_stc.STC_P_COMMENTLINE: 'Comment', 
        wx_stc.STC_P_NUMBER : 'Number', 
        wx_stc.STC_P_STRING : 'String', 
        wx_stc.STC_P_CHARACTER: 'Single quoted string', 
        wx_stc.STC_P_WORD: 'Keyword', 
        wx_stc.STC_P_TRIPLE:'Triple quotes', 
        wx_stc.STC_P_TRIPLEDOUBLE: 'Triple double quotes', 
        wx_stc.STC_P_CLASSNAME: 'Class definition', 
        wx_stc.STC_P_DEFNAME: 'Function or method', 
        wx_stc.STC_P_OPERATOR: 'Operators', 
        wx_stc.STC_P_IDENTIFIER: 'Identifiers', 
        wx_stc.STC_P_COMMENTBLOCK: 'Comment blocks', 
        wx_stc.STC_P_STRINGEOL: 'EOL unclosed string'} 
    styleIdNames.update(commonStyleIdNames)

    braceInfo = {
        'good': (9, 12), 
        'bad': (10, 12)}

    displaySrc ="""## Comment Blocks!
class MyClass(MyParent):
    \"\"\" Class example \"\"\"
    def __init__(self):
        ''' Triple quotes '''
        # Do something silly
        ## Do something silly again
        a = ('Py' + "thon") * 100
        b = 'EOL unclosed string
        c = [Matched braces]
        d = {Unmatched brace"""     
    
    keywords = "and assert break class continue def del elif else except " + \
        "exec finally for from global if import in is lambda not or pass print raise return try while"

    return (commonDefs, styleIdNames, displaySrc, keywords, braceInfo)

def initFromConfig(cfg):
    (commonDefs, styleIdNames, displaySrc, keywords, braceInfo) = getDefs()

    # read in all group names for this language
    predefStyleGroupNames = []

    for val in cfg.sections():    
        if len(val) >= 10 and val[:10] == 'stc.style.' and val != customStyle:
            predefStyleGroupNames.append(val)

    # read in current styles
    styles = readStylesFromConfig(cfg,customStyle)

    # read in predefined styles
    predefStyleGroups = {}
    for group in predefStyleGroupNames:
        predefStyleGroups[group] = readStylesFromConfig(cfg, group)

    return (cfg, commonDefs, styleIdNames, styles, predefStyleGroupNames,
            predefStyleGroups, displaySrc, keywords, braceInfo)

def readStylesFromConfig(config, group):
    styles = []
    if config.has_section(group):
        for val in config.options(group):
            if val[:6] == 'style.' or val[:8] == 'setting.':
                styles.append(val + "=" + config.get(group, val, True))
    return styles

def writeStylesToConfig(config, group, styles):
    if config.has_section(group):
        config.remove_section(group)    
    config.add_section(group)

    for style in styles:
        name, value = string.split(style, '=')
        config.set(group, name, string.strip(value))
 
#-------------------------------------------------------------------------------
def initSTC(stc, config):
    """ Main module entry point. Initialise a wx.STC from given config file."""
    (cfg, commonDefs, styleIdNames, styles, predefStyleGroupNames,
     predefStyleGroups, displaySrc, keywords, braceInfo) = initFromConfig(config)

    setSTCStyles(stc, styles, styleIdNames, commonDefs, keywords)

def setStyle(stc, config, styleName):
    (commonDefs, styleIdNames, displaySrc, keywords, braceInfo) = getDefs()
    styles = readStylesFromConfig(config, styleName)
    setSTCStyles(stc, styles, styleIdNames, commonDefs, keywords)

def SetStyles(stc, config):
    #try:
        styleSetting = config.get("Default","stcstyle")
        if styleSetting == "<default>":
            return False
        setStyle(stc, config, "stc.style." + styleSetting)
        return True
    #except:
        #return False

#-------------------------------------------------------------------------------
if __name__ == '__main__':
    app = wx.PySimpleApp()

    provider = wx.SimpleHelpProvider()
    wx.HelpProvider_Set(provider)

    base = os.path.split(__file__)[0]
    configPath = os.path.abspath(os.path.join(base, 'defaults.cfg'))
    if 0:
        f = wx.Frame(None, -1, 'Test frame (double click for editor)')
        stc = wx.StyledTextCtrl(f, -1)
        def OnDblClick(evt, stc=stc):
            dlg = STCStyleEditDlg(None, 'Python', config, stc)
            try: dlg.ShowModal()
            finally: dlg.Destroy()
        stc.SetText(open('STCStyleEditor.py').read())
        wx.EVT_LEFT_DCLICK(stc, OnDblClick)
        initSTC(stc, config)
        f.Show(True)
        app.MainLoop()
    else:
        config = ConfigParser.ConfigParser()
        config.read(configPath)
        dlg = STCStyleEditDlg(None, 'Python', config, None)
        try: 
            dlg.ShowModal()
        finally: 
            dlg.Destroy()
