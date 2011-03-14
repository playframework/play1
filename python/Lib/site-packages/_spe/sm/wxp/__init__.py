#(c)www.stani.be (read __doc__ for more information)                            
import os, types, sys
import sm
INFO=sm.INFO.copy()

INFO['author']      = 'www.wxpython.org'
INFO['copyright']   = '(c) www.wxpython.org'
INFO['title']       = INFO['titleFull'] = 'wxPython source code'
INFO['description'] =\
"""Changes:
    may 2003: adapted by www.stani.be for spe
"""

__doc__=INFO['doc']%INFO
#_______________________________________________________________________________

try:
    True
except NameError:
    True = 1==1
    False = 1==0
#===Constants==================================================================
MODULE_ERROR="Error: Module(s) %s required, but not installed (%s)!"
WXPYTHON_URL="www.wxpython.org"

#===wxPython: dirDialog========================================================
wx=crust=shell=filling=None#for the demo down


import wx
from wx.py import crust,shell, filling

#---Dialogs
class FileDir:
    def getFile(self,control, style = wx.OPEN|wx.DD_NEW_DIR_BUTTON):
        default = control.GetValue()
        if not default:
            default        = "D:\\"
        defaultDir, defaultFile = os.path.split(default)   
        dlg = wx.FileDialog(self,defaultDir = defaultDir, defaultFile = defaultFile, style = style)
        if dlg.ShowModal() == wx.ID_OK:
            path        = dlg.GetPath()
            control.SetValue(path)
        dlg.Destroy()

    def getDir(self,control,style=wx.DD_NEW_DIR_BUTTON):
        path        = control.GetValue()
        if not path:
            path    = "D:\\"
        if not os.path.isfile(path):
            path = os.path.dirname(path)
        dlg         = wx.DirDialog(self,defaultPath=os.path.dirname(path)   ,style=style)
        if dlg.ShowModal() == wx.ID_OK:
            path    = dlg.GetPath()
            control.SetValue(path)
        dlg.Destroy()
    
def fileDialog(defaultPath='',defaultFile='',message='www.stani.be', 
        wildcard = "*.*",open=1,readOnly=1,overwrite=0,multiple=0,changeDir=0):
    """Launchs file selector dialog."""
    style=wx.OPEN
##    if open:            style|=wx.OPEN
##    else:               style|=wx.SAVE
##    if not readOnly:    style|=wx.HIDE_READONLY
##    if not overwrite:   style|=wx.OVERWRITE_PROMPT
##    if multiple:        style|=wx.MULTIPLE
##    if changeDir:       style|=wx.CHANGE_DIR 
    dlg=wx.FileDialog(None, message = message, defaultDir = defaultPath,
        defaultFile = defaultFile, wildcard = wildcard, style = style)
    print dlg.ShowModal
    if dlg.ShowModal() == wx.ID_OK:path=dlg.GetFileName()
    else:path=''
    dlg.Destroy()
    return path

def dirDialog(defaultPath='',message="www.stani.be",newDir=1):
    """Launchs a directory selector dialog (wxpython)."""
    style=wx.DEFAULT_DIALOG_STYLE
    if newDir:style|=wx.DD_NEW_DIR_BUTTON
    dlg = wx.DirDialog(None,message=message,defaultPath=defaultPath,style=style)
    if dlg.Show() == wx.ID_OK:path=dlg.GetPath()
    else:path=''
    dlg.Destroy()
    return path

def browse(object,parent=None):
    """Browse object with pyfilling"""
    from wx.py.filling import FillingFrame
    filling=FillingFrame(parent=parent, id=-1, title='PyFilling', 
             pos=wx.DefaultPosition, size=wx.Size(600,300), 
             style=wx.DEFAULT_FRAME_STYLE, rootObject=object, 
             rootLabel=str(object), rootIsNamespace=0, static=0)
    filling.Show(1)
        
def message(message='',caption='www.stani.be'):
    dlg = wx.MessageDialog(None, message=message,caption=caption, style=wx.OK )
                      #wx.YES_NO | wx.NO_DEFAULT | wx.CANCEL | wx.ICON_INFORMATION)
    dlg.ShowModal()
    dlg.Destroy()

#---Bitmaps, Icons & Images
def bitmap2Icon(bitmap):
    icon = wx.EmptyIcon()
    icon.CopyFromBitmap(bitmap)
    return icon

#---PyCrust
class SmFilling(filling.Filling):
    """Tweaked PyCrust Filling based on wxSplitterWindow."""
    def __init__(self, parent, id=-1, pos=wx.DefaultPosition, 
                 size=wx.DefaultSize, style=wx.SP_3D,
                 name='Filling Window', rootObject=None,
                 rootLabel=None, rootIsNamespace=0, static=False,filling=filling,welcome=""):
        """Create a PyCrust Filling instance."""
        wx.SplitterWindow.__init__(self, parent, id, pos, size, style, name)
        self.tree = filling.FillingTree(parent=self, rootObject=rootObject, 
                                rootLabel=rootLabel,
                                rootIsNamespace=rootIsNamespace,
                                static=static)
        self.text = filling.FillingText(parent=self, static=static)
        self.SplitVertically(self.tree, self.text, 200)
        self.SetMinimumPaneSize(1)
        # Override the filling so that descriptions go to FillingText.
        self.tree.setText=self.setText
        
        #---custom---
        self.welcome=welcome
        self._max=1000
        self.rootObject=rootObject
        
        # Display the root item.
##        self.tree.SelectItem(self.tree.root)
        #self.tree.display()

    def setText(self,text):
        obj = self.tree.GetPyData(self.tree.item)
        if obj==self.rootObject:self.text.SetText(self.welcome)
        else:
            if type(obj) in [types.DictionaryType,types.ListType,types.DictType]\
                    and len(text)>self._max:
                text=text[:self._max]+' ...\n\nExplore nodes to see more information.'
            self.text.SetText(text)

class SmCrust(crust.Crust):
    """Crust Crust based on wxSplitterWindow."""

    name = 'SmCrust Crust'
    revision = crust.__revision__

    def __init__(self, parent, id=-1, pos=wx.DefaultPosition, 
                 size=wx.DefaultSize, style=wx.SP_3D,
                 name='Crust Window', rootObject=None, rootLabel=None,
                 rootIsNamespace=True, intro='', locals=None, 
                 InterpClass=None, crust=crust, tabs={},welcome="",*args, **kwds):#custom
        """Create a PyCrust Crust instance."""
        wx.SplitterWindow.__init__(self, parent, id, pos, size, style, name)
        self.shell = crust.Shell(parent=self, introText=intro, 
                           locals=locals, InterpClass=InterpClass, 
                           *args, **kwds)
        self.editor = self.shell
        self.notebook = wx.Notebook(parent=self, id=-1)
##        self.shell.interp.locals['notebook'] = self.notebook
        #Namespace
        if rootObject is None:
            rootObject = self.shell.interp.locals
        self.filling = SmFilling(parent=self.notebook, 
                               rootObject=rootObject, 
                               rootLabel=rootLabel, 
                               rootIsNamespace=rootIsNamespace,
                               welcome=welcome)
        self.notebook.AddPage(page=self.filling, text='Namespace', select=True)

        
        #custom start
        tabKeys=tabs.keys()
        tabKeys.sort()
        for tab in tabKeys:
            tabItem=tabs[tab]
            if type(tabs[tab]).__name__ in ['tuple','list']:
                tabObject=tabItem[0]
                tabWelcome=tabItem[1]
                if len(tabItem)>2:tabLabel=tabItem[2]
                else:tabLabel='Ingredients'
            else:
                tabObject=tabItem
                tabWelcome=""
                tabLabel='Ingredients'
            tabFilling=SmFilling(parent=self.notebook, 
                                       rootObject=tabObject, 
                                       rootLabel=tabLabel, 
                                       rootIsNamespace=rootIsNamespace,
                                       welcome=tabWelcome
                                     )
            self.notebook.AddPage(page=tabFilling, text=tab, select=False)
        #custom end---

            
        #Display
        #self.display = crust.Display(parent=self.notebook)
        #self.notebook.AddPage(page=self.display, text='Display')
        
        #SessionListing
        self.sessionlisting = crust.SessionListing(parent=self.notebook)
        self.notebook.AddPage(page=self.sessionlisting, text='Session')
        #Calltip
        self.calltip = crust.Calltip(parent=self.notebook)
        self.notebook.AddPage(page=self.calltip, text='Calltip')
        self.dispatcherlisting = crust.DispatcherListing(parent=self.notebook)
        self.notebook.AddPage(page=self.dispatcherlisting, text='Dispatcher')

        
        #This is not necessary---
##        from wxd import wx_
##        self.wxdocs = SmFilling(parent=self.notebook, 
##                              rootObject=wx_,
##                              rootLabel='wx', 
##                              rootIsNamespace=False,
##                              static=True)
##        self.notebook.AddPage(page=self.wxdocs, text='wxPython Docs')
##        from wxd import stc_
##        self.stcdocs = SmFilling(parent=self.notebook, 
##                               rootObject=stc_.StyledTextCtrl,
##                               rootLabel='StyledTextCtrl', 
##                               rootIsNamespace=False,
##                               static=True)
##        self.notebook.AddPage(page=self.stcdocs, text='StyledTextCtrl Docs')
        self.SplitHorizontally(self.shell, self.notebook, 300)
        self.SetMinimumPaneSize(1)

class SmCrustFrame(crust.CrustFrame):
    """Frame containing all the PyCrust components."""
    
    name = 'PyCrust Frame'
    revision = crust.__revision__

    def __init__(self, parent=None, id=-1, title='PyCrust tweaked by www.stani.be', 
                 pos=wx.DefaultPosition, size=wx.DefaultSize, 
                 style=wx.DEFAULT_FRAME_STYLE, 
                 rootObject=None, rootLabel=None, rootIsNamespace=True, 
                 locals=None, InterpClass=None, crust=crust,tabs={},*args, **kwds):#custom
        """Create a PyCrust CrustFrame instance."""
        wx.Frame.__init__(self, parent, id, title, pos, size, style)
        intro = 'PyCrust %s - The Flakiest Python Shell' % crust.VERSION
        intro += '\nSponsored by Orbtech - '
        intro += 'Your source for Python programming expertise.'
        self.CreateStatusBar()
        self.SetStatusText(intro.replace('\n', ', '))
        import images
        self.SetIcon(images.getPyCrustIcon())
        self.crust = SmCrust(parent=self, intro=intro, 
                           rootObject=rootObject, 
                           rootLabel=rootLabel, 
                           rootIsNamespace=rootIsNamespace, 
                           locals=locals, 
                           InterpClass=InterpClass, tabs=tabs,*args, **kwds)
        self.shell = self.crust.shell
        # Override the filling so that status messages go to the status bar.
        self.crust.filling.tree.setStatusText = self.SetStatusText
        # Override the shell so that status messages go to the status bar.
        self.crust.shell.setStatusText = self.SetStatusText
        # Fix a problem with the sash shrinking to nothing.
        self.crust.filling.SetSashPosition(200)
##        self.createMenus()
##        wx.EVT_CLOSE(self, self.OnCloseWindow)
        # Set focus to the shell editor.
        self.crust.shell.SetFocus()

def Browser(self):
    from wxPython.lib.activexwrapper import MakeActiveXClass
    import win32com.client.gencache
    try:
        browserModule = win32com.client.gencache.EnsureModule("{EAB22AC0-30C1-11CF-A7EB-0000C05BAE0B}", 0, 1, 1)
    except:
        raise ImportError("IE4 or greater does not appear to be installed.")
    return MakeActiveXClass(browserModule.WebBrowser,eventObj = self)
#---Frames
class Motion:
    def __init__(self):
        wx.EVT_LEFT_DOWN(self,self.onLeftDown)
        wx.EVT_RIGHT_DOWN(self,self.onRightDown)
        wx.EVT_MOTION(self,self.onMotion) 
        
    def onLeftDown(self,event):
        self.dragStartPos = event.GetPosition()
        
    def onRightDown(self,event):
        self.Close()
        
    def onMotion(self,event):
        if event.Dragging():
            self.SetPosition(self.GetPosition() + event.GetPosition() - self.dragStartPos)
            
class VideoFrame(wx.Frame):
    """(c)Doug Holton
    Based on the code samples above and code by Kevin Altis. 
    Code from: http://wiki.wxpython.org/index.cgi/IntegratingPyGame"""
    def play(self, filename):
        import sys
        ##Note we call the GetHandle() method of a control in the window/frame, not the wxFrame itself
        self.hwnd = self.GetChildren()[0].GetHandle()
        if sys.platform == "win32":
            os.environ['SDL_VIDEODRIVER'] = 'windib'
        os.environ['SDL_WINDOWID'] = str(self.hwnd) #must be before init

        ## NOTE WE DON'T IMPORT PYGAME UNTIL NOW.  Don't put "import pygame" at the top of the file.
        import pygame
        pygame.display.init()

        self.movie = pygame.movie.Movie(filename)

        if self.movie.has_video():
            w,h = self.movie.get_size()
            if w<=0 or h<=0: w,h = 1,1
        else:
            #? need something to display if audio only.
            #We can't have a 0,0 canvas, pygame/SDL doesn't like that.
            w,h = 1,1
        self.display = pygame.display.set_mode((w,h)) #size no matter

        self.movie.set_display(self.display)
        self.movie.play()
        
class FrameApp(wx.App):
    def __init__(self,Frame, **keyw):
        self.Frame = Frame
        self.keyw  = keyw
        wx.App.__init__(self, redirect=0)

    def OnInit(self):
        wx.InitAllImageHandlers()
        frame = self.Frame(parent=None, id=-1, **self.keyw)
        frame.Show(True)
        self.SetTopWindow(frame)
        self.frame = frame
        return True
    
def frameApp(Frame,**keyw):
    application = FrameApp(Frame,**keyw)
    application.MainLoop()
#---Html
import webbrowser

try:
    5/0
    from wx.lib.iewin import IEHtmlWindow as _HtmlWindow
    IE = 1
except:
    from wx.html import HtmlWindow as _HtmlWindow
    IE = 0

class HtmlWindow(_HtmlWindow):
    """Customized wxHtmlwindow, so that the links are opened in an external webbrowser."""
    def __init__(self, parent, id, **keyw):
        _HtmlWindow.__init__(self, parent, id, style=wx.NO_FULL_REPAINT_ON_RESIZE,**keyw)
    def OnLinkClicked(self, linkinfo):
        webbrowser.open(linkinfo.GetHref(), 1)
    def SetPage(self,code):
        if IE:
            _HtmlWindow.LoadString(self,code)
        else:
            _HtmlWindow.SetPage(self,code)
    def LoadPage(self,page):
        if IE:
            _HtmlWindow.LoadUrl(self,page)
        else:
            _HtmlWindow.LoadPage(self,page)

#---Panels
class PanelApp(wx.App):
    def __init__(self, Panel, title="www.stani.be", redirect=0, ToolBar = None, icon = None,**keyw):
        self.Panel      = Panel
        self.title      = title
        self.keyw       = keyw
        self.ToolBar    = ToolBar
        self.icon   = icon
        wx.App.__init__(self, redirect=redirect)

    def OnInit(self):
        wx.InitAllImageHandlers()
        self.frame  = frame = wx.Frame(None, -1, self.title, pos=(50,50), 
            size=(300,300), style=wx.NO_FULL_REPAINT_ON_RESIZE|wx.DEFAULT_FRAME_STYLE)
        if self.icon:
            self.SetIcon(self.icon)
        if self.ToolBar:
            frame.toolBar = self.ToolBar(parent=frame,id=-1)
            frame.SetToolBar(frame.toolBar)
        try:
            self.panel = panel = self.Panel(frame,id=-1,**self.keyw)
        except:
            self.panel = panel = self.Panel(frame,**self.keyw)
        panel.Destroy=frame.Destroy
        if self.ToolBar:
            panel.toolBar       = frame.toolBar
            panel.toolBar.panel = panel
        frame.Fit()
        frame.Move(panel.GetPosition())
        self.SetTopWindow(frame)
        frame.Show(True)
        return True
        
    def SetIcon(self,icon):
        self.frame.SetIcon(wx.Icon(self.icon,wx.BITMAP_TYPE_ICO))
    
def panelApp(Panel,title='www.stani.be',**keyw):
    application = PanelApp(Panel,title,**keyw)
    application.MainLoop()

def testPanelApp():
    from sm.todo.wxPanelTodo import PanelTodo as todo
    panelApp(todo)

#---main
if __name__=='__main__':
    testPanelApp()
