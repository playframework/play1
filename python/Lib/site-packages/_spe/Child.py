####(c)www.stani.be-------------------------------------------------------------

import _spe.info as info
INFO=info.copy()

INFO['description']=\
"""File browser as tab."""

__doc__=INFO['doc']%INFO

####Modules---------------------------------------------------------------------
import codecs, compiler, inspect, os, sys, re, shutil, thread, time, types

import wx
from wx.lib.evtmgr import eventManager

import sm, sm.osx, sm.spy, sm.uml, sm.wxp, sm.wxp.smdi
from sm.wxp.stc import PythonSTC
from sm.wxp.realtime import TreeCtrl, ListCtrl
import view.documentation

import _spe.help as help
from Menu import STATUS
import _spe.plugins.Pycheck as Pycheck
from sidebar.Browser import Browser 

####Constants-------------------------------------------------------------------
DEFAULT                 = "<default>"
MAXINT                  = sys.maxint #for ListCtrl
NEWFILE                 = 'unnamed'
SPE_ALLOWED_EXTENSIONS  = ['.py','.pyw','.tpy','.txt','.htm','.html','.bak']
STYLE_LIST              = wx.LC_REPORT
STYLE_NOTEBOOK          = wx.NO_BORDER
STYLE_NOTES             = wx.TE_MULTILINE
if not info.DARWIN:
    STYLE_NOTES         |= wx.TE_DONTWRAP
STYLE_SPLIT             = wx.SP_NOBORDER|wx.FULL_REPAINT_ON_RESIZE
STYLE_TREE              = wx.TR_HAS_BUTTONS|wx.TR_HIDE_ROOT
if info.LINUX:
    STYLE_TREE          |= wx.TR_NO_LINES
RE_DOCSTRING            = re.compile(r'(\n|\n__doc__\s*=(\s*|\s*\\\s*\n))("""([^"]*)"""|\'\'\'([^\']*)\'\'\'|"([^"]*)"|\'([^\']*)\')')
RE_DOCSTRING_FIRST      = re.compile(r'(|__doc__\s*=(\s*|\s*\\\s*\n))("""([^"]*)"""|\'\'\'([^\']*)\'\'\'|"([^"]*)"|\'([^\']*)\')')
RE_TODO                 = re.compile('.*#[ ]*TODO[ ]*:(.+)', re.IGNORECASE)
RE_SEPARATOR            = re.compile('^.*(#-{3})')
RE_SEPARATOR_HIGHLIGHT  = re.compile('^.*(#{4})')
RE_ENCODING             = re.compile('coding[:=]\s*([-\w.]+)', re.IGNORECASE)
UML_PAGE                = 1
STATUS_TEXT_LINE_POS    = 3
STATUS_TEXT_COL_POS     = STATUS_TEXT_LINE_POS+1

BLENDER_REF_SIGNATURE   = "Blender_signature.py" #you may customize this file
BLENDER_REF_TRACE       = "#!BPY" #required first characters of the Blender signature 

####Utilities-------------------------------------------------------------------
def umlAdd(classes, umlClass):
    """Add umlClass to classes dictionary"""
    if umlClass:
        classes[umlClass.name.split('(')[0]] = umlClass

def isUtf8(text):
    try:
        if text.startswith('\xef\xbb\xbf'):
            return True
        else:
            return False
    except:
        return False


####Child Panel class-----------------------------------------------------------
class Source(PythonSTC):
    def __init__(self,parent):
        child = parent
        while child.__class__ != Panel:
            child = child.GetParent()
        PythonSTC.__init__(self,parent=parent,
            namespace= child.parentPanel.shell.interp.locals,
            path=child._fileName,config=child.parentPanel.config)
        self.SetHelpText(help.CHILD_SOURCE)
        child.source = self


class Panel(wx.SplitterWindow):
    ####Constructors------------------------------------------------------------
    def __init__(self,parent,name='',fileName='',source='',*args,**kwds):
        self._fileName          = fileName
        self.name               = os.path.basename(fileName)
        self._source            = source
        #initialize
        self.argumentsPrevious  = []
        self.changed            = 0
        self.checkBusy          = False
        self.column             = 1
        self.eventChanged       = False
        self.exitPrevious       = True
        self.inspectPrevious    = False
        self.line               = 1
        self.position           = 0
        self.sashPosition       = [285,310][info.DARWIN]
        self.minSashPosition    = [120,310][info.DARWIN]
        self.sidebarHidden      = False
        self.saved              = ''
        self.todoMax            = 1
        self.toggleExploreSelection = False
        self.warning            = ''
        #delete when fixed
        self.updateBug          = False
        #construct
        wx.SplitterWindow.__init__(self, id=-1, parent=parent,style=STYLE_SPLIT)
        self.SetMinimumPaneSize(1)
        if info.DARWIN:
            self.SetSashSize(6)
        #Remember if this file contains DOS line endings (\r\n)
        #Otherwise assume Unix (\n)
        self.dosLines = (source.find('\r\n') >= 0)
        self.sashDelta      = 1
        if os.path.exists(fileName) and fileName != NEWFILE:
            self.fileTime   = os.path.getmtime(fileName)
        else:
            self.fileTime   = 0


    def __finish__(self):
        frame = self.frame
        if self._fileName not in self.parentPanel.workspace['openfiles']:
            #self.name   = '~'+self.name
            frame.setTitle(page=self.name,extra=self._fileName)
        else:
            frame.setTitle(page=self.name,extra=self._fileName,colour=wx.WHITE)
        frame.SetIcon(sm.wxp.bitmap2Icon(self.app.bitmap('icon_py.png')))
        self.__sideBar__()
        self.__source__(self._fileName,self._source)
        #update
        self.updateExplore()
        #events
        self.source.SetDropTarget(DropOpen(self.parentPanel.openList))
        eventManager.Register(self.onSetFocus, wx.EVT_SET_FOCUS, self)
        #eventManager.Register(self.onSetSourceFocus, wx.EVT_SET_FOCUS, self.source)
        eventManager.Register(self.onSash,wx.EVT_SPLITTER_SASH_POS_CHANGED,self)
        #events
        self.source.SetModEventMask(wx.stc.STC_MOD_DELETETEXT | wx.stc.STC_PERFORMED_USER)
        eventManager.Register(self.onSourceChange,wx.stc.EVT_STC_CHANGE,self.source)
        eventManager.Register(self.onSourceFromExplore,wx.EVT_TREE_ITEM_ACTIVATED,self.explore)
        if info.WIN:
            #Mac has already always triangles
            eventManager.Register(self.onToggleExploreTree,wx.EVT_LEFT_DOWN,self.explore)
        eventManager.Register(self.onSourceFromExplore,wx.EVT_TREE_ITEM_MIDDLE_CLICK,self.explore)
        eventManager.Register(self.onSourceFromExplore,wx.EVT_TREE_ITEM_RIGHT_CLICK,self.explore)
        eventManager.Register(self.onSourceFromTodo,wx.EVT_LIST_ITEM_SELECTED,self.todo)
        eventManager.Register(self.onSourceFromTodo,wx.EVT_LIST_ITEM_RIGHT_CLICK,self.todo)
        eventManager.Register(self.onSourceFromIndex,wx.EVT_LIST_ITEM_RIGHT_CLICK,self.index)
        eventManager.Register(self.onSourceFromIndex,wx.EVT_LIST_ITEM_SELECTED,self.index)
        eventManager.Register(self.updateSidebar,wx.EVT_NOTEBOOK_PAGE_CHANGED,self.notebook)
        #split
        self.SplitVertically(self.notebook, self.main, self.sashPosition)

    def __sideBar__(self):
        """Create notebook contents."""
        notebook = self.notebook = wx.Notebook(id=-1, parent=self,
              style=STYLE_NOTEBOOK)
        self.updateSidebarTab=[self.updateExplore,self.updateTodo,self.updateIndex,self.doNothing,self.doNothing]
        self.notebookLabel  = ['Explore','Todo','Index','Notes','Check']
        self.notebookIcons  = wx.ImageList(16,16)
        self.exploreIcon    = self.notebookIcons.Add(self.parentPanel.icons['explore.png'])
        self.browserIcon    = self.notebookIcons.Add(self.parentPanel.icons['browser.png'])
        self.todoIcon       = self.notebookIcons.Add(self.parentPanel.icons['todo.png'])
        self.indexIcon      = self.notebookIcons.Add(self.parentPanel.icons['index.png'])
        self.notesIcon      = self.notebookIcons.Add(self.parentPanel.icons['notes.png'])
        self.pycheckerIcon  = self.notebookIcons.Add(self.parentPanel.icons['pychecker.png'])
##        if info.LINUX:
##            #todo: check with linux users if this is really necessary?!
##            self.notebook.SetBackgroundColour(wx.WHITE)
        notebook.AssignImageList(self.notebookIcons)
        notebook.parentPanel=self.parentPanel

        #explore
        explore     = self.explore = TreeCtrl(parent=self.notebook,style=STYLE_TREE)#wx.TreeCtrl
        explore.SetBackgroundColour(wx.WHITE)
        self.root   = self.explore.AddRoot('Right click to locate')
        #explore.SetPyData(self.root,0)
        explore.SetImageList(self.parentPanel.iconsList)
##        explore.SetItemImage(self.root,self.parentPanel.iconsListIndex['note.png'])
##        explore.SetItemImage(self.root,self.parentPanel.iconsListIndex['note.png'],wx.TreeItemIcon_SelectedExpanded)
##        explore.SetItemImage(self.root,self.parentPanel.iconsListIndex['note.png'],wx.TreeItemIcon_Expanded)
##        explore.SetItemImage(self.root,self.parentPanel.iconsListIndex['note.png'],wx.TreeItemIcon_Selected)
        explore.SetHelpText(help.CHILD_EXPLORE)
        notebook.AddPage(page=self.explore, text='Explore',imageId=self.exploreIcon)
        #todo
        todo            = self.todo = ListCtrl(parent=self.notebook,style=STYLE_LIST)
        todo.InsertColumn(col=0, format=wx.LIST_FORMAT_LEFT,
                heading='Line',width=40)
        todo.InsertColumn(col=1, format=wx.LIST_FORMAT_LEFT,
                heading='!',width=20)
        todo.InsertColumn(col=2, format=wx.LIST_FORMAT_LEFT,
                heading='Task',width=500)
        todo.SetHelpText(help.CHILD_TODO)
        self.previousTodoHighlights = []
        notebook.AddPage(page=self.todo, text='',imageId=self.todoIcon)
        #index
        index = self.index = ListCtrl(parent=self.notebook,style=STYLE_LIST)
        index.SetImageList(self.parentPanel.iconsList,wx.IMAGE_LIST_SMALL)
        index.InsertColumn(col=0, format=wx.LIST_FORMAT_RIGHT,
                heading='Line',width=60)
        index.InsertColumn(col=1, format=wx.LIST_FORMAT_LEFT,
                heading='Entry',width=500)
        index.SetHelpText(help.CHILD_INDEX)
        notebook.AddPage(page=self.index, text='',imageId=self.indexIcon)
        if info.WIN:
            self.indexCharIcon  = self.parentPanel.iconsListIndex['index_char_win.png']
        else:
            self.indexCharIcon  = self.parentPanel.iconsListIndex['index_char.png']
        #notes
        self.notes = wx.TextCtrl(parent=self.notebook,id=-1,
            style=STYLE_NOTES)
        self.notes.SetHelpText(help.CHILD_NOTES)
        self.notebook.AddPage(page=self.notes, text='',imageId=self.notesIcon)
        #pyChecker
        self.pychecker          = Pycheck.Panel(self.notebook,page=4)
        self.notebook.AddPage(page=self.pychecker, text='',imageId=self.pycheckerIcon)
        #browser
        if not info.DARWIN or wx.VERSION >= (2,6,2):
            self.sidebarAddBrowser()
    def sidebarAddBrowser(self):
        self.notebookLabel.append('Browse')
        self.updateSidebarTab.append(self.updateBrowser)
        browser         = self.browser = Browser(self.notebook, -1, os.path.dirname ( self._fileName ))
        browser.open    = self.onOpenFromBrowser
        self.notebook.AddPage(page=self.browser, text='', imageId=self.browserIcon)

    def __source__(self,fileName,source):
        #notebook
        self.main               = wx.Notebook(id=-1,
                                    parent=self,
                                    #size=wx.Size(5000, 5000),
                                    style=wx.NO_BORDER)
        self.main.childPanel    = self
        self.mainIcons          = wx.ImageList(16,16)
        self.sashIcon           = self.mainIcons.Add(self.parentPanel.icons['source.png'])
        self.umlIcon            = self.mainIcons.Add(self.parentPanel.icons['uml.png'])
        self.documentationIcon  = self.mainIcons.Add(self.parentPanel.icons['documentinfo.png'])
        self.main.AssignImageList(self.mainIcons)

        #sash
        self.sash   = PythonSTC(
            parent      = self.main,
            namespace   = self.parentPanel.shell.interp.locals,
            path        = os.path.dirname(fileName),
            config      = self.parentPanel.config,
            menu        = self.parentFrame.menuBar.edit)
        self.sash.SetHelpText(help.CHILD_SOURCE)
        self.source = self.sash
        #todo: implement this again with sashview
        #if wx.Platform == "__WXMAC__":
        #    self.source = self.sash
        #else:
        #    self.source = self.sash.view
        if fileName:
            self.fileName   = fileName
            self.revert(source)
        else:
            self.fileName   = NEWFILE
            self.notesText  = ''
            self.frame.setTitle()
        self.name   = os.path.basename(self.fileName)
        self.source.EmptyUndoBuffer()
        self.source.Colourise(0, -1)
        self.main.AddPage(page=self.sash, text='Source',imageId=self.sashIcon)

        #uml
        self.uml    = sm.uml.Canvas(parent=self.main,style=wx.FULL_REPAINT_ON_RESIZE)
        self.main.AddPage(page=self.uml, text='Uml',imageId=self.umlIcon)

        #documentation
        self.documentation  = view.documentation.Panel(parent=self.main,id=-1)
        self.main.AddPage(page=self.documentation, text='PyDoc',imageId=self.documentationIcon)

        #events
        eventManager.Register(self.onKillFocus, wx.EVT_KILL_FOCUS, self.source)
        eventManager.Register(self.updateMain,wx.EVT_NOTEBOOK_PAGE_CHANGED,self.main)
    ####Menu--------------------------------------------------------------------
    #---file
    def save(self,fileName=None):
        """Saves the file."""
        if fileName: self.setFileName(fileName)
        if self.fileName==NEWFILE or not(os.path.exists(os.path.dirname(self.fileName))):
            self.saveAs()
        else:
            #get & fix source
            self.source.assertEOL()
            if self.encoding:
                previous        = wx.GetDefaultPyEncoding()
                wx.SetDefaultPyEncoding(self.encoding)
                source          = self.source.GetText()
                wx.SetDefaultPyEncoding(previous)
            else:
                source          = self.source.GetText()
            if self.parentPanel.getValue('StripTrailingSpaces'):
                source          = '\n'.join([l.rstrip() for l in source.split('\n')])
            if not self.dosLines:
                #convert to Unix lines
                source          = source.replace('\r\n','\n')

            #get encoding
            self.getEncoding(source)
            #convert source to unicode
            if type(source) is types.UnicodeType:
                sourceUnicode   = source
            else:
                sourceUnicode   = source.decode(self.encoding)

            #check if source can be encoded, to avoid overwriting with empty file
            try:
                sourceUnicode.encode(self.encoding)
            except Exception, message:
                self.parentPanel.messageError(\
"""Error: SPE is unable to save with "%s" encoding:

%s

Please save your file by Copying&Pasting it into another program
to make sure you don't loose data and contact %s.

Please try then to change the encoding or save it again."""%(self.encoding,message,INFO['author_email']))
                return

            #backup file
            if self.parentPanel.getValue('Backup') and os.path.exists(self.fileName):
                backup  = self.fileName + (sys.platform == 'win32' and '.bak' or '~')
                try:
                    os.remove(backup)
                except:
                    pass
                try:
                    shutil.copy2(self.fileName,backup)
                except:
                    self.setStatus('Warning: could not create backup.')

            #save the file
            try:
                #Note that the mode here must be "wb" to allow
                #line endings to be preserved.
                file        = codecs.open(self.fileName,'wb',self.encoding)
                file.write(sourceUnicode)
                file.close()
            except Exception, message:
                #This is a serious bug (user looses its file) if it would happen
                self.parentPanel.messageError(\
"""Fatal Error: SPE is unable to save with "%s" encoding:

%s

SPE probably overwrote your file with an empty file,
but made a backup of the previous version as "%s".

Please save your file by Copying&Pasting it into another program
to make sure you don't loose data and contact %s.

Please try then to change the encoding or save it again."""%(self.encoding,message,backup,INFO['author_email']))
                return

            #save succesfull
            self.notesSave(file=1)
            self.changed    = 0
            self.saved      = source
            self.parentPanel.recent.add([self.fileName])
            if self.parentPanel.getValue('CheckFileOnSave'):
                if not self.check():
                    self.parentPanel.shell.prompt()
            else:
                self.SetStatusText("File '%s' saved"%self.fileName,1)
            if fileName:
                self.frame.setTitle(os.path.basename(fileName),fileName,colour=wx.WHITE)
            else:
                self.frame.setTitle()
        if os.path.exists(self.fileName):
            self.fileTime   = os.path.getmtime(self.fileName)
        else:
            self.fileTime   = 0
        if self.parentPanel.get('UpdateSidebar')!='realtime':
            self.updateSidebar()

    def saveAs(self):
        defaultDir      = os.path.dirname(self.fileName)
        dlg             = wx.FileDialog(self, "Save As - www.stani.be",
            defaultDir  = defaultDir,
            wildcard    = info.WILDCARD,
            style       = wx.SAVE|wx.OVERWRITE_PROMPT|wx.CHANGE_DIR)
        if dlg.ShowModal() == wx.ID_OK:
            path        = dlg.GetPaths()[0]
            self.save(path)
            if hasattr(self,'browser'):
                self.browser.SetDefaultPath(os.path.dirname(path))
                self.browser.ReCreateTree()
        dlg.Destroy()

    def saveCopy(self):
        """firstly save the current file, then make a copy of it"""
        self.save(self.fileName)
        defaultDir      = os.path.dirname(self.fileName)
        dlg             = wx.FileDialog(self, "Save a Copy - www.stani.be", 
            defaultDir  = defaultDir, 
            wildcard    = info.WILDCARD, 
            style       = wx.SAVE|wx.OVERWRITE_PROMPT|wx.CHANGE_DIR)
        if dlg.ShowModal() == wx.ID_OK:
            path        = dlg.GetPaths()[0]
            try:
                shutil.copyfile(self.fileName, path)
            except IOError:
                self.parentPanel.messageError("Sorry, I was unable to copy %s to %s" % (
                    self.fileName, path))
        dlg.Destroy()

    def saveUmlAs(self):
        self.main.SetSelection(UML_PAGE)
        self.uml.OnDoSave()

    def printUml(self):
        self.main.SetSelection(UML_PAGE)
        self.uml.OnDoPrint()

    def printUmlPreview(self):
        self.main.SetSelection(UML_PAGE)
        self.uml.OnPrintPreview()

    def printUmlSetup(self):
        self.main.SetSelection(UML_PAGE)
        self.uml.OnPrintSetup()

    #---edit
    def comment(self):
        """Comment section"""
        doc = self.source
        sel = doc.GetSelection()
        start = doc.LineFromPosition(sel[0])
        end = doc.LineFromPosition(sel[1])
        if end > start and doc.GetColumn(sel[1]) == 0:
            end = end - 1
        doc.BeginUndoAction()
        for lineNumber in range(start, end + 1):
            firstChar = doc.PositionFromLine(lineNumber)
            doc.InsertText(firstChar, '##')
        doc.SetCurrentPos(doc.PositionFromLine(start))
        doc.SetAnchor(doc.GetLineEndPosition(end))
        doc.EndUndoAction()

    def uncomment(self):
        """Uncomment section"""
        doc = self.source
        sel = doc.GetSelection()
        start = doc.LineFromPosition(sel[0])
        end = doc.LineFromPosition(sel[1])
        if end > start and doc.GetColumn(sel[1]) == 0:
            end = end - 1
        doc.BeginUndoAction()
        for lineNumber in range(start, end + 1):
            firstChar = doc.PositionFromLine(lineNumber)
            if chr(doc.GetCharAt(firstChar)) == '#':
                if chr(doc.GetCharAt(firstChar + 1)) == '#':
                    # line starts with ##
                    doc.SetCurrentPos(firstChar + 2)
                else:
                    # line starts with #
                    doc.SetCurrentPos(firstChar + 1)
                doc.DelLineLeft()
        doc.SetSelection(sel[0],doc.PositionFromLine(end+1))
        doc.SetCurrentPos(doc.PositionFromLine(start))
        doc.EndUndoAction()

    def insert_separator(self):
        from dialogs import separatorDialog
        separatorDialog.create(self).ShowModal()

    def insert_signature(self):
        '''Insert asignature into the current document'''
        signature   = self.parentPanel.get('Signature')
        if not os.path.exists(signature):
            # No or invalid signature in preferences
            dlg = wx.FileDialog(self,
                message="SPE - Choose a signature file",
                #defaultDir  = os.getcwd(),
                #defaultFile = "",
                wildcard    = "Python source (*.py)|*.py|Text (*.txt)|*.txt|All files (*.*)|*.*",
                style       = wx.OPEN | wx.FILE_MUST_EXIST
                )
            answer      = dlg.ShowModal()
            signature   = dlg.GetPath()
            dlg.Destroy()
            if answer == wx.ID_CANCEL: return
        # Have a signature file
        try:
            self.source.ReplaceSelection(open(signature).read()+'\n')
        except:
            self.setStatus('SPE could not open signature "%s"!'% signature)
        return

    def go_to_line(self,scroll=1):
        """Go to line dialog & action"""
        line=self.parentPanel.messageEntry('Enter line number:')
        if line: self.scrollTo(int(line)-1)

    #---View
    def refresh(self):
        if self.parentPanel.redraw:self.parentPanel.redraw()
        if self.parentPanel.get('UpdateSidebar')!='realtime':
            self.updateSidebar()

    def onSash(self,event):
        if self.sidebarHidden:
            self.showSidebar()
        else:
            pos = event.GetSashPosition()
            if pos < self.minSashPosition:
                self.hideSidebar(self.minSashPosition)
            else: event.Skip()

    def toggle_sidebar(self,event):
        pos     = self.GetSashPosition()
        show    = pos <= 5
        if show:
            self.showSidebar()
        else:
            self.hideSidebar(pos)
        if self.frame.menuBar:
            self.frame.menuBar.check_sidebar(show)
        else:
            self.parentFrame.menuBar.check_sidebar(show)

    def hideSidebar(self,pos):
        self.sidebarHidden  = True
        self.sashPosition   = pos
        self.notebook.Hide()
        self.SetSashPosition(1)

    def showSidebar(self):
        self.sidebarHidden  = False
        self.notebook.Show()
        self.SetSashPosition(self.sashPosition)

    #---Tools
    def open_terminal_emulator(self):
        """Open terminal emulator"""
        path,fileName=os.path.split(self.fileName)
        params = {'file':fileName,'path':path}
        terminal=self.parentPanel.get('Terminal')
        if terminal==DEFAULT:
            if info.WIN:
                os.system('start "Spe console - Press Ctrl+Break to stop" /D"%(path)s"'%params)
            elif info.DARWIN:
                sm.osx.startAppleScript([['cd',params['path']]], activateFlag=True)
            elif os.path.isfile('/usr/bin/konsole'):
                os.system('/usr/bin/konsole --caption SPE --workdir "%(path)s" &'%params)
            elif os.path.isfile('/usr/bin/gnome-terminal'):
                os.system('/usr/bin/gnome-terminal --title SPE --working-directory="%(path)s" &'%params)
            else:
                os.system('cd %(path)s;xterm &'%params)
        else:
            os.system(terminal%params)

    def run(self):
        if not self.confirmSave():
            return
        if self.isNew(): return
        from _spe.dialogs.runTerminalDialog import RunTerminalDialog
        runTerminalDialog   = RunTerminalDialog(self.fileName,
                                self.argumentsPrevious,
                                self.inspectPrevious,
                                self.exitPrevious,
                                parent=self.app.parentFrame,
                                id=-1)
        answer              = runTerminalDialog.ShowModal()
        arguments           = runTerminalDialog.arguments.GetValue()
        inspct              = runTerminalDialog.inspect.GetValue()
        exit                = runTerminalDialog.exit.GetValue()
        runTerminalDialog.Destroy()
        if answer == wx.ID_OK:
            self.argumentsPrevious.append(arguments)
            self.inspectPrevious    = inspct
            self.exitPrevious       = exit
            self.run_with_arguments(arguments,inspct,exit,confirm=False)

    def run_with_arguments(self,arguments='', inspct=False, exit=False, confirm=True):
        """Run in terminal emulator"""
        if confirm and not self.confirmSave():
            return
        if self.isNew(): return
        # todo: input stuff from preferences dialog box!
        path, fileName  = os.path.split(self.fileName)
        params          = { 'file':         fileName,
                            'path':         path,
                            'arguments':    arguments,
                            'python':       info.PYTHON_EXEC+['',' -i'][inspct]}
        if exit:
            terminal=self.parentPanel.get('TerminalRunExit')
        else:
            terminal        = self.parentPanel.get('TerminalRun')
        if terminal == DEFAULT:
            if info.WIN:
                if info.WIN98:
                    params['start'] = 'start command'
                else:
                    params['start'] = 'start "SPE - %(file)s - Press Ctrl+Break to stop" /D"%(path)s" cmd'%params
                if exit:
                    os.system('%(start)s /c %(python)s "%(file)s" %(arguments)s'%params)
                else:
                    os.system('%(start)s /k %(python)s "%(file)s" %(arguments)s'%params)
            elif info.DARWIN:
                commandList = [
                        ['cd', params['path']],
                        [params['python'], params['file'], [params['arguments']]]
                        ]
                if exit:
                    commandList.append(['exit'])
                sm.osx.startAppleScript(commandList, activateFlag=True)
            elif os.path.isfile('/usr/bin/gnome-terminal'):
                if exit:
                    os.system("""/usr/bin/gnome-terminal --title "SPE - %(file)s - %(path)s - Press Ctrl+C to stop" --working-directory="%(path)s" -e '%(python)s "%(file)s" %(arguments)s' &"""%params)
                else:
                    os.system("""/usr/bin/gnome-terminal --title "SPE - %(file)s - %(path)s - Press Ctrl+C to stop" --working-directory="%(path)s" -x bash -c "%(python)s \\"%(file)s\\" %(arguments)s; cat" """%params)
            elif os.path.isfile('/usr/bin/konsole'):
                if exit:
                    os.system("""/usr/bin/konsole --caption SPE --workdir "%(path)s" -e %(python)s "%(file)s" %(arguments)s &"""%params)
                else:
                    os.system("""/usr/bin/konsole --caption SPE --noclose --workdir "%(path)s" -e %(python)s "%(file)s" %(arguments)s &"""%params)
            else:
                os.system('%(python)s "%(file)s" %(arguments)s'%params)
        else:
            os.system(terminal%params)

    def check_source_with_pychecker(self):
        """Check source with pychecker"""
        self.pychecker.check()

    #---Blender
    def load_in_blender(self):
        """Load in blender"""
        if self.confirmSave():
            return
        if self.isNew(): return
        if self.parentPanel.checkBlender():
            child   = self.app.childActive
            answer  = child.confirmSave('Only saved contents will be loaded in Blender.')
            if answer:
                import Blender
                #first: let's remove previous copies of this file from Blender's Texts
                for t in filter(lambda x: x.filename == child.fileName, Blender.Text.Get()) : 
                    Blender.Text.unlink(t)
                #second: let's load the just saved file. 
                Blender.Text.Load(child.fileName)
                #BEWARE: this text will not be the selected text in Blender -
                #you still have to select it as the actual.
                self.setStatus(("File successfully loaded as a Blender's Text Editor item, named '%s'" % os.path.basename(child.fileName)))

    def reference_in_blender(self):
        """Reference in blender"""
        if self.confirmSave():
            return
        if self.isNew(): return
        if self.parentPanel.checkBlender():
            import Blender
            child   = self.app.childActive
            msg = "" #message text, that will be displayed on the status bar
            
            #Check: maybe it is a cmpletly new file?:
            if child.fileName==NEWFILE: child.saveAs()
            #It will be still named NEWFILE, if the user has declined to reference it
            if child.fileName==NEWFILE:  return #nothing to do - user has changed his mind

            #First: add the Blender signature at the beginning of the file
            doc = child.source
            #let's move to begining of the file and check, if the Blender signature already exists:
            doc.ScrollToLine(0)
            doc.SetSelection(0,len(BLENDER_REF_TRACE))
            if doc.GetSelectedText() == BLENDER_REF_TRACE:
                msg = "File alread contains reference to Blender menus"
            else:
                template = os.path.join(child.parentPanel.path,BLENDER_REF_SIGNATURE)
                if not os.path.exists(template):
                    child.parentPanel.messageError("Template file:\n%s\nnot found.\n\nCannot reference this script to Blender menu." % template)
                    return
                else: #adding the content of Blender signature file to the source
                    import getpass
                    values = {  'Command':os.path.basename(child.fileName), \
                                'Blender version':Blender.Get('version'), \
                                'User':getpass.getuser()                      }
                    text = open(template).read()
                    text = text % values #apply values into signature
                    doc.SetSelection(0,0)
                    doc.ReplaceSelection(text + "\n")
                    doc.ScrollToLine(0) #It looks better
                    msg = "Blender's signature added."
            # Second: if the script is not located in Blender directory - move it there
            actdir = os.path.dirname(child.fileName)
            if actdir != Blender.Get('uscriptsdir') and actdir != Blender.Get('scriptsdir'):
                if Blender.Get('uscriptsdir')==None:
                    varname = 'scriptsdir'
                else:
                    varname = 'uscriptsdir'
                child.save(os.path.join(Blender.Get(varname),os.path.basename(child.fileName)))
                msg = ("File saved as '%s'" % child.fileName) + ", " + msg
            else:
                child.save() #we have save it, to be referenced in Blender menus.
            #Third: let the script appear in the Blender menu
            Blender.UpdateMenus()
            #Four: feedback for the user
            self.setStatus(msg)

    ####Events------------------------------------------------------------------
    #---Smdi events
    def onActivate(self,event=None):
        if self.frame.menuBar:
            self.frame.menuBar.check_sidebar()
        else:
            self.parentFrame.menuBar.check_sidebar()
            self.updateStatus()
        if hasattr(self,'source'):
            self.source.SetFocus()
            
    def onDeactivate(self,event=None):
        if hasattr(self,'source'):
            self.source.AutoCompCancel()
            self.source.CallTipCancel()

    def onClose(self, event=None):
        if self.confirmSave():
            eventManager.DeregisterWindow(self)
            self.frame.dead = 1
            if len(self.app.children)==1:
                self.parentFrame.menuBar.enable(0)
            return True
        else: return False
        return True

    def onSize(self, event=None):
        self.source.SetFocus()

    #---Panel events
    def onSetFocus(self,event):
        event.Skip()
        self.checkTime()
        try:
            self.source.SetFocus()
        except:
            pass

    def onSetSourceFocus(self,event):
        if self.app.DEBUG:
            print 'Event:  Child: %s: %s.onSetFocus(dead=%s)'%(self.fileName, self.__class__,self.frame.dead)
        event.Skip()
        if self.app.children and self.app.childActive != self and sm.wxp.smdi.MdiSplitChildFrame == self.frame.__class__:
            self.frame.onFrameActivate()

    #---Source events
    def onSourceChange(self,event):
        self.eventChanged = True

    def onSourcePositionChange(self,event=None):
        """Updates statusbar with current position."""

    def idle(self,event=None):
        #if dead, return immediately
        if self.frame.dead or self.parentFrame.dead or not hasattr(self,'source'):
            return
        #update line & column in status
        pos = self.source.GetCurrentPos()
        if pos!= self.position:
            self.updateStatus(pos)
        if self.toggleExploreSelection:
            self.toggleExploreSelection = False
            self.onToggleExploreSelection()
        #only if source is changed...
        if self.eventChanged:
            self.eventChanged   = False
            #title
            if self.changed     == 0:
                self.changed    = 1
                self.frame.setTitle()
            elif self.changed   < 0:
                self.changed+=1
            #sidebar
            if self.parentPanel.get('UpdateSidebar')=='realtime':
                self.updateSidebar()
            #check
            if self.checkBusy:
                return
            if self.parentPanel.get('CheckSourceRealtime')=='compiler':
                thread.start_new(self.idleCheck,())

    def idleCheck(self):
        self.checkBusy  = True
        source          = self.source.GetText()
        length          = len(source)
        source          = source.replace('\r\n','\n') + '\n'
        try:
            tree        = compiler.parse(source)
            warning     = ''
            e           = None
        except Exception, e:
            if hasattr(e,'text'):
                if type(e.text) in types.StringTypes:
                    text= e.text.strip()
                else:
                    text= ''
                warning = '%s: %s at line %s, col %s.'%(self.name,e.msg,e.lineno,e.offset)
            else:
                warning = repr(e)
        if warning  != self.warning:
            #todo: how to implement indicators?!!
            if warning:
                wx.CallAfter(self.setStatus,warning)
                wx.CallAfter(self.statusBar.throbber.playFile,'warning.gif')
                if e and hasattr(e,'lineno') and not (e.lineno is None):
                    wx.CallAfter(self.source.clearError,length)
                    wx.CallAfter(self.source.markError,e.lineno,e.offset)
            else:
                wx.CallAfter(self.setStatus,STATUS)
                wx.CallAfter(self.statusBar.throbber.stop)
                if self.e and hasattr(self.e,'lineno'):
                    wx.CallAfter(self.source.clearError,length)
            self.warning = warning
            self.e       = e
        self.checkBusy = False

    def onKillFocus(self,event=None):
        if self.app.DEBUG:
            print 'Event:  Child: %s: %s.onKillFocus(dead=%s)'%(self.fileName, self.__class__,self.frame.dead)
        try:
            if not (self.frame.dead or self.parentFrame.dead):
                if hasattr(self.parentFrame,'tabs'):
                    tabs        = self.parentFrame.tabs
                    index       = self.frame.getIndex() - tabs.getZero() - 1
                    source      = self.source.GetText()
                    docstring   = '%s\n\n%d lines | %d chars | %d classes | %d defs'%(os.path.dirname(self.fileName),source.count('\n'),len(source),source.count('class '),source.count('def '))
                    if source and source[0] in ["'",'"']:
                        regex = RE_DOCSTRING_FIRST
                    else:
                        regex = RE_DOCSTRING
                    match = regex.search(source)
                    if match:
                        match       = match.group(3).strip('"\'').strip()
                        if match:
                            docstring = '%s\n\n%s'%(docstring,match)
                    tabs.SetPageToolTip(index,docstring.replace('\r\n','\n'),1000,winsize=300)
                if self.parentPanel.get('UpdateSidebar')=='when clicked':
                    self.source.SetSTCFocus(0)
                self.updateSidebar()
            event.Skip()
        except:
            pass

    #---Sidebar update methods & jump events
    def updateSidebar(self,event=None):
        if self.frame.dead: return
        if event:
            tab = event.GetSelection()
            self.notebook.SetPageText(event.GetOldSelection(),'')
            self.notebook.SetPageText(tab,self.notebookLabel[tab])
            event.Skip()
        else:
            tab = self.notebook.GetSelection()
        self.updateSidebarTab[tab]()

    def updateBrowser(self):
        self.browser.update()

    def updateStatus(self,pos=None):
        if hasattr(self,'source'):
            source          = self.source
            if not pos: pos = source.GetCurrentPos()
            self.position   = pos
            line            = source.LineFromPosition(pos)
            column          = source.GetColumn(pos)
            if line != self.line:
                self.line   = line
                self.SetStatusText('Line %05d'%(line+1),STATUS_TEXT_LINE_POS)
            if column != self.column:
                self.column=column
                self.SetStatusText('Column %03d'%column,STATUS_TEXT_COL_POS)
        else:
            self.SetStatusText('',STATUS_TEXT_LINE_POS)
            self.SetStatusText('',STATUS_TEXT_COL_POS)

    def updateTodo(self):
        """Update todo tab in sidebar."""
        #get text
        try:
            text=self.source.GetText().split('\n')
        except:
            return
        #initialize
        tryMode                     = 0 #try, except are false indentations
        hierarchyIndex              = 0
        todoData                    = []
        todoIndex                   = 0
        self.todoMax                = 1
        self.todoHighlights         = []
        self.todoList               = []
        hierarchy                   = [(-1,self.root)]
        self.todo.DeleteAllItems()
        #loop through code wxPython.lib.evtmgr
        for line in range(len(text)):
            l                       = text[line].strip()
            todo_hit                = RE_TODO.match(l)
            first                   = l.split(' ')[0]
            if first=='try:':
                tryMode         += 1
            elif first[:6]=='except':
                tryMode         = max(0,tryMode-1)
            elif first[:7]=='finally':
                tryMode         = max(0,tryMode-1)
            elif todo_hit:
                #todo entry
                task                    = todo_hit.group(1)
                urgency                 = task.count('!')
                self.todoList.append((line,urgency,task))
                item                    = self.todo.InsertStringItem(todoIndex, str(line+1))
                self.todo.SetStringItem(item, 1, str(urgency))
                self.todo.SetStringItem(item, 2, task)
                self.todo.SetItemData(item,line+1)
                #highlights
                newMax                  = max(self.todoMax,urgency)
                if newMax>self.todoMax:
                    self.todoMax        =   newMax
                    self.todoHighlights = [item]
                elif urgency==self.todoMax:
                    self.todoHighlights.append(item)
                todoIndex+=1
        #highlight most urgent todos
        for i in self.todoHighlights:
            if i not in self.previousTodoHighlights:
                self.todo.SetItemBackgroundColour(i,wx.Colour(255,255,0))
##                item=self.todo.GetItem(i)
##                item.SetBackgroundColour(wx.Colour(255,255,0))
##                self.todo.SetItem(item)
        for i in self.previousTodoHighlights:
            if i not in self.todoHighlights:
                self.todo.SetItemBackgroundColour(i,wx.Colour(255,255,255))
##                item=self.todo.GetItem(i)
##                item.SetBackgroundColour(wx.Colour(255,255,255))
##                self.todo.SetItem(item)
        self.previousTodoHighlights = self.todoHighlights
        self.todo.Update()

    def updateIndex(self):
        """Update index tab in sidebar."""
        #get code
        try:
            text            = self.source.GetText().split('\n')
        except:
            return
        #initialize
        tryMode             = 0
        hierarchyIndex      = 0
        self.indexData      = []
        #loop through code
        for line in range(len(text)):
            l               = text[line].split('#')[0].replace(':','').strip()
            first           = l.split(' ')[0]
            if first=='try:':
                tryMode     += 1
            elif first[:6]  =='except':
                tryMode         = max(0,tryMode-1)
            elif first[:7]  == 'finally':
                tryMode         = max(0,tryMode-1)
            elif first in ['class','def'] and l[:8]!='__init__':
                if first    == 'class':
                    colour  = wx.Colour(255,0,0)
                    icon    = 'class.png'
                    l       = l.replace('class ','')
                else:
                    colour  = wx.Colour(0,0,255)
                    icon    = 'def.png'
                    l       = l.replace('def ','')
                self.indexData.append((l.replace('_','').strip().upper(),l,line+1,colour,
                    self.parentPanel.iconsListIndex[icon],self.fileName))
        #make index tab
        self.indexData.sort()
        firstLetter         = ''
        self.index.DeleteAllItems()
        for element in self.indexData:
            stripped, entry, line, colour, icon, fileName = element
            if stripped[0]!=firstLetter:
                firstLetter = stripped[0]
                item        = self.index.InsertImageStringItem(MAXINT, ' ', self.indexCharIcon)
                self.index.SetStringItem(item,1,firstLetter)
                self.index.SetItemBackgroundColour(item,(230,230,230))
            item            = self.index.InsertImageStringItem(MAXINT, str(line), icon)
            self.index.SetStringItem(item, 1, entry)
            self.index.SetItemData(item,line-1)
            self.index.SetItemTextColour(item,colour)
        self.index.Update()
        #if self.parentPanel.indexVisible...

    def updateExplore(self,uml=0):
        """Updates explore in sidebar."""
        #get text
        try:
            text=self.source.GetText().split('\n')
        except:
            return
        #initialize
        if uml:
            self.umlClass   = None
            previous    = 0
        classes         = {}
        n               = len(text)
        tryMode         = 0
        hierarchyIndex  = 0
        hierarchy       = [(-1,self.root)]
        separators      = []
        self.encoding   = None
        self.explore.CollapseAndReset(self.root)
        for line in range(len(text)):
            l           = text[line].strip()
            first       = l.split(' ')[0]
            sepa_hit    = RE_SEPARATOR.match(l)
            sepb_hit    = RE_SEPARATOR_HIGHLIGHT.match(l)
            encode_hit  = False
            if line < 3:
                if line == 0 and isUtf8(l):
                    self.encoding = "utf8"
                    encode_hit = True
                else:
                    enc = RE_ENCODING.search(l)
                    if enc:
                        self.encoding = str(enc.group(1))
                        encode_hit = True
            if first in ['class','def','import'] or encode_hit or (first == 'from' and 'import' in l):
                if 1 or l.find('(')!=-1 or l.find(':') !=-1 or first in ['from','import'] or encode_hit:
                    #indentation--------------------------------------------
                    indentation         = max(0,len(text[line])-
                        len(text[line].lstrip())-tryMode*4)
                    #situate in hierachy------------------------------------
                    hierarchyIndex      = 0
                    while hierarchyIndex+1<len(hierarchy) and \
                            hierarchy[hierarchyIndex+1][0]<indentation:
                        hierarchyIndex  += 1
                    hierarchy=hierarchy[:hierarchyIndex+1]
                    if uml and hierarchyIndex<=previous:
                        umlAdd(classes,self.umlClass)
                        self.umlClass    = None
                    #get definition-----------------------------------------
                    if encode_hit:
                        l = self.encoding
                    else:
                        l = l.split('#')[0].strip()
                    i=1
                    if not(first in ['import','from'] or encode_hit):
                        search  = 1
                        rest    = ' '
                        while search and l[-1] != ':' and i+line<n:
                            #get the whole multi-line definition
                            next = text[line+i].split('#')[0].strip()
                            if next.find('class')==-1 and next.find('def')==-1 and next.find('import')==-1:
                                rest    += next
                                i       += 1
                            else:
                                search = 0
                        if rest[-1] == ':':l+= rest
                    #put in tree with color---------------------------------
                    l=l.split(':')[0].replace('class ','').replace('def ','').strip()
                    if separators:
                        self.appendSeparators(separators,hierarchy,hierarchyIndex,uml)
                        separators      = []
                    if l:
                        item                = self.explore.AppendItem(hierarchy[hierarchyIndex][1],l,data=line)
                        intensity=max(50,255-indentation*20)
                        if encode_hit:
                            colour              = wx.Colour(intensity-50,0,intensity-50)
                            icon                = iconExpand = 'encoding.png'
                        elif first == 'class':
                            if uml:
                                umlAdd(classes,self.umlClass)
                                self.umlClass   = sm.uml.Class(name=l,data=line)
                                previous        = hierarchyIndex
                            colour              = wx.Colour(intensity,0,0)
                            icon                = 'class.png'
                            iconExpand          = 'class.png'
                        elif first in ['import','from']:
                            colour              = wx.Colour(0,intensity-50,0)
                            icon                = iconExpand = 'import.png'
                        else:
                            if uml and self.umlClass: self.umlClass.append(l)
                            colour          = wx.Colour(0,0,intensity)
                            icon            = iconExpand = 'def.png'
                        self.explore.SetItemBold(item,False)
                        self.explore.SetItemTextColour(item,colour)
                        self.explore.SetItemImage(item,
                            self.parentPanel.iconsListIndex[icon],
                            which=wx.TreeItemIcon_Normal)
                        if first=='class':
                            self.explore.SetItemImage(item,
                                self.parentPanel.iconsListIndex[iconExpand],
                                which       = wx.TreeItemIcon_Expanded)
                        hierarchy.append((indentation,item))
            elif sepa_hit:
                #separator
                pos = sepa_hit.end()
                colours=l[pos:].split('#')
                if len(colours)==3:
                    s       = sm.rstrip(colours[0],'_')
                    fore    = '#'+colours[1][:6]
                    back    = '#'+colours[2][:6]
                else:
                    s=sm.rstrip(l[pos:],'-')
                    fore=wx.Colour(128,128,128)
                    back=None
                if s.strip(): separators.append((s,line,fore,back))
            elif sepb_hit:
                #highlighted separator (yellow)
                pos = sepb_hit.end()
                s   = sm.rstrip(l[pos:],'-')
                if s.strip(): separators.append((s,line,wx.Colour(0,0,0),wx.Colour(255,255,0)))
            elif first=='try:':
                tryMode         += 1
            elif first[:6]=='except':
                tryMode         = max(0,tryMode-1)
            elif first[:7]=='finally':
                tryMode         = max(0,tryMode-1)
        self.appendSeparators(separators,hierarchy,hierarchyIndex,uml)
        if uml: umlAdd(classes,self.umlClass)
        #expand root of explore
        #self.explore.Expand(self.root)
        #if self.parentPanel.exploreVisible: ...
        self.explore.Update()
        return classes

    def updateMain(self,event=None):
        if event:
            tab = event.GetSelection()
            event.Skip()
            if tab == 0:
                self.source.SetFocus()
            elif tab == 1:
                self.uml.DrawUml(classes=self.updateExplore(uml=1))
            elif tab == 2:
                self.documentation.main()
        else:
            tab = self.notebook.GetSelection()
        self.updateSidebarTab[tab]()

    def refreshMain(self):
        pos = self.GetSashPosition()
        self.sashDelta *= -1
        self.SetSashPosition(pos+self.sashDelta, redraw=1)

    def doNothing(self):
        pass

    def appendSeparators(self,separators,hierarchy,hierarchyIndex,uml):
        explore = self.explore
        for separator in separators:
            label,line,fore,back=separator
            sep=explore.AppendItem(hierarchy[hierarchyIndex][1],label,data=line)
            explore.SetItemBold(sep)
            explore.SetItemTextColour(sep,fore)
            if back:explore.SetItemBackgroundColour(sep,back)
            explore.SetItemImage(sep,self.parentPanel.iconsListIndex['separator.png'])
            if uml and self.umlClass: self.umlClass.append(label,t=sm.uml.SEPARATOR)

    def onSourceFromExplore(self,event):
        """Jump to source line by clicking class or function in explore."""
        line=self.explore.GetPyData(event.GetItem())
        self.scrollTo(line,select='line')

    def onToggleExplore(self,event):
        """Toggle item between collapse and expand."""
        self.explore.Toggle(event.GetItem())

    def onToggleExploreTree(self,event):
        event.Skip()
        self.toggleExploreSelection = True

    def onToggleExploreSelection(self):
        self.explore.Toggle(self.explore.GetSelection())

    def onOpenFromBrowser(self, fname):
        if os.path.splitext(fname)[-1] in SPE_ALLOWED_EXTENSIONS:
            self.parentPanel.openList([fname])
        else:
            os.startfile(fname)

    def onSourceFromTodo(self,event):
        """Jump to source line by clicking task in todo."""
        line=event.GetData()
        self.scrollTo(line-1,scroll=1,select='line')

    def onSourceFromIndex(self,event):
        """Jump to source line by clicking task in todo."""
        line=event.GetData()
        self.scrollTo(line,scroll=1,select='line')

#---methods---------------------------------------------------------------------
    def isNew(self):
        return self.fileName == NEWFILE
    
    def check(self):
        pythonFile=(os.path.splitext(self.fileName)[1].lower() in ['.py','.pyw'])
        if pythonFile:
            from sm.scriptutils import CheckFile
            return CheckFile(self.fileName,jump=self.parentPanel.openList,
                status=self.setStatus)
        else: return 1

    def checkTime(self):
        if (not (self.frame.dead or self.parentFrame.dead)) and \
            hasattr(self,'fileName') and os.path.exists(self.fileName):
            try:
                pos=self.source.GetCurrentPos()
                fileTime=os.path.getmtime(self.fileName)
                if fileTime>self.fileTime:
                    #file is modified
                    self.fileTime=fileTime
                    baseName=os.path.basename(self.fileName)
                    message=baseName+' is modified externally.\nDo you want to reload it%s?'
                    if  (self.changed>0 and self.parentPanel.messageConfirm(message%' and loose current changes')) or\
                    (not self.changed>0 and (self.parentPanel.getValue('AutoReloadChangedFile') or self.parentPanel.messageConfirm(message%''))):
                        self.revert()
                        self.source.GotoPos(pos)
                        return 1
            except:
                return 0


    def confirmSave(self, message=''):
        self.notesSave(file=1)
        if self.changed>0 or self.fileName == NEWFILE:
            self.Raise()
            message+='\nSave changes to "%s"?'%self.fileName
            answer=self.parentPanel.messageCancel(message)
            if answer==wx.ID_CANCEL:
                return 0
            elif self.parentPanel.messageIsOk(answer):
                self.save()
                return 1
            else:return 1
        else:return 1

    def refreshTitle(self):
        if self.app.DEBUG:
            print 'Method: Child: %s.refreshTitle("%s")'%(self.__class__,self.fileName)
        self.frame.setTitle()

    def revert(self,source=None):
        if not source:
            try:
                sourceFile      = open(self.fileName,'rb')
                source          = sourceFile.read()
                sourceFile.close()
                if self.parentPanel.getValue('ConvertTabsToSpaces'):
                    source=source.replace('\t',' '.ljust(self.parentPanel.getValue('TabWidth')))
            except IOError:
                source          = ''
        self.getEncoding(source)
        try:
            if source and self.encoding:
                #read the source
                sourceFile      = codecs.open(self.fileName,'rb',self.encoding)
                source          = sourceFile.read()
                sourceFile.close()
                #set it with the right encoding
                previous        = wx.GetDefaultPyEncoding()
                wx.SetDefaultPyEncoding(self.encoding)
                self.source.SetText(source)
                wx.SetDefaultPyEncoding(previous)
            else:
                self.source.SetText(source)
        except Exception, message:
            self.SetStatusText("Unicode Error for '%s' (%s)"%(self.fileName, message),1)
        self.source.assertEOL()
        if os.path.exists(self.fileName) and self.fileName != NEWFILE:
            self.fileTime   = os.path.getmtime(self.fileName)
        else:
            self.fileTime   = 0
        try:
            self.notesText=open(self.notesFile()).read()
        except:
            self.notesText=''
        self.notes.SetValue(self.notesText)
        self.frame.setTitle()
        self.changed=0

    def setFileName(self,fileName):
        self.fileName   = fileName
        self.name       = os.path.basename(self.fileName)
##        if fileName not in self.parentPanel.workspace['openfiles']:
##            self.name   = '~'+self.name
        index           = self.frame.getIndex()
        mdi             = self.app.mdi
        if not mdi:index+= 1
        if hasattr(self.frame,'tabs'):
            self.frame.tabs.SetPageText(index,self.name)
        self.frame.setTitle()
        if not mdi:
            for child in self.app.children:
                child.frame.tabs.SetPageText(index,self.name)

    def setStatus(self,text,i=1):
        self.SetStatusText(text,i)

    def sidebarVisible(self):
        return self.GetSashPosition() > 5

    def scrollTo(self,line=0,column=0,select='pos',scroll=0):
        source  = self.source
        source.EnsureVisible(line)
        #line    = source.VisibleFromDocLine(line)
        linePos = source.PositionFromLine(line)
        pos     = linePos+column
        if select=='line':
            source.SetSelection(linePos, source.GetLineEndPosition(line))
        else: #select=='pos':
            source.GotoPos(pos)
        source.ScrollToLine(line)
        source.ScrollToColumn(0)
        source.SetFocus()

    def notesFile(self):
        return os.path.splitext(self.fileName)[0]+'_notes.txt'

    def notesSave(self,file=0):
        if not hasattr(self,'notes'):
            return
        self.notesText=self.notes.GetValue()
        if file:
            try:
                if not self.notesText:
                    os.remove(self.notesFile())
                else:
                    f=open(self.notesFile(),'w')
                    f.write(self.notesText)
                    f.close()
            except (OSError, IOError):
                pass # tolerate IO failure otherwise app hangs

    def selectLine(self,line):
        source=self.source

    def getEncoding(self,source):
        if isUtf8(source):
            self.encoding = "utf8"
            return
        first2lines         = "".join(source.split("\n")[:2])
        encode_hit          = RE_ENCODING.search(first2lines)
        if encode_hit:
            #find in source
            self.encoding   = encode_hit.group(1)
        else:
            #get default values
            if self.parentPanel.defaultEncoding == '<default>':
                #wx.GetDefaultPyEncoding() when SPE was launched
                self.encoding   = INFO['encoding']
            else:
                #as in preferences
                self.encoding   = self.parentPanel.defaultEncoding
        if self.encoding == 'ascii':
            #avoid obvious trap
            try:
                str(source)
            except:
                self.setStatus('Warning: SPE uses "utf8" instead of "ascii" codec.')
                self.encoding   = 'utf8'
        self.encoding = str(self.encoding)

class DropOpen(wx.FileDropTarget):
    """Opens a file when dropped on parent frame."""
    def __init__(self,openList):
        wx.FileDropTarget.__init__(self)
        self.openList   = openList
    def OnDropFiles(self,x,y,fileNames):
        fileNames       = [script for script in fileNames
            if os.path.splitext(script)[-1].lower() in SPE_ALLOWED_EXTENSIONS]
        if fileNames:
            self.openList(fileNames)
            return 1
        else:return 0
