# Name:         core.py
# Purpose:      Core components
# Author:       Roman Rolinsky <rolinsky@femagsoft.com>
# Created:      31.05.2007
# RCS-ID:       $Id: core.py 49455 2007-10-26 10:58:11Z ROL $

from component import *
import images
import _bitmaps as bitmaps

TRACE('*** creating core components')

# Set panel images
Manager.panelImages['Windows'] = images.getToolPanel_WindowsImage()
Manager.panelImages['Menus'] = images.getToolPanel_MenusImage()
Manager.panelImages['Sizers'] = images.getToolPanel_SizersImage()
Manager.panelImages['Panels'] = images.getToolPanel_PanelsImage()
Manager.panelImages['Gizmos'] = images.getToolPanel_GizmosImage()

### wxFrame

class Frame(Container):
    def getChildObject(self, node, obj, index):
        # Do not count toolbar and menubar
        objects = filter(is_object, node.childNodes)
        for i,o in enumerate(objects):
            if o.getAttribute('class') == 'wxMenuBar':
                if i == index:  return obj.GetMenuBar()
                elif i < index: index -= 1
            elif o.getAttribute('class') == 'wxToolBar':
                if i == index:  return obj.GetToolBar()
                elif i < index: index -= 1
        return Container.getChildObject(self, node, obj, index)

c = Frame('wxFrame', ['frame','window','top_level'], 
              ['pos', 'size', 'title', 'centered'],
              image=images.getTreeFrameImage())
c.isTopLevel = True
c.addStyles('wxDEFAULT_FRAME_STYLE', 'wxDEFAULT_DIALOG_STYLE', 'wxCAPTION', 
            'wxSTAY_ON_TOP', 'wxSYSTEM_MENU', 'wxTHICK_FRAME',
            'wxRESIZE_BORDER', 'wxRESIZE_BOX', 'wxCLOSE_BOX',
            'wxMAXIMIZE_BOX', 'wxMINIMIZE_BOX',
            'wxFRAME_NO_TASKBAR', 'wxFRAME_SHAPED', 'wxFRAME_TOOL_WINDOW',
            'wxFRAME_FLOAT_ON_PARENT',
            'wxNO_3D', 'wxTAB_TRAVERSAL')
c.addExStyles('wxFRAME_EX_CONTEXTHELP', 'wxWS_EX_VALIDATE_RECURSIVELY', 'wxFRAME_EX_METAL')
c.addEvents('EVT_SIZE', 'EVT_CLOSE', 'EVT_MENU_HIGHLIGHT', 'EVT_ICONIZE', 'EVT_MAXIMIZE',
            'EVT_ACTIVATE', 'EVT_UPDATE_UI')
Manager.register(c)
Manager.setMenu(c, 'TOP_LEVEL', 'frame', 'wxFrame', 10)
Manager.setTool(c, 'Windows', bitmaps.getwxFrameBitmap(), (0,0))

### wxDialog

c = Container('wxDialog', ['frame','window','top_level'], 
              ['pos', 'size', 'title', 'centered', 'icon'],
              image=images.getTreeFrameImage())
c.isTopLevel = True
c.setSpecial('icon', BitmapAttribute)
c.addStyles('wxDEFAULT_DIALOG_STYLE', 'wxDEFAULT_FRAME_STYLE', 'wxCAPTION', 
            'wxSTAY_ON_TOP', 'wxSYSTEM_MENU', 'wxTHICK_FRAME',
            'wxRESIZE_BORDER', 'wxRESIZE_BOX', 'wxCLOSE_BOX',
            'wxMAXIMIZE_BOX', 'wxMINIMIZE_BOX',
            'wxDIALOG_MODAL', 'wxDIALOG_MODELESS', 'wxDIALOG_NO_PARENT',
            'wxNO_3D', 'wxTAB_TRAVERSAL')
c.addExStyles('wxDIALOG_EX_CONTEXTHELP', 'wxWS_EX_VALIDATE_RECURSIVELY', 'wxDIALOG_EX_METAL')
c.addEvents('EVT_INIT_DIALOG', 'EVT_SIZE', 'EVT_CLOSE', 
            'EVT_ICONIZE', 'EVT_MAXIMIZE', 'EVT_ACTIVATE', 'EVT_UPDATE_UI')
Manager.register(c)
Manager.setMenu(c, 'TOP_LEVEL', 'dialog', 'wxDialog', 20)
Manager.setTool(c, 'Windows', bitmaps.getwxDialogBitmap(), (0,1))

### wxPanel

c = Container('wxPanel', ['window', 'top_level', 'control'], 
              ['pos', 'size'],
              image=images.getTreePanelImage())
c.addStyles('wxNO_3D', 'wxTAB_TRAVERSAL')
Manager.register(c)
Manager.setMenu(c, 'TOP_LEVEL', 'panel', 'wxPanel', 30)
Manager.setMenu(c, 'container', 'panel', 'wxPanel', 10)
Manager.setTool(c, 'Windows', bitmaps.getwxPanelBitmap(), (0,2))

### wxWizard

class Wizard(Container):
    genericStyles = genericExStyles = []
    def makeTestWin(self, res, name):
        wiz = wx.wizard.PreWizard()
        res.LoadOnObject(wiz, view.frame, STD_NAME, self.klass)
        # Find and select first page
        firstPage = None
        for w in wiz.GetChildren():
            if isinstance(w, wx.wizard.WizardPage):
                firstPage = w
                break
        if firstPage:
            wiz.RunWizard(w)
        else:
            wx.LogMessage('Wizard is empty')
        wiz.Destroy()
        return None, None
c = Wizard('wxWizard', ['wizard', 'top_level'], 
           ['pos', 'title', 'bitmap'],
           image=images.getTreePanelImage())
c.addExStyles('wxWIZARD_EX_HELPBUTTON')
c.setSpecial('bitmap', BitmapAttribute)
Manager.register(c)
Manager.setMenu(c, 'TOP_LEVEL', 'wizard', 'wxWizard', 40)
Manager.setTool(c, 'Windows', bitmaps.getwxWizardBitmap(), (1,0), (1,2))

### wxWizardPage

c = Container('wxWizardPage', ['wizard_page', 'window'], ['bitmap'],
              image=images.getTreePanelImage())
c.setSpecial('bitmap', BitmapAttribute)
Manager.register(c)
Manager.setMenu(c, 'container', 'wizard page', 'wxWizardPage')

### wxWizardPageSimple

c = Container('wxWizardPageSimple', ['wizard_page', 'window'], ['bitmap'],
              image=images.getTreePanelImage())
c.setSpecial('bitmap', BitmapAttribute)
Manager.register(c)
Manager.setMenu(c, 'container', 'simple wizard page', 'wxWizardPageSimple')

### wxPropertySheetDialog

class ParamButtons(params.ParamBinaryOr):
    '''Button flags.'''
    values = ['wxOK', 'wxCANCEL', 'wxYES', 'wxNO', 'wxHELP', 'wxNO_DEFAULT']
class ParamSheetStyle(params.ParamBinaryOr):
    '''Button flags.'''
    values = ['wxPROPSHEET_DEFAULT', 'wxPROPSHEET_NOTEBOOK',
            'wxPROPSHEET_CHOICEBOOK', 'wxPROPSHEET_LISTBOOK', 
# Tool book needs an image list
#            'wxPROPSHEET_TOOLBOOK', 'wxPROPSHEET_BUTTONTOOLBOOK',
            'wxPROPSHEET_TREEBOOK', 'wxPROPSHEET_SHRINKTOFIT']
c = SmartContainer('wxPropertySheetDialog', ['frame','book','window','top_level'], 
                   ['pos', 'size', 'title', 'centered', 'icon', 'sheetstyle', 'buttons'],
                   params={'buttons': ParamButtons, 'sheetstyle': ParamSheetStyle},
                   implicit_klass='propertysheetpage', 
                   implicit_page='PropertySheetPage', 
                   implicit_attributes=['label', 'selected', 'bitmap'],
                   implicit_params={'label': params.ParamText, 'selected': params.ParamBool},
                   image=images.getTreeDialogImage())
c.isTopLevel = True
c.setSpecial('bitmap', BitmapAttribute)
c.setSpecial('icon', BitmapAttribute)
c.addStyles('wxDEFAULT_DIALOG_STYLE', 'wxCAPTION', 'wxFRAME_SHAPED',
            'wxTAB_TRAVERSAL', 'wxSTAY_ON_TOP', 'wxSYSTEM_MENU', 
            'wxRESIZE_BORDER', 'wxCLOSE_BOX', 'wxMAXIMIZE_BOX', 'wxMINIMIZE_BOX',
            'wxDIALOG_MODAL', 'wxDIALOG_MODELESS', 'wxDIALOG_NO_PARENT',
            'wxNO_3D', 'wxTAB_TRAVERSAL')
c.addExStyles('wxDIALOG_EX_CONTEXTHELP', 'wxWS_EX_VALIDATE_RECURSIVELY', 'wxDIALOG_EX_METAL')
c.addEvents('EVT_INIT_DIALOG', 'EVT_SIZE', 'EVT_CLOSE', 
            'EVT_ICONIZE', 'EVT_MAXIMIZE', 'EVT_ACTIVATE', 'EVT_UPDATE_UI')
Manager.register(c)
Manager.setMenu(c, 'TOP_LEVEL', 'propery sheet dialog', 'wxPropertySheetDialog', 50)
Manager.setTool(c, 'Windows', bitmaps.getwxPropertySheetDialogBitmap(), (1,1))

### wxBoxSizer

c = BoxSizer('wxBoxSizer', ['sizer'], ['orient'], 
             defaults={'orient': 'wxVERTICAL'},
             images=[images.getTreeSizerVImage(), images.getTreeSizerHImage()])
Manager.register(c)
Manager.setMenu(c, 'sizer', 'box sizer', 'wxBoxSizer', 10)
Manager.setTool(c, 'Sizers', pos=(0,0))

### wxStaticBoxSizer

c = BoxSizer('wxStaticBoxSizer', ['sizer'], ['label', 'orient'], 
             defaults={'orient': 'wxVERTICAL'},
             images=[images.getTreeSizerVImage(), images.getTreeSizerHImage()])
Manager.register(c)
Manager.setMenu(c, 'sizer', 'static box sizer', 'wxStaticBoxSizer', 20)
Manager.setTool(c, 'Sizers', pos=(0,2))

### wxGridSizer

c = Sizer('wxGridSizer', ['sizer'],
          ['cols', 'rows', 'vgap', 'hgap'], 
          defaults={'cols': '2', 'rows': '2'},
          image=images.getTreeSizerGridImage())
Manager.register(c)
Manager.setMenu(c, 'sizer', 'grid sizer', 'wxGridSizer', 30)
Manager.setTool(c, 'Sizers', pos=(0,1))

### wxFlexGridSizer

c = Sizer('wxFlexGridSizer', ['sizer'],
          ['cols', 'rows', 'vgap', 'hgap', 'growablecols', 'growablerows'],
          defaults={'cols': '2', 'rows': '2'},
          image=images.getTreeSizerFlexGridImage())
c.setSpecial('growablecols', MultiAttribute)
c.setParamClass('growablecols', params.ParamIntList)
c.setSpecial('growablerows', MultiAttribute)
c.setParamClass('growablerows', params.ParamIntList)
Manager.register(c)
Manager.setMenu(c, 'sizer', 'flex grid sizer', 'wxFlexGridSizer', 40)
Manager.setTool(c, 'Sizers', pos=(1,0))

### wxGridBagSizer

c = Sizer('wxGridBagSizer', ['sizer'],
          ['vgap', 'hgap', 'growablecols', 'growablerows'],
          image=images.getTreeSizerGridBagImage(),
          implicit_attributes=['option', 'flag', 'border', 'minsize', 'ratio', 'cellpos', 'cellspan'])
c.setSpecial('growablecols', MultiAttribute)
c.setParamClass('growablecols', params.ParamIntList)
c.setSpecial('growablerows', MultiAttribute)
c.setParamClass('growablerows', params.ParamIntList)
c.setImplicitParamClass('cellpos', params.ParamPosSize)
c.setImplicitParamClass('cellspan', params.ParamPosSize)
Manager.register(c)
Manager.setMenu(c, 'sizer', 'grid bag sizer', 'wxGridBagSizer', 50)
Manager.setTool(c, 'Sizers', pos=(1,1))

### wxStdDialogButtonSizer

class StdDialogButtonSizer(Sizer):
    def getChildObject(self, node, obj, index):
        # This sizer orders buttons by fixed ordering, so we must
        # get the ID to find them
        try:
            n = filter(is_object, node.childNodes)[index]
            n = filter(is_object, n.childNodes)[0]
            id = n.getAttribute('name')
        except IndexError:
            return None
        items = filter(wx.SizerItem.IsWindow, obj.GetChildren())
        for item in items:
            w = item.GetWindow()
            if w.GetName() == id: return w
        return None
c = StdDialogButtonSizer('wxStdDialogButtonSizer', ['btnsizer'], [],
          implicit_klass='button', 
          implicit_attributes=[])
Manager.register(c)
Manager.setMenu(c, 'sizer', 'dialog button sizer', 'wxStdDialogButtonSizer', 60)
#Manager.setTool(c, 'Sizers', pos=(0,2))

### spacer

c = SimpleComponent('spacer', ['spacer'], ['size', 'option', 'flag', 'border'])
c.hasName = False
Manager.register(c)
Manager.setMenu(c, 'sizer', 'spacer', 'spacer', 70)
Manager.setTool(c, 'Sizers', pos=(1,2))

################################################################################
# Containers

# wxPanel is already added

### wxScrolledWindow

c = Component('wxScrolledWindow', ['window', 'control'], ['pos', 'size'])
c.addStyles('wxHSCROLL', 'wxVSCROLL', 'wxNO_3D', 'wxTAB_TRAVERSAL')
c.addEvents('EVT_SCROLLWIN_TOP',
            'EVT_SCROLLWIN_BOTTOM',
            'EVT_SCROLLWIN_LINEUP',
            'EVT_SCROLLWIN_LINEDOWN',
            'EVT_SCROLLWIN_PAGEUP',
            'EVT_SCROLLWIN_PAGEDOWN',
            'EVT_SCROLLWIN_THUMBTRACK',
            'EVT_SCROLLWIN_THUMBRELEASE')
Manager.register(c)
Manager.setMenu(c, 'container', 'scrolled window', 'wxScrolledWindow', 20)
Manager.setTool(c, 'Panels', pos=(1,4))

### wxSplitterWindow

c = Container('wxSplitterWindow', ['book', 'window', 'control'],
              ['pos', 'size', 'orientation', 'sashpos', 'minsize'],
              params={'orientation': params.ParamOrientation, 
                      'sashpos': params.ParamUnit, 
                      'minsize': params.ParamUnit})
c.addStyles('wxSP_3D', 'wxSP_3DSASH', 'wxSP_3DBORDER', 
            'wxSP_FULLSASH', 'wxSP_NOBORDER', 'wxSP_PERMIT_UNSPLIT', 'wxSP_LIVE_UPDATE',
            'wxSP_NO_XP_THEME')
c.addEvents('EVT_SPLITTER_SASH_POS_CHANGING', 'EVT_SPLITTER_SASH_POS_CHANGED',
            'EVT_SPLITTER_UNSPLIT', 'EVT_SPLITTER_DCLICK')
Manager.register(c)
Manager.setMenu(c, 'container', 'splitter window', 'wxSplitterWindow', 30)
Manager.setTool(c, 'Panels', pos=(0,4))

### wxNotebook

c = SmartContainer('wxNotebook', ['book', 'window', 'control'], ['pos', 'size'], 
                   implicit_klass='notebookpage', 
                   implicit_page='NotebookPage', 
                   implicit_attributes=['label', 'selected', 'bitmap'],
                   implicit_params={'label': params.ParamText, 'selected': params.ParamBool})
c.addStyles('wxNB_TOP', 'wxNB_LEFT', 'wxNB_RIGHT', 'wxNB_BOTTOM',
            'wxNB_FIXEDWIDTH', 'wxNB_MULTILINE', 'wxNB_NOPAGETHEME', 
            'wxNB_FLAT')
c.setSpecial('bitmap', BitmapAttribute)
c.addEvents('EVT_NOTEBOOK_PAGE_CHANGED', 'EVT_NOTEBOOK_PAGE_CHANGING')
Manager.register(c)
Manager.setMenu(c, 'container', 'notebook', 'Notebook control', 40)
Manager.setTool(c, 'Panels', pos=(1,0))

### wxChoicebook

c = SmartContainer('wxChoicebook', ['book', 'window', 'control'], ['pos', 'size'],
                   implicit_klass='choicebookpage', 
                   implicit_page='ChoicebookPage', 
                   implicit_attributes=['label', 'selected', 'bitmap'],
                   implicit_params={'label': params.ParamText, 'selected': params.ParamBool})
c.addStyles('wxCHB_DEFAULT', 'wxCHB_LEFT', 'wxCHB_RIGHT', 'wxCHB_TOP', 'wxCHB_BOTTOM')
c.setSpecial('bitmap', BitmapAttribute)
c.addEvents('EVT_CHOICEBOOK_PAGE_CHANGED', 'EVT_CHOICEBOOK_PAGE_CHANGING')
Manager.register(c)
Manager.setMenu(c, 'container', 'choicebook', 'wxChoicebook', 50)
Manager.setTool(c, 'Panels', pos=(1,3))

### wxListbook

class ListBook(SmartContainer):
    def getChildObject(self, node, obj, index):
        # Listbook's first child is ListView
        return obj.GetChildren()[index+1]
c = ListBook('wxListbook', ['book', 'window', 'control'], ['pos', 'size'],
             implicit_klass='listbookpage', 
             implicit_page='ListbookPage', 
             implicit_attributes=['label', 'selected', 'bitmap'],
             implicit_params={'label': params.ParamText, 'selected': params.ParamBool})
c.addStyles('wxLB_DEFAULT', 'wxLB_LEFT', 'wxLB_RIGHT', 'wxLB_TOP', 'wxLB_BOTTOM')
c.setSpecial('bitmap', BitmapAttribute)
c.addEvents('EVT_LISTBOOK_PAGE_CHANGED', 'EVT_LISTBOOK_PAGE_CHANGING')
Manager.register(c)
Manager.setMenu(c, 'container', 'listbook', 'wxListbook', 60)
Manager.setTool(c, 'Panels', pos=(0,3))

### wxTreebook

class TreeBook(SmartContainer):
    def getChildObject(self, node, obj, index):
        # Listbook's first child is ListView
        return obj.GetChildren()[index+1]
c = TreeBook('wxTreebook', ['book', 'window', 'control'], ['pos', 'size'],
             implicit_klass='treebookpage', 
             implicit_page='TreebookPage', 
             implicit_attributes=['label', 'selected', 'bitmap', 'depth'],
             implicit_params={'label': params.ParamText, 
                              'selected': params.ParamBool, 
                              'depth': params.ParamInt})
c.addStyles('wxBK_DEFAULT', 'wxBK_LEFT', 'wxBK_RIGHT', 'wxBK_TOP', 'wxBK_BOTTOM')
c.setSpecial('bitmap', BitmapAttribute)
c.addEvents('EVT_TREEBOOK_PAGE_CHANGED', 'EVT_TREEBOOK_PAGE_CHANGING',
            'EVT_TREEBOOK_NODE_COLLAPSED', 'EVT_TREEBOOK_NODE_EXPANDED')
Manager.register(c)
Manager.setMenu(c, 'container', 'treebook', 'wxTreebook', 70)
Manager.setTool(c, 'Panels', pos=(1,1), span=(1,2))

################################################################################
# Menus

### wxMenuBar

class CMenuBar(SimpleContainer):
    # Menubar should be shown in a normal frame
    def makeTestWin(self, res, name):
        '''Method can be overrided by derived classes to create test view.'''
        frame = wx.Frame(None, -1, '%s: %s' % (self.klass, name), name=STD_NAME)
        object = res.LoadMenuBarOnFrame(frame, STD_NAME)
        return None, frame
    def getRect(self, obj):
        return None

c = CMenuBar('wxMenuBar', ['menubar', 'top_level'], [],
             image=images.getTreeMenuBarImage())
c.addStyles('wxMB_DOCKABLE')
c.addEvents('EVT_MENU', 'EVT_MENU_OPEN', 'EVT_MENU_CLOSE', 'EVT_MENU_HIGHLIGHT_ALL')
Manager.register(c)
Manager.setMenu(c, 'TOP_LEVEL', 'menu bar', 'wxMenuBar', 40)
Manager.setMenu(c, 'bar', 'menu bar', 'wxMenuBar', 10)
Manager.setTool(c, 'Menus', pos=(1,0))

### wxMenu

c = SimpleContainer('wxMenu', ['menu', 'top_level'], ['label', 'help'],
                    image=images.getTreeMenuImage())
c.addStyles('wxMENU_TEAROFF')
c.addEvents('EVT_MENU', 'EVT_MENU_OPEN', 'EVT_MENU_CLOSE', 'EVT_MENU_HIGHLIGHT_ALL')
Manager.register(c)
Manager.setMenu(c, 'TOP_LEVEL', 'menu', 'wxMenu', 50)
Manager.setMenu(c, 'ROOT', 'menu', 'wxMenu', 20)
Manager.setTool(c, 'Menus', pos=(1,1), span=(2,1))

### wxMenuItem

c = SimpleComponent('wxMenuItem', ['menu_item'],
                    ['label', 'bitmap', 'accel', 'help',
                     'checkable', 'radio', 'enabled', 'checked'],
                    image=images.getTreeMenuItemImage())
c.setSpecial('bitmap', BitmapAttribute)
c.addEvents('EVT_MENU', 'EVT_MENU_HIGHLIGHT')
Manager.register(c)
Manager.setMenu(c, 'ROOT', 'menu item', 'wxMenuItem', 10)
Manager.setTool(c, 'Menus', pos=(1,2))

### wxToolBar

class CToolBar(SimpleContainer):
    # Toolbar should be shown in a normal frame
    def makeTestWin(self, res, name):
        '''Method can be overrided by derived classes to create test view.'''
        frame = wx.Frame(None, -1, '%s: %s' % (self.klass, name), name=STD_NAME)
        object = res.LoadToolBar(frame, STD_NAME)
        return None, frame
    def getRect(self, obj):
        return None

c = CToolBar('wxToolBar', ['toolbar', 'top_level'],
             ['bitmapsize', 'margins', 'packing', 'separation',
              'dontattachtoframe', 'pos', 'size'],
             image=images.getTreeToolBarImage())
c.addStyles('wxTB_FLAT', 'wxTB_DOCKABLE', 'wxTB_VERTICAL', 'wxTB_HORIZONTAL',
            'wxTB_3DBUTTONS','wxTB_TEXT', 'wxTB_NOICONS', 'wxTB_NODIVIDER',
            'wxTB_NOALIGN', 'wxTB_HORZ_LAYOUT', 'wxTB_HORZ_TEXT')
c.setParamClass('dontattachtoframe', params.ParamBool)
c.setParamClass('bitmapsize', params.ParamPosSize)
c.setParamClass('margins', params.ParamPosSize)
c.setParamClass('packing', params.ParamUnit)
c.setParamClass('separation', params.ParamUnit)
c.renameDict = {'dontattachtoframe': "don't attach"}
c.addEvents('EVT_TOOL', 'EVT_TOOL_ENTER', 'EVT_TOOL_RCLICKED')
Manager.register(c)
Manager.setMenu(c, 'TOP_LEVEL', 'tool bar', 'wxToolBar', 50)
Manager.setMenu(c, 'bar', 'tool bar', 'wxToolBar', 20)
Manager.setTool(c, 'Menus', pos=(0,0))

### wxTool

c = SimpleComponent('tool', ['tool'],
                    ['bitmap', 'bitmap2', 'radio', 'toggle',
                     'tooltip', 'longhelp', 'label'],
                    image=images.getTreeToolImage())
Manager.register(c)
c.setSpecial('bitmap', BitmapAttribute)
c.setSpecial('bitmap2', BitmapAttribute)
c.setParamClass('bitmap2', params.ParamBitmap)
c.setParamClass('toggle', params.ParamBool)
c.addEvents('EVT_TOOL', 'EVT_TOOL_ENTER', 'EVT_TOOL_RCLICKED')
Manager.setMenu(c, 'ROOT', 'tool', 'wxTool', 10)
Manager.setTool(c, 'Menus', pos=(0,1))

### wxSeparator

c = SimpleComponent('separator', ['separator'], [],
                    image=images.getTreeSeparatorImage())
c.hasName = False
Manager.register(c)
Manager.setMenu(c, 'ROOT', 'separator', 'separator', 20)
Manager.setTool(c, 'Menus', pos=(0,2))

### wxStatusBar

c = SimpleComponent('wxStatusBar', ['statusbar'], ['fields', 'widths', 'styles'])
c.addStyles('wxST_SIZEGRIP')
c.setParamClass('fields', params.ParamIntP)
Manager.register(c)
Manager.setMenu(c, 'bar', 'status bar', 'wxStatusBar', 30)
Manager.setTool(c, 'Menus', pos=(2,0))

################################################################################

### wxBitmap

c = SimpleComponent('wxBitmap', ['top_level'], ['object'])
c.renameDict = {'object': ''}
c.setSpecial('object', BitmapAttribute)
c.setParamClass('object', params.ParamBitmap)
Manager.register(c)
Manager.setMenu(c, 'TOP_LEVEL', 'bitmap', 'wxBitmap', 60)

### wxIcon

c = SimpleComponent('wxIcon', ['top_level'], ['object'])
c.renameDict = {'object': ''}
c.setSpecial('object', BitmapAttribute)
c.setParamClass('object', params.ParamBitmap)
Manager.register(c)
Manager.setMenu(c, 'TOP_LEVEL', 'icon', 'wxIcon', 70)

### wxXXX

#c = Component('wxXXX', ['control','tool'],
#              ['pos', 'size', ...])
#c.addStyles(...)
#Manager.register(c)
#Manager.setMenu(c, 'control', 'XXX', 'wxXXX', NN)
