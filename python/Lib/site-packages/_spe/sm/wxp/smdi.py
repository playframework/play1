####(c)www.stani.be-------------------------------------------------------------

try:
    import sm, sm.osx
    INFO=sm.INFO.copy()

    INFO['title']     = INFO['titleFull'] = 'Sdi/Mdi Framework'

    INFO['description']=\
    """Framework which makes it easy to switch between Sdi (Linux/Mac)
    and Mdi (Windows).
    """

    __doc__=INFO['doc']%INFO
except:
    __doc__="Stani's Multiple Document Interface (c)www.stani.be"

"""
Attributes of Application:
    - properties:
        - children
        - config
        - DEBUG
        - imagePath
        - mdi
        - title
        - parentFrame
        - parentPanel
        - pos
        - size
        - style
    - methods:
        - SetMdi
    - classes:
        - ChildFrame
        - ChildPanel
        - MenuBar
        - ParentFrame
        - ParentPanel
        - StatusBar
        - ToolBar

Attributes of Frame:
    - properties:
        - app
        - dead
        - menuBar
        - parentFrame
        - toolBar
    - methods:
        - getIndex
        - (maximize)
        - setTitle
        - SetStatusText
    - events:
        - bindTabs
        - unbindTabs
        - onFrameActivate
        - onFrameClose
        - onFrameMove
        - onFrameSize
        - (onFrameTab)
    - classes:
        - Panel

Attributes of Panel:
    - properties:
        - changed
        - frame
        - title
        - parentFrame
        - parentPanel
    - methods:

Attributes of MenuBar:
    - properties:
        - app
        - frame
        - parentFrame
        - parentPanel
        - toolBar

Attributes of ToolBar:
    - properties:
        - app
        - frame
        - menuBar

Todo:
- MDI_TABS IS NOT WORKING!! THIS SHOULD BE FIXED
- icon support
"""

####Modules
import  os, sys, pprint
import  wx
from    wx.lib.evtmgr import eventManager
import singleApp
import NotebookCtrl
wx_Notebook = NotebookCtrl.NotebookCtrl
#import sm.spy

####Constants
#values
SDI                 = 0
MDI_SASH            = 1
MDI_SASH_TABS       = 2
MDI                 = 3
MDI_TABS            = 4
MDI_SPLIT           = 5
#descriptions
SDI_MAC             = "single with tabs (mac, linux, windows)"
MDI_SASH_WIN        = "multiple with sash (windows)"
MDI_SASH_TABS_LINUX = "multiple with sash & tabs (linux)"
MDI_SASH_TABS_WIN   = "multiple with sash & tabs (windows default)"
MDI_WIN             = "multiple with palette (windows)"
MDI_MAC             = "single with palette (mac)"
MDI_TABS_LINUX      = "multiple with tabs (linux)"
MDI_TABS_WIN        = "multiple with tabs (windows)"
MDI_TABS_MAC        = "single with tabs (mac)"
MDI_SPLIT_ALL       = "multiple with sash & tabs (mac default,linux default,windows)"

DEFAULT             = "<default>"
DI                  = {SDI_MAC                  : SDI,
                       MDI_SASH_WIN             : MDI_SASH,
                       MDI_SASH_TABS_LINUX      : MDI_SASH,
                       MDI_SASH_TABS_WIN        : MDI_SASH_TABS,
                       MDI_WIN                  : MDI,
                       MDI_MAC                  : MDI,
                       MDI_TABS_LINUX           : MDI,
                       #MDI_TABS_WIN             : MDI_TABS,
                       MDI_TABS_MAC             : MDI_TABS,
                       MDI_SPLIT_ALL            : MDI_SPLIT,
                       DEFAULT  : -1}

PLATFORM                    = sys.platform
WIN                         = PLATFORM.startswith('win')
DARWIN                      = PLATFORM.startswith('darwin')
GTK                         = not (WIN or DARWIN)

if DARWIN:
    print 'If spe is unstable, try this interface from the preferences:\n  "%s"\n'%MDI_SPLIT_ALL

#wx related
FULL_REPAINT_ON_RESIZE      = wx.FULL_REPAINT_ON_RESIZE
POS                         = (10,10)
SIZE                        = (600,400)
SINGLE_INSTANCE_APP         = False
STYLE_CHILDFRAME            = wx.DEFAULT_FRAME_STYLE
STYLE_NOTEBOOK              = FULL_REPAINT_ON_RESIZE|wx.CLIP_CHILDREN|wx.NO_BORDER
STYLE_PARENTFRAME           = wx.DEFAULT_FRAME_STYLE #| wx.MAXIMIZE
STYLE_SPLIT                 = wx.SP_NOBORDER
STYLE_TOOLBAR               = wx.TB_HORIZONTAL | wx.NO_BORDER | wx.TB_FLAT | wx.TB_TEXT
TABSASH_HEIGHT              = 30
TITLE                       = 'www.stani.be'
UNNAMED                     = 'unnamed'

####Menu helper function
def _(x):
    if DARWIN:
        return x#.replace('Ctrl','Cmd')
    else:
        return x

def _strip(x):
    return x.replace(' ','_').replace('-','_').replace('&& ','').replace('&','').replace('.','').replace('(','').replace(')','')

def menuWrite(menuBar,f='menu.txt'):
    labels = []
    for m in range(menuBar.GetMenuCount()):
        menu        = menuBar.GetMenu(m)
        menuLabel   = menuBar.GetLabelTop(m)
        for item in menu.GetMenuItems():
            label   = _strip(item.GetLabel())
            if label:
                labels.append((menuLabel,item.GetLabel()))
    #events
    result = '\tdef __smdi__(self):\n'
    for label in labels:
        label = _strip(label[1])
        result+= '\t\twx.EVT_MENU(self,%s,self.menu_%s)\n'%(label.upper(),label.lower())
    result+= '\n'
    for label in labels:
        result+='\tdef menu_%s(self):\n\t\t"""%s"""\n\t\tpass\n\n'%(_strip(label[1].lower()),'%s > %s'%label)
    print result
    print os.getcwd()
    print f
    f   = open(f,'w')
    f.write(result)
    f.close()

def test_menuWrite():
    import menu
    menuWrite(wxgMenu.Bar())

####Base
WX_BITMAP       = wx.Bitmap

class Bitmap:
    def __init__(self,path,app):
        self.path = path
        self.app = app
    def __call__(self,x,t=wx.BITMAP_TYPE_ANY):
        path = os.path.join(self.path,os.path.basename(x))
        #if self.app.DEBUG:
        #    print 'Bitmap: %s<%s'%(x,path)
        return WX_BITMAP(path,t)

class DummyPage(wx.StaticText):
    """Page to fill the tabs (not meant to be visible)."""
    def __init__(self,tabs):
        wx.StaticText.__init__(self, tabs, wx.ID_ANY, "SPE bug: This shouldn't be visible")

class NativeNotebookPlus(wx.Notebook):
    """Fall back for linux"""
    def __init__(self,app,*args,**keyw):
        wx.Notebook.__init__(self,*args,**keyw)
        self.app = app
        self.Bind(wx.EVT_MIDDLE_UP,self.onFrameMiddleClick)
        self.Bind(wx.EVT_LEFT_DCLICK,self.onFrameMiddleClick)

    def onFrameMiddleClick(self,event):
        """When a tab is middle clicked (EVT_MOUSE_LEFT&HitTest)."""
        mousePos    = event.GetPosition()
        index, other = self.HitTest(mousePos)
        if self.app.mdi in [SDI,MDI_TABS]: #no parent tab
            zero = 0
        else:
            zero = -1
        if index>zero:
            self.app.children[index-zero-1].frame.onFrameClose()

    def EnableToolTip(self,*args,**keyw):
        pass

    def SetPageToolTip(self,*args,**keyw):
        pass

    def Tile(self,*args,**keyw):
        pass
        
    def BindPageChange(self,method):
        self.Bind(wx.EVT_NOTEBOOK_PAGE_CHANGED,method,self)

    def UnbindPageChange(self):
        self.Unbind(wx.EVT_NOTEBOOK_PAGE_CHANGED)

class AndreaNotebookPlus(NotebookCtrl.NotebookCtrl):
    def __init__(self,app,*args,**keyw):
        self.app = app
        keyw['size'] = wx.Size(25,25)
        keyw['margin'] = 0
        if keyw.has_key('style'):
            del keyw['style']
        NotebookCtrl.NotebookCtrl.__init__(self,*args,**keyw)
        #theme
        self.tabstyle   = NotebookCtrl.ThemeStyle()
        if DARWIN:
            self.SetControlBackgroundColour(wx.NullColour)#wx.Colour(236,236,236))
            self.tabstyle.EnableAquaTheme(True,2)
        else:
            self.SetHighlightSelection(True)
            self.tabstyle.EnableSilverTheme(True)
        self.ApplyTabTheme(self.tabstyle)
        #general settings
        if GTK:
            self.SetTabHeight(30)
        else:
            self.SetTabHeight(25)
        self.SetDrawX(True, 2)
        self.SetPadding(wx.Point(4,4))
        self.SetUseFocusIndicator(False)
        self.EnableChildFocus(True)
        self.EnableDragAndDrop(True)
        #self.EnableHiding(True)
        self.SetToolTipBackgroundColour(wx.Colour(240,255,240))
        #events
        self.Bind(NotebookCtrl.EVT_NOTEBOOKCTRL_PAGE_CLOSING,self.onClosing)
        self.Bind(NotebookCtrl.EVT_NOTEBOOKCTRL_PAGE_DND, self.onDragAndDrop)
        #self.Bind(NotebookCtrl.EVT_NOTEBOOKCTRL_PAGE_DCLICK, self.OnLeftDClick)

    def onClosing(self,event):
        """When a tab is middle clicked (EVT_MOUSE_LEFT&HitTest)."""
        index   = event.GetSelection()
        zero    = self.getZero()
        if index>zero:
            self.app.children[index-zero-1].frame.onFrameClose()

    def onDragAndDrop(self, event):
        old     = event.GetOldPosition()
        new     = event.GetNewPosition()
        zero    = self.getZero()
        if new != zero: #child can not be before parent if this is a tab
            children                = self.app.children
            child                   = children[old-zero-1]
            children.remove(child)
            children.insert(new-zero-1,child)
            event.Skip()

    def getZero(self):
        if self.app.mdi in [SDI,MDI_TABS]: #no parent tab
            return 0
        else:
            return -1

    def setIcons(self,bitmaps):
        #todo: here this should go somewhere else!!
        pageIcons      = []
        for bitmap in bitmaps:
            self.pageIcons.append(self.notebookIcons.Add(bitmap))

        self.notebookIcons  = wx.ImageList(16,16)
        self.AssignImageList(self.notebookIcons)

    def OnLeftDClick(self, event):

        nPage = event.GetSelection()
        self.ReparentToFrame(nPage, False)

        event.Skip()
        
    def BindPageChange(self,method):
        self.Bind(NotebookCtrl.EVT_NOTEBOOKCTRL_PAGE_CHANGED,method)

    def UnbindPageChange(self):
        self.Unbind(NotebookCtrl.EVT_NOTEBOOKCTRL_PAGE_CHANGED)


##try:
##    import wx.aui
##    NotebookPlus        = wx.aui.AuiNotebook
##except:
if sys.platform.startswith('linux'):
    NotebookPlus    = NativeNotebookPlus
else:
    NotebookPlus    = AndreaNotebookPlus

####Foundation Classes
class Framework:
    """Foundation class for every frame."""
    def __init__(self,app,Panel,parentFrame,page='',extra='',**options):
        self.Freeze()
        #stage
        self.__before__(app         = app,
                        Panel       = Panel,
                        parentFrame = parentFrame,
                        page        = page,
                        extra       = extra)
        self.__stage__(page         = page,
                       extra        = extra,
                       **options)
        self.__after__()
        ##rest
        self.__menu__()
        self.__tool__(app)
        self.__statusBar__()
        self.__finish__()
        self.__events__()
        #show
        self.Thaw()
        self.Show(True)

    #---components
    def __before__(self,app,Panel,parentFrame,page,extra):
        """Reference frame attributes (not overwritten)."""
        self.app            = app
        self.Panel          = Panel
        self.parentFrame    = parentFrame
        self.dead           = False # todo: is this still necessary
        if not hasattr(self,'isSdiParent'):
            self.isSdiParent    = False
        self.page           = page
        self.extra          = extra


    def __stage__(self,page,extra,**options):
        """Create notebook (to switch between documents) & panel
        This is mostly overwritten."""
        self.panel  = self.Panel(parent=self,**options)

    def __after__(self):
        """Reference panel attributes  (not overwritten)."""
        panel               = self.panel
        panel.app           = self.app
        panel.frame         = self
        panel.parentFrame   = self.parentFrame
        panel.parentPanel   = self.parentFrame.panel
        panel.changed       = False

    def __menu__(self):
        """Create: Framework: menu."""
        app                     = self.app
        if app.MenuBar and not self.noMenu:
            if app.DEBUG:
                print """Create: Framework: menu."""
            if app.mdi:
                frame           = self.app
            else:
                frame           = self
            self.menuBar        = menuBar = app.MenuBar(app=self.app,frame=frame)
            self.SetMenuBar(menuBar)
            #reference
            menuBar.app         = app
            #create
            menuBar.frame       = self
            menuBar.parentFrame = self.parentFrame
            menuBar.parentPanel = self.parentFrame.panel
            if hasattr(self,'palette'):
                self.palette.panel.app      = app
                self.palette.panel.menuBar  = menuBar
        else:
            self.menuBar        = None

    def __tool__(self,app):
        """Create toolbar

        Very important: in the custom wx.ToolBar class after the
        wx.ToolBar.__init__ the following code must be written:
            parent.SetToolBar(self)"""
        if self.app.ToolBar and not (self.noMenu or self.isSdiParent):
            if self.app.DEBUG:
                print """Create: Framework: toolbar."""
            #create
            self.toolBar                = self.app.ToolBar(parent=self, app=app,
                id=wx.ID_ANY, style=STYLE_TOOLBAR)
            #self.SetToolBar(self.toolBar)-> do this in toolbar class
            #reference
            self.toolBar.app            = self.app
            self.toolBar.frame          = self
            if self.menuBar:
                self.toolBar.menuBar        = self.menuBar
                self.menuBar.toolBar        = self.toolBar
        else:
            self.toolBar = None
            if self.menuBar:
                self.menuBar.toolBar        = None

    def __statusBar__(self):
        """Create statusbar (to be overwritten)."""
        if self.noMenu:
            self.panel.SetStatusText    = self.parentFrame.SetStatusText
            self.panel.statusBar        = self.parentFrame.statusBar
        else:
            self.panel.SetStatusText    = self.SetStatusText
            if self.app.StatusBar:
                if self.app.DEBUG:
                    print """Create: Framework: statusbar."""
                self.statusBar = self.app.StatusBar(parent=self,id=wx.ID_ANY)
                self.SetStatusBar(self.statusBar)

    #---other
    def __finish__(self):
        self.setTitle(self.page,self.extra)
        if hasattr(self.panel,'__finish__'):
            self.panel.__finish__()

    def __layoutTabs__(self,parent=None):
        """Not for mdi children"""
        if not parent: parent = self
        #sizer layout
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer.Add(self.tabs, 1, wx.EXPAND, 0)
        parent.SetAutoLayout(True)
        parent.SetSizer(sizer)
        parent.Layout()
        #events
        self.bindTabs()

    #---events
    def _isActiveEvent(self,event):
        return (event is None) or (not hasattr(event,'GetActive')) or event.GetActive()

    def __events__(self):
        """Initialize events."""
        eventManager.Register(self.onFrameActivate, wx.EVT_ACTIVATE,    self)
        eventManager.Register(self.onFrameClose,    wx.EVT_CLOSE,       self)
        eventManager.Register(self.onFrameMove,     wx.EVT_MOVE,        self)
        eventManager.Register(self.onFrameSize,     wx.EVT_SIZE,        self)
        if self.menuBar:    self.menuBar.__events__()
        if self.toolBar:    self.toolBar.__events__()
        if hasattr(self.panel,'onIdle'):
            eventManager.Register(self.onFrameIdle, wx.EVT_IDLE, self)

    def onFrameActivate(self, event):
        """Activate event (to be overwritten)."""
        getActive   = self._isActiveEvent(event)
        if getActive:
            if self.app.DEBUG:
                print 'Event<: Framework: %s.Activate(%s)'%(self.__class__,getActive)
            if hasattr(self.panel,'onActivate'):
                self.panel.onActivate(event)
        elif hasattr(self.panel,'onDeactivate'):
            self.panel.onDeactivate(event)
        if self.app.DEBUG:
            print 'Event>: Framework: %s.Activate(%s)'%(self.__class__,getActive)
        if event: event.Skip()

    def onFrameClose(self, event=None, destroy = 1):
        """Close event (to be overwritten/extended)."""
        debug = self.app.DEBUG
        if debug:
            print 'Event<: Framework: %s.Close'%self.__class__
        if hasattr(self.panel,'onClose'):
            self.dead = self.panel.onClose()
        else:
            self.dead = True
        if self.dead:
            if destroy:
                eventManager.DeregisterWindow(self)
                self.Destroy()
                if event: event.Skip()
            if debug:
                print 'Event>: Framework: %s.Close returns True'%self.__class__
            return True
        else:
            if debug:
                print 'Event>: Framework: %s.Close returns False'%self.__class__
            return False

    def onFrameMove(self, event=None):
        """Move event (to be overwritten)."""
        if self.app.DEBUG:
            print 'Event<: Framework: %s.Move'%self.__class__
        if event: event.Skip()
        #sm.spy.frame(1)
        if hasattr(self.panel,'onMove'):
            self.panel.onMove(event)
        if self.app.DEBUG:
            print 'Event>: Framework: %s.Move'%self.__class__

    def onFrameSize(self, event=None):
        """Size event (to be overwritten)."""
        #sm.spy.frame(1)
        if self.app.DEBUG:
            print 'Event<: Framework: %s.Size'%self.__class__
        if hasattr(self.panel,'onSize'):
            self.panel.onSize(event)
        if self.app.DEBUG:
            print 'Event>: Framework: %s.Size'%self.__class__
        if event: event.Skip()

    def onFrameIdle(self, event):
        """To be overwritten."""
        if not self.dead:
            if hasattr(self.panel,'onIdle'):
                self.panel.onIdle()

    def bindTabs(self):
        """Bind events to notebook tabs (to be overwriten)."""
        pass

    def unbindTabs(self):
        """Unbind events to notebook tabs (to be overwriten)."""
        pass

    def getIndex(self):
        """Get index of current child."""
        try:
            return self.app.children.index(self.panel)
        except ValueError:
            return -1


class Tabs(Framework):
    #---events
    def bindTabs(self):
        self.tabs.Bind(self.app.EVENT_NOTEBOOK, self.onFrameTab)
        #eventManager.Register(self.onFrameTab, self.app.EVENT_NOTEBOOK, self.tabs)

    def unbindTabs(self):
        self.tabs.Unbind(self.app.EVENT_NOTEBOOK)
        #eventManager.DeregisterWindow(self.tabs)

    def raiseTab(self,index):
        if index > -1:
            app = self.app
            if app.DEBUG:
                print 'Event<: Tab:   %s.onFrameTab(%s)'%(self.__class__,index)
            parent      = app.mdi in [SDI,MDI_TABS]
            if index == 0 and parent:
                window  = self.parentFrame
                if hasattr(window,'panelFrame'):
                    window = window.panelFrame
            else:
                window = self.app.children[index-[0,1][parent]].frame
            if app.DEBUG:
                print '%s.Raise()'%window
                print window.Raise
            window.Raise()
            if app.DEBUG:
                print 'Event>: Tab:   %s.onFrameTab(%s)'%(self.__class__,index)

#---SDI Platform dependent
class TabWin32(Tabs):
    """SDI Implementation for windows (see also App.SetMdi)"""
##    def onFrameTab(self,event):
##        """When a tab is changed (EVT_MOUSE_LEFT&HitTest)."""
##        mousePos    = event.GetPosition()
##        index, other = self.tabs.HitTest(mousePos)
##        self.raiseTab(index)

    def onFrameTab(self,event):
        self.raiseTab(event.GetSelection())
        event.Skip()

class TabUnix(Tabs):
    """SDI Implementation for windows (see also App.SetMdi)"""
    def onFrameTab(self,event):
        """When a tab is changed (EVT_NOTEBOOK_CHANGING)."""
        index       = event.GetSelection()
        event.Veto()#instead of event.Skip() (don't do this here anyway)
        self.raiseTab(index)

if PLATFORM == 'win32':
    TabPlatform     = TabWin32
else:
    TabPlatform     = TabUnix

####Parent classes
class Parent(Framework):
    #---initialize
    def __init__(self, app, page = '', **options):
        self.options            = options
        self.noMenu         = False
        Framework.__init__(self,
            app             = app,
            Panel           = app.ParentPanel,
            page            = page,
            parentFrame     = self,
            **options)

    def __finish__(self):
        Framework.__finish__(self)

    #---events
    def onFrameClose(self,event=None):
        self.dead = Framework.onFrameClose(self,event,destroy=0)
        if not self.dead: return
        #Avoid event exceptions of child frames
        for child in self.app.children:
            if child:
                if hasattr(child,'frame'):
                    eventManager.DeregisterWindow(child.frame)
                child.dead = 1
        #Destroy itself
        self.Destroy()
        if event: event.Skip()

    #---menu
    def menu_new(self, event=None):
        self.child()

    def menu_close(self,event=None):
        if self.app.children:
            childActive = self.app.childActive
            if childActive:
                childActive.onFrameClose()

    #---parentPanel
    def child(self,*args,**keyw):
        self.ChildFrame(self,*args,**keyw)

    def maximize(self):
        if self.app.mdi == MDI_SPLIT:
            return True
        i = 0
        m = 0
        for child in self.app.children:
            if child.frame.IsMaximized():
                return True
        return False

    #---other
    def setTitle(self,page='',extra='',draw=True,colour=None):
        if draw:
            t           = self.app.title
            if page:
                t       = '%s - %s'%(page,t)
            self.SetTitle(t)


class MdiParentFrame(Parent,wx.MDIParentFrame):
    """Uniformed parent Mdi/Sdi class based on Mdi.

    self.Panel is defined by joined class"""
    def __init__(self,app,
            id      = wx.ID_ANY,
            page   = 'parentFrame',
            parent  = None,
            size    = SIZE,
            style   = STYLE_PARENTFRAME,
            pos     = POS,
            **options):
        wx.MDIParentFrame.__init__(self,
            id      = id,
            name    = page,
            parent  = parent,
            size    = size,
            style   = style | FULL_REPAINT_ON_RESIZE,
            title   = page,
            pos     = pos)
        if style & wx.MAXIMIZE:
            try: #not working on mandrake9
                self.Maximize(1)
            except:
                pass
        #This always has to be last!
        Parent.__init__(self,app=app,page=page,**options)

    def __stage__(self,page,extra,**options):
        self.panelFrame     = wx.MDIChildFrame(parent=self,id=wx.ID_ANY)
        self.panelFrame.SetTitle(self.app.panelFrameTitle)
        #parentPanel
        self.panel          = self.Panel(parent=self.panelFrame,**options)
        eventManager.Register(self.onSashClose, wx.EVT_CLOSE, self.panelFrame)
        #palette
        if self.app.Palette:
            self.palette    = self.app.Palette(parent=self,id=wx.ID_ANY)
            self.palette.Show()

    def onSashClose(self,event):
        if hasattr(self.panel,'onClosePanelFrame'):
            self.panel.onClosePanelFrame(event)

    def setTitle(self,page='',extra='',draw=True,colour=None):
        if draw:
            self.SetTitle(self.app.title)

class MdiTabsParentFrame(TabPlatform,MdiParentFrame):
    def __stage__(self,page,extra,**options):
        app                 = self.app
        self.panelFrame     = wx.MDIChildFrame(parent=self,id=wx.ID_ANY)
        self.panelFrame.SetTitle(app.panelFrameTitle)
        self.panelFrame.Raise= self.panelFrame.Activate
        #parentPanel
        self.tabs   = NotebookPlus(app=app,parent=self.parentFrame, id=wx.ID_ANY,
            style = STYLE_NOTEBOOK )
        self.panel  = self.Panel(parent=self.tabs,**options)
        self.tabs.AddPage(self.panel, page)
        self.__layoutTabs__()
        #events
        eventManager.Register(self.onSashClose, wx.EVT_CLOSE, self.panelFrame)
        #palette
        if app.Palette:
            self.palette    = self.app.Palette(parent=self,id=wx.ID_ANY)
            self.palette.Show()


class MdiSashParentFrame(MdiParentFrame):
    """Uniformed parent Mdi/Sdi class based on Mdi.

    self.Panel is defined by joined class"""
    def __stage__(self,page,extra,**options):
        """Create tabs to switch between documents as an wx.SashLayoutWindow"""
        if self.app.DEBUG:
            print 'Create: Mdi:   %s.tabs'%(self.__class__,)
        #sash for parentPanel
        self.sashId = wx.NewId()
        self.sash = wx.SashLayoutWindow(id=self.sashId,
              name='sash', parent=self, style=wx.NO_BORDER)
        self.sash.SetDefaultSize(wx.Size(792, 200))
        self.sash.SetOrientation(wx.LAYOUT_HORIZONTAL)
        self.sash.SetAlignment(wx.LAYOUT_BOTTOM)
        self.sash.SetSashVisible(wx.SASH_TOP, 1)
        self.sash.SetMinimumSizeY(1)
        #self.sash.Show(True)
        eventManager.Register(self.onFrameSashDragged, wx.EVT_SASH_DRAGGED, self.sash)
        #parentPanel
        self.panel          = self.Panel(parent=self.sash,**options)
        #layout
        self.panelSizer     = wx.BoxSizer(wx.VERTICAL)
        self.panelSizer.Add(self.panel, 1, wx.ALL | wx.EXPAND, 0)
        self.sash.SetAutoLayout(1)
        self.sash.SetSizer(self.panelSizer)

    def __finish__(self):
        Parent.__finish__(self)
        wx.LayoutAlgorithm().LayoutMDIFrame(self)

    #---events
    def onFrameSashDragged(self,event):
        """Called when the shashwindow is dragged."""
        if event.GetDragStatus() == wx.SASH_STATUS_OUT_OF_RANGE:
            return
        eID = event.GetId()
        if eID == self.sashId:
            self.sash.SetDefaultSize(wx.Size(1000,event.GetDragRect().height))
        wx.LayoutAlgorithm().LayoutMDIFrame(self)

    def onFrameSize(self, event):
        """Overwritten for sash dragging."""
        Framework.onFrameSize(self)
        wx.LayoutAlgorithm().LayoutMDIFrame(self)

class MdiSashTabsParentFrame(TabPlatform,MdiSashParentFrame):
    def __stage__(self,page,**options):
        """Create tabs to switch between documents as an wx.SashLayoutWindow"""
        if self.app.DEBUG:
            print 'Create: Mdi: %s.tabs'%(self.__class__,)
        self.tabsSash = wx.SashLayoutWindow(id=wx.ID_ANY,
              name='tabs', parent=self, style=wx.CLIP_CHILDREN)
        self.tabsSash.SetOrientation(wx.LAYOUT_HORIZONTAL)
        self.tabsSash.SetAlignment(wx.LAYOUT_TOP)
        self.tabsSash.SetDefaultSize(wx.Size(792, TABSASH_HEIGHT))
        self.tabs = NotebookPlus(app=self.app,id=wx.ID_ANY, parent=self.tabsSash, style=STYLE_NOTEBOOK)
        self.__layoutTabs__(self.tabsSash)
        MdiSashParentFrame.__stage__(self,page,**options)

    def onFrameTab(self,event):
        """When a tab is changed (EVT_MOUSE_LEFT&HitTest)."""
        TabPlatform.onFrameTab(self,event)
        event.Skip()

class MdiSplitParentFrame(Parent,wx.Frame):
    """Default on Linux."""
    def __init__(self,app,
            id      = wx.ID_ANY,
            page    = 'parentFrame',
            parent  = None,
            size    = SIZE,
            style   = STYLE_PARENTFRAME,
            pos     = POS,
            **options):
        wx.Frame.__init__(self,
            id      = id,
            name    = page,
            parent  = parent,
            size    = size,
            style   = style | FULL_REPAINT_ON_RESIZE,
            title   = page,
            pos     = pos)
        if style & wx.MAXIMIZE:
            try: #not working on mandrake9
                self.Maximize(1)
            except:
                pass
        #This always has to be last!
        Parent.__init__(self,app=app,page=page,**options)

    def __stage__(self,page,extra,**options):
        self.split  = split = wx.SplitterWindow(self,wx.ID_ANY,style=STYLE_SPLIT)
        self.tabs   = NotebookPlus(app=self.app,parent=split, id=wx.ID_ANY,
            style = STYLE_NOTEBOOK )
        #self.tabs   = wx.Notebook(parent=split, id=wx.ID_ANY,
        #    style = STYLE_NOTEBOOK )
        self.panel  = self.Panel(parent=split,**options)
        split.SetMinimumPaneSize(20)
        size = self.GetSize()
        self.SetSize((size[0],size[1]-1))
        self.SetSize((size[0],size[1]))
        split.UpdateSize()
        if sys.platform.startswith('linux'):
            split.SplitHorizontally(self.tabs, self.panel, size[1]-20)
        else:
            split.SplitHorizontally(self.tabs, self.panel, -200)
        self.bindTabs()

    def bindTabs(self,event=None):
        self.tabs.BindPageChange(self.onFrameTab)

    def unbindTabs(self):
        self.tabs.UnbindPageChange()
        
    def onFrameTab(self,event):
        if not self.dead:
            index = event.GetSelection()
            #print index
            if index>-1:
                self.app.children[index].frame.onFrameActivate()
        event.Skip()

    def setTitle(self,page='',extra='',draw=True,colour=None):
        if draw:
            t           = self.app.title
            if page:
                t       = '%s - %s - %s'%(page,os.path.dirname(extra),t)
            self.SetTitle(t)

class SdiParentFrame(TabPlatform,Parent,wx.Frame):
    """Uniformed parent Mdi/Sdi class based on Sdi."""
    def __init__(self,app,
            id      = wx.ID_ANY,
            page    = 'parentFrame',
            parent  = None,
            size    = SIZE,
            style   = STYLE_PARENTFRAME,
            pos     = POS,
            **options):
        wx.Frame.__init__(self,
            id      = id,
            name    = page,
            parent  = parent,
            size    = size,
            style   = style | FULL_REPAINT_ON_RESIZE,
            title   = page,
            pos     = pos)
        if style & wx.MAXIMIZE:
            try: #not working on mandrake9
                self.Maximize(1)
            except:
                pass
        self.isSdiParent    = True
        #This always has to be last!
        Parent.__init__(self,app=app,page=page,**options)

    def __stage__(self,page,extra,**options):
        """Create tabs to switch between documents as an wx.Notebook"""
        if self.app.DEBUG:
            print 'Create: Sdi:   %s.tabs'%(self.__class__,)
        self.tabs   = NotebookPlus(app=self.app,parent=self, id=wx.ID_ANY,
            style = STYLE_NOTEBOOK )
        self.panel  = self.Panel(parent=self.tabs,**options)
        self.tabs.AddPage(self.panel, page)
        self.__layoutTabs__()

    def setTitle(self,page='',extra='',draw=True,colour=None):
        if draw:
            self.SetTitle(self.app.title)

####Child classes
class Child(Framework):
    #---initialize
    def __init__(self,parentFrame,page='',extra='',parent=None,**options):
        app                 = parentFrame.app
        self.pageTitle      = ''
        self._pageTitle     = '?'
        self.pageIcons      = []
        self.extraTitle     = ''
        self._extraTitle    = ''
        Framework.__init__(self,
            app             = app,
            Panel           = app.ChildPanel,
            parentFrame     = parentFrame,
            page            = page,
            extra           = extra,
            **options)

    def __finish__(self):
        app             = self.app
        panel           = self.panel
        app.childActive = panel
        app.children.append(panel)
        self.addPageToParent(panel,app.mdi)
        #finish
        Framework.__finish__(self)

    def addPageToParent(self,panel,mdi):
        """"Add page with childs title to parent
        Can be overwritten."""
        parentFrame     = self.parentFrame
        tabs            = parentFrame.tabs
        tabs.AddPage(page=DummyPage(tabs), text=self.page,select=(mdi not in [SDI,MDI_TABS]))

    #---events
    def onFrameActivate(self, event=None):
        """Activate event."""#todo: update tabs above if not right
        if self._isActiveEvent(event):
            if self.app.DEBUG:
                'Event:  Child: %s.Activate'%self.__class__
            self.app.childActive    = self.panel
        Framework.onFrameActivate(self,event)

    def onFrameClose(self, event = None):
        """Close event.
        Try to avoid self here once DeletePage has been called."""
        #do here all references to self
        app         = self.app
        debug       = app.DEBUG
        index       = self.getIndex()
        panel       = self.panel
        mdi         = app.mdi
        destroyed   = False
        parentFrame = self.parentFrame
        if debug:
            print 'Event<: Child: %s.Close'%self.__class__
        self.dead = Framework.onFrameClose(self,destroy=0)
        if not self.dead:
            if debug:
                print 'Event>: Child: %s.Close returns False'%self.__class__
            return False
        #no references to self after this point
        if event: event.Skip()
        #index
        if mdi in [SDI,MDI_TABS]:  delta = 1
        else:                      delta = 0
        #Update children
        children    = app.children
        children.remove(panel)
        sdi         = (mdi in [SDI,MDI_TABS] and children)
        #deregister events
        eventManager.DeregisterWindow(self)
        if sdi: #not for mdichild
            eventManager.DeregisterWindow(self.tabs)
        #update rest
        if hasattr(parentFrame,'tabs'):
            #Update childBook tabs
            current = index+delta
            # * parent frame (mdi & sdi)
            parentFrame.unbindTabs()
            parentFrame.tabs.DeletePage(current)
            destroyed   = True
            selected    = parentFrame.tabs.GetSelection()
            parentFrame.bindTabs()
            # * children frames (sdi)
            if sdi: #not for mdichild
                c       = 1
                for child in children:
                    child.frame.unbindTabs()
                    tabs   = child.frame.tabs
                    tabs.DeletePage(current)#remove closed item from other children
                    if c>=current:
                        tabs.SetSelection(c)#adapt selection
                    child.frame.bindTabs()
                    c += 1
            selected    = parentFrame.tabs.GetSelection()
        else:
            selected    = 0
        if children:
            app.childActive = children[selected]
            app.childActive.frame.Activate()
        else:
            app.childActive = None
        if not destroyed and mdi!=MDI_SPLIT:
                self.Destroy()
        if debug:
            print 'Event>: Child: %s.Close returns True'%self.__class__
        return True

    def setTitle(self,page='',extra='',new=True,draw=True,colour=None):
        if new:
            #parameters
            if page:    self.pageTitle  = page
            if extra:   self.extraTitle = extra
            #go
            if self.pageTitle:
                m               = ['',' *'][self.panel.changed and not self.pageIcons]
                self._pageTitle =  '%s%s'%(self.pageTitle,m)
            else:
                self._pageTitle =  ''
        if draw:
            if self.app.mdiName == SDI or WIN:
                self.SetTitle(self.extraTitle)
            elif hasattr(self,'SetTitle'):
                self.SetTitle(self.pageTitle)
            self.setIcon()

    def setIcon(self):
        pass

class MdiSashTabsChildFrame(Child,wx.MDIChildFrame):
    def __init__(self,parentFrame,
        id          = wx.ID_ANY,
        page        = UNNAMED,
        extra       = '',
        style       = STYLE_CHILDFRAME,
        maximize    = None,
        **options):
        self.noMenu = True
        app             = parentFrame.app
        #debug message
        if app.DEBUG:
            print 'Create: Mdi: %s'%self.__class__
        if maximize == None:
            maximize = parentFrame.maximize()
        if maximize:
            style   |= wx.MAXIMIZE
        wx.MDIChildFrame.__init__(self,
            id      = id,
            name    = page,
            parent  = parentFrame,
            style   = style | FULL_REPAINT_ON_RESIZE,
            title   = page)
        self.Maximize()#if maximize and PLATFORM != 'win32':#sm:seems better to leave this away.
        self.Raise  = self.Activate #raise doesn't work here
        #This always has to be last!
        Child.__init__(self,
            parentFrame = parentFrame,
            page        = page,
            extra       = extra,
            **options)

    def __finish__(self):
        self.tabs       = self.parentFrame.tabs
        Child.__finish__(self)

    def setTitle(self,page='',extra='',new=True,draw=True,colour=None):
        Child.setTitle(self,page,extra,new,draw)
        self.parentFrame.setTitle(self._pageTitle,draw=draw)
        if new and draw:
            index = self.getIndex()
            self.tabs.SetPageText(index,self._pageTitle)
            if not(colour is None) and hasattr(self.tabs,'SetPageColour'):
                self.tabs.SetPageColour(index,colour)

    def onFrameActivate(self, event):
        if self._isActiveEvent(event):
            self.setTitle(new=False)
            self.parentFrame.tabs.SetSelection(self.getIndex())
        Child.onFrameActivate(self,event)

class MdiChildFrame(MdiSashTabsChildFrame, Child):
    """Mdi Child frame without tabs."""
    def addPageToParent(self,panel,mdi):
        pass

    def setTitle(self,page='',extra='',new=True,draw=True,colour=None):
        Child.setTitle(self,page,extra,new,draw)
        self.parentFrame.setTitle(self._pageTitle,draw=draw)

    def __finish__(self):
        Child.__finish__(self)

    def onFrameActivate(self, event):
        if self._isActiveEvent(event):
            self.setTitle(new=False)
        Child.onFrameActivate(self,event)

class MdiTabsChildFrame(TabPlatform,MdiSashTabsChildFrame, Child):
    def __stage__(self,page,extra,**options):
        """Create tabs to switch between documents as an wx.SashLayoutWindow"""
        if self.app.DEBUG:
            print 'Create: Sdi:   %s.tabs'%(self.__class__,)
        tabs = self.tabs   = NotebookPlus(app=self.app,parent=self, id=wx.ID_ANY,
            style = STYLE_NOTEBOOK )
        panel = self.panel = self.Panel(parent=tabs,**options)
        #Add parent tab to itself
        tabs.AddPage(page=DummyPage(tabs),text=self.app.title)
        #Add child tabs to itself and vice versa
        for child in self.app.children:
            childTabs   = child.frame.tabs
            tabs.AddPage(page=DummyPage(tabs),text=child.frame._pageTitle)
            childTabs.AddPage(page=DummyPage(childTabs),text=page)
        #Add itself to itself
        tabs.AddPage(page=panel,text=page,select=True)
        self.__layoutTabs__()

    def __finish__(self):
        Child.__finish__(self)

    def setTitle(self,page='',extra='',new=True,draw=True,colour=None):
        Child.setTitle(self,page,extra,new)
        if new and draw:
            index   = self.getIndex()+1
            self.tabs.SetPageText(index,self._pageTitle)
            if not(colour is None) and hasattr(self.tabs,'SetPageColour'):
                self.tabs.SetPageColour(index,colour)

    def onFrameActivate(self, event):
        if self._isActiveEvent(event):
            self.setTitle(new=False)
            self.parentFrame.tabs.SetSelection(self.getIndex()+1)
        Child.onFrameActivate(self,event)

class MdiSplitChildFrame(Child,wx.Panel):
    def __init__(self,parentFrame,
        id          = wx.ID_ANY,
        style       = STYLE_CHILDFRAME,
        page        = '',
        extra       = '',
        maximize    = None,
        **options):
        self.noMenu = True
        app         = parentFrame.app
        #debug message
        if app.DEBUG:
            print 'Create: MdiSplit: %s'%self.__class__,page,extra
        wx.Panel.__init__(self,
            id      = id,
            name    = page,
            parent  = parentFrame.tabs,
            size    = wx.Size(600,400),
            style   = style | FULL_REPAINT_ON_RESIZE,)
            #title   = page)
        #This always has to be last!
        Child.__init__(self,
            parentFrame = parentFrame,
            page        = page,
            extra       = extra,
            **options)
        eventManager.Register(self.onFrameActivate, wx.EVT_SET_FOCUS,    self)

    def __stage__(self,page,extra,**options):
        """Create tabs to switch between documents as an wx.SashLayoutWindow"""
        if self.app.DEBUG:
            print 'Create: Sdi:   %s.tabs'%(self.__class__,)
        self.panel = self.Panel(parent=self,name=page,**options)
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer.Add(self.panel, 1, wx.EXPAND, 0)
        self.SetAutoLayout(True)
        self.SetSizer(sizer)
        self.Layout()

    def addPageToParent(self,panel,mdi):
        """"Add page with childs title to parent
        Can be overwritten."""
        parentFrame     = self.parentFrame
        tabs            = self.tabs = parentFrame.tabs
        parentFrame.unbindTabs()
        tabs.AddPage(page=self, text=self.page,select=(mdi not in [SDI,MDI_TABS]))
        parentFrame.bindTabs()

    def setTitle(self,page='',extra='',new=True,draw=True,colour=None):
        Child.setTitle(self,page,extra,new)
        self.parentFrame.setTitle(self._pageTitle,extra=self.extraTitle,draw=draw)
        if new and draw:
            index   = self.getIndex()
            self.tabs.SetPageText(index,self._pageTitle)
            if not(colour is None) and hasattr(self.tabs,'SetPageColour'):
                self.tabs.SetPageColour(index,colour)

    def SetIcon(self,*args,**keyw):
        pass

    def IsMaximized(self):
        return True

    def Activate(self):
        self.setTitle()
        self.parentFrame.tabs.SetSelection(self.getIndex())

    def Raise(self):
        self.Activate()

    def onFrameActivate(self, event=None):
        if self._isActiveEvent(event):
            self.setTitle(new=False)
        Child.onFrameActivate(self,event)

##    def onFrameActivate(self, event=None):
##        if (not event) or event.GetActive():
##            self.setTitle(new=False)
##        Child.onFrameActivate(self,event)

class SdiChildFrame(TabPlatform,Child,wx.Frame):
    def __init__(self,parentFrame,
        id          = wx.ID_ANY,
        style       = STYLE_CHILDFRAME,
        page        = '',
        extra       = '',
        maximize    = None,
        **options):
        self.noMenu = False
        app             = parentFrame.app
        #debug message
        if app.DEBUG:
            print 'Create: Sdi: %s'%self.__class__,page,extra
        #maximize&init
        if maximize == None:
            maximize = parentFrame.maximize()
        if maximize:
            style   |= wx.MAXIMIZE
        wx.Frame.__init__(self,
            id      = id,
            name    = page,
            parent  = parentFrame,
            size    = wx.Size(600,400),
            style   = style | FULL_REPAINT_ON_RESIZE,
            title   = page)
        if maximize:
            if PLATFORM != 'win32':
                self.Maximize()
            else:
                self.SetSize(app.size)
        #This always has to be last!
        Child.__init__(self,
            parentFrame = parentFrame,
            page        = page,
            extra       = extra,
            **options)

    def __stage__(self,page,extra,**options):
        """Create tabs to switch between documents as an wx.SashLayoutWindow"""
        if self.app.DEBUG:
            print 'Create: Sdi:   %s.tabs'%(self.__class__,)
        tabs = self.tabs   = NotebookPlus(app=self.app,parent=self, id=wx.ID_ANY,
            style = STYLE_NOTEBOOK )
        panel = self.panel = self.Panel(parent=tabs,**options)
        #Add parent tab to itself
        tabs.AddPage(page=DummyPage(tabs),text=self.app.title)
        #Add child tabs to itself and vice versa
        for child in self.app.children:
            childTabs   = child.frame.tabs
            tabs.AddPage(page=DummyPage(tabs),text=child.frame._pageTitle)
            childTabs.AddPage(page=DummyPage(childTabs),text=page)
        #Add itself to itself
        tabs.AddPage(page=panel,text=page,select=True)
        self.__layoutTabs__()

    def setTitle(self,page='',extra='',new=True,draw=True,colour=None):
        Child.setTitle(self,page,extra,new)
        if new and draw:
            self.tabs.SetPageText(self.getIndex()+1,self._pageTitle)

####Application

class App(singleApp.SingleInstanceApp):
    def __init__(self, ParentPanel, ChildPanel, MenuBar, ToolBar, StatusBar,
            Palette=None, mdi=DEFAULT, debug=0, title='name',
            panelFrameTitle='panel',size=wx.Size(800,400),
            imagePath = None, pos=wx.Point(wx.ID_ANY,wx.ID_ANY),
            singleInstance = False,
            style=STYLE_PARENTFRAME,**attributes):
        #passing arguments
        global CHILDPANEL
        self.ParentPanel    = ParentPanel
        self.ChildPanel     = CHILDPANEL = ChildPanel
        self.MenuBar        = MenuBar
        self.ToolBar        = ToolBar
        self.StatusBar      = StatusBar
        self.Palette        = Palette
        self.SetMdi(mdi)
        self.DEBUG          = debug
        self.title          = title
        self.panelFrameTitle= panelFrameTitle
        self.size           = size
        self.imagePath      = imagePath
        self.pos            = pos
        self.singleInstance = singleInstance
        self.active         = True
        self.style          = style
        #initialization
        self.children       = []
        self.childActive    = None
        if self.imagePath:
            self.bitmap     = wx.Bitmap = Bitmap(imagePath,self)
        else:
            self.bitmap     = wx.Bitmap
        #options
        self.attributes     = attributes
        for key in attributes:
            if hasattr(self,key):
                print "Warning: Application can't accept attribute '%s'."%key
            else:
                setattr(self,key,attributes[key])
        #start
        if singleInstance:
            print "Launching single instance application (with xml-rpc server) ..."
            singleApp.SingleInstanceApp.__init__(self,redirect=not debug,name=title)
        else:
            print "Launching application..."
            wx.App.__init__(self,redirect=not debug)


    def OnArgs(self, evt):
        if hasattr(self.parentPanel,'onArgs'):
            self.parentPanel.onArgs(evt.data)
        self.GetTopWindow().Raise()
        self.GetTopWindow().Iconize(False)

    def OnInit(self):
        if self.singleInstance:
            if self.active:
                return False
            self.Bind(singleApp.EVT_POST_ARGS, self.OnArgs)
        wx.InitAllImageHandlers()
        self.parentFrame = self.ParentFrame(self,
            size    = self.size,
            page    = self.title,
            pos     = self.pos,
            style   = self.style,
            **self.attributes)
        self.parentPanel = self.parentFrame.panel
        self.parentFrame.Show(True)
        self.SetTopWindow(self.parentFrame)
        return True

    def SetMdi(self,mdiName=DEFAULT):
        """Defines parent and children frame classes."""
        self.mdiName    = mdiName
        if not DI.has_key(mdiName):
            mdiName     = DEFAULT
        if mdiName == DEFAULT:
            if   WIN:
                mdiName = MDI_SASH_TABS_WIN
            elif DARWIN:#mac osx
                mdiName = MDI_SPLIT_ALL
            else:
                mdiName = MDI_SPLIT_ALL#MDI_SASH_TABS_LINUX

        self.mdi    = DI[mdiName]
        if      self.mdi == SDI:
            self.ParentFrame        = SdiParentFrame
            self.ChildFrame         = SdiChildFrame
        elif    self.mdi == MDI_SASH:
            self.ParentFrame        = MdiSashParentFrame
            self.ChildFrame         = MdiChildFrame
        elif    self.mdi == MDI_SASH_TABS:
            self.ParentFrame        = MdiSashTabsParentFrame
            self.ChildFrame         = MdiSashTabsChildFrame
            #self.EVENT_NOTEBOOK     = wx.EVT_NOTEBOOK_PAGE_CHANGED
        elif    self.mdi == MDI:
            self.ParentFrame        = MdiParentFrame
            self.ChildFrame         = MdiChildFrame
        elif    self.mdi == MDI_TABS:
            self.ParentFrame        = MdiTabsParentFrame
            self.ChildFrame         = MdiTabsChildFrame
        elif    self.mdi == MDI_SPLIT:
            self.ParentFrame        = MdiSplitParentFrame
            self.ChildFrame         = MdiSplitChildFrame

        self.EVENT_NOTEBOOK = NotebookCtrl.EVT_NOTEBOOKCTRL_PAGE_CHANGED
##        #Tabs: notebook event is platformdependent
##        if WIN:
##            #Under Windows, GetSelection() will return the same value as
##            #GetOldSelection() when called from EVT_NOTEBOOK_PAGE_CHANGING handler and
##            #not the page which is going to be selected
##            #Therefore on Windows a combination of mouse click and hittest must be used.
##            self.EVENT_NOTEBOOK = wx.EVT_LEFT_DOWN
##        else:
##            self.EVENT_NOTEBOOK = wx.EVT_NOTEBOOK_PAGE_CHANGING


####Test app
class TestMenuBar(wx.MenuBar):
    def __init__(self,app,frame,*args,**kwds):
        wx.MenuBar.__init__(self,*args,**kwds)
        self.file   = wx.Menu()
        self.file.Append(wx.ID_NEW, _("&New\tCtrl+N"), "", wx.ITEM_NORMAL)
        self.file.Append(wx.ID_CLOSE, _("&Close\tCtrl+W"), "", wx.ITEM_NORMAL)
        self.file.Append(wx.ID_EXIT, _("&Exit\tAlt+F4"), "", wx.ITEM_NORMAL)
        self.Append(self.file, "&File")
    def __events__(self):
        wx.EVT_MENU(self.frame,wx.ID_NEW,self.menu_new)
        wx.EVT_MENU(self.frame,wx.ID_CLOSE,self.menu_close)
        wx.EVT_MENU(self.frame,wx.ID_EXIT,self.menu_exit)
    def menu_new(self,event=None):
        self.parentPanel.new()
    def menu_close(self,event=None):
        self.parentPanel.close()
    def menu_exit(self,event=None):
        self.parentFrame.onFrameClose()

ArtIDs = [ wx.ART_FILE_OPEN,
           wx.ART_PRINT,
           wx.ART_ADD_BOOKMARK,
           wx.ART_REPORT_VIEW,
           wx.ART_LIST_VIEW,
           wx.ART_HELP,
           ]

class TestToolBar(wx.ToolBar):
    def __init__(self,parent=None,id=wx.ID_ANY,menu=None,app=None,**kwds):
        self.app = app
        wx.ToolBar.__init__(self,parent=parent,id=id,**kwds)
        parent.SetToolBar(self)
        self.tools = []
        for id in ArtIDs:
            toolId = wx.NewId()
            bmp = wx.ArtProvider_GetBitmap(id, wx.ART_TOOLBAR, (16,16))
            self.AddLabelTool(toolId, "", bmp, wx.NullBitmap, wx.ITEM_NORMAL, "info", "")
            self.tools.append(toolId)
        self.Realize()

    def __events__(self):
        for id in self.tools:
            wx.EVT_TOOL(self,id, self.test)
    def test(self,event):
        print 'test seems ok'

class TestParentPanel(wx.TextCtrl):
    def __init__(self,parent,**kwds):
        wx.TextCtrl.__init__(self,parent=parent,id=wx.ID_ANY,value='parent',**kwds)
        self.test_child = 0
    def new(self):
        value           = 'child%02d'%self.test_child
        self.app.ChildFrame(self.frame,page=value,value=value)
        self.test_child += 1
    def close(self):
        if self.app.children:
            active = self.app.childActive
            if active: active.frame.onFrameClose()

class TestChildPanel(wx.TextCtrl):
    def __init__(self,parent,**kwds):
        wx.TextCtrl.__init__(self,parent=parent,id=wx.ID_ANY,**kwds)

def __test__(debug,mdi=MDI):
    app = App(TestParentPanel,
              TestChildPanel,
              TestMenuBar,
              TestToolBar,
              wx.StatusBar,
              mdi=mdi,
              title='Parent',
              debug=debug)
    app.MainLoop()

if __name__=='__main__':
    __test__(debug=1,mdi=MDI_TABS_WIN)#multiple document interface for windows
    #__test__(debug=1,mdi=MDI_TABS_MAC)#single document interface for mac
    #__test__(debug=1,mdi=MDI_SPLIT_ALL)#multiple document interface for mac
