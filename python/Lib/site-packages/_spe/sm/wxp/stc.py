#(c)www.stani.be (read __doc__ for more information)
import sm
INFO=sm.INFO.copy()

INFO['author']    = 'Robin Dunn'
INFO['date']      = 'A long time ago, in a galaxy far, far away...'
INFO['copyright'] ='(c) 1999 by Total Control Software'
INFO['title']     = INFO['titleFull'] = 'wxPython stc control'

INFO['description']=\
"""Changes:
    - apr 2004:
        + wx namespace rewrite by SM
    - sep 2003:
        + Indent/dedent fix by SM
        + Autocompletion keyboard generic by GF (guillermo.fernandez@epfl.ch)
    - may 2003:
        + Adapted by SM (www.stani.be) for spe to include autocompletion and
          callbacks
"""

__doc__=INFO['doc']%INFO
#_______________________________________________________________________________
import re

#Original header:

#-------------------------------------------------------------------------------
# Author:       Robin Dunn
#
# Created:      A long time ago, in a galaxy far, far away...
# Copyright:    (c) 1999 by Total Control Software
# Licence:      wxWindows license
#
#-------------------------------------------------------------------------------

import wx
import wx.stc as wx_stc
import wx.gizmos as wx_gizmos

import inspect,keyword,os,sys,types

#-------------------------------------------------------------------------------

WORDCHARS = "_.abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

#-------------------------------------------------------------------------------
try:
    True
except NameError:
    True = 1==1
    False = 1==0



#----------------------------------------------------------------------

class PythonBaseSTC(wx_stc.StyledTextCtrl):
    def __init__(self, parent, id=-1,namespace={},path=None,config=None,
            ignore=None,menu=None):
        wx_stc.StyledTextCtrl.__init__(self, parent, id,
                                  style = wx.FULL_REPAINT_ON_RESIZE|wx.NO_BORDER)
        #PASSING VALUES
        self.namespace=namespace
        self.config=config
        self.ignore=ignore
        if path and path not in sys.path: sys.path.append(path)

        #INITIALIZE
        self.calltip    = 0 #calltip counter
        self.menu       = menu
        self.SetLexer(wx_stc.STC_LEX_PYTHON)

        #KEYBOARD SHORTCUTS (what are they doing here?)
        self.CmdKeyAssign(ord('B'), wx_stc.STC_SCMOD_CTRL, wx_stc.STC_CMD_ZOOMIN)
        self.CmdKeyAssign(ord('N'), wx_stc.STC_SCMOD_CTRL, wx_stc.STC_CMD_ZOOMOUT)
        
        #KEYPAD DEFINITIONS
        self.CmdKeyAssign(wx.WXK_NUMPAD_UP, 0, wx_stc.STC_CMD_LINEUP)
        self.CmdKeyAssign(wx.WXK_NUMPAD_DOWN, 0, wx_stc.STC_CMD_LINEDOWN)
        self.CmdKeyAssign(wx.WXK_NUMPAD_LEFT, 0, wx_stc.STC_CMD_CHARLEFT)
        self.CmdKeyAssign(wx.WXK_NUMPAD_RIGHT, 0, wx_stc.STC_CMD_CHARRIGHT)
        self.CmdKeyAssign(wx.WXK_NUMPAD_HOME, 0, wx_stc.STC_CMD_HOME)
        self.CmdKeyAssign(wx.WXK_NUMPAD_END, 0, wx_stc.STC_CMD_LINEEND)
        self.CmdKeyAssign(wx.WXK_NUMPAD_HOME, wx_stc.STC_SCMOD_CTRL, wx_stc.STC_CMD_DOCUMENTSTART)
        self.CmdKeyAssign(wx.WXK_NUMPAD_END, wx_stc.STC_SCMOD_CTRL, wx_stc.STC_CMD_DOCUMENTEND)
        self.CmdKeyAssign(wx.WXK_NUMPAD_PAGEUP, 0, wx_stc.STC_CMD_PAGEUP)
        self.CmdKeyAssign(wx.WXK_NUMPAD_PAGEDOWN, 0, wx_stc.STC_CMD_PAGEDOWN)
        self.CmdKeyAssign(wx.WXK_NUMPAD_INSERT, 0, wx_stc.STC_CMD_EDITTOGGLEOVERTYPE)
        self.CmdKeyAssign(wx.WXK_NUMPAD_DELETE, 0, wx_stc.STC_CMD_CLEAR)

        #PYTHON
        self.SetLexer(wx_stc.STC_LEX_PYTHON)
        keywords=keyword.kwlist
        keywords.extend(['None','as','True','False'])
        self.SetKeyWords(0, " ".join(keywords))

        #GENERAL
        self.AutoCompSetIgnoreCase(False)

        #FOLDING
        self.SetProperty("fold", "1")
        self.SetProperty("tab.timmy.whinge.level", "1")
        self.SetProperty("fold.comment.python", "0")
        self.SetProperty("fold.quotes.python", "0")

        #USER SETTINGS
        if self.config:
            self.update()
        else:
            self.SetViewWhiteSpace(0)
            self.SetTabWidth(4)
            self.SetIndentationGuides(1)
            self.SetUseTabs(0)
            self.SetEdgeMode(wx_stc.STC_EDGE_LINE)
            self.SetEdgeColumn(79)
            self.getDefaultFaces()
            self.SetWordChars(WORDCHARS)
        self.SetStyles()

        self.SetBackSpaceUnIndents(1)
##        self.SetTabIndents(0)
##        self.SetIndent(1)
        self.SetEdgeColumn(79)
        self.SetEdgeColour(wx.Colour(200,200,200))

        #MARGINS
        self.SetMargins(0,0)
        #margin 1 for line numbers
        self.SetMarginType(1, wx_stc.STC_MARGIN_NUMBER)
        if self.getint('ViewLineNumbers'):
            self.SetMarginWidth(1, 50)
        else:
            self.SetMarginWidth(1, 0)
        #margin 2 for markers
        self.SetMarginType(2, wx_stc.STC_MARGIN_SYMBOL)
        self.SetMarginMask(2, wx_stc.STC_MASK_FOLDERS)
        self.SetMarginSensitive(2, True)
        self.SetMarginWidth(2, 12)
        if 0: # simple folder marks, like the old version
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDER, wx_stc.STC_MARK_ARROW, "navy", "navy")
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDEROPEN, wx_stc.STC_MARK_ARROWDOWN, "navy", "navy")
            # Set these to an invisible mark
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDEROPENMID, wx_stc.STC_MARK_BACKGROUND, "white", "black")
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDERMIDTAIL, wx_stc.STC_MARK_BACKGROUND, "white", "black")
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDERSUB, wx_stc.STC_MARK_BACKGROUND, "white", "black")
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDERTAIL, wx_stc.STC_MARK_BACKGROUND, "white", "black")

        else: # more involved "outlining" folder marks
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDEREND,     wx_stc.STC_MARK_BOXPLUSCONNECTED,  "white", "black")
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDEROPENMID, wx_stc.STC_MARK_BOXMINUSCONNECTED, "white", "black")
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDERMIDTAIL, wx_stc.STC_MARK_TCORNER,  "white", "black")
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDERTAIL,    wx_stc.STC_MARK_LCORNER,  "white", "black")
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDERSUB,     wx_stc.STC_MARK_VLINE,    "white", "black")
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDER,        wx_stc.STC_MARK_BOXPLUS,  "white", "black")
            self.MarkerDefine(wx_stc.STC_MARKNUM_FOLDEROPEN,    wx_stc.STC_MARK_BOXMINUS, "white", "black")
        wx_stc.EVT_STC_UPDATEUI(self,    id, self.OnUpdateUI)
        wx_stc.EVT_STC_MARGINCLICK(self, id, self.OnMarginClick)

        # STYLES
        # Make some styles,  The lexer defines what each style is used for, we
        # just have to define what each style looks like.  This set is adapted from
        # Scintilla sample property files.
        # Default style
        self.StyleSetSpec(wx_stc.STC_STYLE_DEFAULT,
                          "face:%(mono)s,size:%(size)d" % \
                          self.faces)
        self.StyleSetBackground(wx_stc.STC_STYLE_BRACELIGHT,"#AAAAFF")

        self.SetCaretForeground("BLACK")
        self.SetSelBackground(1,'DARK TURQUOISE')

        #EVENTS
        self.Bind(wx_stc.EVT_STC_UPDATEUI, self.OnUpdateUI)
        self.Bind(wx_stc.EVT_STC_MARGINCLICK, self.OnMarginClick)
        self.Bind(wx.EVT_CHAR, self.OnChar)
        self.Bind(wx.EVT_KEY_DOWN, self.OnKeyDown)
        self.Bind(wx.EVT_MIDDLE_DOWN, self.OnMiddleDown)
        if wx.Platform=='__WXMAC__':
            self.Bind(wx.EVT_LEFT_DOWN, self.OnLeftDown)
        if self.menu:
            self.UsePopUp(False)
            if wx.Platform=='__WXMAC__':
                self.Bind(wx.EVT_RIGHT_DOWN, self.OnRightClick)
            else:
                self.Bind(wx.EVT_RIGHT_UP, self.OnRightClick)

    #---events
    def OnLeftDown(self,event):
        if not event.ShiftDown():
            self.SetSelectionEnd(0)
        event.Skip()

    def OnMiddleDown(self,event):
        code    = self.GetSelectedText()
        pos     = self.PositionFromPointClose(event.GetX(),event.GetY())
        event.Skip()
        if pos>-1 and code.strip():
            self.SetSelection(pos,pos)
            self.ReplaceSelection(code)

    def OnKeyDown(self, event):
        """"""
        key     = event.GetKeyCode()
        control = event.ControlDown()
        #shift=event.ShiftDown()
        alt     = event.AltDown()
        if key == wx.WXK_RETURN and not control and not alt and not self.AutoCompActive():
            #auto-indentation
            if self.CallTipActive():
                self.CallTipCancel()
                self.calltip=0
            line        = self.GetCurrentLine()
            txt         = self.GetLine(line)
            pos         = self.GetCurrentPos()
            linePos     = self.PositionFromLine(line)
            self.CmdKeyExecute(wx_stc.STC_CMD_NEWLINE)
            indent      = self.GetLineIndentation(line)
            padding     = self.indentation * (indent/max(1,self.tabWidth))
            newpos      = self.GetCurrentPos()
            # smart indentation
            stripped    = txt[:pos-linePos].split('#')[0].strip()
            firstWord   = stripped.split(" ")[0]
            if stripped and self.needsIndent(firstWord,lastChar=stripped[-1]):
                padding += self.indentation
            elif self.needsDedent(firstWord):
                padding  = padding[:-self.tabWidth]
            self.InsertText(newpos, padding)
            newpos  += len(padding)
            self.SetCurrentPos(newpos)
            self.SetSelection(newpos, newpos)
        else:
            event.Skip()

    def OnChar(self,event):
        key     = event.GetKeyCode()
        control = event.ControlDown()
        alt     = event.AltDown()
        # GF We avoid an error while evaluating chr(key), next line.
        if key > 255 or key < 0:
            event.Skip()
        # GF No keyboard needs control or alt to make '(', ')' or '.'
        # GF Shift is not included as it is needed in some keyboards.
        elif chr(key) in ['(',')','.'] and not control and not alt:
            CallTips    = self.get('CallTips').lower()
            if key == ord('(') and CallTips!='disable':
                # ( start tips
                if self.CallTipActive():
                    self.calltip    += 1
                    self.AddText('(')
                else:
                    self.showCallTip('(')
            elif key == ord(')'):
                # ) end tips
                self.AddText(')')
                if self.calltip:
                    self.calltip    -=1
                    if not self.calltip:
                        self.CallTipCancel()
            elif key == ord('.') and self.getint('AutoComplete'):
                # . Code completion
                self.autoComplete(object=1)
            else:
                event.Skip()
        else:
            event.Skip()

    def OnUpdateUI(self, evt):
        # check for matching braces
        braceAtCaret = -1
        braceOpposite = -1
        charBefore = None
        caretPos = self.GetCurrentPos()
        if caretPos > 0:
            charBefore = self.GetCharAt(caretPos - 1)
            styleBefore = self.GetStyleAt(caretPos - 1)

        # check before
        if charBefore and chr(charBefore) in "[]{}()" and styleBefore == wx_stc.STC_P_OPERATOR:
            braceAtCaret = caretPos - 1

        # check after
        if braceAtCaret < 0:
            charAfter = self.GetCharAt(caretPos)
            styleAfter = self.GetStyleAt(caretPos)
            if charAfter and chr(charAfter) in "[]{}()" and styleAfter == wx_stc.STC_P_OPERATOR:
                braceAtCaret = caretPos

        if braceAtCaret >= 0:
            braceOpposite = self.BraceMatch(braceAtCaret)

        if braceAtCaret != -1  and braceOpposite == -1:
            self.BraceBadLight(braceAtCaret)
        else:
            self.BraceHighlight(braceAtCaret, braceOpposite)
            #pt = self.PointFromPosition(braceOpposite)
            #self.Refresh(True, wx.Rect(pt.x, pt.y, 5,5))
            #print pt
            #self.Refresh(False)


    def OnMarginClick(self, evt):
        # fold and unfold as needed
        if evt.GetMargin() == 2:
            if evt.GetShift() and evt.GetControl():
                self.FoldAll()
            else:
                lineClicked = self.LineFromPosition(evt.GetPosition())
                if self.GetFoldLevel(lineClicked) & wx_stc.STC_FOLDLEVELHEADERFLAG:
                    if evt.GetShift():
                        self.SetFoldExpanded(lineClicked, True)
                        self.Expand(lineClicked, True, True, 1)
                    elif evt.GetControl():
                        if self.GetFoldExpanded(lineClicked):
                            self.SetFoldExpanded(lineClicked, False)
                            self.Expand(lineClicked, False, True, 0)
                        else:
                            self.SetFoldExpanded(lineClicked, True)
                            self.Expand(lineClicked, True, True, 100)
                    else:
                        self.ToggleFold(lineClicked)


    def OnRightClick(self, event):
        self.PopupMenu(self.menu)

    def SetViewEdge(self,check):
            if check:
                self.SetEdgeMode(wx_stc.STC_EDGE_LINE)
            else:
                self.SetEdgeMode(wx_stc.STC_EDGE_NONE)

    def FoldAll(self):
        lineCount = self.GetLineCount()
        expanding = True

        # find out if we are folding or unfolding
        for lineNum in range(lineCount):
            if self.GetFoldLevel(lineNum) & wx_stc.STC_FOLDLEVELHEADERFLAG:
                expanding = not self.GetFoldExpanded(lineNum)
                break;

        lineNum = 0
        while lineNum < lineCount:
            level = self.GetFoldLevel(lineNum)
            if level & wx_stc.STC_FOLDLEVELHEADERFLAG and \
               (level & wx_stc.STC_FOLDLEVELNUMBERMASK) == wx_stc.STC_FOLDLEVELBASE:

                if expanding:
                    self.SetFoldExpanded(lineNum, True)
                    lineNum = self.Expand(lineNum, True)
                    lineNum = lineNum - 1
                else:
                    lastChild = self.GetLastChild(lineNum, -1)
                    self.SetFoldExpanded(lineNum, False)
                    if lastChild > lineNum:
                        self.HideLines(lineNum+1, lastChild)

            lineNum = lineNum + 1

    def Expand(self, line, doExpand, force=False, visLevels=0, level=-1):
        lastChild = self.GetLastChild(line, level)
        line = line + 1
        while line <= lastChild:
            if force:
                if visLevels > 0:
                    self.ShowLines(line, line)
                else:
                    self.HideLines(line, line)
            else:
                if doExpand:
                    self.ShowLines(line, line)

            if level == -1:
                level = self.GetFoldLevel(line)

            if level & wx_stc.STC_FOLDLEVELHEADERFLAG:
                if force:
                    if visLevels > 1:
                        self.SetFoldExpanded(line, True)
                    else:
                        self.SetFoldExpanded(line, False)
                    line = self.Expand(line, doExpand, force, visLevels-1)

                else:
                    if doExpand and self.GetFoldExpanded(line):
                        line = self.Expand(line, True, force, visLevels-1)
                    else:
                        line = self.Expand(line, False, force, visLevels-1)
            else:
                line = line + 1;

        return line

#---preferences-----------------------------------------------------------------
    def get(self,name):
        return self.config.get('Default',name)

    def getint(self,name):
        try:
            return self.config.getint('Default',name)
        except:#True,False
            if eval(self.config.get('Default',name)):
                return 1
            else:
                return 0

    def update(self):
        #general
        try:
            font, size  = self.get('Font').split(',')
            font        = font.strip()
            size        = eval(size.strip())
            self.faces  = { 'times': font, 'mono' : font, 'helv' : font, 'other': font,
                            'size' : size, 'size2': size}
        except:
            self.getDefaultFaces()
        self.SetStyles()
        #guides
        self.SetEdgeColumn(self.getint('EdgeColumn'))
        self.SetViewEdge(self.getint('ViewEdge'))
        self.SetIndentationGuides(self.getint('IndentationGuides'))
        #tabs & whitespaces
        self.tabWidth = self.getint('TabWidth')
        self.SetTabWidth(self.getint('TabWidth'))
        self.SetUseTabs(self.getint('UseTabs'))
        self.SetViewWhiteSpace(self.getint('ViewWhiteSpace'))
        #line numbers
        if self.getint('ViewLineNumbers'):
            self.SetMarginWidth(1, 50)
        else:
            self.SetMarginWidth(1, 0)
        
            
        if self.getint('UseTabs'):
            self.indentation = '\t'
        else:
            self.indentation = " " * self.tabWidth
        self.SetWordChars(self.get('WordChars'))

    def SetStyles(self):
        # anti-aliasing
        if hasattr(self,'SetUseAntiAliasing'):
            self.SetUseAntiAliasing(True)

        #INDICATOR STYLES FOR ERRORS (self.errorMark)
        self.IndicatorSetStyle(2, wx_stc.STC_INDIC_SQUIGGLE)
        self.IndicatorSetForeground(2, wx.RED)

        #import dialogs.stcStyleEditor
        if 1:#dialogs.stcStyleEditor.SetStyles(self, self.config):
            self.StyleSetSpec(wx_stc.STC_P_DEFAULT, "face:%(mono)s,size:%(size)d" % self.faces)
            self.StyleClearAll()

            # Global default styles for all languages
            self.StyleSetSpec(wx_stc.STC_STYLE_DEFAULT,     "face:%(mono)s,size:%(size)d" % self.faces)
            self.StyleSetSpec(wx_stc.STC_STYLE_LINENUMBER,  "back:#C0C0C0,face:%(mono)s,size:%(size)d" % self.faces)
            self.StyleSetSpec(wx_stc.STC_STYLE_CONTROLCHAR, "face:%(mono)s" % self.faces)
            self.StyleSetSpec(wx_stc.STC_STYLE_BRACELIGHT,  "fore:#FFFFFF,back:#0000FF,bold")
            self.StyleSetSpec(wx_stc.STC_STYLE_BRACEBAD,    "fore:#000000,back:#FF0000,bold")

            # Python styles
            # White space
            self.StyleSetSpec(wx_stc.STC_P_DEFAULT, "face:%(mono)s,size:%(size)d" % self.faces)
            # Comment
            self.StyleSetSpec(wx_stc.STC_P_COMMENTLINE, "face:%(mono)s,fore:#007F00,back:#E8FFE8,italic,size:%(size)d" % self.faces)
            # Number
            self.StyleSetSpec(wx_stc.STC_P_NUMBER, "face:%(mono)s,fore:#007F7F,size:%(size)d" % self.faces)
            # String
            self.StyleSetSpec(wx_stc.STC_P_STRING, "face:%(mono)s,fore:#7F007F,size:%(size)d" % self.faces)
            # Single quoted string
            self.StyleSetSpec(wx_stc.STC_P_CHARACTER, "face:%(mono)s,fore:#7F007F,size:%(size)d" % self.faces)
            # Keyword
            self.StyleSetSpec(wx_stc.STC_P_WORD, "face:%(mono)s,fore:#00007F,bold,size:%(size)d" % self.faces)
            # Triple quotes
            self.StyleSetSpec(wx_stc.STC_P_TRIPLE, "face:%(mono)s,fore:#7F0000,size:%(size)d" % self.faces)
            # Triple double quotes
            self.StyleSetSpec(wx_stc.STC_P_TRIPLEDOUBLE, "face:%(mono)s,fore:#7F0000,size:%(size)d" % self.faces)
            # Class name definition
            self.StyleSetSpec(wx_stc.STC_P_CLASSNAME, "face:%(mono)s,fore:#0000FF,bold,underline,size:%(size)d" % self.faces)
            # Function or method name definition
            self.StyleSetSpec(wx_stc.STC_P_DEFNAME, "face:%(mono)s,fore:#007F7F,bold,size:%(size)d" % self.faces)
            # Operators
            self.StyleSetSpec(wx_stc.STC_P_OPERATOR, "face:%(mono)s,bold,size:%(size)d" % self.faces)
            # Identifiers
            self.StyleSetSpec(wx_stc.STC_P_IDENTIFIER, "")
            # Comment-blocks
            self.StyleSetSpec(wx_stc.STC_P_COMMENTBLOCK, "face:%(mono)s,fore:#990000,back:#C0C0C0,italic,size:%(size)d" % self.faces)
            # End of line where string is not closed
            self.StyleSetSpec(wx_stc.STC_P_STRINGEOL, "face:%(mono)s,fore:#000000,face:%(mono)s,back:#E0C0E0,eol,size:%(size)d" % self.faces)

    #---get
    def getWord(self,whole=None):
        for delta in (0,-1,1):
            word    = self._getWord(whole=whole,delta=delta)
            if word: return word
        return ''

    def _getWord(self,whole=None,delta=0):
        pos     = self.GetCurrentPos()+delta
        line    = self.GetCurrentLine()
        linePos = self.PositionFromLine(line)
        txt     = self.GetLine(line)
        start   = self.WordStartPosition(pos,1)
        if whole:
            end = self.WordEndPosition(pos,1)
        else:
            end = pos
        return txt[start-linePos:end-linePos]

    def getWords(self,word=None,whole=None):
        if not word: word = self.getWord(whole=whole)
        if not word:
            return []
        else:
            return sm.unique([x for x in re.findall(r"\b" + word + r"\w*\b", self.GetText())
                if x.find(',')==-1 and x[0]!= ' '])

    def getWordObject(self,word=None,whole=None):
        if not word: word=self.getWord(whole=whole)
        try:
            obj = self.evaluate(word)
            return obj
        except:
            return None

    def getWordFileName(self,whole=None):
        wordList=self.getWord(whole=whole).split('.')
        wordList.append('')
        index=1
        n=len(wordList)
        while index<n:
            word='.'.join(wordList[:-index])
            try:
                fileName = self.getWordObject(word=word).__file__.replace('.pyc','.py').replace('.pyo','.py')
                if os.path.exists(fileName):
                    return fileName
            except:
                pass
            index+=1
        return '"%s.py"'%'.'.join(wordList[:-1])

    def getDefaultFaces(self):
        if wx.Platform == '__WXMSW__':
            self.faces = { 'times': 'Courier New',
                      'mono' : 'Courier',
                      'helv' : 'Courier',
                      'other': 'Courier',
                      'size' : 10,
                      'size2': 10,
                     }
        elif  wx.Platform == '__WXMAC__':
            self.faces = { 'times': 'Times',
                      'mono' : 'Courier',
                      'helv' : 'Courier',
                      'other': 'Courier',
                      'size' : 12,
                      'size2': 10,
                     }
        else:
            self.faces = { 'times': 'Times',
                      'mono' : 'Courier',
                      'helv' : 'Courier',
                      'other': 'Courier',
                      'size' : 10,
                      'size2': 10,
                     }


    #---methods
    def assertEOL(self):
        self.ConvertEOLs(self.GetEOLMode())

    def autoComplete(self,object=0):
        word    = self.getWord()
        if not word:
            if object:
                self.AddText('.')
            return
        if object:
            self.AddText('.')
            word+='.'
        words   = self.getWords(word=word)
        for dot in range(len(word)):
            if word[-dot-1] == '.':
                try:
                    obj = self.getWordObject(word[:-dot-1])
                    if obj:
                        for attr in dir(obj):
                            #attr = '%s%s'%(word[:-dot],attr)
                            attr = '%s%s'%(word,attr)
                            if attr not in words: words.append(attr)
                except:
                    pass
                break
        if words:
            words.sort()
            try:
                self.AutoCompShow(len(word), " ".join(words))
            except:
                pass

    def evaluate(self,word):
        if word in self.namespace.keys():return self.namespace[word]
        try:
            self.namespace[word]=eval(word,self.namespace)
            return self.namespace[word]
        except:
            try:
                self.get('AutoCompleteIgnore').index(word)
                return None
            except:
                try:
                    components = word.split('.')
                    try:
                        mod= __import__(word)
                    except:
                        if len(components) < 2:
                            return None
                        mod = '.'.join(components[:-1])
                        try:
                            mod= __import__(mod)
                        except:
                            return None
                    for comp in components[1:]:
                        mod = getattr(mod, comp)
                    self.namespace[word]=mod
                    return mod
                except:
                    return None

    def markError(self,lineno,offset):
        self.StartStyling(self.PositionFromLine(lineno-1), wx_stc.STC_INDICS_MASK)
        self.SetStyling(offset, wx_stc.STC_INDIC2_MASK)
        self.Colourise(0, -1)

    def clearError(self,length):
        self.StartStyling(0, wx_stc.STC_INDICS_MASK)
        self.SetStyling(length, 0)
        self.Colourise(0, -1)

    def needsIndent(self,firstWord,lastChar):
        "Tests if a line needs extra indenting, ie if, while, def, etc "
        # remove trailing : on token
        if len(firstWord) > 0:
            if firstWord[-1] == ":":
                firstWord = firstWord[:-1]
        # control flow keywords
        if firstWord in ["for","if", "else", "def","class","elif", "try","except","finally","while"] and lastChar == ':':
            return True
        else:
            return False

    def needsDedent(self,firstWord):
        "Tests if a line needs extra dedenting, ie break, return, etc "
        # control flow keywords
        if firstWord in ["break","return","continue","yield","raise"]:
            return True
        else:
            return False

    def showCallTip(self,text=''):
        #prepare
        obj                 = self.getWordObject()
        self.AddText(text)
        if not obj: return
        #classes, methods & functions
        if type(obj) in [types.ClassType,types.TypeType] and hasattr(obj,'__init__'):
            init            = obj.__init__
            tip             = getargspec(init).strip()
            if tip == '(self, *args, **kwargs)':
                tip         = ""
            else:
                tip         = "%s\n"%tip
            doci            = init.__doc__
            if doci:
                doc         = '%s\n'%(doci.strip())
            else:
                doc         = ""
            tip             = getargspec(init)
        else:
            doc             = ""
            tip             = getargspec(obj)
        #normal docstring
        _doc                = obj.__doc__
        #compose
        if _doc: doc        += _doc
        if doc:
            if self.get('CallTips').lower() == 'first paragraph only':
                tip         += doc.split('\n')[0]
            else:
                tip         += doc
        if tip:
            pos             = self.GetCurrentPos()
            self.calltip    = 1
            tip+='\n(Press ESC to close)'
            self.CallTipSetBackground('#FFFFE1')
            self.CallTipShow(pos, tip.replace('\r\n','\n'))





class PythonViewSTC(PythonBaseSTC):
    """Mutation for dynamic class"""
    def __init__(self,parent, child = None, *args,**kwds):
        PythonBaseSTC.__init__(self,parent,*args,**kwds)
        self.dyn_sash = parent
        self.child    = child
        self._args = args
        self._kwds = kwds
        self.SetupScrollBars()
        wx_gizmos.EVT_DYNAMIC_SASH_SPLIT(self,-1,self.OnSplit)
        wx_gizmos.EVT_DYNAMIC_SASH_UNIFY(self,-1,self.OnUnify)
        wx.EVT_SET_FOCUS(self,self.OnSetFocus)
        wx.EVT_KILL_FOCUS(self,self.OnKillFocus)
        self.SetScrollbar(wx.HORIZONTAL, 0, 0, 0)
        self.SetScrollbar(wx.VERTICAL, 0, 0, 0)
##        eventManager.Register(self.OnSplit,wx_gizmos.EVT_DYNAMIC_SASH_SPLIT,self)
##        eventManager.Register(self.OnUnify,wx_gizmos.EVT_DYNAMIC_SASH_UNIFY,self)

    def SetupScrollBars(self):
        # hook the scrollbars provided by the wxDynamicSashWindow
        # to this view
        v_bar = self.dyn_sash.GetVScrollBar(self)
        h_bar = self.dyn_sash.GetHScrollBar(self)
        wx.EVT_SCROLL(v_bar,self.OnSBScroll)
        wx.EVT_SCROLL(h_bar,self.OnSBScroll)
        wx.EVT_SET_FOCUS(v_bar, self.OnSBFocus)
        wx.EVT_SET_FOCUS(h_bar, self.OnSBFocus)
##        eventManager.Register(self.OnSBScroll, wx.EVT_SCROLL, v_bar)
##        eventManager.Register(self.OnSBScroll, wx.EVT_SCROLL, h_bar)
##        eventManager.Register(self.OnSBFocus,  wx.EVT_SET_FOCUS, v_bar)
##        eventManager.Register(self.OnSBFocus,  wx.EVT_SET_FOCUS, h_bar)

        # And set the wxStyledText to use these scrollbars instead
        # of its built-in ones.
        self.SetVScrollBar(v_bar)
        self.SetHScrollBar(h_bar)

    def OnSetFocus(self,event):
        self.child.source = self
        event.Skip()

    def OnKillFocus(self,event):
        self.AutoCompCancel()
        event.Skip()

    def OnSplit(self, evt):
        newview = PythonViewSTC(self.dyn_sash, child = self.child, *self._args, **self._kwds)
        newview.SetDocPointer(self.GetDocPointer())     # use the same document
        self.SetupScrollBars()

    def OnUnify(self, evt):
        self.SetupScrollBars()
        children = self.dyn_sash.GetChildren()[-1].GetChildren()
        while children[-1].__class__!=PythonViewSTC:
            children = children[-1].GetChildren()
        source = self.child.source = self.dyn_sash.view = children[-1]

    def OnSBScroll(self, evt):
        # redirect the scroll events from the dyn_sash's scrollbars to the STC
        self.GetEventHandler().ProcessEvent(evt)

    def OnSBFocus(self, evt):
        # when the scrollbar gets the focus move it back to the STC
        self.SetFocus()

class PythonSashSTC(wx_gizmos.DynamicSashWindow):
    def __init__(self,parent,*args,**kwds):
        wx_gizmos.DynamicSashWindow.__init__(self, parent,-1, style =  wx.CLIP_CHILDREN | wx.FULL_REPAINT_ON_RESIZE
                                  #| wxDS_MANAGE_SCROLLBARS
                                  #| wxDS_DRAG_CORNER
                                  )
        self.parent = parent
        self.view = PythonViewSTC(parent=self, id=-1, child = parent, *args, **kwds)
        #print dir(self)

if wx.Platform == "__WXMAC__":
    #The dynamic sash currently fails on the Mac. The problem is being looked into...
    PythonSTC = PythonBaseSTC
else:
    PythonSTC = PythonBaseSTC#PythonSashSTC

#-------------------------------------------------------------------------------

def getargspec(func):
    """Get argument specifications"""
    try:
        func=func.im_func
    except:
        pass
    try:
        return inspect.formatargspec(*inspect.getargspec(func)).replace('self, ','')+'\n\n'
    except:
        pass
    try:
        return inspect.formatargvalues(*inspect.getargvalues(func)).replace('self, ','')+'\n\n'
    except:
        return ''

