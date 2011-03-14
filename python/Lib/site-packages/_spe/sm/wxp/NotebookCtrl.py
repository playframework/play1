# --------------------------------------------------------------------------- #
# NOTEBOOKCTRL Control wxPython IMPLEMENTATION
# Python Code By:
#
# Andrea Gavana, @ 11 Nov 2005
# Latest Revision: 06 Oct 2006, 18.10 GMT
#
# 
# AKNOWLEDGEMENTS
#
# A big load of thanks goes to Julianne Sharer that has implemented the new
# features of left/right tabs, rotated or horizontal, with the ability to
# switch between the two views by a single mouse click. Moreover, all the
# work done to refactor NotebookCtrl in a more readable way has been done
# by Julianne Sharer. Thanks Julianne.
#
#
# TODO List/Caveats
#
# 1. Ay Idea?
#
# For All Kind Of Problems, Requests Of Enhancements And Bug Reports, Please
# Write To Me At:
#
# andrea.gavana@gmail.com
# gavana@kpo.kz
#
# Or, Obviously, To The wxPython Mailing List!!!
#
#
# End Of Comments
# --------------------------------------------------------------------------- #


"""
A full-featured notebook control, worked out by Andrea Gavana And Julianne Sharer.

Description:

NotebookCtrl Mimics The Behavior Of wx.Notebook, And Most Of Its Functionalities
Are Implemented In NotebookCtrl. However, NotebookCtrl Has A Lot Of Options That
wx.Notebook Does Not Have, And It Is Therefore Quite Customizable.
wx.Notebook Styles Not Implemented in NotebookCtrl Are:

    - wx.NB_MULTILINE (But NotebookCtrl Has A SpinButton To Navigate 
      Through Tabs).

Supported Customizations For NotebookCtrl Include:

    - Setting Individual Tab Font And Text Colour;
    - Images On Tabs (Line wx.Notebook);
    - Setting Individual Tab Colours;
    - Disabling/Enabling Individual Tabs (Also Visually Effective); 
      Now Supports Grayed Out Icons When A Page Is Disabled;
    - Drawing Of A Small Closing "X" At The Right Of Every Tab, That Enable The User
      To Close A Tab With A Mouse Click (Like eMule Tab Style);
    - Enabling Highlighted Tabs On Selection;
    - Drawing Focus Indicator In Each Tab (Like wx.Notebook);
    - Ctrl-Tab Keyboard Navigation Between Pages;
    - Tab With Animated Icons (Animation On Tabs);
    - Drag And Drop Tabs In NotebookCtrl (Plus A Visual Arrow Effect
      To Indicate Dropping Position);
    - Drag And Drop Event;
    - ToolTips On Individual Tabs, With Customizable ToolTip Time
      Popup And ToolTip Window Size For Individual Tabs;
    - Possibility To Hide The TabCtrl There Is Only One Tab (Thus
      Maximizing The Corresponding Window);
    - Possibility To Convert The Tab Image Into A Close Button While
      Mouse Is Hovering On The Tab Image;
    - Popup Menus On Tabs (Popup Menus Specific To Each Tab);
    - Showing Pages In "Column/Row Mode", Which Means That All Pages
      Will Be Shown In NotebookCtrl While The Tabs Are Hidden. They
      Can Be Shown In Columns (Default) Or In Rows;
    - Possibility To Hide Tabs On User Request, Thus Showing Only The 
      Current Panel;
    - Multiple Tabs Selection (Hold Ctrl Key Down And Left Mouse
      Click), Useful When You Use The Show All The Panels In
      Columns/Rows. In This Case, Only The Selected Tabs Are Shown In
      Columns/Rows;
    - Events For Mouse Events (Left Double Click, Middle Click, Right Click);
    - Possibility To Reparent A NotebookCtrl Page To A Freshly Created
      Frame As A Simple Panel Or To A New NotebookCtrl Created Inside
      That New Frame.
    - Possibility To Add A Custom Panel To Show A Logo Or HTML
      Information Or Whatever You Like When There Are No Tabs In
      NotebookCtrl;
    - Possibility To Change The ToolTip Window Background Colour;
    - Possibility To Draw Vertical Or Horizontal Gradient Coloured Tabs 
      (2 Colours);
    - Themes On Tabs: Built-In Themes Are KDE (Unix/Linux), Metal,
      Aqua Light And Aqua Dark (MacOS), Windows Silver (Windows) Or
      Generic Gradient Coloured Tabs. It's Also Possible To Define A
      Separate Theme For Selected Tabs And Control Background (The
      Last Two Are Work In Progress);
    - Contour Line Colour Around Tabs Is Customizable;
    - Highlight Colour Of Selected Tab Is Customizable;
    - Each Tab Can Have Its Own Gradient Colouring (2 Colours For Every Tab);
    - Custom Images May Be Drawn As A "X" Close Buttons On Tabs;
    - Possibility To Hide A Particular Tab Using A wx.PopupMenu That
      Is Shown If You Call EnableHiding(True). Look At The Top Right
      Of NotebookCtrl;
    - Allows Drag And Drop Of Tabs/Pages Between Different
      NotebookCtrls In The Same Application.
    - Draw tabs on the left or right side, rotated or horizontal
    - Allow user to switch between rotated and horizontal displays of
      tabs on the left or right side.

  
Usage:

NotebookCtrl Construction Is Quite Similar To wx.Notebook::

    NotebookCtrl.__init__(self, parent, id, pos=wx.DefaultPosition,
                          size=wx.DefaultSize, style=style, sizer=nbsizer)

See L{NotebookCtrl.__init__} Method For The Definition Of Non Standard (Non
wxPython) Parameters.

NotebookCtrl Control Is Freeware And Distributed Under The wxPython License. 

Latest Revision: Andrea Gavana @ 06 Oct 2006, 18.10 GMT

@undocumented: NC_MAC*, topaqua*, botaqua*, distaqua*, disbaqua*,
    kdetheme, silvertheme*, wxEVT*, attrs, GetMenuButton*, NCDragInfo,
    NCDropTarget, TabbedPage, TabCtrl, TransientTipWindow,
    macPopupWindow, macTransientTipWindow, NCFrame, DEFAULT_SIZE,
    NotebookSpinButton, NotebookMenuButton
"""

__docformat__ = "epytext"


#----------------------------------------------------------------------
# Beginning Of NOTEBOOKCTRL wxPython Code
#----------------------------------------------------------------------

import wx
from wx.lib.buttons import GenBitmapButton as BitmapButton
import wx.xrc  as  xrc

import cStringIO, zlib
import cPickle
import weakref

# HitTest Results 
NC_HITTEST_NOWHERE = 0   # Not On Tab 
"""Indicates mouse coordinates not on any tab of the notebook"""
NC_HITTEST_ONICON  = 1   # On Icon 
"""Indicates mouse coordinates on an icon in a tab of the notebook"""
NC_HITTEST_ONLABEL = 2   # On Label 
"""Indicates mouse coordinates on a label in a tab of the notebook"""
NC_HITTEST_ONITEM  = 4   # Generic, On Item
"""Indicates mouse coordinates on a tab of the notebook"""
NC_HITTEST_ONX = 8       # On Small Square On Every Page
"""Indicates mouse coordinates on the closing I{X} in a tab of the notebook"""

# NotebookCtrl Styles
# NotebookCtrl Placed On Top (Default)
NC_TOP = 1
"""Specify tabs at the top of the notebook control."""
# NotebookCtrl Placed At The Bottom
NC_BOTTOM = 2
"""Specify tabs at the bottom of the notebook control."""
# NotebookCtrl With Fixed Width Tabs
NC_FIXED_WIDTH = 4
"""Specify tabs of a fixed width in the notebook control."""
# NotebookCtrl Placed At The Left
NC_LEFT = 8
"""Specify tabs on the left side of the notebook control."""
# NotebookCtrl Placed At The Right
NC_RIGHT = 16
"""Specify tabs on the right side of the notebook control."""
# NotebookCtrl tab rotated
NC_ROTATE = 32
"""Specify rotated tabs (with vertical text) in the notebook control."""
# NotebookCtrl switchable between compact and expanded sizes
NC_EXPANDABLE = 64
"""Specify that the notebook control includes a toggle button to
switch between compact tabs (rotated on the left or right side) 
expanded tabs (horizontal on the left or right side)."""

NC_DEFAULT_STYLE = NC_TOP | wx.NO_BORDER
"""The default style for the notebook control (tabs on top with no border)"""
# Also wx.STATIC_BORDER Is Supported

# NotebookCtrl theme styles
NC_GRADIENT_VERTICAL = 1
"""Specify tabs rendered with a vertical gradient background."""
NC_GRADIENT_HORIZONTAL = 2
"""Specify tabs rendered with a horizontal gradient background."""
NC_GRADIENT_SELECTION = 4
NC_AQUA_LIGHT = 8
"""Specify tabs rendered with a Mac I{Light Aqua}-like background."""
NC_AQUA_DARK = 16
"""Specify tabs rendered with a Mac I{Dark Aqua}-like background."""
NC_AQUA = NC_AQUA_LIGHT
"""Specify tabs rendered with a Mac I{Light Aqua}-like background."""
NC_METAL = 32
"""Specify tabs rendered with a Mac I{Metal}-like background."""
NC_SILVER = 64
"""Specify tabs rendered with a Windows I{Silver}-like background."""
NC_KDE = 128
"""Specify tabs rendered with a KDE-style background."""

# Patch To Make NotebookCtrl Working Also On MacOS: Thanks To Stani ;-)
if wx.Platform == '__WXMAC__':
    DEFAULT_SIZE = wx.Size(26, 26)
else:
    DEFAULT_SIZE = wx.DefaultSize

# Themes On Mac... This May Slow Down The Paint Event If You Turn It On!
NC_MAC_LIGHT = (240, 236)
NC_MAC_DARK = (232, 228)

topaqua1 = [wx.Colour(106, 152, 231), wx.Colour(124, 173, 236)]
botaqua1 = [wx.Colour(54, 128, 213), wx.Colour(130, 225, 249)]

topaqua2 = [wx.Colour(176, 222, 251), wx.Colour(166, 211, 245)]
botaqua2 = [wx.Colour(120, 182, 244), wx.Colour(162, 230, 245)]

distaqua = [wx.Colour(248, 248, 248), wx.Colour(243, 243, 243)]
disbaqua = [wx.Colour(219, 219, 219), wx.Colour(248, 248, 248)]

# Themes On KDE... This May Slow Down The Paint Event If You Turn It On!
kdetheme = [wx.Colour(0xf3,0xf7,0xf9), wx.Colour(0xf3,0xf7,0xf9),
            wx.Colour(0xee,0xf3,0xf7), wx.Colour(0xee,0xf3,0xf7),
            wx.Colour(0xea,0xf0,0xf4), wx.Colour(0xea,0xf0,0xf4),
            wx.Colour(0xe6,0xec,0xf1), wx.Colour(0xe6,0xec,0xf1),
            wx.Colour(0xe2,0xe9,0xef), wx.Colour(0xe2,0xe9,0xef),
            wx.Colour(0xdd,0xe5,0xec), wx.Colour(0xdd,0xe5,0xec),
            wx.Colour(0xd9,0xe2,0xea), wx.Colour(0xd9,0xe2,0xea)]

# Themes On Windows... This May Slow Down The Paint Event If You Turn It On!
silvertheme2 = [wx.Colour(255, 255, 255), wx.Colour(190, 190, 216),
                wx.Colour(180, 180, 200)]
silvertheme1 = [wx.Colour(252, 252, 254), wx.Colour(252, 252, 254)]
           
# NotebookCtrl Events:
# wxEVT_NOTEBOOKCTRL_PAGE_CHANGED: Event Fired When You Switch Page;
# wxEVT_NOTEBOOKCTRL_PAGE_CHANGING: Event Fired When You Are About To Switch
# Pages, But You Can Still "Veto" The Page Changing By Avoiding To Call
# event.Skip() In Your Event Handler;
# wxEVT_NOTEBOOKCTRL_PAGE_CLOSING: Event Fired When A Page Is Closing, But
# You Can Still "Veto" The Page Changing By Avoiding To Call event.Skip()
# In Your Event Handler;
# wxEVT_NOTEBOOKCTRL_PAGE_DND: Event Fired When A Drag And Drop Action On
# Tabs Ends.
wxEVT_NOTEBOOKCTRL_PAGE_CHANGED = wx.NewEventType()
wxEVT_NOTEBOOKCTRL_PAGE_CHANGING = wx.NewEventType()
wxEVT_NOTEBOOKCTRL_PAGE_CLOSING = wx.NewEventType()
wxEVT_NOTEBOOKCTRL_PAGE_DND = wx.NewEventType()
wxEVT_NOTEBOOKCTRL_PAGE_DCLICK = wx.NewEventType()
wxEVT_NOTEBOOKCTRL_PAGE_RIGHT = wx.NewEventType()
wxEVT_NOTEBOOKCTRL_PAGE_MIDDLE = wx.NewEventType()

#-----------------------------------#
#        NotebookCtrlEvent
#-----------------------------------#

EVT_NOTEBOOKCTRL_PAGE_CHANGED = wx.PyEventBinder(wxEVT_NOTEBOOKCTRL_PAGE_CHANGED, 1)
"""Notify client objects when the active page in the notebook control 
has changed."""

EVT_NOTEBOOKCTRL_PAGE_CHANGING = wx.PyEventBinder(wxEVT_NOTEBOOKCTRL_PAGE_CHANGING, 1)
"""Notify client objects when the active page in the notebook control 
is changing."""

EVT_NOTEBOOKCTRL_PAGE_CLOSING = wx.PyEventBinder(wxEVT_NOTEBOOKCTRL_PAGE_CLOSING, 1)
"""Notify client objects when a page in the notebook control is closing."""

EVT_NOTEBOOKCTRL_PAGE_DND = wx.PyEventBinder(wxEVT_NOTEBOOKCTRL_PAGE_DND, 1)
"""Enable client objects to override the behavior of the notebook control
when a dragged tab is dropped onto it."""

EVT_NOTEBOOKCTRL_PAGE_DCLICK = wx.PyEventBinder(wxEVT_NOTEBOOKCTRL_PAGE_DCLICK, 1)
"""Notify client objects when the user double-clicks a tab
in the notebook control."""

EVT_NOTEBOOKCTRL_PAGE_RIGHT = wx.PyEventBinder(wxEVT_NOTEBOOKCTRL_PAGE_RIGHT, 1)
"""Notify client objects when the user right-clicks a tab
in the notebook control."""

EVT_NOTEBOOKCTRL_PAGE_MIDDLE = wx.PyEventBinder(wxEVT_NOTEBOOKCTRL_PAGE_MIDDLE, 1)
"""Notify client objects when the user clicks with the
middle mouse button on a tab in the notebook control."""

attrs = ["_backstyle", "_backtooltip", "_borderpen", "_convertimage", "_drawx",
         "_drawxstyle", "_enabledragging", "_focusindpen", "_hideonsingletab",
         "_highlight", "_padding", "_selectioncolour", "_selstyle", "_tabstyle",
         "_upperhigh", "_usefocus", "_usegradients"]


# Check for the new method in 2.7 (not present in 2.6.3.3)
if wx.VERSION_STRING < "2.7":
    wx.Rect.Contains = lambda self, point: wx.Rect.Inside(self, point)


# ---------------------------------------------------------------------------- #
def GetMenuButtonData():

    return zlib.decompress(
"x\xda\xeb\x0c\xf0s\xe7\xe5\x92\xe2b``\xe0\xf5\xf4p\t\x02\xd2\x9c \xcc\xc1\
\x06$\x1fLd\x13\x00R,\xc5N\x9e!\x1c@P\xc3\x91\xd2\x01\xe4[x\xba8\x86HL\xed\
\xbd`\xc8\xc7\xa0\xc0\xe1|q\xdb\x9d\xff'\xba\xb4\x1d\x05v\xff}\xe2\xab\x9a:c\
\x99\xc4\xbe\xe9\xfd+\x9a\xc5W%t\x1a\xe5\x08\xa6\xd6,\xe2\xf0\x9a\xc2\xc8\
\xf0\xe1\xf9r\xe6\xa3\xc9\x02b\xd9\x0c35\x80f0x\xba\xfa\xb9\xacsJh\x02\x00\
\xcd-%1")


def GetMenuButtonBitmap():

    return wx.BitmapFromImage(GetMenuButtonImage())


def GetMenuButtonImage():

    stream = cStringIO.StringIO(GetMenuButtonData())
    return wx.ImageFromStream(stream)

# ---------------------------------------------------------------------------- #

def GrayOut(anImage):
    """
    Convert The Given Image (In Place) To A Grayed-Out Version,
    Appropriate For A 'Disabled' Appearance.
    """
    
    factor = 0.7        # 0 < f < 1.  Higher Is Grayer
    
    if anImage.HasMask():
        maskColor = (anImage.GetMaskRed(), anImage.GetMaskGreen(), anImage.GetMaskBlue())
    else:
        maskColor = None
        
    data = map(ord, list(anImage.GetData()))

    for i in range(0, len(data), 3):
        
        pixel = (data[i], data[i+1], data[i+2])
        pixel = MakeGray(pixel, factor, maskColor)

        for x in range(3):
            data[i+x] = pixel[x]

    anImage.SetData(''.join(map(chr, data)))
    
    return anImage


def MakeGray((r,g,b), factor, maskColor):
    """
    Make A Pixel Grayed-Out. If The Pixel Matches The MaskColor, It Won't Be
    Changed.
    """
    
    if (r,g,b) != maskColor:
        return map(lambda x: int((230 - x) * factor) + x, (r,g,b))
    else:
        return (r,g,b)

def GetDefaultTabStyle():
    tabstyle = ThemeStyle()
    
    # Draw Mac Themes On Tabs?
    if wx.Platform == "__WXMAC__" or wx.Platform == "__WXCOCOA__":
        tabstyle.EnableAquaTheme(True, 2)
    # Draw Windows Silver Theme On Tabs?
    elif wx.Platform == "__WXMSW__":
        tabstyle.EnableSilverTheme(True)
    else:
        tabstyle.EnableKDETheme(True)
    
    return tabstyle
    
# ---------------------------------------------------------------------------- #
# Class NotebookCtrlEvent
# ---------------------------------------------------------------------------- #

class NotebookCtrlEvent(wx.PyCommandEvent):
    """
    Represent details of the events that the L{NotebookCtrl} object sends.
    """
    
    def __init__(self, eventType, id=1, nSel=-1, nOldSel=-1):
        """ Default Class Constructor. """
        
        wx.PyCommandEvent.__init__(self, eventType, id)
        self._eventType = eventType


    def SetSelection(self, nSel):
        """ Sets Event Selection. """
        
        self._selection = nSel
        

    def SetOldSelection(self, nOldSel):
        """ Sets Old Event Selection. """
        
        self._oldselection = nOldSel


    def GetSelection(self):
        """ Returns Event Selection. """
        
        return self._selection
        

    def GetOldSelection(self):
        """ Returns Old Event Selection """
        
        return self._oldselection


    def SetOldPosition(self, pos):
        """ Sets Old Event Position. """
        
        self._oldposition = pos        


    def SetNewPosition(self, pos):
        """ Sets New Event Position. """
        
        self._newposition = pos
        

    def GetOldPosition(self):
        """ Returns Old Event Position. """

        return self._oldposition


    def GetNewPosition(self):
        """ Returns New Event Position. """

        return self._newposition    
    

# ---------------------------------------------------------------------------- #
# Class NCDragInfo
# Stores All The Information To Allow Drag And Drop Between Different
# NotebookCtrls In The Same Application.
# ---------------------------------------------------------------------------- #

class NCDragInfo:

    _map = weakref.WeakValueDictionary()

    def __init__(self, container, pageindex):
        """ Default Class Constructor. """
        
        self._id = id(container)
        NCDragInfo._map[self._id] = container
        self._pageindex = pageindex


    def GetContainer(self):
        """ Returns The NotebookCtrl Page (Usually A Panel). """
        
        return NCDragInfo._map.get(self._id, None)


    def GetPageIndex(self):
        """ Returns The Page Index Associated With A Page. """

        return self._pageindex        


# ---------------------------------------------------------------------------- #
# Class NCDropTarget
# Simply Used To Handle The OnDrop() Method When Dragging And Dropping Between
# Different NotebookCtrls.
# ---------------------------------------------------------------------------- #

class NCDropTarget(wx.DropTarget):

    def __init__(self, parent):
        """ Default Class Constructor. """
        
        wx.DropTarget.__init__(self)

        self._parent = parent
        self._dataobject = wx.CustomDataObject(wx.CustomDataFormat("NotebookCtrl"))
        self.SetDataObject(self._dataobject)


    def OnData(self, x, y, dragres):
        """ Handles The OnData() Method TO Call The Real DnD Routine. """
        
        if not self.GetData():
            return wx.DragNone

        draginfo = self._dataobject.GetData()
        drginfo = cPickle.loads(draginfo)
        
        return self._parent.OnDropTarget(x, y, drginfo.GetPageIndex(), drginfo.GetContainer())


# ---------------------------------------------------------------------------- #
# Class ThemeStyle. Used To Define A Custom Style For Tabs And Control
# Background Colour.
# ---------------------------------------------------------------------------- #

class ThemeStyle:
    """
    Represent the style for rendering a notebook tab.
    """
    
    GRADIENT_VERTICAL = 1
    GRADIENT_HORIZONTAL = 2
    DIFFERENT_GRADIENT_FOR_SELECTED = 4

    def __init__(self):
        """ Default Constructor For This Class."""
        
        self.ResetDefaults()


    def ResetDefaults(self):
        """ Resets Default Theme. """

        self._normal = True
        self._aqua = False
        self._metal = False
        self._macstyle = False
        self._kdetheme = False
        self._silver = False
        self._gradient = False
        self._firstcolour = wx.WHITE
        self._secondcolour = wx.SystemSettings_GetColour(wx.SYS_COLOUR_BTNFACE)
        self._firstcolourselected = wx.WHITE
        self._secondcolourselected = wx.SystemSettings_GetColour(wx.SYS_COLOUR_BTNFACE)


    def EnableMacTheme(self, enable=True, style=1):
        """
        Enables/Disables Mac Themes. style=1 Is The Light Style, While style=2
        Is The Dark Style. Mainly Used For Control Background Colour, Not For Tabs.
        """
        
        if enable:
            self._normal = False
            self._macstyle = style
            self._kdetheme = False
            self._metal = False
            self._aqua = False
            self._silver = False
            self._gradient = False
        else:
            self._macstyle = 0


    def EnableKDETheme(self, enable=True):
        """ Globally Enables/Disables Unix-Like KDE Theme For Tabs. """
        
        self._kdetheme = enable
        
        if enable:
            self._normal = False
            self._macstyle = False
            self._metal = False
            self._aqua = False
            self._silver = False
            self._gradient = False


    def EnableMetalTheme(self, enable=True):
        """ Globally Enables/Disables Mac-Like Metal Theme For Tabs. """
        
        self._metal = enable

        if enable:
            self._normal = False
            self._macstyle = False
            self._kdetheme = False
            self._aqua = False
            self._silver = False
            self._gradient = False
            

    def EnableAquaTheme(self, enable=True, style=1):
        """ Globally Enables/Disables Mac-Like Aqua Theme For Tabs. """

        if enable:            
            self._aqua = style
            self._normal = False
            self._macstyle = False
            self._kdetheme = False
            self._metal = False
            self._silver = False
            self._gradient = False
        else:
            self._aqua = 0


    def EnableSilverTheme(self, enable=True):
        """ Globally Enables/Disables Windows Silver Theme For Tabs. """

        self._silver = enable
        
        if enable:
            self._normal = False
            self._macstyle = False
            self._kdetheme = False
            self._metal = False
            self._aqua = False
            self._gradient = False
        

    def EnableGradientStyle(self, enable=True, style=1):
        """
        Enables/Disables Gradient Drawing On Tabs. style=1 Is The Vertical Gradient,
        While style=2 Is The Horizontal Gradient.
        If style flag 4 is set, the style has a separate set of colors for the
        selected tab.
        """

        if enable:
            self._normal = False
            if style & self.GRADIENT_VERTICAL == 0 and style & self.GRADIENT_HORIZONTAL == 0:
                style |= self.GRADIENT_VERTICAL
            self._gradient = style
            self._macstyle = False
            self._kdetheme = False
            self._metal = False
            self._aqua = False
            self._silver = False
        else:
            self._gradient = 0
            

    def SetFirstGradientColour(self, colour=None):
        """ Sets The First Gradient Colour. """
        
        if colour is None:
            colour = wx.WHITE

        self._firstcolour = colour
        
    def SetFirstGradientColourSelected(self, colour=None):
        """Sets The First Gradient Colour For The Selected Tab."""
        if colour is None:
            colour = wx.WHITE
            
        self._firstcolourselected = colour

    def SetSecondGradientColour(self, colour=None):
        """ Sets The Second Gradient Colour. """

        if colour is None:
            color = self.GetBackgroundColour()
            r, g, b = int(color.Red()), int(color.Green()), int(color.Blue())
            color = ((r >> 1) + 20, (g >> 1) + 20, (b >> 1) + 20)
            colour = wx.Colour(color[0], color[1], color[2])

        self._secondcolour = colour

    def SetSecondGradientColourSelected(self, colour=None):
        """ Sets The Second Gradient Colour For The Selected Tab. """

        if colour is None:
            color = self.GetBackgroundColour()
            r, g, b = int(color.Red()), int(color.Green()), int(color.Blue())
            color = ((r >> 1) + 20, (g >> 1) + 20, (b >> 1) + 20)
            colour = wx.Colour(color[0], color[1], color[2])

        self._secondcolourselected = colour

    def GetFirstGradientColour(self, selected=False):
        """ Returns The First Gradient Colour. """
        
        if selected and self._gradient & self.DIFFERENT_GRADIENT_FOR_SELECTED:
            return self._firstcolourselected
        else:
            return self._firstcolour

    def GetSecondGradientColour(self, selected=False):
        """ Returns The Second Gradient Colour. """
        
        if selected and self._gradient & self.DIFFERENT_GRADIENT_FOR_SELECTED:
            return self._secondcolourselected
        else:
            return self._secondcolour
            

# ---------------------------------------------------------------------------- #
# Class TabbedPage
# This Is Just A Container Class That Initialize All The Default Settings For
# Every Tab.
# ---------------------------------------------------------------------------- #

class TabbedPage:

    def __init__(self, text="", image=-1, hidden=False):
        """ Default Class Constructor. """
        
        self._text = text
        self._image = image
        self._font = wx.SystemSettings_GetFont(wx.SYS_DEFAULT_GUI_FONT)
        self._secondaryfont = wx.SystemSettings_GetFont(wx.SYS_DEFAULT_GUI_FONT)
        self._pagetextcolour = wx.BLACK
        self._pagecolour = wx.SystemSettings_GetColour(wx.SYS_COLOUR_BTNFACE)
        self._enable = True
        self._animationimages = []
        self._tooltip = ""
        self._tooltiptime = 500
        self._winsize = 400
        self._menu = None
        self._ishidden = hidden
        self._firstcolour = color = wx.WHITE
        r, g, b = int(color.Red()), int(color.Green()), int(color.Blue())
        color = ((r >> 1) + 20, (g >> 1) + 20, (b >> 1) + 20)
        colour = wx.Colour(color[0], color[1], color[2])
        self._secondcolour = colour
        

# ---------------------------------------------------------------------------- #
# Class NotebookSpinButton
# This SpinButton Is Created/Shown Only When The Total Tabs Size Exceed The
# Client Size, Allowing The User To Navigate Between Tabs By Clicking On The
# SpinButton. It Is Very Similar To The wx.Notebook SpinButton
# ---------------------------------------------------------------------------- #

class NotebookSpinButton(wx.SpinButton):

    def __init__(self, parent, id=wx.ID_ANY, pos=wx.DefaultPosition,
                 size=wx.DefaultSize, style=wx.SP_HORIZONTAL):
        """ Default Class Constructor. """
        
        wx.SpinButton.__init__(self, parent, id, pos, size, style)        
        self._nb = parent
        self._oldvalue = 0
        self._style = style
        self.Bind(wx.EVT_SPIN, self.OnSpin)
        
    def GetValue(self):
        result = super(NotebookSpinButton, self).GetValue()
        if self._style & wx.SP_VERTICAL:
            result = -result
        return result

    def OnSpin(self, event):
        """ Handles The User's Clicks On The SpinButton. """

        if type(event) != type(1):
            pos = event.GetPosition()
        else:
            pos = event
            
        if pos < self.GetMin():
            self.SetValue(self.GetMin())
            return

        if type(event) != type(1):
            if self._nb._enablehiding:
                if pos < self._oldvalue:
                    incr = -1
                else:
                    incr = 1
                while self._nb._pages[pos]._ishidden:
                    pos = pos + incr
                    
            self.SetValue(pos)
    
        if self._nb.IsLastVisible():
            if (self._style & wx.SP_HORIZONTAL and self._oldvalue < pos) or \
                (self._style & wx.SP_VERTICAL and self._oldvalue > pos):
                self.SetValue(self._oldvalue)
                return

        self._oldvalue = pos
        
        self._nb.Refresh()
                

# ---------------------------------------------------------------------------- #
# Class NotebookMenuButton
# This MenuButton Is Created/Shown Only When You Activate The Option EnableHiding
# Of NotebookCtrl. This Small Button Will Be Shown Right Above The Spin Button
# (If Present), Or In The Position Of The Spin Button.
# ---------------------------------------------------------------------------- #

class NotebookMenuButton(BitmapButton):

    def __init__(self, parent, id=-1, pos=wx.DefaultPosition, size=(15, 11),
                 style=0):
        """ Default Class Constructor. """
        
        bmp = GetMenuButtonBitmap()
        
        BitmapButton.__init__(self, parent, id, bmp, pos, size, style)
        
        self.SetUseFocusIndicator(False)
        self.SetBezelWidth(1)
        
        self._originalcolour = self.GetBackgroundColour()
        self._nb = parent
        
        self.Bind(wx.EVT_BUTTON, self.OnButton)
        self.Bind(wx.EVT_ENTER_WINDOW, self.OnEnterWindow)
        self.Bind(wx.EVT_LEAVE_WINDOW, self.OnLeaveWindow)
        self.Bind(wx.EVT_MENU, self.OnMenu)


    def OnButton(self, event):
        """ Handles The wx.EVT_BUTTON For NotebookMenuButton (Opens The wx.PopupMenu) """
        
        count = self._nb.GetPageCount()

        if count <= 0:
            return
        
        menu = wx.Menu()
        id = wx.NewId()
        myids = []
        
        for ii in xrange(count):

            id = id + 1
            myids.append(id)
            name = self._nb.GetPageText(ii)

            if self._nb._pages[ii]._ishidden:
                msg = "Page Hidden"
                check = False
            else:
                msg = "Page Shown"
                check = True

            item = wx.MenuItem(menu, id, name, msg, wx.ITEM_CHECK)
            menu.AppendItem(item)
            
            if not self._nb._pages[ii]._ishidden:
                item.Check()

            menu.SetHelpString(id, msg)

        self._myids = myids
        
        self.PopupMenu(menu)
        
        event.Skip()


    def OnMenu(self, event):
        """ Handles The wx.EVT_MENU For NotebookMenuButton. Calls HideTab(). """

        indx = self._myids.index(event.GetId())
        checked = not event.GetEventObject().IsChecked(event.GetId())

        self._nb.HideTab(indx, not checked)
                
        event.Skip()
        

    def OnEnterWindow(self, event):
        """
        Changes The NotebookMenuButton Background Colour When The Mouse
        Enters The Button Region.
        """
        
        entercolour = self.GetBackgroundColour()
        firstcolour  = entercolour.Red()
        secondcolour = entercolour.Green()
        thirdcolour = entercolour.Blue()
        
        if entercolour.Red() > 235:
            firstcolour = entercolour.Red() - 40
        if entercolour.Green() > 235:
            secondcolour = entercolour.Green() - 40
        if entercolour.Blue() > 235:
            thirdcolour = entercolour.Blue() - 40
            
        entercolour = wx.Colour(firstcolour+20, secondcolour+20, thirdcolour+20)
        
        self.SetBackgroundColour(entercolour)
        self.Refresh()
        
        event.Skip()


    def OnLeaveWindow(self, event):
        """
        Restore The NotebookMenuButton Background Colour When The Mouse
        Leaves The Button Region.
        """
        
        self.SetBackgroundColour(self._originalcolour)
        self.Refresh()

        event.Skip()
        
class _TabCtrlPaintTools(object):
    # Structure-like object for passing data among
    # private rendering methods
    def __init__(self, backBrush, backPen, borderPen, highlightPen, 
        shadowPen, upperHighlightPen, selectionPen, selectionEdgePen,
        xPen, focusPen):
        self.BackBrush = backBrush
        self.BackPen = backPen
        self.BorderPen = borderPen
        self.HighlightPen = highlightPen
        self.ShadowPen = shadowPen
        self.UpperHighlightPen = upperHighlightPen
        self.SelectionPen = selectionPen
        self.SelectionEdgePen = selectionEdgePen
        self.XPen = xPen
        self.FocusPen = focusPen
    

# ---------------------------------------------------------------------------- #
# Class TabCtrl
# This Class Handles The Drawing Of Every Tab In The NotebookCtrl, And Also
# All Settings/Methods For Every Tab.
# ---------------------------------------------------------------------------- #

class TabCtrl(wx.PyControl):

    def __init__(self, parent, id=wx.ID_ANY, pos=wx.DefaultPosition,
                 size=DEFAULT_SIZE, style=NC_DEFAULT_STYLE,
                 validator=wx.DefaultValidator, name="TabCtrl"):
        """
        Default Class Constructor.
        Used Internally. Do Not Call It Explicitely!
        """        

        wx.PyControl.__init__(self, parent, id, pos, size, wx.NO_BORDER | wx.WANTS_CHARS,
                              validator, name)

        # Set All The Default Parameters For TabCtrl        
        self._selection = -1
        self._imglist = 0
        self._style = style
        self._expanded = False
        self._pages = []
        self._enabledpages = []

        self._padding = wx.Point(8, 4)
        self._spacetabs = 2
        self._xrect = []
        self._xrefreshed = False
        self._imageconverted = False
        self._convertimage = False
        self._disabledcolour = wx.SystemSettings_GetColour(wx.SYS_COLOUR_GRAYTEXT)

        self._hover = False
        self._parent = parent
        self._firsttime = True
        self._somethingchanged = True
        self._isdragging = False
        self._tabID = -1
        self._enabledragging = False
        self._olddragpos = -1
        self._fromdnd = False
        self._isleaving = False
        self._highlight = False
        self._usefocus = True
        self._hideonsingletab = False
        self._selectioncolour = wx.Colour(255, 200, 60)
        self._selectionedgecolour = wx.Colour(230, 139, 44)

        self._tabstyle = ThemeStyle()
        self._backstyle = ThemeStyle()
        self._selstyle = ThemeStyle()
        self._usegradients = False
        
        self._insidetab = -1        
        self._showtooltip = False
        self._istooltipshown = False
        self._tipwindow = None
        self._tiptimer = wx.PyTimer(self.OnShowToolTip)
        self._backtooltip = wx.Colour(255, 255, 230)
        self._xvideo = wx.SystemSettings_GetMetric(wx.SYS_SCREEN_X)
        self._yvideo = wx.SystemSettings_GetMetric(wx.SYS_SCREEN_Y)

        self._selectedtabs = []        

        self._timers = []
        
        self._dragcursor = wx.StockCursor(wx.CURSOR_HAND)
        self._dragstartpos = wx.Point()

        self._drawx = False
        self._drawxstyle = 1

        self._pmenu = None

        self._enablehiding = False
        if (style & NC_LEFT or style & NC_RIGHT) and style & NC_EXPANDABLE:
            self._InitExpandableStyles(style)
            self._InitExpandableTabStyles(self._style, self._expanded, self._tabstyle)
            self._CreateSizeToggleButton()
        else:
            self._sizeToggleButton = None

        self.SetDefaultPage()        
        
        if style & NC_TOP or style & NC_BOTTOM:
            self.SetBestSize((-1, 28))
            self._firsttabpos = wx.Point(3, 0)
        else:
            self.SetBestSize((28, -1))
            self._firsttabpos = wx.Point(0, 3 + self._CalcSizeToggleBestSize()[1])

        self._borderpen = wx.Pen(wx.SystemSettings_GetColour(wx.SYS_COLOUR_BTNSHADOW)) 
        self._highlightpen2 = wx.Pen(wx.SystemSettings_GetColour(wx.SYS_COLOUR_WINDOW))
        self._highlightpen = wx.Pen((145, 167, 180))
        self._upperhigh = wx.Pen(wx.SystemSettings_GetColour(wx.SYS_COLOUR_WINDOW))
        self._shadowpen = wx.Pen(wx.SystemSettings_GetColour(wx.SYS_COLOUR_3DDKSHADOW), 2)
        self._shadowpen.SetCap(wx.CAP_BUTT)
        self._highlightpen.SetCap(wx.CAP_BUTT)
        self._highlightpen2.SetCap(wx.CAP_BUTT)
        
        if wx.Platform == "__WXMAC__":
            self._focusindpen = wx.Pen(wx.BLACK, 1, wx.SOLID)
        else:
            self._focusindpen = wx.Pen(wx.BLACK, 1, wx.USER_DASH)
            self._focusindpen.SetDashes([1,1])
            self._focusindpen.SetCap(wx.CAP_BUTT)

        self.Bind(wx.EVT_LEFT_DOWN, self.OnMouseLeftDown)
        self.Bind(wx.EVT_LEFT_DCLICK, self.OnMouseLeftDClick)
        self.Bind(wx.EVT_MOTION, self.OnMouseMotion)
        self.Bind(wx.EVT_LEFT_UP, self.OnMouseLeftUp)
        self.Bind(wx.EVT_RIGHT_UP, self.OnMouseRightUp)
        self.Bind(wx.EVT_RIGHT_DOWN, self.OnMouseRightDown)
        self.Bind(wx.EVT_MIDDLE_DOWN, self.OnMouseMiddleDown)
        self.Bind(wx.EVT_SIZE, self.OnSize)
        
        self.Bind(wx.EVT_PAINT, self.OnPaint)
        self.Bind(wx.EVT_ERASE_BACKGROUND, lambda x: None)

        self.Bind(wx.EVT_TIMER, self.AnimateTab)
        self.Bind(wx.EVT_KEY_DOWN, self.OnKeyDown)
        self.Bind(wx.EVT_LEAVE_WINDOW, self.OnLeaveWindow)

        self._droptarget = NCDropTarget(self)
        self.SetDropTarget(self._droptarget)
        
    def Contract(self):
        if self._style & NC_EXPANDABLE and self._expanded:
            self._ToggleSize()

    def Expand(self):
        if self._style & NC_EXPANDABLE and not self._expanded:
            self._ToggleSize()

    def _ToggleSize(self, event=None):
        if self._style & NC_EXPANDABLE and not self._expanded:
            # contract
            self._style = self._expandedstyle
            self._tabstyle = self._expandedtabstyle
            self._expanded = True
            self._sizeToggleButton.SetLabel("<<")
        elif self._style & NC_EXPANDABLE and self._expanded:
            # expand
            self._style = self._contractedstyle
            self._tabstyle = self._contractedtabstyle
            self._expanded = False
            self._sizeToggleButton.SetLabel(">>")
        self._OnStyleChange()


    def OnDropTarget(self, x, y, nPage, oldcont):
        """ Handles The OnDrop Action For Drag And Drop Between Different NotebookCtrl. """
        
        where = self.HitTest(wx.Point(x, y))

        oldNotebook = oldcont.GetParent()
        newNotebook = self.GetParent()

        if oldNotebook == newNotebook:
            if where >= 0 and where != self._tabID:

                self._isdragging = False
                self._olddragpos = -1
                eventOut = NotebookCtrlEvent(wxEVT_NOTEBOOKCTRL_PAGE_DND, self.GetId())
                eventOut.SetOldPosition(self._tabID)
                eventOut.SetNewPosition(where)
                eventOut.SetEventObject(self)
                
                if self.GetEventHandler().ProcessEvent(eventOut):
                    self._tabID = -1
                    self._olddragpos = -1
                    self.SetCursor(wx.STANDARD_CURSOR)
                    self.Refresh()
                    return
                
                self._parent.Freeze()

                try:
                    text = self.GetPageText(self._tabID)
                    image = self.GetPageImage(self._tabID)
                    font1 = self.GetPageTextFont(self._tabID)
                    font2 = self.GetPageTextSecondaryFont(self._tabID)
                    fontcolour = self.GetPageTextColour(self._tabID)
                    pagecolour = self.GetPageColour(self._tabID)
                    enabled = self.IsPageEnabled(self._tabID)
                    tooltip, ontime, winsize = self.GetPageToolTip(self._tabID)
                    menu = self.GetPagePopupMenu(self._tabID)
                    firstcol = self.GetPageFirstGradientColour(self._tabID)
                    secondcol = self.GetPageSecondGradientColour(self._tabID)
                    ishidden = self._pages[self._tabID]._ishidden
                except:
                    self._parent.Thaw()
                    self._tabID = -1 
                    self.SetCursor(wx.STANDARD_CURSOR)
                    return
                
                isanimated = 0
                if self._timers[self._tabID].IsRunning():
                    isanimated = 1
                    timer = self._timers[self._tabID].GetInterval()
                    
                self.StopAnimation(self._tabID)
                animatedimages = self.GetAnimationImages(self._tabID)
                            
                pagerange = range(self.GetPageCount())
                    
                newrange = pagerange[:]
                newrange.remove(self._tabID)
                newrange.insert(where, self._tabID)
                        
                newpages = []
                counter = self.GetPageCount() - 1
                
                for ii in xrange(self.GetPageCount()):  
                    newpages.append(self._parent.GetPage(ii))
                    self._parent.bsizer.Detach(counter-ii)

                cc = 0

                self._parent._notebookpages = []
                
                for jj in newrange:
                    self._parent.bsizer.Add(newpages[jj], 1, wx.EXPAND | wx.ALL, 2)
                    self._parent.bsizer.Show(cc, False)
                    self._parent._notebookpages.append(newpages[jj])
                    cc = cc + 1
                        
                self.DeletePage(self._tabID)
                
                if enabled:
                    if id == self.GetPageCount():
                        self.AddPage(text, True, image)
                    else:
                        self.InsertPage(where, text, True, image)
                else:
                    if id == self.GetPageCount():
                        self.AddPage(text, False, image)
                    else:
                        self.InsertPage(where, text, False, image)

                self.SetPageImage(where, image)
                self.SetPageText(where, text)
                self.SetPageTextFont(where, font1)
                self.SetPageTextSecondaryFont(where, font2)
                self.SetPageTextColour(where, fontcolour)
                self.SetPageColour(where, pagecolour)
                self.EnablePage(where, enabled)
                self.SetPageToolTip(where, tooltip, ontime, winsize)
                self.SetPagePopupMenu(where, menu)
                self.SetPageFirstGradientColour(where, firstcol)
                self.SetPageSecondGradientColour(where, secondcol)
                self._pages[where]._ishidden = ishidden
                
                if isanimated and len(animatedimages) > 1:
                    self.SetAnimationImages(where, animatedimages)
                    self.StartAnimation(where, timer)
                    
                if enabled:
                    self._parent.bsizer.Show(where, True)
                else:
                    sel = self.GetSelection()
                    
                    if sel == -1:
                        sel = 0
                    self._parent.bsizer.Show(where, False)
                    self._parent.SetSelection(sel)
                    self._parent.bsizer.Show(sel, True)
                    
                self._parent.bsizer.Layout()
                
                self._parent.Thaw()

            self._isdragging = False
            self._olddragpos = -1
            self._fromdnd = True
            self.Refresh()
            self._tabID = -1 
            self.SetCursor(wx.STANDARD_CURSOR)

            return            

        if nPage >= 0 and where >= 0:
            panel = oldNotebook.GetPage(nPage)
            
            if panel:
                eventOut = NotebookCtrlEvent(wxEVT_NOTEBOOKCTRL_PAGE_DND, oldNotebook.GetId())
                eventOut.SetOldPosition(nPage)
                eventOut.SetNewPosition(where)
                eventOut.SetEventObject(oldNotebook)
                
                if oldNotebook.GetEventHandler().ProcessEvent(eventOut):
                    oldNotebook.nb._tabID = -1
                    oldNotebook.nb._olddragpos = -1
                    oldNotebook.SetCursor(wx.STANDARD_CURSOR)
                    oldNotebook.Refresh()
                    return
                
                oldNotebook.Freeze()
                infos = oldNotebook.GetPageInfo(nPage)
                
                text = infos["text"]
                image = infos["image"]
                hidden = infos["ishidden"]

                panel.Reparent(newNotebook)                
                newNotebook.InsertPage(where, panel, text, True, image, hidden)
                newNotebook.SetPageInfo(where, infos)

                oldNotebook.nb.DeletePage(nPage)

                oldNotebook.bsizer.Detach(nPage)
                oldNotebook.bsizer.Layout()
                oldNotebook.sizer.Layout()

                oldNotebook._notebookpages.pop(nPage)
                
                oldNotebook.AdvanceSelection()

                if oldNotebook.GetPageCount() == 0:
                    if oldNotebook._style & NC_TOP:
                        oldNotebook.sizer.Show(0, False)
                        oldNotebook.sizer.Show(1, False)
                    else:
                        oldNotebook.sizer.Show(1, False)
                        oldNotebook.sizer.Show(2, False)

                    oldNotebook.sizer.Layout()

                oldNotebook.Thaw()
                newNotebook.Refresh()
                
        return wx.DragMove

    def OnLeaveWindow(self, event):
        """ Handles The wx.EVT_LEAVE_WINDOW Events For TabCtrl. """
        
        if self._enabledragging:
            if self._isdragging:

                page = self._parent.GetPage(self._tabID)                
                draginfo = NCDragInfo(page, self._tabID)
                drginfo = cPickle.dumps(draginfo)
                dataobject = wx.CustomDataObject(wx.CustomDataFormat("NotebookCtrl"))
                dataobject.SetData(drginfo)
                dragSource = wx.DropSource(self)
                dragSource.SetData(dataobject)
                dragSource.DoDragDrop(wx.Drag_DefaultMove)
                
                self._isleaving = True
                self.Refresh()

        if self._istooltipshown:
            self._tipwindow.Destroy()
            self._istooltipshown = False
            self.Refresh()
                
        event.Skip()
        

    def OnKeyDown(self, event):
        """
        Handles The wx.EVT_KEY_DOWN Event For TabCtrl. This Is Only Processed If
        The User Navigate Through Tabs With Ctrl-Tab Keyboard Navigation.
        """

        if event.GetKeyCode() == wx.WXK_TAB:
            if event.ControlDown():
                sel = self.GetSelection()
                if sel == self.GetPageCount() - 1:
                    sel = 0
                else:
                    sel = sel + 1

                while not self.IsPageEnabled(sel):
                    sel = sel + 1
                    if sel == self.GetPageCount() - 1:
                        sel = 0
                        
                self._parent.SetSelection(sel)

        event.Skip()
    

    def AddPage(self, text, select=False, img=-1, hidden=False):
        """
        Add A Page To The Notebook.
        
        @param text: The Tab Text;
        @param select: Whether The Page Should Be Selected Or Not;
        @param img: Specifies The Optional Image Index For The New Page.
        """

        self._pages.append(TabbedPage(text, img, hidden))
        self._somethingchanged = True
        
        self._firsttime = True
        self._timers.append(wx.Timer(self))
        
        # JS: The following two lines caused this control not to fire
        # the EVT_NOTEBOOKCTRL_PAGE_CHANGING/CHANGED events for the
        # first page. The NotebookCtrl sets the selection later anyway,
        # and without these two lines, the events fire.
##        if select or self.GetSelection() == -1:
##            self._selection = self.GetPageCount() - 1
        
        if self._style & NC_LEFT or self._style & NC_RIGHT:
            self.SetBestSize((self._CalcBestWidth(wx.ClientDC(self)), -1))
            
        self.Refresh()
 

    def InsertPage(self, nPage, text, select=False, img=-1, hidden=False):
        """
        Insert A Page Into The Notebook.
        
        @param nPage: Specifies The Position For The New Page;
        @param text: The Tab Text;
        @param select: Whether The Page Should Be Selected Or Not;
        @param img: Specifies The Optional Image Index For The New Page.
        """
        
        if nPage < 0 or (self.GetSelection() >= 0 and nPage >= self.GetPageCount()):
            raise "\nERROR: Invalid Notebook Page In InsertPage: (" + str(nPage) + ")"

        oldselection = self.GetSelection()
        
        self._pages.insert(nPage, TabbedPage(text, img, hidden))
        self._timers.insert(nPage, wx.Timer(self))
        
        self._somethingchanged = True
        self._firsttime = True
        
        if select or self.GetSelection() == -1:
            self._selection = nPage
            self.SetSelection(nPage)
        else:
            if nPage <= oldselection:
                self._selection = self._selection + 1
        
        if self._style & NC_LEFT or self._style & NC_RIGHT:
            self.SetBestSize((self._CalcBestWidth(wx.ClientDC(self)), -1))
                        
        self.Refresh()
            

    def DeleteAllPages(self):
        """ Deletes All NotebookCtrl Pages. """
        
        for tims in self._timers:
            if tims.IsRunning():
                tims.Stop()
                
            tims.Destroy()

        self._timers = []            
        self._pages = []
        self._selection = -1
        self._somethingchanged = True
        self._firsttime = True
        self.Refresh()
         

    def DeletePage(self, nPage, oncontinue=True):
        """ Deletes The Page nPage, And The Associated Window. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In DeletePage: (" + str(nPage) + ")"

        oldselection = self.GetSelection()

        self._pages.pop(nPage)
        
        if self._timers[nPage].IsRunning():
            self._timers[nPage].Stop()
            
        self._timers[nPage].Destroy()
        
        if self._istooltipshown:
            self._tipwindow.Destroy()
            self._istooltipshown = False

        if not oncontinue:
            self._somethingchanged = True
            self._firsttime = True
            self.Refresh()
            return
        
        if nPage < self._selection:
            self._selection = self._selection - 1
        elif self._selection == nPage and self._selection == self.GetPageCount():
            self._selection = self._selection - 1
        else:
            self._selection = oldselection

        self._somethingchanged = True
        self._firsttime = True
        self.Refresh()
         

    def SetSelection(self, nPage):
        """
        Sets The Current Tab Selection To The Given nPage. This Call Generates The
        EVT_NOTEBOOKCTRL_PAGE_CHANGING Event.
        """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetSelection: (" + str(nPage) + ")"
        
        oldselection = self._selection
        
        if nPage != self._selection:

            if not self.IsPageEnabled(nPage):
                return
            
            eventOut = NotebookCtrlEvent(wxEVT_NOTEBOOKCTRL_PAGE_CHANGING, self.GetId())
            eventOut.SetSelection(nPage)
            eventOut.SetOldSelection(self._selection)
            eventOut.SetEventObject(self)
        
            if not self.GetEventHandler().ProcessEvent(eventOut):

                # Prevent full paint unless never fully painted
                if hasattr(self, "_initrect"):
                    self._firsttime = False
                # Program Allows The Page Change 
                self._selection = nPage
                eventOut.SetEventType(wxEVT_NOTEBOOKCTRL_PAGE_CHANGED) 
                eventOut.SetOldSelection(self._selection) 
                self.GetEventHandler().ProcessEvent(eventOut)
                
                if oldselection != -1:
                    self._parent.bsizer.Show(oldselection, False)

                self.EnsureVisible(self._selection)
                self._parent.bsizer.Show(self._selection, True)
                self._parent.bsizer.Layout()
                
                self.Refresh() 

                self._shown = nPage


    def EnsureVisible(self, selection):
        
        if self.GetPageCount() < 2:
            return

        if not self.HasSpinButton():
            return

        fullrect = self.GetClientSize()
        count = self._tabvisible[0:selection].count(0)

        #stani patch: correct for too big values, play safe
        sindex = selection-self._firstvisible-count
        if sindex >= len(self._tabrect):
            sindex      = len(self._tabrect)-1
            selection   = sindex+self._firstvisible+count
        currect = self._tabrect[sindex]
                
        spinval = self._spinbutton.GetValue()
        firstrect = self._initrect[spinval]
        if self._style & NC_LEFT or self._style & NC_RIGHT:
            pos = currect.y
            size = currect.height
            posIndex = 1
        else:
            pos = currect.x
            size = currect.width
            posIndex = 0
        torefresh = 0
        
        while pos + size > fullrect[posIndex] - self._spinbutton.GetSize()[posIndex]:
            
            if self._style & NC_LEFT or self._style & NC_RIGHT:
                pos -= firstrect.height
            else:
                pos -= firstrect.width

            if not self._enablehiding:
                spinval = spinval + 1
            else:
                oldspinval = spinval
                spinval = spinval + self._tabvisible[0:selection].count(0)
                if spinval == oldspinval:
                    spinval = spinval + 1

                if spinval >= len(self._initrect):
                    spinval = spinval - 1
                
            firstrect = self._initrect[spinval]
            
            if self._style & NC_LEFT or self._style & NC_RIGHT:
                self._spinbutton.OnSpin(-spinval)
                self._spinbutton.SetValue(-spinval)
            else:
                self._spinbutton.OnSpin(spinval)
                self._spinbutton.SetValue(spinval)

            torefresh = 1

        if torefresh:
            self.Refresh()
        
                
    def GetPageCount(self):
        """ Returns The Number Of Pages In NotebookCtrl. """

        return len(self._pages)


    def GetSelection(self):
        """ Returns The Current Selection. """

        return self._selection


    def GetImageList(self):
        """ Returns The Image List Associated With The NotebookCtrl. """

        return self._imglist


    def SetImageList(self, imagelist):
        """ Associate An Image List To NotebookCtrl. """
        
        self._imglist = imagelist
        self._grayedlist = wx.ImageList(16, 16, True, 0)
        
        for ii in xrange(imagelist.GetImageCount()):
            
            bmp = imagelist.GetBitmap(ii)
            image = wx.ImageFromBitmap(bmp)
            image = GrayOut(image)
            newbmp = wx.BitmapFromImage(image)
            self._grayedlist.Add(newbmp)


    def AssignImageList(self, imagelist):
        """ Associate An Image List To NotebookCtrl. """

        self._imglist = imagelist
        self._grayedlist = wx.ImageList(16, 16, True, 0)
        
        for ii in xrange(imagelist.GetImageCount()):
            
            bmp = imagelist.GetBitmap(ii)
            image = wx.ImageFromBitmap(bmp)
            image = GrayOut(image)
            newbmp = wx.BitmapFromImage(image)
            self._grayedlist.Add(newbmp)
            

    def GetPadding(self):
        """ Returns The (Horizontal, Vertical) Padding Of The Text Inside Tabs. """

        return self._padding


    def SetPadding(self, padding):
        """ Sets The (Horizontal, Vertical) Padding Of The Text Inside Tabs. """
        
        self._padding = padding
        self._somethingchanged = True
        self._firsttime = True
        self.Refresh()
        

    def GetPageText(self, nPage):
        """ Returns The String For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageText: (" + str(nPage) + ")"
        
        return self._pages[nPage]._text 


    def SetPageText(self, nPage, text):
        """ Sets The String For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageText: (" + str(nPage) + ")"
        
        if self._pages[nPage]._text != text:
            self._pages[nPage]._text = text
            self._somethingchanged = True
            self._firsttime = True
            self.Refresh()
        
     
    def GetPageImage(self, nPage):
        """ Returns The Image Index For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageImage: (" + str(nPage) + ")"
        
        return self._pages[nPage]._image
     

    def SetPageImage(self, nPage, img):
        """ Sets The Image Index For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageImage: (" + str(nPage) + ")"
        
        if self._pages[nPage]._image != img:
            self._pages[nPage]._image = img
            self._somethingchanged = True
            self._firsttime = True

            if self._style & NC_LEFT or self._style & NC_RIGHT:
                self.SetBestSize((self._CalcBestWidth(wx.ClientDC(self)), -1))
                self._parent.GetSizer().Layout()
            
            self.Refresh()        


    def SetPageTextFont(self, nPage, font=None):
        """ Sets The Primary Font For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageTextFont: (" + str(nPage) + ")"
        
        if font is None:
            font = wx.SystemSettings_GetFont(wx.SYS_DEFAULT_GUI_FONT)

        normalfont = wx.SystemSettings_GetFont(wx.SYS_DEFAULT_GUI_FONT)
        
        self._pages[nPage]._font = font

        if font == normalfont:
            self._parent.GetSizer().Layout()
            self._somethingchanged = True
            self._firsttime = True
            self.Refresh()
            return

        dc = wx.ClientDC(self)
        dc.SetFont(font)
        w1, h1 = dc.GetTextExtent("Aq")
        dc.SetFont(normalfont)
        wn, hn = dc.GetTextExtent("Aq")
        w2, h2 = (0, 0)
        
        if hasattr(self._pages[nPage], "_secondaryfont"):
            dc.SetFont(self._pages[nPage]._secondaryfont)
            w2, h2 = dc.GetTextExtent("Aq")

        h = max(h1, h2)

        if h < hn:
            self._somethingchanged = True
            self._firsttime = True
            self.Refresh()
            return

        if h + 2*self._padding.y < 24:
            newheight = 24
        else:
            newheight = h + 2*self._padding.y

        oldsize = self.GetSize()

        if newheight < oldsize[1]:
            newheight = oldsize[1]
            
        if self._style & NC_TOP or self._style & NC_BOTTOM:
            self.SetBestSize((-1, newheight))
        else:
            self.SetBestSize((self._CalcBestWidth(dc), -1))
        self._parent.GetSizer().Layout()
        self._somethingchanged = True
        self._firsttime = True
        self.Refresh()

    def SetTabHeight(self, height=28):
        """ Sets The Tabs Height. """
        
        if self._style & NC_TOP or self._style & NC_BOTTOM:
            self.SetBestSize((-1, height))
            self._bestsize = height
        

    def SetControlBackgroundColour(self, colour=None):
        """ Sets The TabCtrl Background Colour (Behind The Tabs). """

        if colour is None:
            colour = wx.SystemSettings_GetColour(wx.SYS_COLOUR_3DFACE)
            
        self.SetBackgroundColour(colour)
        self.Refresh()
        
        
    def GetPageTextFont(self, nPage):
        """ Returns The Primary Font For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageTextFont: (" + str(nPage) + ")"
        
        return self._pages[nPage]._font


    def SetPageTextSecondaryFont(self, nPage, font=None):
        """ Sets The Secondary Font For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageTextSecondaryFont: (" + str(nPage) + ")"
        
        if font is None:
            font = wx.SystemSettings_GetFont(wx.SYS_DEFAULT_GUI_FONT) 

        normalfont = wx.SystemSettings_GetFont(wx.SYS_DEFAULT_GUI_FONT)
        
        self._pages[nPage]._secondaryfont = font

        if font == normalfont:
            self._somethingchanged = True
            self._firsttime = True
            self.Refresh()
            return

        dc = wx.ClientDC(self)
        dc.SetFont(font)
        w1, h1 = dc.GetTextExtent("Aq")
        dc.SetFont(normalfont)
        wn, hn = dc.GetTextExtent("Aq")
        w2, h2 = (0, 0)
        
        if hasattr(self._pages[nPage], "_font"):
            dc.SetFont(self._pages[nPage]._font)
            w2, h2 = dc.GetTextExtent("Aq")

        h = max(h1, h2)

        if h < hn:
            self._somethingchanged = True
            self._firsttime = True
            self.Refresh()
            return
        
        if h + 2*self._padding.y < 24:
            newheight = 24
        else:
            newheight = h + 2*self._padding.y

        oldsize = self.GetSize()

        if newheight < oldsize[-1]:
            newheight = oldsize[-1]
            
        if self._style & NC_TOP or self._style & NC_BOTTOM:
            self.SetBestSize((-1, newheight))
        else:
            self.SetBestSize((self._CalcBestWidth(dc), -1))
        self._parent.GetSizer().Layout()
        
        self._somethingchanged = True
        self._firsttime = True
        self.Refresh()

        
    def GetPageTextSecondaryFont(self, nPage):
        """ Returns The Secondary Font For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageTextSecondaryFont: (" + str(nPage) + ")"
        
        return self._pages[nPage]._secondaryfont


    def SetPageTextColour(self, nPage, colour=None):
        """ Sets The Text Colour For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageTextColour: (" + str(nPage) + ")"
        
        if colour is None:
            colour = wx.BLACK

        self._pages[nPage]._pagetextcolour = colour
        self._somethingchanged = True
        self.Refresh()


    def GetPageTextColour(self, nPage):
        """ Returns The Text Colour For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageTextColour: (" + str(nPage) + ")"            

        return self._pages[nPage]._pagetextcolour


    def SetPageColour(self, nPage, colour=None):
        """ Sets The Tab Background Colour For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageColour: (" + str(nPage) + ")"
        
        if colour is None:
            colour = wx.SystemSettings_GetColour(wx.SYS_COLOUR_BTNFACE)

        self._pages[nPage]._pagecolour = colour
        self._somethingchanged = True
        self.Refresh()


    def GetPageColour(self, nPage):
        """ Returns The Tab Background Colour For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageColour: (" + str(nPage) + ")"            
        
        if not self._tabstyle._normal or self._usegradients:
            if self._usegradients:
                return self._tabstyle._firstcolour
            else:
                return self._GetThemePageColour(nPage)
        else:
            return self._pages[nPage]._pagecolour
    

    def EnablePage(self, nPage, enable=True):
        """ Enable/Disable The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In EnablePage: (" + str(nPage) + ")"

        self._pages[nPage]._enable = enable
        
        if not enable and self.GetSelection() == nPage:
            defpage = self.GetDefaultPage()
            if defpage < 0:
                self.AdvanceSelection()
            else:
                if defpage >= self.GetPageCount():
                    self.AdvanceSelection()
                else:
                    if defpage == nPage:
                        self.AdvanceSelection()
                    else:
                        self.SetSelection(defpage)
            
        self.Refresh()


    def IsPageEnabled(self, nPage):
        """ Returns Whether A Page Is Enabled Or Not. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In IsPageEnabled: (" + str(nPage) + ")"            

        return self._pages[nPage]._enable
    

    def SetHighlightSelection(self, highlight=True):
        """ Globally Enables/Disables Tab Highlighting On Tab Selection. """
        
        self._highlight = highlight
        self.Refresh()

        
    def GetHighlightSelection(self):
        """ Returns Globally Enable/Disable State For Tab Highlighting On Tab Selection. """

        return self._highlight


    def SetUseFocusIndicator(self, focus=True):
        """ Globally Enables/Disables Tab Focus Indicator. """
        
        self._usefocus = focus
        self.Refresh()


    def GetUseFocusIndicator(self):
        """ Returns Globally Enable/Disable State For Tab Focus Indicator. """
        
        return self._usefocus

    
    def SetPageToolTip(self, nPage, tooltip="", timer=500, winsize=400):
        """
        Sets A ToolTip For The Given Page nPage.
        
        @param nPage: The Given Page;
        @param tooltip: The ToolTip String;
        @param timer: The Timer After Which The Tip Window Is Popped Up;
        @param winsize: The Maximum Width Of The Tip Window.
        """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageToolTip: (" + str(nPage) + ")"
        
        self._pages[nPage]._tooltip = tooltip
        self._pages[nPage]._tooltiptime = timer
        self._pages[nPage]._winsize = winsize


    def GetPageToolTip(self, nPage):
        """ Returns A Tuple With All Page ToolTip Parameters. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageToolTip: (" + str(nPage) + ")"        

        return self._pages[nPage]._tooltip, self._pages[nPage]._tooltiptime, \
               self._pages[nPage]._winsize


    def EnableToolTip(self, show=True):
        """ Globally Enables/Disables Tab ToolTips. """

        self._showtooltip = show
        
        if show:
            try:
                wx.PopupWindow(self)
                self.TransientTipWindow = TransientTipWindow
            
            except NotImplementedError:
                
                self.TransientTipWindow = macTransientTipWindow

        else:
            if self._istooltipshown:
                self._tipwindow.Destroy()
                self._istooltipshown = False
                self.Refresh()
                
            if self._tiptimer.IsRunning():
                self._tiptimer.Stop()


    def GetToolTipBackgroundColour(self):
        """ Returns The ToolTip Window Background Colour. """

        return self._backtooltip


    def SetToolTipBackgroundColour(self, colour=None):
        """ Sets The ToolTip Window Background Colour. """

        if colour is None:
            colour = wx.Colour(255, 255, 230)
            
        self._backtooltip = colour


    def EnableTabGradients(self, enable=True):
        """ Globally Enables/Disables Drawing Of Gradient Coloured Tabs For Each Tab. """

        self._usegradients = enable
        
        if enable:
            self._tabstyle.ResetDefaults()
            self._selstyle.ResetDefaults()
            
        self.Refresh()
        

    def SetPageFirstGradientColour(self, nPage, colour=None):
        """ Sets The Single Tab First Gradient Colour. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageFirstGradientColour: (" + str(nPage) + ")"
        
        if colour is None:
            colour = wx.WHITE

        self._pages[nPage]._firstcolour = colour
        self.Refresh()
        

    def SetPageSecondGradientColour(self, nPage, colour=None):
        """ Sets The Single Tab Second Gradient Colour. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageSecondGradientColour: (" + str(nPage) + ")"
        
        if colour is None:
            color = self._pages[nPage]._firstcolour
            r, g, b = int(color.Red()), int(color.Green()), int(color.Blue())
            color = ((r >> 1) + 20, (g >> 1) + 20, (b >> 1) + 20)
            colour = wx.Colour(color[0], color[1], color[2])
            
        self._pages[nPage]._secondcolour = colour
        self.Refresh()


    def GetPageFirstGradientColour(self, nPage):
        """ Returns The Single Tab First Gradient Colour. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageFirstGradientColour: (" + str(nPage) + ")"

        return self._pages[nPage]._firstcolour


    def GetPageSecondGradientColour(self, nPage):
        """ Returns The Single Tab Second Gradient Colour. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageSecondGradientColour: (" + str(nPage) + ")"

        return self._pages[nPage]._secondcolour
    

    def CancelTip(self):
        """ Destroys The Tip Window (Probably You Won't Need This One). """
        
        if self._istooltipshown:
            self._istooltipshown = False
            self._tipwindow.Destroy()
            self.Refresh()


    def AdvanceSelection(self, forward=True):
        """
        Cycles Through The Tabs. The Call To This Function Generates The
        EVT_NOTEBOOKCTRL_PAGE_CHANGING Event.
        """

        if self.GetPageCount() <= 1:
            return
        
        sel = self.GetSelection()
        count = 0
        
        if forward:          
            if sel == self.GetPageCount() - 1:
                sel = 0
            else:
                sel = sel + 1

            while not self.IsPageEnabled(sel) or \
                  (self._enablehiding and self._pages[sel]._ishidden):
                
                count = count + 1
                sel = sel + 1
                
                if self._enablehiding and self._pages[sel]._ishidden:
                    count = count + 1
                    sel = sel + 1
                    
                if sel == self.GetPageCount() - 1:
                    sel = 0
                    
                if count > self.GetPageCount() + 1:
                    return None

        else:
            if sel == 0:
                sel = self.GetPageCount() - 1
            else:
                sel = sel - 1

            while not self.IsPageEnabled(sel):
                count = count + 1
                sel = sel - 1
                if sel == 0:
                    sel = self.GetPageCount() - 1

                if count > self.GetPageCount() + 1:
                    return None
                    
        self._parent.SetSelection(sel)


    def SetDefaultPage(self, defaultpage=-1):
        """
        Sets The Default Page That Will Be Selected When An Active And Selected
        Tab Is Made Inactive.
        """
        
        if defaultpage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetDefaultPage: (" + str(defaultpage) + ")"
        
        self._defaultpage = defaultpage
        

    def GetDefaultPage(self):
        """ Returns The Default Page. """
        
        return self._defaultpage
    

    def UpdateSpinButton(self):
        """ Update The NotebookSpinButton. Used Internally. """

        count = self.GetPageCount()
        
        if count == 0:        
            return

        nbsize = []
        nbsize.append(self._initrect[-1][0] + self._initrect[-1][2])
        nbsize.append(self._initrect[-1][1] + self._initrect[-1][3])
        clsize = self.GetClientSize()

        if self._style & NC_TOP or self._style & NC_BOTTOM:
            spinstyle = wx.SP_HORIZONTAL
            showspin = nbsize[0] > clsize[0]
        else:
            spinstyle = wx.SP_VERTICAL
            showspin = nbsize[1] > clsize[1]

        if showspin:
            if not hasattr(self, "_spinbutton"):
                self._spinbutton = NotebookSpinButton(self, pos=(10000,10000),
                    style=spinstyle)
                self._spinbutton.SetValue(0)
                self._originalspinsize = self._spinbutton.GetSize()

            sbsize = self._spinbutton.GetSize()
            if self._style & NC_LEFT or self._style & NC_RIGHT:
                ypos = clsize[1] - sbsize[1]
                if self._style & NC_LEFT:
                    xpos = nbsize[0] - (2 + sbsize[0])
                else:
                    xpos = 2
            else:                
                xpos = clsize[0] - sbsize[0]
                if self._style & NC_BOTTOM:
                    ypos = 2
                else:
                    ypos = clsize[1] - sbsize[1]

            if self.HasMenuButton():
                self._spinbutton.SetSize((-1, 16))
            else:
                self._spinbutton.SetSize(self._originalspinsize)
                
            self._spinbutton.Move((xpos, ypos))
            self._spinbutton.Show()
            if self._style & NC_LEFT or self._style & NC_RIGHT:
                self._spinbutton.SetRange(-(count-1), 0)
            else:
                self._spinbutton.SetRange(0, count-1)
        
        else:

            if hasattr(self, "_spinbutton") and self._spinbutton.IsShown():
                self._spinbutton.Hide()
                self._spinbutton.SetValue(0)
            

    def HasSpinButton(self):
        """ Returns Wheter The NotebookSpinButton Exists And Is Shown. """

        return hasattr(self, "_spinbutton") and self._spinbutton.IsShown()


    def IsLastVisible(self):
        """ Returns Whether The Last Tab Is Visible Or Not. """

        if self.HasSpinButton():
            if self._style & NC_LEFT or self._style & NC_RIGHT:
                pos, size = (1, 3)
            else:
                pos, size = (0, 2)
            lastpos = self._tabrect[-1][pos] + self._tabrect[-1][size]
            if lastpos < self._spinbutton.GetPosition()[pos]:
                return True

        return False            


    def UpdateMenuButton(self, show):
        """ Updates The Notebook Menu Button To Show/Hide Tabs. Used Internally. """
        
        count = self.GetPageCount()
        
        if count == 0:   
            return

        if not hasattr(self, "_initrect"):
            return

        if not show and not hasattr(self, "_menubutton"):
            return

        if not hasattr(self, "_menubutton"):
            self._menubutton = NotebookMenuButton(self, pos=(10000,10000))

        sbsize = self._menubutton.GetSize()
        nbsize = []
        nbsize.append(self._initrect[-1][0] + self._initrect[-1][2])
        nbsize.append(self._initrect[-1][1] + self._initrect[-1][3])
        clsize = self.GetClientSize()
        
        xpos = clsize[0] - sbsize[0]
        ypos = clsize[1] - sbsize[1]

        if self.HasSpinButton():
            self._menubutton.Move((xpos-1, ypos-16))
        else:
            self._menubutton.Move((xpos-1, ypos-1))
            
        self._menubutton.Show(show)


    def HasMenuButton(self):
        """ Returns Wheter The NotebookMenuButton Exists And Is Shown. """

        return hasattr(self, "_menubutton") and self._menubutton.IsShown()
    

    def HideTab(self, nPage, hide=True):
        """ Hides A Tab In The NotebookCtrl. """
        
        if hide:
            self._pages[nPage]._ishidden = True
        else:
            self._pages[nPage]._ishidden = False

        if nPage == self.GetSelection():
            self.AdvanceSelection()
                
        self._firsttime = True
        self.Refresh()

        
    def HitTest(self, point, flags=0):
        """
        Standard NotebookCtrl HitTest() Method. If Called With 2 Outputs, It
        Returns The Page Clicked (If Any) And One Of These Flags:

        NC_HITTEST_NOWHERE = 0   ==> Hit Not On Tab 
        NC_HITTEST_ONICON  = 1   ==> Hit On Icon 
        NC_HITTEST_ONLABEL = 2   ==> Hit On Label 
        NC_HITTEST_ONITEM  = 4   ==> Hit Generic, On Item
        NC_HITTEST_ONX = 8       ==> Hit On Closing "X" On Every Page
        """        

        mirror = self._style & NC_BOTTOM 
        size = self.GetSize() 
        dc = wx.ClientDC(self)

        height = self._tabrect[0].height

        if flags:
            flags = wx.NB_HITTEST_NOWHERE

        if point.x <= 0 or point.x >= size.x:
            if flags:
                return wx.NOT_FOUND, flags
            else:
                return wx.NOT_FOUND
     
        if not point.y >= self._tabrect[0].y and point.y < self._tabrect[0].y + height:
            if flags:
                return wx.NOT_FOUND, flags
            else:
                return wx.NOT_FOUND

        posx = self._firsttabpos.x
        posy = self._firsttabpos.y
        maxwidth = max(self._maxtabwidths)
        
        for ii in xrange(self._firstvisible, self.GetPageCount()):

            if not self._enablehiding or not self._pages[ii]._ishidden:
                
                width = self._CalcTabTextWidth(dc, ii)
                
                bmpWidth, bmpHeight = self._CalcTabBitmapSize(ii)
                
                tabrect = self._CalcTabRect(ii, posx, posy, width, bmpWidth, bmpHeight)
                
                if tabrect.Contains(point):
                    
                    if flags:
                        flags = NC_HITTEST_ONITEM 

                    #onx attempt
                    if self.GetDrawX()[0]:
                        count = self._tabvisible[0:ii].count(0)
                        if flags and self._xrect[ii-self._firstvisible-count].Contains(point):
                            flags = NC_HITTEST_ONX

                   #onicon attempt 
                    if flags and bmpWidth > 0 and \
                        wx.RectPS(wx.Point(*self._CalcTabBitmapPosition(ii, 
                            bmpWidth, bmpHeight, tabrect)), 
                            wx.Size(bmpWidth, bmpHeight)).Contains(point):
                        flags = NC_HITTEST_ONICON 

                   #onlabel attempt 
                    elif flags and wx.RectPS(wx.Point(*self._CalcTabTextPosition(ii, 
                        tabrect, self._CalcTabBitmapSpace(bmpWidth, bmpHeight))),
                            wx.Size(width, height)).Contains(point):
                        flags = NC_HITTEST_ONLABEL
                            
                    if flags:
                        return ii, flags
                    else:
                        return ii
                 
                if self._style & NC_TOP or self._style & NC_BOTTOM:
                    posx += tabrect.width
                else:
                    posy += tabrect.height

        if flags:
            return wx.NOT_FOUND, flags
        else:
            return wx.NOT_FOUND


    def EnableDragAndDrop(self, enable=True):
        """ Globall Enables/Disables Tabs Drag And Drop. """
        
        self._enabledragging = enable


    def EnableHiding(self, enable=True):
        """ Globally Enables/Disables Hiding On Tabs In Runtime. """

        self._enablehiding = enable
        self.UpdateMenuButton(enable)
        
        wx.FutureCall(1000, self.UpdateMenuButton, enable)
        

    def SetAnimationImages(self, nPage, imgarray):
        """
        Sets An Animation List Associated To The Given Page nPage.
        
        @param nPage: The Given Page
        @param imgarray: A List Of Image Indexes Of Images Inside The
          ImageList Associated To NotebookCtrl.
        """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetAnimationImages: (" + str(nPage) + ")"

        if not imgarray:
            raise "\nERROR: Invalid Image Array In SetAnimationImages: (" + repr(imgarray) + ")"

        if min(imgarray) < 0:
            raise "\nERROR: Invalid Image Array In SetAnimationImages: (Min(ImgArray) = " + \
                  str(min(imgarray)) + " < 0)"

        if max(imgarray) > self.GetImageList().GetImageCount() - 1:
            raise "\nERROR: Invalid Image Array In SetAnimationImages: (Max(ImgArray) = " + \
                  str(max(imgarray)) + " > " + str(self.GetImageList().GetImageCount()-1) + ")"
        
        self._pages[nPage]._animationimages = imgarray
        

    def GetAnimationImages(self, nPage):
        """ Returns The Animation Images List Associated To The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetAnimationImages: (" + str(nPage) + ")"

        return self._pages[nPage]._animationimages
    

    def AnimateTab(self, event):
        """ Called When The Refreshing Animation Timer Expires. Used Internally"""

        obj = event.GetEventObject()
        nPage = self._timers.index(obj)

        if not self.IsPageEnabled(nPage):
            return
        
        indx = self.GetPageImage(nPage)
        images = self.GetAnimationImages(nPage)
        myindx = images.index(indx)
        
        if indx == images[-1]:
            myindx = -1
        
        myindx = myindx + 1
            
        self.SetPageImage(nPage, images[myindx])


    def StartAnimation(self, nPage, timer=500):
        """ Starts The Animation On The Given Page, With Refreshing Time Rate "timer". """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In StartAnimation: (" + str(nPage) + ")"
        
        images = self.GetAnimationImages(nPage)

        if not images:
            raise "\nERROR: No Images Array Defined For Page: (" + str(nPage) + ")"

        if len(images) == 1:
            raise "\nERROR: Impossible To Animate Tab: " + str(nPage) + " With Only One Image"

        self._timers[nPage].Start(timer)


    def StopAnimation(self, nPage):
        """ Stops The Animation On The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In StopAnimation: (" + str(nPage) + ")"

        if self._timers[nPage].IsRunning():
            self._timers[nPage].Stop()


    def SetDrawX(self, drawx=True, style=1, image1=None, image2=None):
        """
        Globally Enables/Disables The Drawing Of A Closing "X" In The Tab.
        
        @param drawx: C{True} to enable drawing a closing "X"; C{False} to
          disable it
        @param style: the style of the X to draw when C{drawx} is C{True};
          possible values are:
            - C{1}: Small "X" At The Top-Right Of The Tab;
            - C{2}: Bigger "X" In The Middle Vertical Of The Tab (Like Opera Notebook);
            - C{3}: Custom "X" Image Is Drawn On Tabs.
        @param image1: if C{style} is C{3}, the image to use when drawing
          the X on an unhighlighted tab
        @param image2: if C{style} is C{3}, the image to use when drawing
          the X on a highlighted tab
        """

        self._drawx = drawx
        self._drawxstyle = style

        if style == 3:
            self._imglist2 = wx.ImageList(16, 16, True, 0)
            self._imglist2.Add(image1)
            self._imglist2.Add(image2)

        if self._style & NC_LEFT or self._style & NC_RIGHT:
            self.SetBestSize((self._CalcBestWidth(wx.ClientDC(self)), -1))
            self._parent.GetSizer().Layout()
            
        self.Refresh()


    def GetDrawX(self):
        """
        Returns The Enable/Disable State Of Drawing Of A Small "X" At The Top-Right Of
        Every Page.
        """

        return self._drawx, self._drawxstyle
    

    def GetInsideTab(self, pt):
        """ Returns The Tab On Which The Mouse Is Hovering On. """

        count = 0
        
        for tabs in self._tabrect:
            if tabs.Contains(pt):
                return count
            
            count = count + 1

        return -1
    

    def GetInsideX(self, pt):
        """ Returns The Tab On Which The Mouse Is Hovering On The "X" Button. """
        
        count = 0
        
        for rects in self._xrect:
            if rects.Contains(pt):
                return count
            
            count = count + 1

        return -1


    def SetImageToCloseButton(self, convert=True):
        """ Set Whether The Tab Icon Should Be Converted To The Close Button Or Not. """
        
        self._convertimage = convert

        
    def GetImageToCloseButton(self):
        """ Get Whether The Tab Icon Should Be Converted To The Close Button Or Not. """
        
        return self._convertimage        


    def ConvertImageToCloseButton(self, page):
        """ Globally Converts The Page Image To The "Opera" Style Close Button. """

        bmpindex = self.GetPageImage(page)
        if  bmpindex < 0:
            return

        tabrect = self._tabrect[page]
        size = self.GetSize()

        maxfont = self._maxfont

        dc = wx.ClientDC(self)        

        dc.SetFont(maxfont) 
        pom, height = dc.GetTextExtent("Aq")
        
        bmp = self._imglist.GetBitmap(bmpindex)
                
        bmpposx = tabrect.x + self._padding.x
        bmpposy = size.y - (height + 2*self._padding.y + bmp.GetHeight())/2 - 1

        ypos = size.y - height - self._padding.y*2
        ysize = height + self._padding.y*2 + 3

        if page == self.GetSelection():
            bmpposx = bmpposx + 1
            bmpposy = bmpposy - 1
            ypos = ypos - 3                    
            ysize = ysize + 2
            
        colour = self.GetPageColour(page)
        bmprect = wx.Rect(bmpposx, bmpposy, bmp.GetWidth()+self._padding.x, bmp.GetHeight())
        
        dc.SetBrush(wx.Brush(colour))
        dc.SetPen(wx.TRANSPARENT_PEN)
        dc.DrawRectangleRect(bmprect)
        
        colour = self.GetPageTextColour(page)
        
        r = colour.Red()
        g = colour.Green()
        b = colour.Blue()

        hr, hg, hb = min(255,r+64), min(255,g+64), min(255,b+64)
                
        colour = wx.Colour(hr, hg, hb)
        back_colour = wx.WHITE

        yypos = ypos+(ysize-height-self._padding.y/2)/2

        xrect = wx.Rect(bmprect.x+(bmprect.width - self._padding.x - height)/2,
                        yypos, height, height)
       
        # Opera Style
        dc.SetPen(wx.Pen(colour, 1))
        dc.SetBrush(wx.Brush(colour))
        dc.DrawRoundedRectangleRect(xrect, 2)
        dc.SetPen(wx.Pen(back_colour, 2))
        dc.DrawLine(xrect[0]+2, xrect[1]+2, xrect[0]+xrect[2]-3, xrect[1]+xrect[3]-3)
        dc.DrawLine(xrect[0]+2, xrect[1]+xrect[3]-3, xrect[0]+xrect[2]-3, xrect[1]+2)
        

    def RedrawClosingX(self, pt, insidex, drawx, highlight=False):
        """ Redraw The Closing "X" Accordingly To The Mouse "Hovering" Position. """
        
        colour = self.GetPageTextColour(insidex)
        back_colour = self.GetBackgroundColour()
        imagelist = 0
        
        if highlight:
            r = colour.Red()
            g = colour.Green()
            b = colour.Blue()

            hr, hg, hb = min(255,r+64), min(255,g+64), min(255,b+64)
                
            colour = wx.Colour(hr, hg, hb)
            back_colour = wx.WHITE
            imagelist = 1
            
        dc = wx.ClientDC(self)
        xrect = self._xrect[insidex]
        
        if drawx == 1:
            # Emule Style
            dc.SetPen(wx.Pen(colour, 1))
            dc.SetBrush(wx.TRANSPARENT_BRUSH)
            dc.DrawRectangleRect(xrect)
        elif drawx == 2:
            # Opera Style
            dc.SetPen(wx.Pen(colour, 1))
            dc.SetBrush(wx.Brush(colour))
            dc.DrawRoundedRectangleRect(xrect, 2)
            dc.SetPen(wx.Pen(back_colour, 2))
            dc.DrawLine(xrect[0]+2, xrect[1]+2, xrect[0]+xrect[2]-3, xrect[1]+xrect[3]-3)
            dc.DrawLine(xrect[0]+2, xrect[1]+xrect[3]-3, xrect[0]+xrect[2]-3, xrect[1]+2)
        else:
            self._imglist2.Draw(imagelist, dc, xrect[0], xrect[1],
                                wx.IMAGELIST_DRAW_TRANSPARENT, True)
    

    def HideOnSingleTab(self, hide=True):
        """ Hides The TabCtrl When There Is Only One Tab In NotebookCtrl. """
        
        self._hideonsingletab = hide
        

    def SetPagePopupMenu(self, nPage, menu):
        """ Sets A Popup Menu Specific To A Single Tab. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPagePopupMenu: (" + str(nPage) + ")"
        
        self._pages[nPage]._menu = menu


    def GetPagePopupMenu(self, nPage):
        """ Returns The Popup Menu Associated To A Single Tab. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPagePopupMenu: (" + str(nPage) + ")"        
        
        return self._pages[nPage]._menu


    def DrawInsertionMark(self, dc, nPage):
        """
        Draw An Insertion Arrow To Let The User Understand Where A Dragged Tab Will
        Be Dropped (Between Which Tabs).
        """

        if not self._enablehiding:
            if nPage < 0 or nPage >= len(self._tabrect):
                return
        else:
            if nPage < 0 or nPage >= len(self._tabrect) + self._tabvisible.count(0):
                return
            
        colour = wx.BLACK
        somehidden = False

        if self._enablehiding:
            for ii in xrange(nPage):
                if self._pages[ii]._ishidden:
                    nPage = nPage - 1
                    somehidden = True

        rect = self._tabrect[nPage]

        x1 = rect.x - 4
        y1 = rect.y - 1
        x2 = rect.x
        y2 = y1 + 5
        x3 = rect.x + 3
        y3 = y1

        mybrush = wx.Brush(self.GetPageTextColour(nPage))
        
        if not self._enablehiding:
            if nPage > self._tabID:
                x1 = x1 + rect.width
                x2 = x2 + rect.width
                x3 = x3 + rect.width
        else:
            mybrush = wx.Brush(self.GetPageTextColour(nPage))
            if nPage >= self._tabID:
                x1 = x1 + rect.width
                x2 = x2 + rect.width
                x3 = x3 + rect.width
        
        dc.SetPen(wx.Pen(wx.BLACK, 1))
        dc.SetBrush(mybrush)
        dc.DrawPolygon([(x1, y1), (x2, y2), (x3, y3)])
        

    def OnMouseMotion(self, event):
        """ Handles The wx.EVT_MOTION Event For TabCtrl. """

        pt = event.GetPosition()
        
        if self._enabledragging:
            
            if event.Dragging() and not event.RightIsDown() and not event.MiddleIsDown():
                
                tolerance = 2
                
                dx = abs(pt.x - self._dragstartpos.x)
                dy = abs(pt.y - self._dragstartpos.y)
                
                if dx <= tolerance and dy <= tolerance:
                    self.SetCursor(wx.STANDARD_CURSOR)
                    return

                self.SetCursor(self._dragcursor)
                self._isdragging = True
                self._isleaving = False
                newpos = self.HitTest(pt)

                if newpos >= 0 and newpos != self._olddragpos:
                    self._olddragpos = newpos
                    self.Refresh()
                    
            else:

                self._isdragging = False
                self.SetCursor(wx.STANDARD_CURSOR)

        if not event.Dragging():
            drawx = self.GetDrawX()
            
            if drawx[0]:
                insidex = self.GetInsideX(pt)
                if insidex >= 0:
                    if self.IsPageEnabled(insidex):
                        self.RedrawClosingX(pt, insidex, drawx[1], True)
                        self._xrefreshed = False
                else:
                    if not self._xrefreshed:
                        insidetab = self.GetInsideTab(pt)
                        if insidetab >= 0:
                            if self.IsPageEnabled(insidetab):
                                self.RedrawClosingX(pt, insidetab, drawx[1])
                                self._xrefreshed = True
            else:
                if self.GetImageToCloseButton():
                    page, flags = self.HitTest(pt, 1)
                    if page >= 0:
                        if self.IsPageEnabled(page):
                            if flags == NC_HITTEST_ONICON:
                                if not self._imageconverted:
                                    self.ConvertImageToCloseButton(page)
                                    self._imageconverted = True
                            else:
                                if self._imageconverted:
                                    self.Refresh()
                                    self._imageconverted = False
                            
        if self._showtooltip:
            if not event.Dragging():
                if not event.LeftDown():
                    
                    oldinside = self._insidetab
                    self._insidetab = self.GetInsideTab(pt)

                    if self._insidetab >= 0:
                        if oldinside != self._insidetab:

                            if self._istooltipshown:
                                self._tipwindow.Destroy()
                                self._istooltipshown = False
                                self.Refresh()
                                
                            if self._tiptimer.IsRunning():
                                self._tiptimer.Stop()
                                
                            tip, ontime, winsize= self.GetPageToolTip(self._insidetab)
                            
                            if tip.strip() != "":
                                self._currenttip = tip
                                self._currentwinsize = winsize
                                self._tiptimer.Start(ontime, wx.TIMER_ONE_SHOT)
                            
                    else:
                        if self._istooltipshown:
                            self._tipwindow.Destroy()
                            self._istooltipshown = False
                            self.Refresh()

        self._mousepos = pt
        
        event.Skip()
        

    def OnShowToolTip(self):
        """ Called When The Timer For The ToolTip Expires. Used Internally. """

        pt = self.ScreenToClient(wx.GetMousePosition())
        
        oldinside = self._insidetab
        self._insidetab = self.GetInsideTab(pt)

        if self._insidetab != oldinside or self._insidetab < 0:
            return
                    
        self._istooltipshown = True
        self._tipwindow = self.TransientTipWindow(self, self._currenttip,
                                                  self._currentwinsize)

        xsize, ysize = self._tipwindow.GetSize()
        xpos, ypos = self.ClientToScreen(self._mousepos)

        if xpos + xsize > self._xvideo - 10:
            if ypos + ysize > self._yvideo - 10:  # SW Tip Positioning
                posx = xpos - xsize
                posy = ypos - ysize
            else: # NE Tip Positioning
                posx = xpos - xsize
                posy = ypos
        else:
            if ypos + ysize > self._yvideo - 10:  # SE Tip Positioning
                posx = xpos + 10
                posy = ypos - ysize
            else: # NW Tip Positioning
                posx = xpos + 10
                posy = ypos

        if posy < 0:
            posy = ypos

        if posx < 0:
            posx = xpos
        
        self._tipwindow.SetPosition((posx, posy))
        self._tipwindow.Show()
        

    def OnMouseLeftDown(self, event):
        """ Handles The wx.EVT_LEFT_DOWN Event For TabCtrl. """
        
        pos = event.GetPosition()        
        page, flags = self.HitTest(pos, 1)
        self._dragstartpos = pos

        if page != wx.NOT_FOUND:

            if self.IsPageEnabled(page):

                if event.m_controlDown:
                    if page in self._selectedtabs:
                        self._selectedtabs.remove(page)
                    else:
                        self._selectedtabs.append(page)
                    self.Refresh()
                else:
                    self._selectedtabs = []
                    if flags == NC_HITTEST_ONX or (flags == NC_HITTEST_ONICON and self.GetImageToCloseButton()):
                        eventOut = NotebookCtrlEvent(wxEVT_NOTEBOOKCTRL_PAGE_CLOSING, self.GetId())
                        eventOut.SetOldSelection(self._selection)
                        eventOut.SetSelection(page)
                        eventOut.SetEventObject(self)
                        
                        if not self.GetEventHandler().ProcessEvent(eventOut):
                            self._parent.DeletePage(page)
                            self._parent.bsizer.Layout()

                    else:
                        self.SetSelection(page)
                        self._tabID = page
                        
        event.Skip()


    def OnMouseLeftDClick(self, event):
        """ Handles The wx.EVT_LEFT_DCLICK Event For TabCtrl. """
        
        pos = event.GetPosition()        
        page = self.HitTest(pos)
        self._selectedtabs = []

        if page == wx.NOT_FOUND:
            return

        if not self.IsPageEnabled(page):
            return

        eventOut = NotebookCtrlEvent(wxEVT_NOTEBOOKCTRL_PAGE_DCLICK, self.GetId())
        eventOut.SetOldSelection(self._selection)
        eventOut.SetSelection(page)
        eventOut.SetEventObject(self)
                
        if not self.GetEventHandler().ProcessEvent(eventOut):
            return

        event.Skip()        
        

    def OnMouseLeftUp(self, event):
        """ Handles The wx.EVT_LEFT_UP Event For TabCtrl. """

        if not self._enabledragging:
            event.Skip()
            return

        if not self._isdragging:
            self.SetCursor(wx.STANDARD_CURSOR)
            event.Skip()
            return

        id = self.HitTest(wx.Point(event.GetX(), event.GetY()))

        if id >= 0 and id != self._tabID: 

            self._isdragging = False
            self._olddragpos = -1
            eventOut = NotebookCtrlEvent(wxEVT_NOTEBOOKCTRL_PAGE_DND, self.GetId())
            eventOut.SetOldPosition(self._tabID)
            eventOut.SetNewPosition(id)
            eventOut.SetEventObject(self)
            
            if self.GetEventHandler().ProcessEvent(eventOut):
                self._tabID = -1
                self._olddragpos = -1
                self.SetCursor(wx.STANDARD_CURSOR)
                self.Refresh()
                return
            
            self._parent.Freeze()

            try:
                text = self.GetPageText(self._tabID)
                image = self.GetPageImage(self._tabID)
                font1 = self.GetPageTextFont(self._tabID)
                font2 = self.GetPageTextSecondaryFont(self._tabID)
                fontcolour = self.GetPageTextColour(self._tabID)
                pagecolour = self.GetPageColour(self._tabID)
                enabled = self.IsPageEnabled(self._tabID)
                tooltip, ontime, winsize = self.GetPageToolTip(self._tabID)
                menu = self.GetPagePopupMenu(self._tabID)
                firstcol = self.GetPageFirstGradientColour(self._tabID)
                secondcol = self.GetPageSecondGradientColour(self._tabID)
                ishidden = self._pages[self._tabID]._ishidden
            except:
                self._parent.Thaw()
                self._tabID = -1 
                self.SetCursor(wx.STANDARD_CURSOR)
                return
            
            isanimated = 0
            if self._timers[self._tabID].IsRunning():
                isanimated = 1
                timer = self._timers[self._tabID].GetInterval()
                
            self.StopAnimation(self._tabID)
            animatedimages = self.GetAnimationImages(self._tabID)
                        
            pagerange = range(self.GetPageCount())
                
            newrange = pagerange[:]
            newrange.remove(self._tabID)
            newrange.insert(id, self._tabID)
                    
            newpages = []
            counter = self.GetPageCount() - 1
            
            for ii in xrange(self.GetPageCount()):  
                newpages.append(self._parent.GetPage(ii))
                self._parent.bsizer.Detach(counter-ii)

            cc = 0

            self._parent._notebookpages = []
            
            for jj in newrange:
                self._parent.bsizer.Add(newpages[jj], 1, wx.EXPAND | wx.ALL, 2)
                self._parent.bsizer.Show(cc, False)
                self._parent._notebookpages.append(newpages[jj])
                cc = cc + 1
                    
            self.DeletePage(self._tabID)
            
            if enabled:
                if id == self.GetPageCount():
                    self.AddPage(text, True, image)
                else:
                    self.InsertPage(id, text, True, image)
            else:
                if id == self.GetPageCount():
                    self.AddPage(text, False, image)
                else:
                    self.InsertPage(id, text, False, image)

            self.SetPageImage(id, image)
            self.SetPageText(id, text)
            self.SetPageTextFont(id, font1)
            self.SetPageTextSecondaryFont(id, font2)
            self.SetPageTextColour(id, fontcolour)
            self.SetPageColour(id, pagecolour)
            self.EnablePage(id, enabled)
            self.SetPageToolTip(id, tooltip, ontime, winsize)
            self.SetPagePopupMenu(id, menu)
            self.SetPageFirstGradientColour(id, firstcol)
            self.SetPageSecondGradientColour(id, secondcol)
            self._pages[id]._ishidden = ishidden
            
            if isanimated and len(animatedimages) > 1:
                self.SetAnimationImages(id, animatedimages)
                self.StartAnimation(id, timer)
                
            if enabled:
                self._parent.bsizer.Show(id, True)
            else:
                sel = self.GetSelection()
                
                if sel == -1:
                    sel = 0
                self._parent.bsizer.Show(id, False)
                self._parent.SetSelection(sel)
                self._parent.bsizer.Show(sel, True)
                
            self._parent.bsizer.Layout()
            
            self._parent.Thaw()

        self._isdragging = False
        self._olddragpos = -1
        self._fromdnd = True
        self.Refresh()
        self._tabID = -1 
        self.SetCursor(wx.STANDARD_CURSOR)
        
        event.Skip()
        
        
    def OnSize(self, event=None):
        """ Handles The wx.EVT_SIZE Event For TabCtrl. """

        if self._sizeToggleButton:
            width = self.GetSize()[0]
            height = self._CalcSizeToggleBestSize()[1]
            self._sizeToggleButton.SetSize(wx.Size(width, height))
        self.Refresh()

        if event is not None:
            event.Skip()
             

    def OnMouseRightUp(self, event):
        """ Handles The wx.EVT_RIGHT_UP Event For TabCtrl. """
        
        pt = event.GetPosition()
        id = self.HitTest(pt)

        self._selectedtabs = []        

        if id >= 0:
            if self.IsPageEnabled(id):
                menu = self.GetPagePopupMenu(id)
                if menu:
                    self.PopupMenu(menu)

        event.Skip()


    def OnMouseRightDown(self, event):
        """ Handles The wx.EVT_RIGHT_DOWN Event For TabCtrl. """

        pos = event.GetPosition()        
        page = self.HitTest(pos)

        self._selectedtabs = []
        
        if page == wx.NOT_FOUND:
            return

        if not self.IsPageEnabled(page):
            return
        
        eventOut = NotebookCtrlEvent(wxEVT_NOTEBOOKCTRL_PAGE_RIGHT, self.GetId())
        eventOut.SetOldSelection(self._selection)
        eventOut.SetSelection(page)
        eventOut.SetEventObject(self)
                
        if not self.GetEventHandler().ProcessEvent(eventOut):
            return

        event.Skip()


    def OnMouseMiddleDown(self, event):
        """ Handles The wx.EVT_MIDDLE_DOWN Event For TabCtrl. """

        pos = event.GetPosition()        
        page = self.HitTest(pos)

        self._selectedtabs = []        

        if page == wx.NOT_FOUND:
            return

        if not self.IsPageEnabled(page):
            return
        
        eventOut = NotebookCtrlEvent(wxEVT_NOTEBOOKCTRL_PAGE_MIDDLE, self.GetId())
        eventOut.SetOldSelection(self._selection)
        eventOut.SetSelection(page)
        eventOut.SetEventObject(self)
                
        if not self.GetEventHandler().ProcessEvent(eventOut):
            return

        event.Skip()


    def SetSelectionColour(self, colour=None):
        """ Sets The Tab Selection Colour (Thin Line Above The Selected Tab). """
        
        if colour is None:
            colour = wx.Colour(255, 180, 0)

        self._selectioncolour = colour


    def SetContourLineColour(self, colour=None):
        """ Sets The Contour Line Colour (Controur Line Around Tabs). """

        if colour is None:
            if not self._tabstyle._normal or self._usegradients:
                colour = wx.Colour(145, 167, 180)
            else:
                colour = wx.SystemSettings_GetColour(wx.SYS_COLOUR_WINDOW)

        if not self._tabstyle._normal or self._usegradients:
            self._highlightpen = wx.Pen(colour)
            self._highlightpen.SetCap(wx.CAP_BUTT)
        else:
            self._highlightpen2 = wx.Pen(colour)
            self._highlightpen2.SetCap(wx.CAP_BUTT)

        self.Refresh()
        

    def ApplyTabTheme(self, theme=None):
        """ Applies A Particular Theme To Be Drawn On Tabs. """
        
        if theme is None:
            theme = ThemeStyle()

        self._tabstyle = theme
        if self._style & NC_EXPANDABLE:
            self._InitExpandableTabStyles(self._style, self._expanded, theme)
        self.Refresh()        

    def DrawMacTheme(self, dc, tabrect, theme):
        """ Draws The Mac Theme On Tabs, If It Is Enabled. """
        
        if theme == 1:
            col1, col2 = NC_MAC_LIGHT
        else:
            col1, col2 = NC_MAC_DARK

        colour1 = wx.Colour(col1, col1, col1)
        colour2 = wx.Colour(col2, col2, col2)

        x, y, w, h = tabrect
        endrange = self._style & NC_ROTATE and w or h

        index = 0
        
        for ii in xrange(0, endrange, 2):
            if index%2 == 0:
                colour = colour1
            else:
                colour = colour2

            dc.SetBrush(wx.Brush(colour))
            dc.SetPen(wx.Pen(colour))
            if self._style & NC_ROTATE:
                if ii > 3:
                    dc.DrawRectangle(x+ii, y, 2, w)
                else:
                    dc.DrawRoundedRectangle(x+ii, y, 3, 3)
            else:
                if ii > 3:
                    dc.DrawRectangle(x, y+ii, w, 2)
                else:
                    dc.DrawRoundedRectangle(x, y+ii, w, 3, 3)
                
            index = index + 1

        self._lastcolour = colour            


    def DrawKDETheme(self, dc, rect):
        """ Draws Unix-Style KDE Theme On Tabs. """
        
        x, y, w, h = rect
        
        if self._style & NC_ROTATE and self._style & NC_RIGHT:
            bandrange = xrange(13, -1, -1)
            self._lastcolour = kdetheme[13]
            brush = wx.Brush(kdetheme[0], wx.SOLID)
        else:
            bandrange = xrange(14)
            self._lastcolour = kdetheme[0]
            brush = wx.Brush(kdetheme[13], wx.SOLID)

        dc.SetBackground(brush)
        for band in bandrange:
            pen = wx.Pen(kdetheme[band])
            dc.SetPen(pen)
            if self._style & NC_ROTATE:
                if self._style & NC_RIGHT:
                    dc.DrawLine(x+1+band, y+1, x+1+band, y+h-1)
                    dc.DrawLine(x+w-(1+band), y+1, x+w-(1+band), y+h-2)
                else:
                    dc.DrawLine(x+1+band, y+1, x+1+band, y+h-1)
                    dc.DrawLine(x+w-(2+band), y+1, x+w-(2+band), y+h-2)
            else:
                dc.DrawLine(x+1, y+band, x+w-1, y+band)
                dc.DrawLine(x+1, y+h-1-band, x+w-2, y+h-1-band)

        
    def DrawSilverTheme(self, dc, rect, selected):
        """ Draws Windows XP Silver-Like Theme. """

        x, y, w, h = rect

        if selected:
            r1 = silvertheme1[0].Red()
            g1 = silvertheme1[0].Green()
            b1 = silvertheme1[0].Blue()
            r2 = silvertheme1[1].Red()
            g2 = silvertheme1[1].Green()
            b2 = silvertheme1[1].Blue()
        else:
            r1 = silvertheme2[0].Red()
            g1 = silvertheme2[0].Green()
            b1 = silvertheme2[0].Blue()
            r2 = silvertheme2[1].Red()
            g2 = silvertheme2[1].Green()
            b2 = silvertheme2[1].Blue()
            rend = silvertheme2[2].Red()
            gend = silvertheme2[2].Green()
            bend = silvertheme2[2].Blue()

        if self._style & NC_ROTATE:
            flrect = float(w-2)
        else:
            flrect = float(h-2)

        rstep = float((r2 - r1)) / flrect
        gstep = float((g2 - g1)) / flrect
        bstep = float((b2 - b1)) / flrect

        rf, gf, bf = 0, 0, 0

        counter = 0
        
        if self._style & NC_ROTATE:
            if self._style & NC_RIGHT:
                bandrange = xrange(x+w-2, x, -1)
            else:
                bandrange = xrange(x+1, x+w-1)
        else:
            bandrange = xrange(y+1, y+h)
        
        for band in bandrange:
            currCol = (int(round(r1 + rf)), int(round(g1 + gf)), int(round(b1 + bf)))
            dc.SetBrush(wx.Brush(currCol, wx.SOLID))
            dc.SetPen(wx.Pen(currCol))
            if self._style & NC_ROTATE:
                if counter == 0:
                    ypos = y + 2
                    yend = h - 4
                elif counter == 1:
                    ypos = y + 1
                    yend = h - 2
                else:
                    ypos = y + 1
                    yend = h - 2
    
                dc.DrawRectangle(band, ypos, 1, yend)
                
            else:
                if counter == 0:
                    xpos = x + 2
                    xend = w - 4
                elif counter == 1:
                    xpos = x + 1
                    xend = w - 2
                else:
                    xpos = x + 1
                    xend = w - 2
    
                dc.DrawRectangle(xpos, band, xend, 1)
                
            counter = counter + 1
            rf = rf + rstep
            gf = gf + gstep
            bf = bf + bstep
            self._lastcolour = currCol
            
        if not selected and self._style & NC_TOP:
            dc.SetBrush(wx.Brush((rend, gend, bend)))
            dc.SetPen(wx.Pen((rend, gend, bend)))
            if self._style & NC_ROTATE:
                if self._style & NC_LEFT:
                    xpos = x + w - 4
                else:
                    xpos = x
                
                dc.DrawRectangle(xpos, ypos, 3, yend)
            else:
                dc.DrawRectangle(xpos, y+h-3, xend, 3)
            self._lastcolour = wx.Colour(rend, gend, bend)
            

    def DrawAquaTheme(self, dc, rect, style, selected):
        """ Draws Mac-Style Aqua Theme On Tabs. """
        
        x, y, w, h = rect
        
        if selected:
            if style == 1:  # Dark Aqua
                r1 = topaqua1[0].Red()
                g1 = topaqua1[0].Green()
                b1 = topaqua1[0].Blue()

                r2 = topaqua1[1].Red()
                g2 = topaqua1[1].Green()
                b2 = topaqua1[1].Blue()
            else:
                r1 = topaqua2[0].Red()
                g1 = topaqua2[0].Green()
                b1 = topaqua2[0].Blue()

                r2 = topaqua2[1].Red()
                g2 = topaqua2[1].Green()
                b2 = topaqua2[1].Blue()

        else:
            r1 = distaqua[0].Red()
            g1 = distaqua[0].Green()
            b1 = distaqua[0].Blue()

            r2 = distaqua[1].Red()
            g2 = distaqua[1].Green()
            b2 = distaqua[1].Blue()

        flrect = float((h-2)/2)

        rstep = float((r2 - r1)) / flrect
        gstep = float((g2 - g1)) / flrect
        bstep = float((b2 - b1)) / flrect

        rf, gf, bf = 0, 0, 0

        counter = 0
        dc.SetPen(wx.TRANSPARENT_PEN)
        
        if self._style & NC_ROTATE:
            startrange, endrange = (x, w)
        else:
            startrange, endrange = (y, h)
        if self._style & NC_ROTATE and self._style & NC_RIGHT:
            bandrange = xrange(startrange+endrange, startrange+endrange/2, -1)
        else:
            bandrange = xrange(startrange+1, startrange+endrange/2)
        
        for band in bandrange:
            currCol = (int(round(r1 + rf)), int(round(g1 + gf)), int(round(b1 + bf)))
            dc.SetBrush(wx.Brush(currCol, wx.SOLID))
            if self._style & NC_ROTATE:
                if counter == 0:
                    ypos = y + 2
                    yend = h - 4
                elif counter == 1:
                    ypos = y + 1
                    yend = h - 2
                else:
                    ypos = y + 1
                    yend = h - 2
                    
                dc.DrawRectangle(band, ypos, 1, yend)
            else:
                if counter == 0:
                    xpos = x + 2
                    xend = w - 4
                elif counter == 1:
                    xpos = x + 1
                    xend = w - 2
                else:
                    xpos = x + 1
                    xend = w - 2
            
                dc.DrawRectangle(xpos, band, xend, 1)

            counter = counter + 1

            rf = rf + rstep
            gf = gf + gstep
            bf = bf + bstep

        if selected:
            if style == 1:  # Dark Aqua 
                r1 = botaqua1[0].Red()
                g1 = botaqua1[0].Green()
                b1 = botaqua1[0].Blue()

                r2 = botaqua1[1].Red()
                g2 = botaqua1[1].Green()
                b2 = botaqua1[1].Blue()
            else:
                r1 = botaqua2[0].Red()
                g1 = botaqua2[0].Green()
                b1 = botaqua2[0].Blue()

                r2 = botaqua2[1].Red()
                g2 = botaqua2[1].Green()
                b2 = botaqua2[1].Blue()
        else:
            r1 = disbaqua[0].Red()
            g1 = disbaqua[0].Green()
            b1 = disbaqua[0].Blue()

            r2 = disbaqua[1].Red()
            g2 = disbaqua[1].Green()
            b2 = disbaqua[1].Blue()

        flrect = float((h-2)/2)

        rstep = float((r2 - r1)) / flrect
        gstep = float((g2 - g1)) / flrect
        bstep = float((b2 - b1)) / flrect

        rf, gf, bf = 0, 0, 0

        counter = 0

        if self._style & NC_ROTATE and self._style & NC_RIGHT:
            bandrange = xrange(startrange+endrange/2, startrange+1, -1)
        else:
            bandrange = xrange(startrange+endrange/2, startrange+endrange)
        for band in bandrange:
            currCol = (int(round(r1 + rf)), int(round(g1 + gf)), int(round(b1 + bf)))
            dc.SetBrush(wx.Brush(currCol, wx.SOLID))
            if self._style & NC_ROTATE:
                dc.DrawRectangle(band, y + 1, 1, h-2)
            else:
                dc.DrawRectangle(x+1, band, w-2, 1)
            rf = rf + rstep
            gf = gf + gstep
            bf = bf + bstep

        self._lastcolour = currCol
        
        
    def DrawMetalTheme(self, dc, rect):
        """ Draws Mac-Style Metal Gradient On Tabs. """

        x, y, w, h = rect
        
        dc.SetPen(wx.TRANSPARENT_PEN)
        counter = 0
        
        if self._style & NC_ROTATE:
            bandrange = xrange(x+1, x+w)
        else:
            bandrange = xrange(y+1, h+y)
        for band in bandrange:
            if self._style & NC_ROTATE:
                intens = (230 + 80 * (x-band)/w)
            else:
                intens = (230 + 80 * (y-band)/h)

            colour = wx.Colour(intens, intens, intens)
            dc.SetBrush(wx.Brush(colour))

            if self._style & NC_ROTATE:
                if counter == 0:
                    ypos = y + 2
                    yend = h - 4
                elif counter == 1:
                    ypos = y + 1
                    yend = h - 2
                else:
                    ypos = y + 1
                    yend = h - 2
                if self._style & NC_RIGHT:
                    dc.DrawRectangle(x+w-band, ypos, 1, yend)
                else:
                    dc.DrawRectangle(x+band, ypos, 1, yend)
                
            else:
                if counter == 0:
                    xpos = x + 2
                    xend = w - 4
                elif counter == 1:
                    xpos = x + 1
                    xend = w - 2
                else:
                    xpos = x + 1
                    xend = w - 2
                dc.DrawRectangle(xpos, band, xend, 1)

            counter = counter + 1              
                
        self._lastcolour = colour


    def DrawVerticalGradient(self, dc, rect, index):
        """ Gradient Fill From Colour 1 To Colour 2 From Top To Bottom. """

        dc.SetPen(wx.TRANSPARENT_PEN)

        # calculate gradient coefficients
        col2 = self._tabstyle.GetSecondGradientColour(index == self.GetSelection())
        col1 = self._tabstyle.GetFirstGradientColour(index == self.GetSelection())

        r1, g1, b1 = int(col1.Red()), int(col1.Green()), int(col1.Blue())
        r2, g2, b2 = int(col2.Red()), int(col2.Green()), int(col2.Blue())

        flrect = float(rect.height)

        rstep = float((r2 - r1)) / flrect
        gstep = float((g2 - g1)) / flrect
        bstep = float((b2 - b1)) / flrect

        rf, gf, bf = 0, 0, 0

        counter = 0
        
        bandrange = xrange(rect.y+1, rect.y + rect.height-1)
        lenc = len(bandrange)
        
        for y in bandrange:
            currCol = (r1 + rf, g1 + gf, b1 + bf)
                
            dc.SetBrush(wx.Brush(currCol, wx.SOLID))

            # adjust along x-axis to preserve the curved tab edge
            def GetXAdjust(counter):
                if counter >=2 or counter <=lenc-2:
                    return 1
                if self._style & NC_LEFT or self._style & NC_RIGHT and not self._style & NC_ROTATE:
                    if counter == 0 and self._style & NC_RIGHT or \
                        counter == lenc - 1 and self._style & NC_LEFT:
                        return 3
                    elif counter == 1 and self._style & NC_RIGHT or \
                        counter == lend - 2 and self._style & NC_LEFT:
                        return 2
                    else:
                        return 1
                else:
                    if counter == lenc - 2:
                        return 2
                    elif counter == lenc - 1:
                        return 3
                    else:
                        return 1
                    
            xadjust = GetXAdjust(counter)
            xpos = rect.x + xadjust
            xend = rect.width - xadjust


            counter = counter + 1
            
            dc.DrawRectangle(xpos, y, xend, 1)
            rf = rf + rstep
            gf = gf + gstep
            bf = bf + bstep

        self._lastcolour = currCol
        

    def DrawHorizontalGradient(self, dc, rect, index):
        """ Gradient Fill From Colour 1 To Colour 2 From Left To Right. """

        dc.SetPen(wx.TRANSPARENT_PEN)

        # calculate gradient coefficients
        col2 = self._tabstyle.GetSecondGradientColour(index == self.GetSelection())
        col1 = self._tabstyle.GetFirstGradientColour(index == self.GetSelection())

        r1, g1, b1 = int(col1.Red()), int(col1.Green()), int(col1.Blue())
        r2, g2, b2 = int(col2.Red()), int(col2.Green()), int(col2.Blue())

        flrect = float(rect.width)

        rstep = float((r2 - r1)) / flrect
        gstep = float((g2 - g1)) / flrect
        bstep = float((b2 - b1)) / flrect

        rf, gf, bf = 0, 0, 0
        counter = 0
        
        bandrange = xrange(rect.x + 1, rect.x + rect.width - 1)
        lenc = len(bandrange)

        for x in bandrange:
            currCol = (r1 + rf, g1 + gf, b1 + bf)
            
            dc.SetBrush(wx.Brush(currCol, wx.SOLID))
            # adjust along y-axis to preserve the curved tab edge
            def GetYAdjust(counter):
                if counter >=2 or counter <=lenc-2:
                    return 1
                if self._style & NC_TOP or self._style & NC_BOTTOM or \
                    (self._style & NC_LEFT and self._style & NC_ROTATE):
                    if counter == 0 or counter == lenc - 1 and not self._style & NC_LEFT:
                        return 3
                    elif counter == 1 or counter == lenc - 2 and not self._style & NC_LEFT:
                        return 2
                    else:
                        return 1
                else:
                    if counter == lenc - 2:
                        return 2
                    elif counter == lenc - 1:
                        return 3
                    else:
                        return 1
                    
            yadjust = GetYAdjust(counter)
            ypos = rect.y + yadjust
            yend = rect.height - yadjust

            counter = counter + 1                
            
            dc.DrawRectangle(x, ypos, 1, yend)
            rf = rf + rstep
            gf = gf + gstep
            bf = bf + bstep

        self._lastcolour = currCol
        
        
    def GetAllTextExtents(self, dc):
        """ Returns All Tabs Text Extents. Used Internally. """

        self._mintabwidths = []
        self._maxtabwidths = []
        self._mintabheights = []
        self._maxtabheights = []
        self._incrtext = []
        minheight = 0

        for ii in xrange(self.GetPageCount()):

            txts = self.GetPageText(ii)
            font1 = self.GetPageTextFont(ii)
            dc.SetFont(font1)
            w1, h1 = dc.GetTextExtent(txts)
            minheight = max(minheight, h1)
            self._mintabwidths.append(w1)
            self._mintabheights.append(h1)
            font2 = self.GetPageTextSecondaryFont(ii)
            dc.SetFont(font2)
            w2, h2 = dc.GetTextExtent(txts)
            minheight = max(minheight, h2)
            
            self._maxtabwidths.append(w2)
            self._maxtabheights.append(h2)
            self._incrtext.append(abs(self._mintabwidths[ii] - self._maxtabwidths[ii]))

        mh1 = max(self._mintabheights)
        font1 = self.GetPageTextFont(self._mintabheights.index(mh1))
        mh2 = max(self._maxtabheights)
        font2 = self.GetPageTextSecondaryFont(self._maxtabheights.index(mh2))

        mhend = max(mh1, mh2)
        
        if mhend == mh1:
            maxfont = font1
        else:
            maxfont = font2

        minheight = self.GetSize()[1]
        
        return  minheight, maxfont
                

    def DrawBuiltinStyle(self, dc, style, rect, index, selection):
        """ Methods That Holds All The Theme Styles. """
        
        if style._aqua:
            if self._selstyle._normal:
                self.DrawAquaTheme(dc, rect, style._aqua, index==selection)
            else:
                oldselstyle = self._selstyle[:]
                self._selstyle._normal = True
                self.DrawBuiltinStyle(dc, self._selstyle, rect, index, selection)
                self._selstyle = oldselstyle

        elif style._metal:
            if self._selstyle._normal:
                self.DrawMetalTheme(dc, rect)
            else:
                oldselstyle = self._selstyle[:]
                self._selstyle._normal = True
                self.DrawBuiltinStyle(dc, self._selstyle, rect, index, selection)
                self._selstyle = oldselstyle

        elif style._kdetheme:
            if self._selstyle._normal:
                self.DrawKDETheme(dc, rect)
            else:
                oldselstyle = self._selstyle[:]
                self._selstyle._normal = True
                self.DrawBuiltinStyle(dc, self._selstyle, rect, index, selection)
                self._selstyle = oldselstyle
        
        elif style._macstyle:
            if self._selstyle._normal:
                self.DrawMacTheme(dc, rect, style._macstyle)
            else:
                oldselstyle = self._selstyle[:]
                self._selstyle._normal = True
                self.DrawBuiltinStyle(dc, self._selstyle, rect, index, selection)
                self._selstyle = oldselstyle

        elif style._gradient:
            if self._selstyle._normal:
                if style._gradient & ThemeStyle.GRADIENT_VERTICAL:
                    self.DrawVerticalGradient(dc, rect, index)
                else:
                    self.DrawHorizontalGradient(dc, rect, index)
            else:
                oldselstyle = self._selstyle[:]
                self._selstyle._normal = True
                self.DrawBuiltinStyle(dc, self._selstyle, rect, index, selection)
                self._selstyle = oldselstyle
                
        elif style._silver:
            if self._selstyle._normal:
                self.DrawSilverTheme(dc, rect, index==selection)
            else:
                oldselstyle = self._selstyle[:]
                self._selstyle._normal = True
                self.DrawBuiltinStyle(dc, self._selstyle, rect, index, selection)
                self._selstyle = oldselstyle


    def DrawGradientOnTab(self, dc, rect, col1, col2):
        """ Draw A Gradient Coloured Tab. """

        dc.SetPen(wx.TRANSPARENT_PEN)

        r1, g1, b1 = int(col1.Red()), int(col1.Green()), int(col1.Blue())
        r2, g2, b2 = int(col2.Red()), int(col2.Green()), int(col2.Blue())

        flrect = float(rect.height)

        rstep = float((r2 - r1)) / flrect
        gstep = float((g2 - g1)) / flrect
        bstep = float((b2 - b1)) / flrect

        rf, gf, bf = 0, 0, 0

        counter = 0
        
        for y in xrange(rect.y+1, rect.y + rect.height):
            currCol = (r1 + rf, g1 + gf, b1 + bf)
                
            dc.SetBrush(wx.Brush(currCol, wx.SOLID))
            if counter == 0:
                xpos = rect.x + 2
                xend = rect.width - 4
            elif counter == 1:
                xpos = rect.x + 1
                xend = rect.width - 2
            else:
                xpos = rect.x
                xend = rect.width

            counter = counter + 1
            
            dc.DrawRectangle(xpos, y, xend, 1)
            rf = rf + rstep
            gf = gf + gstep
            bf = bf + bstep

        self._lastcolour = currCol            

    def _CalcBestWidth(self, dc):
        return max(self._CalcMaxTabWidth(dc), self._CalcSizeToggleBestSize()[0])

    def _CalcMaxTabWidth(self, dc):
        self._CalcMaxTextHeight(dc)
        textWidth = max(self._maxtabwidths)
        tabIndex = self._maxtabwidths.index(textWidth)
        bmpWidth, bmpHeight = self._CalcTabBitmapSize(tabIndex)
        tabrect = self._CalcTabRect(tabIndex, 0, 0, textWidth, bmpWidth, bmpHeight)
        # return the width based on the longest label, plus 3 for
        # the additional width of the selected tab
        return tabrect.width + 3
            
    def _CalcMaxTextHeight(self, dc):
        if self._somethingchanged:
            minheight, maxfont = self.GetAllTextExtents(dc)
            self._minheight = minheight
            self._maxfont = maxfont
        else:
            minheight = self._minheight
            maxfont = self._maxfont

        dc.SetFont(maxfont) 
        _, height = dc.GetTextExtent("Aq")
        self._maxtextheight = height

    def _CalcSizeToggleBestSize(self):
        if self._sizeToggleButton:
            return self._sizeToggleButton.GetBestSize()
        else:
            return wx.Size(0,0)

    def _CalcTabBitmapPosition(self, tabIndex, bmpWidth, bmpHeight, tabrect):
            
        if self._style & NC_ROTATE:
            bmpposx = tabrect.x + (tabrect.width - bmpWidth) / 2
            yoffset = self._padding.x
            if self._style & NC_LEFT:
                bmpposx += 1
                bmpposy = tabrect.y + tabrect.height - (yoffset + bmpHeight)
            else:
                bmpposy = tabrect.y + yoffset
            if tabIndex == self.GetSelection():
                bmpposx += self._style & NC_LEFT and -1 or 1
        else:
            bmpposx = tabrect.x + self._padding.x
            bmpposy = tabrect.y + (tabrect.height - bmpHeight) / 2
            if tabIndex == self.GetSelection() and self._style & NC_TOP:
                bmpposy -= 1
        
        return (bmpposx, bmpposy)
        
    def _CalcTabBitmapSize(self, tabIndex):
        result = (0, 0)
        bmp = self._GetTabBitmap(tabIndex)                
        bmpOk = bmp.Ok()                          
        if bmpOk:
            result = (bmp.GetWidth(), bmp.GetHeight())
        return result

    def _CalcTabBitmapSpace(self, bmpWidth, bmpHeight):
        space = self._padding.x
        bmpSpace = self._style & NC_ROTATE and bmpHeight or bmpWidth
        if bmpSpace:
            space = space + self._padding.x + bmpSpace
        return space
    
    def _CalcTabRect(self, tabIndex, posx, posy, textWidth, bmpWidth, bmpHeight):
            
        xpos = posx
        if self._style & NC_BOTTOM:
            ypos = 1
        elif self._style & NC_TOP:
            ypos = self.GetSize().y - self._maxtextheight - self._padding.y*2
        else:
            ypos = posy
        
        xsize = textWidth + self._CalcTabBitmapSpace(bmpWidth, bmpHeight) + \
            self._padding.x + self._incrtext[tabIndex] + \
            self._CalcXWidth()
        
        ysize = self._maxtextheight + self._padding.y*2
        if self._style & NC_TOP:
            ysize += 3
        
        if self._style & NC_ROTATE:
            xsize, ysize = (ysize, xsize)
            
        if tabIndex == self.GetSelection():
            if self._style & NC_TOP or self._style & NC_BOTTOM:
                xsize = xsize + self._spacetabs
                if tabIndex > 0:
                    xpos = xpos - self._spacetabs
                    xsize = xsize + self._spacetabs
                if self._style & NC_TOP:
                    ypos -= 3
                ysize = ysize + 2
            else:
                xsize += 3
                
        if self._style & NC_LEFT:
            xpos = self.GetSize().width - xsize
        return wx.Rect(xpos, ypos, xsize, ysize)
        
    def _CalcTabTextPosition(self, tabIndex, tabrect, space):
        xtextpos = tabrect.x + space + self._incrtext[tabIndex]/2

        if self._style & NC_BOTTOM:
            ytextpos = self._padding.y
        else: 
            ytextpos = tabrect.y + self._padding.y + 1
        if tabIndex == self.GetSelection():
            if tabIndex == 0 and self._style & NC_TOP or self._style & NC_BOTTOM:
                xtextpos = xtextpos + self._spacetabs/2.0 + 1
            if self._style & NC_BOTTOM:
                ytextpos += 2
            elif self._style & NC_TOP:
                ytextpos -= 2
                
        if self._style & NC_ROTATE:
            xoffset = ytextpos - tabrect.y
            yoffset = xtextpos - tabrect.x
            if self._style & NC_LEFT:
                xtextpos, ytextpos = (tabrect.x + xoffset - 1, 
                    tabrect.y + tabrect.height - yoffset)
            else:
                yoffset += self._CalcXWidth()
                xtextpos, ytextpos = (tabrect.x + tabrect.width - xoffset,
                    tabrect.y + yoffset)
        
        return (xtextpos, ytextpos)
    
    def _CalcTabTextWidth(self, dc, tabIndex):
        if self._style & NC_FIXED_WIDTH:
            result = max(self._maxtabwidths)
        else:
            dc.SetFont(self.GetPageTextFont(tabIndex))
            result, _ = dc.GetTextExtent(self.GetPageText(tabIndex))
        return result
        
    def _CalcXRect(self, tabrect):
        result = None
        drawx, dxstyle = self.GetDrawX()
        if drawx:
            if dxstyle == 1:
                mins = min(self._padding.x, self._padding.y) + 1
                mins = min(mins, 6)
                xoffset = tabrect.width-mins-3
                yoffset = 2
                xsize = ysize = mins+1
            else:
                if self._style & NC_ROTATE:
                    xoffset = (tabrect.width-self._maxtextheight-self._padding.y/2)/2
                    yoffset = self._padding.x/2                    
                else:
                    xoffset = tabrect.width-self._maxtextheight-self._padding.x
                    yoffset = (tabrect.height-self._maxtextheight-self._padding.y/2)/2
                xsize = ysize = self._maxtextheight
            result = wx.Rect(tabrect.x+xoffset, tabrect.y+yoffset, xsize, ysize)
        return result
    
    def _CalcXWidth(self):
        drawx, dxstyle = self.GetDrawX()
        if drawx:
            if dxstyle == 1:
                xxspace = self._padding.x/2
            else:
                xxspace = self._padding.x + self._maxtextheight
        else:
            xxspace = 0
        return xxspace
    
    def _ClipAtPaperEdge(self, dc, tabrect, tabIndex):
        selected = tabIndex == self.GetSelection()
        if self._style & NC_TOP:
            cliprect = (tabrect.x, tabrect.y, tabrect.width, 
                selected and tabrect.height - 2 or tabrect.height-3)
        elif self._style & NC_LEFT:
            cliprect = (tabrect.x, tabrect.y, tabrect.width - 2, tabrect.height)
        elif self._style & NC_BOTTOM:
            cliprect = (tabrect.x, tabrect.y + 2, tabrect.width, tabrect.height - 2)
        else:
            cliprect = (tabrect.x + 2, tabrect.y, tabrect.width - 2, tabrect.height)
        dc.SetClippingRegion(*cliprect)
        
    def _CreateSizeToggleButton(self):
        buttonlabel = self._expanded and "<<" or ">>"
        self._sizeToggleButton = wx.Button(self, wx.NewId(), 
            pos = wx.Point(0,0,), label = buttonlabel, style=wx.BU_EXACTFIT)
        font = self._sizeToggleButton.GetFont()
        if font.GetPointSize() > 6:
            font.SetPointSize(6)
            self._sizeToggleButton.SetFont(font)
        self.Bind(wx.EVT_BUTTON, self._ToggleSize, self._sizeToggleButton)
        
                                  
    def _DrawBackground(self, dc, paintTools):
        #background 
        size = self.GetSize()
        dc.SetBrush(paintTools.BackBrush)        

        if not (self._style & wx.NO_BORDER):
            # full border
            dc.SetPen(paintTools.BorderPen)
            dc.SetPen(paintTools.HighlightPen)
            dc.DrawRectangle(0, 0, size.x, size.y) 

        else:  
            dc.SetPen(paintTools.BackPen)
            dc.DrawRectangle(0, 0, size.x, size.y)
            self._DrawPageEdge(dc, paintTools)
                
    def _DrawFocusIndicator(self, dc, paintTools, tabrect):
        if self.GetUseFocusIndicator():
            dc.SetBrush(wx.TRANSPARENT_BRUSH)
            dc.SetPen(paintTools.FocusPen)
            dc.DrawRoundedRectangle(tabrect.x+self._padding.x/2, tabrect.y+self._padding.y/2,
                                    tabrect.width-self._padding.x,
                                    tabrect.height-self._padding.y-2, 2)
                                    
    def _DrawPageEdge(self, dc, paintTools):
        if self._style & NC_TOP:
            dc.SetPen(paintTools.HighlightPen)
            dc.DrawLine(0, self.GetSize().y-1, self.GetSize().x, self.GetSize().y-1)
        else:
            if not self._tabstyle._normal or self._usegradients:
                dc.SetPen(paintTools.HighlightPen)
            else:
                dc.SetPen(paintTools.BorderPen)
            if self._style & NC_BOTTOM:
                dc.DrawLine(0, 1, self.GetSize().x, 1)
            elif self._style & NC_LEFT:
                dc.DrawLine(self.GetSize().width - 1, 0, self.GetSize().width - 1, self.GetSize().height) 
            elif self._style & NC_RIGHT:
                dc.DrawLine(0, 0, 0, self.GetSize().height) 
        
    def _DrawTab(self, dc, paintTools, tabrect, tabIndex):
        size = self.GetSize()
        self._DrawTabGradientOutline(dc, paintTools, tabrect, tabIndex)
        self._FillTab(dc, paintTools, tabrect, tabIndex)            
        self._DrawTabOutline(dc, paintTools, tabrect, tabIndex)
        self._DrawTabPageEdge(dc, paintTools, tabrect, tabIndex)
        self._HighlightTabEdge(dc, paintTools, tabrect)
        self._ShadowTabEdge(dc, paintTools, tabrect)
            
    def _DrawTabBitmap(self, dc, tabIndex, bmpposx, bmpposy):
        bmpindex = self.GetPageImage(tabIndex)
        if self.IsPageEnabled(tabIndex):
            self._imglist.Draw(bmpindex, dc, bmpposx, bmpposy,
                               wx.IMAGELIST_DRAW_TRANSPARENT, True)
        else:
            self._grayedlist.Draw(bmpindex, dc, bmpposx, bmpposy,
                                  wx.IMAGELIST_DRAW_TRANSPARENT, True)
                                  
    def _DrawTabGradientOutline(self, dc, paintTools, tabrect, tabIndex):
        if not self._tabstyle._normal or self._usegradients:
            if tabIndex != self.GetSelection() and self._style & NC_TOP:
                dc.SetBrush(wx.TRANSPARENT_BRUSH)
                dc.SetPen(paintTools.ShadowPen)
                dc.DrawRoundedRectangle(tabrect.x+1, tabrect.y+1, tabrect.width, tabrect.height-1, 3)
            
    def _DrawTabOutline(self, dc, paintTools, tabrect, tabIndex):
        if not self._tabstyle._normal or self._usegradients:
            dc.SetBrush(wx.TRANSPARENT_BRUSH)
            dc.SetPen(paintTools.HighlightPen)
        else:
            dc.SetBrush(wx.Brush(self.GetPageColour(tabIndex)))
            if self._style & NC_TOP:
                dc.SetPen(paintTools.HighlightPen)
            else:
                dc.SetPen(paintTools.BorderPen)
        self._ClipAtPaperEdge(dc, tabrect, tabIndex)
        dc.DrawRoundedRectangle(tabrect.x, tabrect.y, tabrect.width, tabrect.height, 3)
        dc.DestroyClippingRegion()                
            
    def _DrawTabPageEdge(self, dc, paintTools, tabrect, tabIndex):
        if not self._tabstyle._normal or self._usegradients:
            edgePen = paintTools.HighlightPen
        else:
            if self._style & NC_TOP:
                edgePen = paintTools.HighlightPen
            else:
                edgePen = paintTools.BorderPen
                
        if tabIndex == self.GetSelection():
            # un-paint the line at the paper edge
            cancelPen = wx.Pen(self.GetPageColour(tabIndex))
            dc.SetPen(cancelPen)
            if self._style & NC_BOTTOM:
                dc.DrawLine(tabrect.x+1, tabrect.y, tabrect.x + tabrect.width, tabrect.y)
            elif self._style & NC_LEFT:
                dc.DrawLine(tabrect.x + tabrect.width-1, tabrect.y, tabrect.x + tabrect.width-1, tabrect.y + tabrect.height)
            elif self._style & NC_RIGHT:
                dc.DrawLine(tabrect.x, tabrect.y, tabrect.x, tabrect.y + tabrect.height)

        if tabIndex != self.GetSelection():
            if self._style & NC_TOP:
                dc.DrawLine(tabrect.x, self.GetSize().y-1, tabrect.x + tabrect.width, self.GetSize().y-1)
                
        # draw sharp corners at the paper edge
        dc.SetPen(edgePen)
        if self._style & NC_BOTTOM:
            dc.DrawLine(tabrect.x, tabrect.y, tabrect.x, tabrect.y + 2)
            dc.DrawLine((tabrect.x + tabrect.width)-1, tabrect.y, 
                (tabrect.x + tabrect.width)-1, tabrect.y + 2)
        elif self._style & NC_LEFT:
            dc.DrawLine(self.GetSize().width - 2, tabrect.y, self.GetSize().width, tabrect.y)
            dc.DrawLine(self.GetSize().width - 2, tabrect.y + tabrect.height - 1, self.GetSize().width, tabrect.y + tabrect.height - 1)
        elif self._style & NC_RIGHT:
            dc.DrawLine(tabrect.x, tabrect.y, tabrect.x + 2, tabrect.y)
            dc.DrawLine(tabrect.x, tabrect.y + tabrect.height - 1, tabrect.x + 2, tabrect.y + tabrect.height - 1)
        
    def _DrawTabText(self, dc, tabIndex, xtextpos, ytextpos):
        dc.SetFont(self.GetPageTextFont(tabIndex))
        dc.SetTextForeground(self._GetTabTextColour(tabIndex))
        dc.SetBrush(wx.TRANSPARENT_BRUSH)
        if self._style & NC_ROTATE:
            angle = (self._style & NC_LEFT) and 90.0 or 270.0
            dc.DrawRotatedText(self.GetPageText(tabIndex), xtextpos, ytextpos, angle)
        else:                
            dc.DrawText(self.GetPageText(tabIndex), xtextpos, ytextpos)
        
    def _DrawX(self, dc, tabrect, xrect, textColour):
        drawx, dxstyle = self.GetDrawX()
        if drawx:
            if dxstyle == 1:
                mins = min(self._padding.x, self._padding.y) + 1
                mins = min(mins, 6)                        
                dc.SetPen(wx.Pen(textColour, 1))
                dc.SetBrush(wx.TRANSPARENT_BRUSH)
                dc.DrawLine(xrect.x, xrect.y, tabrect.x+tabrect.width-2, tabrect.y+3+mins)
                dc.DrawLine(xrect.x, xrect.y+mins, tabrect.x+tabrect.width-2, tabrect.y+1)
                dc.DrawRectangle(xrect.x, xrect.y, xrect.width, xrect.height)
            elif dxstyle == 2:
                dc.SetPen(wx.Pen(textColour))
                dc.SetBrush(wx.Brush(textColour))
                dc.DrawRoundedRectangle(xrect.x, xrect.y, xrect.width, xrect.height, 2)
                dc.SetPen(wx.Pen(self.GetBackgroundColour(), 2))
                dc.DrawLine(xrect.x+2, xrect.y+2, xrect.x+xrect.width-3, xrect.y+xrect.height-3)
                dc.DrawLine(xrect.x+2, xrect.y+xrect.height-3, xrect.x+xrect.width-3, xrect.y+2)
            else:
                self._imglist2.Draw(0, dc, xrect.x, xrect.y, wx.IMAGELIST_DRAW_TRANSPARENT, True)
                
    def _EnhanceMultiSelectedTab(self, dc, tabIndex, tabrect):
        dc.SetPen(wx.Pen(self._GetTabTextColour(tabIndex), 1, wx.DOT_DASH))
        dc.SetBrush(wx.TRANSPARENT_BRUSH)
        dc.DrawRoundedRectangle(tabrect.x+self._padding.x/2+1, 
            tabrect.y+self._padding.y/2+1,
            tabrect.width-self._padding.x-2,
            tabrect.height-self._padding.y-2-2, 2)
        
    def _EnhanceSelectedTab(self, dc, paintTools, tabrect):
        xselpos = tabrect.x
        xselsize = tabrect.width
        yselsize = tabrect.height

        if self._style & NC_BOTTOM:
            yselpos = (tabrect.y + tabrect.height) - 2
        elif self._style & NC_TOP:
            yselpos = tabrect.y

        self._HighlightSelectedTabEdge(dc, paintTools, tabrect)
        self._ShadowTabEdge(dc, paintTools, tabrect)
        self._DrawFocusIndicator(dc, paintTools, tabrect)
                                    
    def _FillTab(self, dc, paintTools, tabrect, tabIndex):
        if self._usegradients:
            self.DrawGradientOnTab(dc, tabrect, self._pages[tabIndex]._firstcolour,
                                    self._pages[tabIndex]._secondcolour)
        elif not self._tabstyle._normal:
            self.DrawBuiltinStyle(dc, self._tabstyle, tabrect, tabIndex, 
                self.GetSelection())
            
    def _GetPaintTools(self):
        back_colour = self.GetBackgroundColour()
        back_brush = wx.Brush(back_colour)
        back_pen = wx.Pen(back_colour)

        border_pen = self._borderpen
        highlightpen = self._highlightpen
        if self._tabstyle._normal and not self._usegradients:
            highlightpen = self._highlightpen2
        
        shadowpen = self._shadowpen
        upperhighpen = self._upperhigh

        if self.GetHighlightSelection():
            selectionpen = wx.Pen(self._selectioncolour)
            selectionEdgePen = wx.Pen(self._selectionedgecolour)
        else:
            selectionpen = selectionEdgePen = None
            
        x_pen = self.GetDrawX() == 1 and wx.BLACK_PEN or None
        focusindpen = self.GetUseFocusIndicator() and self._focusindpen or None
        
        return _TabCtrlPaintTools(back_brush, back_pen, border_pen, highlightpen, 
            shadowpen, upperhighpen, selectionpen, selectionEdgePen, x_pen, 
            focusindpen)            
            
    def _GetTabBitmap(self, tabIndex):
        bmp = wx.NullBitmap
        if self.GetPageImage(tabIndex) >= 0:
            bmpindex = self.GetPageImage(tabIndex)
            if self.IsPageEnabled(tabIndex):
                bmp = self._imglist.GetBitmap(bmpindex)
            else:
                bmp = self._grayedlist.GetBitmap(bmpindex)
        return bmp     
        
    def _GetTabTextColour(self, tabIndex):
        if self.IsPageEnabled(tabIndex):
            result = self.GetPageTextColour(tabIndex)
        else:
            result = self._disabledcolour          
        return result
    
    def _GetThemePageColour(self, index):
        if self._tabstyle._macstyle:
            return NC_MAC_LIGHT
        elif self._tabstyle._kdetheme:
            return kdetheme[0]
        elif self._tabstyle._aqua:
            if index == self.GetSelection():
                return topaqua1[0]
            else:
                return distaqua[0]
        elif self._tabstyle._metal:
            intens = (230 + 80 * (self._tabrect[0].y-self._tabrect[0].y+1)/self._tabrect[0].height)
            return wx.Colour(intens, intens, intens)
        elif self._tabstyle._silver:
            if index == self.GetSelection():
                return silvertheme1[0]
            else:
                return silvertheme2[0]
        elif self._tabstyle._gradient:
            color = wx.WHITE
            if self._tabstyle._gradient & ThemeStyle.GRADIENT_VERTICAL:
                if  self._style & NC_TOP:
                    color = self._tabstyle.GetSecondGradientColour(index)
                elif self._style & NC_BOTTOM:
                    color = self._tabstyle.GetFirstGradientColour(index)
            elif self._tabstyle._gradient & ThemeStyle.GRADIENT_HORIZONTAL and \
                self._style & NC_ROTATE:
                    if self._style & NC_LEFT:
                        color = self._tabstyle.GetSecondGradientColour(index)
                    else:
                        color = self._tabstyle.GetFirstGradientColour(index)                    
            return color

    def _HighlightSelectedTabEdge(self, dc, paintTools, tabrect):
        if self._style & NC_ROTATE:
            yselpos = tabrect.y + 3
            yselsize = tabrect.height - 6
            xselpos = self._style & NC_RIGHT and tabrect.x + tabrect.width - 1 or tabrect.x
            if self.GetHighlightSelection():
                dc.SetBrush(paintTools.BackBrush)
                dc.SetPen(paintTools.SelectionEdgePen)
                dc.DrawLine(xselpos, yselpos, xselpos, yselpos + yselsize)
                dc.SetPen(paintTools.SelectionPen)
                for band in range(2):
                    if self._style & NC_RIGHT:
                        xselpos -= 1
                    else:
                        xselpos += 1 
                    yselpos -= 1
                    yselsize += 2                
                    dc.DrawLine(xselpos, yselpos, xselpos, yselpos + yselsize)
            else:
                dc.SetPen(paintTools.HighlightPen)                    
                dc.DrawLine(xselpos, yselpos, xselpos, yselpos + yselsize)
        else:
            xselpos = tabrect.x + 3
            xselsize = tabrect.width - 6

            if self._style & NC_BOTTOM:
                yselpos = tabrect.y + tabrect.height - 1
            else:
                yselpos = tabrect.y
                dc.SetPen(paintTools.HighlightPen)                    
                dc.DrawLine(xselpos, yselpos, xselpos + xselsize, yselpos)
            
            if self.GetHighlightSelection():
                dc.SetBrush(paintTools.BackBrush)
                dc.SetPen(paintTools.SelectionEdgePen)
                dc.DrawLine(xselpos, yselpos, xselpos + xselsize, yselpos)
                dc.SetPen(paintTools.SelectionPen)
                for band in range(2):
                    if self._style & NC_BOTTOM:
                        yselpos -= 1
                    else:
                        yselpos += 1
                    xselpos -= 1
                    xselsize += 2 
                    dc.DrawLine(xselpos, yselpos, xselpos + xselsize, yselpos)
        
    def _HighlightTabEdge(self, dc, paintTools, tabrect):
        if not self._tabstyle._normal or self._usegradients:
            if self._style & NC_TOP:
                dc.SetPen(paintTools.UpperHighlightPen)
                dc.DrawLine(tabrect.x+2, tabrect.y-1, tabrect.x + tabrect.width - 2, tabrect.y-1)
        else:
            if self._style & NC_TOP:
                dc.SetPen(paintTools.HighlightPen)
                dc.DrawLine(tabrect.x + 3, tabrect.y, tabrect.x + tabrect.width - 3, tabrect.y)
            
    def _InitExpandableStyles(self, style):
        self._expanded = not style & NC_ROTATE
        if self._expanded:
            self._expandedstyle = style        
            self._contractedstyle = style | NC_ROTATE
        else:
            self._contractedstyle = style
            self._expandedstyle = (style ^ NC_ROTATE) | NC_FIXED_WIDTH
            
    def _InitExpandableTabStyles(self, style, expanded, tabstyle):
        if tabstyle._gradient:
            alternatestyle = ThemeStyle()
            firstcolor = tabstyle.GetFirstGradientColour()
            secondcolor = tabstyle.GetSecondGradientColour()
            swapcolors = (tabstyle._gradient & ThemeStyle.GRADIENT_VERTICAL and style & NC_RIGHT and expanded) or \
                    (tabstyle._gradient & ThemeStyle.GRADIENT_HORIZONTAL and style & NC_RIGHT and not expanded)
            if swapcolors:
                firstcolor, secondcolor = (secondcolor, firstcolor)
            if tabstyle._gradient & ThemeStyle.GRADIENT_VERTICAL:
                othergradient = (tabstyle._gradient ^ ThemeStyle.GRADIENT_VERTICAL) | ThemeStyle.GRADIENT_HORIZONTAL
            else:
                othergradient = (tabstyle._gradient ^ ThemeStyle.GRADIENT_HORIZONTAL) | ThemeStyle.GRADIENT_VERTICAL
            alternatestyle.EnableGradientStyle(True, othergradient)
            alternatestyle.SetFirstGradientColour(firstcolor)
            alternatestyle.SetSecondGradientColour(secondcolor)
            if tabstyle._gradient & ThemeStyle.DIFFERENT_GRADIENT_FOR_SELECTED:
                firstcolor = tabstyle.GetFirstGradientColour(True)
                secondcolor = tabstyle.GetSecondGradientColour(True)
                if swapcolors:
                    firstcolor, secondcolor = (secondcolor, firstcolor)
                alternatestyle.SetFirstGradientColourSelected(firstcolor)
                alternatestyle.SetSecondGradientColourSelected(secondcolor)
            if expanded:
                self._expandedtabstyle = tabstyle
                self._contractedtabstyle = alternatestyle
            else:
                self._contractedtabstyle = tabstyle
                self._expandedtabstyle = alternatestyle
        else:
            self._expandedtabstyle = tabstyle
            self._contractedtabstyle = tabstyle
    
    def _OnStyleChange(self):
        if self._style & NC_TOP or self._style & NC_BOTTOM:
            self.SetBestSize((-1, newheight))
        else:
            self.SetBestSize((self._CalcBestWidth(wx.ClientDC(self)), -1))
        self._parent.GetSizer().Layout()
        self._somethingchanged = True
        self._firsttime = True
        self.Refresh()        
        
    def _ShadowTabEdge(self, dc, paintTools, tabrect):
        dc.SetPen(paintTools.ShadowPen)
        if self._style & NC_BOTTOM:
            dc.DrawLine((tabrect.x + tabrect.width), tabrect.y+1, 
                (tabrect.x+tabrect.width), tabrect.y + tabrect.height-4)
        elif self._style & NC_TOP:
            dc.DrawLine(tabrect.x + tabrect.width, tabrect.y+3, 
                tabrect.x+tabrect.width, tabrect.y+tabrect.height-4) 
    
    def OnPaint(self, event):
        """ Handles The wx.EVT_PAINT Event For TabCtrl. """
        
        dc = wx.BufferedPaintDC(self)

        if self.GetPageCount() == 0:
            event.Skip()
            return
        
        pt = self._GetPaintTools()
        
        dc.BeginDrawing() 

        self._DrawBackground(dc, pt)

        self._CalcMaxTextHeight(dc)
        
        posx = self._firsttabpos.x
        posy = self._firsttabpos.y
        
        if self._style & NC_LEFT:
            _ = 1
                    
        if self._firsttime:
            if not hasattr(self, "_initrect"):
                self._initrect = []
            if self.HasSpinButton() and self._fromdnd:
                self._firstvisible = self._spinbutton.GetValue()
                self._firsttime = False
                self._fromdnd = False
            else:
                self._initrect = []
                self._firstvisible = 0
        else:
            if self.HasSpinButton():
                self._firstvisible = self._spinbutton.GetValue()
            else:
                self._firstvisible = 0

        lastvisible = self.GetPageCount()

        #and tabs
        oncount = -1
        
        self._tabvisible = [1]*self.GetPageCount()
        
        tabrect = []
        # some theme style rendering routines expect this to exist, so
        # set it now:
        self._tabrect = tabrect
        Xrect = []
        
        for ii in xrange(self._firstvisible, lastvisible):
            if not self._enablehiding or not self._pages[ii]._ishidden:

                oncount = oncount + 1

                self._tabvisible[ii] = 1
                
                newwidth = self._CalcTabTextWidth(dc, ii)
                
                bmpWidth, bmpHeight = self._CalcTabBitmapSize(ii)

                tabrect.append(self._CalcTabRect(ii, posx, posy, 
                    newwidth, bmpWidth, bmpHeight))
                
                self._DrawTab(dc, pt, tabrect[-1], ii)

                self._DrawTabText(dc, ii, *self._CalcTabTextPosition(ii, 
                    tabrect[-1], self._CalcTabBitmapSpace(bmpWidth, bmpHeight)))

                if bmpWidth:
                    self._DrawTabBitmap(dc, ii, *self._CalcTabBitmapPosition(ii, 
                        bmpWidth, bmpHeight, tabrect[-1]))    

                if self.GetSelection() in [ii, ii - 1]:
                    # Handle this special case on the selected tab and
                    # on the tab that follows it (if there is one), to ensure
                    # proper rendering of the selected tab's right edge 
                    self._EnhanceSelectedTab(dc, pt, tabrect[self.GetSelection() - self._firstvisible])
                        
                if self.GetDrawX()[0]:
                    Xrect.append(self._CalcXRect(tabrect[-1]))
                    self._DrawX(dc, tabrect[-1], Xrect[-1],  
                        self._GetTabTextColour(ii))

                if ii in self._selectedtabs:
                    self._EnhanceMultiSelectedTab(dc, ii, tabrect[-1])
                    
                if self._style & NC_TOP or self._style & NC_BOTTOM:
                    # horizontally positioned tabs along top or bottom
                    posx = posx + tabrect[-1].width
                else:
                    # vertically stacked tabs along side
                    posy = posy + tabrect[-1].height

                if self._firsttime:
                    self._initrect.append(tabrect[oncount])

            else:

                self._tabvisible[ii] = 0

        self._xrect = Xrect

        if self._firsttime:
            self._firsttime = False

        self.UpdateMenuButton(self.HasMenuButton())            
        self.UpdateSpinButton()

        if self._enabledragging:
            if self._isdragging and not self._isleaving:
                self.DrawInsertionMark(dc, self._olddragpos)
                
        dc.EndDrawing()
      

# ---------------------------------------------------------------------------- #
# Class NotebookCtrl
# This Is The Main Class Implementation
# ---------------------------------------------------------------------------- #

class NotebookCtrl(wx.Panel):
    """
    Display one or more windows in a notebook.
    
    B{Events}:
        - B{EVT_NOTEBOOKCTRL_PAGE_CHANGING}: sent when the active 
            page in the notebook is changing
        - B{EVT_NOTEBOOKCTRL_PAGE_CHANGED}: sent when the active 
            page in the notebook has changed
        - B{EVT_NOTEBOOKCTRL_PAGE_CLOSING}: sent when a page in the 
            notebook is closing
        - B{EVT_NOTEBOOKCTRL_PAGE_DND}: sent when a page has been 
            dropped onto the notebook in a drag-drop operation
        - B{EVT_NOTEBOOKCTRL_PAGE_DCLICK}: sent when the user 
            double-clicks a tab in the notebook
        - B{EVT_NOTEBOOKCTRL_PAGE_RIGHT}: sent when the user
            clicks a tab in the notebook with the right mouse
            button
        - B{EVT_NOTEBOOKCTRL_PAGE_MIDDLE}: sent when the user
            clicks a tab in the notebook with the middle mouse
            button
    """

    def __init__(self, parent, id, pos=wx.DefaultPosition, size=wx.DefaultSize,
                 style=NC_DEFAULT_STYLE, sizer=wx.HORIZONTAL, margin=2, name="NotebookCtrl"):
        """
        Default Class Constructor.
        
        @param style: Style For The NotebookCtrl, Which May Be:
          a) NC_TOP: NotebookCtrl Placed On Top (Default);
          b) NC_BOTTOM: NotebookCtrl Placed At The Bottom;
          c) NC_LEFT: NotebookCtrl Placed At The Left;
          d) NC_RIGHT: NotebookCtrl Placed At The Right;
          e) NC_FIXED_WIDTH: All Tabs Have The Same Width;
          f) wx.NO_BORDER: Shows No Border For The Control (Default, Looks Better);
          g) wx.STATIC_BORDER: Shows A Static Border On The Control.
          
        @param sizer: The Sizer Orientation For The Sizer That Holds
          All The Panels: Changing This Style Is Only Useful When You
          Use The Tile Method. In This Case, If sizer=wx.HORIZONTAL,
          All The Panels Will Be Shown In Columns, While If
          sizer=wx.VERTICAL All The Panels Will Be Shown In Rows.

        @param margin: An Integer Number Of Pixels That Add Space
          Above TabCtrl If style=NC_TOP, Or Below It If
          style=NC_BOTTOM
        """
        
        wx.Panel.__init__(self, parent, -1, style=wx.NO_FULL_REPAINT_ON_RESIZE |
                          wx.CLIP_CHILDREN, name=name)
        
        self.nb = TabCtrl(self, -1, pos, size, style)

        self._notebookpages = []

        if style & NC_TOP == 0 and style & NC_BOTTOM == 0 \
            and style & NC_LEFT == 0 and style & NC_RIGHT == 0:
            style = style | NC_TOP
            
        if style & wx.NO_BORDER == 0 and \
           style & wx.STATIC_BORDER == 0:
            style = style | wx.NO_BORDER
        
        self._style = style
        self._showcolumns = False
        self._showtabs = True
        self._sizerstyle = sizer
        self._custompanel = None
        self._focusswitch = False
        self._oldfocus = None
        
        if style & NC_TOP or style & NC_BOTTOM:
            self.sizer = wx.BoxSizer(wx.VERTICAL)
            self.tabsizer = wx.BoxSizer(wx.VERTICAL)
        else:
            self.sizer = wx.BoxSizer(wx.HORIZONTAL)
            self.tabsizer = wx.BoxSizer(wx.HORIZONTAL)
            
        self.bsizer = wx.BoxSizer(sizer)

        if style & NC_TOP or style & NC_BOTTOM:
            tabBorderFlag = wx.LEFT | wx.RIGHT
        else:
            tabBorderFlag = wx.TOP | wx.BOTTOM
        
        if style & NC_TOP or style & NC_LEFT:
            self.sizer.Add(self.tabsizer, 0, wx.EXPAND | tabBorderFlag, 2)
            self._AddMargin(style, margin)
            self.tabsizer.Add(self.nb, 0, wx.EXPAND)
            self.sizer.Add(self.bsizer, 1, wx.EXPAND)
        elif style & NC_BOTTOM or style & NC_RIGHT:
            self.sizer.Add(self.bsizer, 1, wx.EXPAND)
            self.sizer.Add(self.tabsizer, 0, wx.EXPAND | tabBorderFlag, 2)
            self.tabsizer.Add(self.nb, 0, wx.EXPAND)
            self._AddMargin(style, margin)
            
        self.SetSizer(self.sizer)

        self.tabsizer.Show(self.nb, False)
        
        self.sizer.Layout()  
        self.Bind(wx.EVT_MOTION, self.OnMouseMotion)
        self.Bind(wx.EVT_KEY_DOWN, self.OnKeyDown)


    def OnKeyDown(self, event):
        """
        Handles The wx.EVT_KEY_DOWN Event For NotebookCtrl. This Is Only Processed
        If The User Navigate Through Tabs With Ctrl-Tab Keyboard Navigation.
        """
        
        if event.GetKeyCode() == wx.WXK_TAB:
            if event.ControlDown():
                sel = self.GetSelection()
                if sel == self.GetPageCount() - 1:
                    sel = 0
                else:
                    sel = sel + 1

                while not self.IsPageEnabled(sel):
                    sel = sel + 1
                    if sel == self.GetPageCount() - 1:
                        sel = 0
                        
                self.SetSelection(sel)

        event.Skip()
        

    def OnMouseMotion(self, event):
        """ Handles The wx.EVT_MOTION Event For NotebookCtrl. """

        if self.nb._enabledragging:
            
            if event.Dragging() and not event.RightIsDown() and not event.MiddleIsDown():
                
                tolerance = 2
                pt = event.GetPosition()
                dx = abs(pt.x - self.nb._dragstartpos.x)
                dy = abs(pt.y - self.nb._dragstartpos.y)
                if dx <= tolerance and dy <= tolerance:
                    self.SetCursor(wx.STANDARD_CURSOR)
                    return

                self.SetCursor(self.nb._dragcursor)
                self.nb._isdragging = True

            else:

                self.nb._isdragging = False
                self.SetCursor(wx.STANDARD_CURSOR)

        if self.nb._showtooltip:
            if self.nb._istooltipshown:
                pt = event.GetPosition()
                self.nb._insidetab = self.nb.GetInsideTab(pt)
                if  self.nb._insidetab < 0:
                    try:
                        self.nb._tipwindow.Destroy()
                        self.nb._istooltipshown = False
                    except:
                        self.nb._istooltipshown = False
                        
                    self.nb.Refresh()
                    
        event.Skip()
                    

    def EnableChildFocus(self, enable=True):
        """ Enables/Disables Sending EVT_NOTEBOOKCTRL_PAGE_CHANGING When In Tile Mode. """
        
        self._focusswitch = enable
        

    def FindFocusedPage(self, obj):
        """ Find Which NotebookCtrl Page Has The Focus Based On Its Child Focus. """
        
        while 1:
            if obj in self._notebookpages:
                return obj

            try:
                obj = obj.GetParent()
            except:
                return None

        return None
    
    
    def OnFocus(self, event):
        """ Handles The wx.EVT_CHILD_FOCUS Event For NotebookCtrl. """

        if not self._focusswitch:
            event.Skip()
            return

        newfocus = self.FindFocusedPage(event.GetEventObject())

        if newfocus == self._oldfocus or newfocus is None:
            event.Skip()
            return

        self._oldfocus = newfocus
                
        eventOut = NotebookCtrlEvent(wxEVT_NOTEBOOKCTRL_PAGE_CHANGING, self.GetId())
        
        nPage = self._notebookpages.index(newfocus)
        eventOut.SetSelection(nPage)
        eventOut.SetOldSelection(self.GetSelection())
        eventOut.SetEventObject(self)
    
        if not self.GetEventHandler().ProcessEvent(eventOut):

            # Program Allows The Page Change 
            self.nb._selection = nPage
            eventOut.SetEventType(wxEVT_NOTEBOOKCTRL_PAGE_CHANGED) 
            eventOut.SetOldSelection(self.nb._selection) 
            self.GetEventHandler().ProcessEvent(eventOut)        
        
        event.Skip()

        
    def AddPage(self, page, text, select=False, img=-1, hidden=False):
        """
        Add A Page To The Notebook.
        
        @param page: Specifies The New Page;
        @param text: The Tab Text;
        @param select: Whether The Page Should Be Selected Or Not;
        @param img: Specifies The Optional Image Index For The New Page.
        """
        
        self.Freeze()
        
        oldselection = self.nb.GetSelection()

        if self.GetPageCount() == 0:
            if self.GetCustomPage() is not None:
                self.bsizer.Detach(self._custompanel)
                self._custompanel.Show(False)
                self.bsizer.Layout()
        
        self.bsizer.Add(page, 1, wx.EXPAND | wx.ALL, 2)
                    
        self.nb.AddPage(text, select, img, hidden)
        self._notebookpages.append(page)
        
        page.Bind(wx.EVT_CHILD_FOCUS, self.OnFocus)

        if select:
            if oldselection >= 0:
               self.bsizer.Show(self.GetPage(oldselection), False)
               
            self.nb.SetSelection(self.GetPageCount()-1)
            self.bsizer.Layout()
        else:
            if oldselection >= 0:
                self.bsizer.Show(page, False)
            else:
                self.bsizer.Show(page, True)
                self.nb.SetSelection(self.GetPageCount()-1)
                self.bsizer.Layout()

        if self.GetPageCount() == 1:

            self.bsizer.Show(page, True)
            
            if self.nb._hideonsingletab:
                
                self._ShowTabCtrl(False)

            else:
                self.nb.Show(True)
                self._ShowTabCtrl(True)

        else:
            
            self.nb.Show(True)
            self._ShowTabCtrl(True)

        self.bsizer.Layout()                    
        self.sizer.Layout()
            
        self.Thaw()

        self.Tile(self._showcolumns)
        self.ShowTabs(self._showtabs)
        

    def InsertPage(self, nPage, page, text, select=False, img=-1, hidden=False):
        """
        Insert A Page Into The Notebook.
        
        @param page: Specifies The New Page;
        @param nPage: Specifies The Position For The New Page;
        @param text: The Tab Text;
        @param select: Whether The Page Should Be Selected Or Not;
        @param img: Specifies The Optional Image Index For The New Page.
        @param hidden: C{True} to hide the page; C{False} to display it
        """
        
        if nPage < 0 or (self.GetSelection() >= 0 and nPage >= self.GetPageCount()):
            raise "\nERROR: Invalid Notebook Page In InsertPage: (" + str(nPage) + ")"

        self.Freeze()
        
        oldselection = self.nb.GetSelection()

        if self.GetPageCount() == 0:
            if self.GetCustomPage() is not None:
                self.bsizer.Detach(self._custompanel)
                self._custompanel.Show(False)
                
        if oldselection >= 0:
            self.bsizer.Show(oldselection, False)
            self.bsizer.Layout()
        
        if oldselection >= nPage:
            oldselection = oldselection + 1
        
        self.nb.InsertPage(nPage, text, select, img, hidden)
        self.bsizer.Insert(nPage, page, 1, wx.EXPAND | wx.ALL, 2)
        self._notebookpages.insert(nPage, page)
        self.bsizer.Layout()

        page.Bind(wx.EVT_CHILD_FOCUS, self.OnFocus)        

        for ii in xrange(self.GetPageCount()):
            self.bsizer.Show(ii, False)

        self.bsizer.Layout()
        
        if select:
            self.bsizer.Show(nPage, True)
            self.bsizer.Layout()
        else:
            if oldselection >= 0:
                self.bsizer.Show(oldselection, True)
                self.bsizer.Layout()
            else:
                self.bsizer.Show(nPage, True)

        self.bsizer.Layout()

        if self.GetPageCount() == 1:

            if self.nb._hideonsingletab:
                
                self._ShowTabCtrl(False)

            else:
                
                self.nb.Show(True)
                self._ShowTabCtrl(True)

        else:
            
            self.nb.Show(True)
            self._ShowTabCtrl(True)
                    
        self.sizer.Layout()
                        
        self.Thaw()

        self.Tile(self._showcolumns)
        self.ShowTabs(self._showtabs)

        
    def GetPage(self, nPage):
        """ Returns The Window At The Given Position nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPage: (" + str(nPage) + ")"

        return self._notebookpages[nPage]
    

    def DeleteAllPages(self):
        """ Deletes All NotebookCtrl Pages. """

        self.Freeze()

        counter = self.GetPageCount() - 1
        
        for ii in xrange(self.GetPageCount()):
            self.bsizer.Detach(counter-ii)
            panels = self.GetPage(counter-ii)
            panels.Destroy()

        self.nb.DeleteAllPages()
        self._notebookpages = []
        self.nb._selection = -1

        self.nb.Show(False)

        custom = self.GetCustomPage()
            
        if custom is not None:
            self.SetCustomPage(custom)
            custom.Show(True)
            
        self.bsizer.Layout()
        
        self._ShowTabCtrl(False)

        self.sizer.Layout()

        self.Thaw()
         

    def DeletePage(self, nPage):
        """ Deletes The Page nPage, And The Associated Window. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In DeletePage: (" + str(nPage) + ")"

        oldselection = self.GetSelection()
        
        self.Freeze()
        
        panel = self.GetPage(nPage)
        self.bsizer.Detach(nPage)
        
        self.bsizer.Layout()
        
        self._notebookpages.pop(nPage)
        self.nb.DeletePage(nPage)

        panel.Destroy()
            
        if self.GetPageCount() > 0:
            if oldselection == nPage:
                if self.GetSelection() > 0:
                    self.SetSelection(self.GetSelection())
                else:
                    self.SetSelection(self.GetSelection())
                    self.bsizer.Show(self.GetSelection())
                    self.bsizer.Layout()
                
        if self.GetPageCount() == 0:
            self.nb.Show(False)
            self._ShowTabCtrl(False)

            custom = self.GetCustomPage()
            
            if custom is not None:
                self.bsizer.Add(custom, 1, wx.EXPAND | wx.ALL, 2)
                custom.Show(True)
                
            self.bsizer.Layout()
            self.sizer.Layout()
            self.Thaw()
            return

        if self.GetPageCount() == 1:
            
            if self.nb._hideonsingletab:
                
                self._ShowTabCtrl(False)

            else:
                
                self.nb.Show(True)
                self._ShowTabCtrl(True)

        else:
            
            self.nb.Show(True)
            self._ShowTabCtrl(True)
                    
        self.sizer.Layout()

        self.Thaw()

        self.Tile(self._showcolumns)
        self.ShowTabs(self._showtabs)


    def SetSelection(self, nPage):
        """
        Sets The Current Tab Selection To The Given nPage. This Call Generates The
        EVT_NOTEBOOKCTRL_PAGE_CHANGING Event.
        """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetSelection: (" + str(nPage) + ")"
        
        oldselection = self.GetSelection()
        
        if oldselection == nPage:
            return

        self.nb.SetSelection(nPage)

        self.Tile(self._showcolumns)
        self.ShowTabs(self._showtabs)
       

    def GetPageCount(self):
        """ Returns The Number Of Pages In NotebookCtrl. """

        return self.nb.GetPageCount()


    def GetSelection(self):
        """ Returns The Current Selection. """
        
        return self.nb.GetSelection()


    def GetImageList(self):
        """ Returns The Image List Associated With The NotebookCtrl. """
        
        return self.nb.GetImageList()


    def SetImageList(self, imagelist):
        """ Associate An Image List To NotebookCtrl. """

        self.nb.SetImageList(imagelist)


    def AssignImageList(self, imagelist):
        """ Associate An Image List To NotebookCtrl. """

        self.nb.AssignImageList(imagelist)
        

    def GetPadding(self):
        """ Returns The (Horizontal, Vertical) Padding Of The Text Inside Tabs. """

        return self.nb.GetPadding()


    def SetPadding(self, padding):
        """ Sets The (Horizontal, Vertical) Padding Of The Text Inside Tabs. """
        
        self.nb.SetPadding(padding)


    def SetUseFocusIndicator(self, focus=True):
        """ Globally Enables/Disables Tab Focus Indicator. """

        self.nb.SetUseFocusIndicator(focus)        


    def GetUseFocusIndicator(self):
        """ Returns Globally Enable/Disable State For Tab Focus Indicator. """

        return self.nb.GetUseFocusIndicator()
    

    def EnablePage(self, nPage, enable=True):
        """ Enable/Disable The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In EnablePage: (" + str(nPage) + ")"
        
        self.nb.EnablePage(nPage, enable)


    def IsPageEnabled(self, nPage):
        """ Returns Whether A Page Is Enabled Or Not. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In IsPageEnabled: (" + str(nPage) + ")"        

        return self.nb.IsPageEnabled(nPage)
    

    def SetHighlightSelection(self, highlight=True):
        """ Globally Enables/Disables Tab Highlighting On Tab Selection. """

        self.nb.SetHighlightSelection(highlight)

        
    def GetHighlightSelection(self):
        """ Returns Globally Enable/Disable State For Tab Highlighting On Tab Selection. """

        return self.nb.GetHighlightSelection()
    

    def SetAnimationImages(self, nPage, imgarray):
        """
        Sets An Animation List Associated To The Given Page nPage.
        
        @param nPage: The Given Page;
        @param imgarray: A List Of Image Indexes Of Images Inside The
          ImageList Associated To NotebookCtrl.
        """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetAnimationImages: (" + str(nPage) + ")"

        if not imgarray:
            raise "\nERROR: Invalid Image Array In SetAnimationImages: (" + repr(imgarray) + ")"

        if min(imgarray) < 0:
            raise "\nERROR: Invalid Image Array In SetAnimationImages: (Min(ImgArray) = " + \
                  str(min(imgarray)) + " < 0)"

        if max(imgarray) > self.GetImageList().GetImageCount() - 1:
            raise "\nERROR: Invalid Image Array In SetAnimationImages: (Max(ImgArray) = " + \
                  str(max(imgarray)) + " > " + str(self.GetImageList().GetImageCount()-1) + ")"
        
        self.nb.SetAnimationImages(nPage, imgarray)
        

    def GetAnimationImages(self, nPage):
        """ Returns The Animation Images List Associated To The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetAnimationImages: (" + str(nPage) + ")"

        return self.nb.GetAnimationImages(nPage)
    

    def StartAnimation(self, nPage, timer=500):
        """ Starts The Animation On The Given Page nPage, With Refreshing Time Rate "timer". """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In StartAnimation: (" + str(nPage) + ")"
        
        self.nb.StartAnimation(nPage, timer)


    def StopAnimation(self, nPage):
        """ Stops The Animation On The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In StopAnimation: (" + str(nPage) + ")"

        self.nb.StopAnimation(nPage)


    def EnableDragAndDrop(self, enable=True):
        """ Globall Enables/Disables Tabs Drag And Drop. """

        self.nb.EnableDragAndDrop(enable)


    def EnableHiding(self, enable=True):
        """ Globally Enables/Disables Hiding On Tabs In Runtime. """

        self.nb.EnableHiding(enable)
        

    def SetDrawX(self, drawx=True, style=1, image1=None, image2=None):
        """
        Globally Enables/Disables The Drawing Of A Closing "X" In The Tab.
        
        @param drawx: C{True} to enable drawing a closing "X"; C{False} to
          disable it
        @param style: the style of the X to draw when C{drawx} is C{True};
          possible values are:
            - C{1}: Small "X" At The Top-Right Of The Tab;
            - C{2}: Bigger "X" In The Middle Vertical Of The Tab (Like Opera Notebook);
            - C{3}: Custom "X" Image Is Drawn On Tabs.
        @param image1: if C{style} is C{3}, the image to use when drawing
          the X on an unhighlighted tab
        @param image2: if C{style} is C{3}, the image to use when drawing
          the X on a highlighted tab
        """

        self.nb.SetDrawX(drawx, style, image1, image2)


    def GetDrawX(self):
        """
        Returns The Enable/Disable State Of Drawing Of A Small "X" At The Top-Right Of
        Every Page.
        """

        return self.nb.GetDrawX()
    

    def SetImageToCloseButton(self, convert=True):
        """ Set Whether The Tab Icon Should Be Converted To The Close Button Or Not. """
        
        self.nb.SetImageToCloseButton(convert)

        
    def GetImageToCloseButton(self):
        """ Get Whether The Tab Icon Should Be Converted To The Close Button Or Not. """
        
        return self.nb._convertimage


    def HideOnSingleTab(self, hide=True):
        """ Hides The TabCtrl When There Is Only One Tab In NotebookCtrl. """
        
        self.nb.HideOnSingleTab(hide)
        
        if self.GetPageCount() == 1:
            if hide:
                self._ShowTabCtrl(False)
            else:
                self._ShowTabCtrl(True)

            self.sizer.Layout()
            

    def SetPagePopupMenu(self, nPage, menu):
        """ Sets A Popup Menu Specific To A Single Tab. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPagePopupMenu: (" + str(nPage) + ")"
        
        self.nb.SetPagePopupMenu(nPage, menu)


    def GetPagePopupMenu(self, nPage):
        """ Returns The Popup Menu Associated To A Single Tab. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPagePopupMenu: (" + str(nPage) + ")"        
        
        return self.nb.GetPagePopupMenu(nPage)
    

    def SetPageToolTip(self, nPage, tooltip="", timer=500, winsize=400):
        """
        Sets A ToolTip For The Given Page nPage.
        
        @param nPage: The Given Page;
        @param tooltip: The ToolTip String;
        @param timer: The Timer After Which The Tip Window Is Popped Up;
        @param winsize: The Maximum Width Of The Tip Window.
        """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageToolTip: (" + str(nPage) + ")"
        
        self.nb.SetPageToolTip(nPage, tooltip, timer, winsize)


    def GetPageToolTip(self, nPage):
        """ Returns A Tuple With All Page ToolTip Parameters. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageToolTip: (" + str(nPage) + ")"
        
        return self.nb.GetPageToolTip(nPage)


    def EnableToolTip(self, show=True):
        """ Globally Enables/Disables Tab ToolTips. """

        self.nb.EnableToolTip(show)        


    def GetToolTipBackgroundColour(self):
        """ Returns The ToolTip Window Background Colour. """

        return self.nb.GetToolTipBackgroundColour()


    def SetToolTipBackgroundColour(self, colour=None):
        """ Sets The ToolTip Window Background Colour. """

        if colour is None:
            colour = wx.Colour(255, 255, 230)
            
        self.nb.SetToolTipBackgroundColour(colour)       


    def EnableTabGradients(self, enable=True):
        """ Globally Enables/Disables Drawing Of Gradient Coloured Tabs For Each Tab. """

        self.nb.EnableTabGradients(enable)
        

    def SetPageFirstGradientColour(self, nPage, colour=None):
        """ Sets The Single Tab First Gradient Colour. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageFirstGradientColour: (" + str(nPage) + ")"
        
        if colour is None:
            colour = wx.WHITE

        self.nb.SetPageFirstGradientColour(nPage, colour)
        

    def SetPageSecondGradientColour(self, nPage, colour=None):
        """ Sets The Single Tab Second Gradient Colour. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageSecondGradientColour: (" + str(nPage) + ")"
        
        self.nb.SetPageSecondGradientColour(nPage, colour)


    def GetPageFirstGradientColour(self, nPage):
        """ Returns The Single Tab First Gradient Colour. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageFirstGradientColour: (" + str(nPage) + ")"

        return self.nb.GetPageFirstGradientColour(nPage)


    def GetPageSecondGradientColour(self, nPage):
        """ Returns The Single Tab Second Gradient Colour. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageSecondGradientColour: (" + str(nPage) + ")"

        return self.nb.GetPageSecondGradientColour(nPage)
    

    def CancelTip(self):
        """ Destroys The Tip Window (Probably You Won't Need This One. """
        
        self.nb.CancelTip()        


    def AdvanceSelection(self, forward=True):
        """
        Cycles Through The Tabs. The Call To This Function Generates The
        EVT_NOTEBOOKCTRL_PAGE_CHANGING Event.
        """

        self.nb.AdvanceSelection(forward)

    
    def SetDefaultPage(self, defaultpage=-1):
        """
        Sets The Default Page That Will Be Selected When An Active And Selected
        Tab Is Made Inactive.
        """
        
        if defaultpage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetDefaultPage: (" + str(defaultpage) + ")"
        
        self.nb.SetDefaultPage(defaultpage)        


    def GetDefaultPage(self):
        """ Returns The Default Page. """
        
        return self.nb.GetDefaultPage()
        

    def GetPageText(self, nPage):
        """ Returns The String For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageText: (" + str(nPage) + ")"
        
        return self.nb.GetPageText(nPage)


    def SetPageText(self, nPage, text):
        """ Sets The String For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageText: (" + str(nPage) + ")"

        self.nb.SetPageText(nPage, text)
        
     
    def GetPageImage(self, nPage):
        """ Returns The Image Index For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageImage: (" + str(nPage) + ")"
        
        return self.nb.GetPageImage(nPage)
     

    def SetPageImage(self, nPage, img):
        """ Sets The Image Index For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageImage: (" + str(nPage) + ")"
        
        self.nb.SetPageImage(nPage, img) 
        

    def SetPageTextFont(self, nPage, font=None):
        """ Sets The Primary Font For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageTextFont: (" + str(nPage) + ")"
        
        if font is None:
            font = wx.SystemSettings_GetFont(wx.SYS_DEFAULT_GUI_FONT)

        self.nb.SetPageTextFont(nPage, font)
        
        
    def GetPageTextFont(self, nPage):
        """ Returns The Primary Font For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageTextFont: (" + str(nPage) + ")"
        
        return self.nb.GetPageTextFont(nPage)


    def SetPageTextSecondaryFont(self, nPage, font=None):
        """ Sets The Secondary Font For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageTextSecondaryFont: (" + str(nPage) + ")"
        
        if font is None:
            font = wx.SystemSettings_GetFont(wx.SYS_DEFAULT_GUI_FONT) 

        self.nb.SetPageTextSecondaryFont(nPage, font)

        
    def GetPageTextSecondaryFont(self, nPage):
        """ Returns The Secondary Font For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageTextSecondaryFont: (" + str(nPage) + ")"
        
        return self.nb.GetPageTextSecondaryFont(nPage)


    def SetPageTextColour(self, nPage, colour=None):
        """ Sets The Text Colour For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageTextColour: (" + str(nPage) + ")"
        
        if colour is None:
            colour = wx.BLACK

        self.nb.SetPageTextColour(nPage, colour)


    def GetPageTextColour(self, nPage):
        """ Returns The Text Colour For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageTextColour: (" + str(nPage) + ")"            

        return self.nb.GetPageTextColour(nPage)


    def SetPageColour(self, nPage, colour=None):
        """ Sets The Tab Background Colour For The Given Page nPage. """
        
        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In SetPageColour: (" + str(nPage) + ")"
        
        if colour is None:
            colour = wx.SystemSettings_GetColour(wx.SYS_COLOUR_BTNFACE)

        self.nb.SetPageColour(nPage, colour)


    def GetPageColour(self, nPage):
        """ Returns The Tab Background Colour For The Given Page nPage. """

        if nPage < 0 or nPage >= self.GetPageCount():
            raise "\nERROR: Invalid Notebook Page In GetPageColour: (" + str(nPage) + ")"            

        return self.nb.GetPageColour(nPage)


    def SetTabHeight(self, height=28):
        """ Sets The Tabs Height. """

        if height <= 0:
            raise "\nERROR: Impossible To Set An Height <= 0. "
        
        self.nb.SetTabHeight(height)


    def SetControlBackgroundColour(self, colour=None):
        """ Sets The TabCtrl Background Colour (Behind The Tabs). """

        if colour is None:
            colour = wx.SystemSettings_GetColour(wx.SYS_COLOUR_3DFACE)
            
        self.nb.SetBackgroundColour(colour)
        

    def ApplyTabTheme(self, theme=None):
        """ Apply A Particular Theme To Be Drawn On Tabs. """
        
        if theme is None:
            theme = ThemeStyle()

        self.nb.ApplyTabTheme(theme)
        
                
    def SetSelectionColour(self, colour=None):
        """ Sets The Tab Selection Colour (Thin Line Above The Selected Tab). """
        
        if colour is None:
            colour = wx.Colour(255, 180, 0)

        self.nb.SetSelectionColour(colour)


    def SetContourLineColour(self, colour=None):
        """ Sets The Contour Line Colour (Controur Line Around Tabs). """

        self.nb.SetContourLineColour(colour)
        
    def Tile(self, show=True, orient=None):
        """ Shows Pages In Column/Row Mode (One Panel After The Other In Columns/Rows). """
        
        if self._GetTabCtrlWindow().IsShown() == show and orient is None:
            return

        self.Freeze()

        if orient is not None and show:
            if orient == wx.VERTICAL:
                norient = wx.HORIZONTAL
            else:
                norient = wx.VERTICAL
                    
        if orient is not None and show:
            origorient = self.bsizer.GetOrientation()
            if origorient != norient:
                for ii in xrange(self.GetPageCount()-1, -1, -1):
                    self.bsizer.Detach(ii)

                self.sizer.Detach(self.bsizer)
                self.bsizer.Destroy()
                    
                self.bsizer = wx.BoxSizer(norient)
                
                for ii in xrange(self.GetPageCount()):
                    self.bsizer.Add(self._notebookpages[ii], 1, wx.EXPAND | wx.ALL, 2)

                if self._style & NC_TOP:
                    self.sizer.Add(self.bsizer, 1, wx.EXPAND)
                else:
                    self.sizer.Insert(0, self.bsizer, 1, wx.EXPAND)
        
                self.bsizer.Layout()
                self.sizer.Layout()
                
        selection = self.GetSelection()
        
        if show:
            self._ShowTabCtrl(False)
            if self._style & NC_TOP or self._style & NC_LEFT:
                if len(self.nb._selectedtabs) > 0:
                    for ii in xrange(self.GetPageCount()):
                        if ii in self.nb._selectedtabs:
                            self.bsizer.Show(ii, True)
                        else:
                            self.bsizer.Show(ii, False)
                else:
                    for ii in xrange(self.GetPageCount()):
                        if self.IsPageEnabled(ii):
                            if not self.nb._enablehiding or not self.nb._pages[ii]._ishidden:
                                self.bsizer.Show(ii, True)
                            else:
                                self.bsizer.Show(ii, False)
                        else:
                            self.bsizer.Show(ii, False)
            else:
                if len(self.nb._selectedtabs) > 0:
                    for ii in xrange(self.GetPageCount()):
                        if ii in self.nb._selectedtabs:
                            self.bsizer.Show(ii, True)
                else:
                    for ii in xrange(self.GetPageCount()):
                        if self.IsPageEnabled(ii):
                            if not self.nb._enablehiding or not self.nb._pages[ii]._ishidden:
                                self.bsizer.Show(ii, True)
                            else:
                                self.bsizer.Show(ii, False)
                        else:
                            self.bsizer.Show(ii, False)
        else:
            self._ShowTabCtrl(True)
            if self._style & NC_TOP or self._style & NC_LEFT:
                for ii in xrange(self.GetPageCount()):
                    self.bsizer.Show(ii, False)
            else:
                for ii in xrange(self.GetPageCount()):
                    self.bsizer.Show(ii, False)

            if selection < 0:
                self.bsizer.Layout()
                self.sizer.Layout()
                return
            else:
                self.bsizer.Show(selection, True)
                self.bsizer.Layout()

        self._showcolumns = show
        
        self.bsizer.Layout()
        self.sizer.Layout()

        self.Thaw()        

    

    def ShowTabs(self, show=True):
        """ Shows/Hides Tabs On Request. """
        
        if self._GetTabCtrlWindow().IsShown() == show:
            return

        if self.GetPageCount() == 0:
            return
        
        self.Freeze()
        
        self._ShowTabCtrl(show)

        self._showtabs = show
        
        self.sizer.Layout()
        
        self.Thaw()


    def GetIndex(self, page):
        """ Returns The Page Index (Position) Based On The NotebookCtrl Page Passed. """

        if page in self._notebookpages:
            return self._notebookpages.index(page)

        return -1
    

    def ReparentPage(self, nPage, newParent):
        """ Reparents The NotebookCtrl Page nPage To A New Parent. """
        
        if nPage < 0 or (self.GetSelection() >= 0 and nPage >= self.GetPageCount()):
            raise "\nERROR: Invalid Notebook Page In ReparentPage: (" + str(nPage) + ")"
        
        page = self.GetPage(nPage)
        page.Reparent(newParent)
        

    def ReparentToFrame(self, nPage, createNotebook=False):
        """ Reparents The NotebookCtrl Page nPage To A New Frame. """
        
        if nPage < 0 or (self.GetSelection() >= 0 and nPage >= self.GetPageCount()):
            raise "\nERROR: Invalid Notebook Page In ReparentToFrame: (" + str(nPage) + ")"
        
        self.Freeze()

        infos = self.GetPageInfo(nPage)
        panel = self.GetPage(nPage)
        text = infos["text"]
        oldparent = panel.GetParent()

        frame = NCFrame(None, -1, text, nb=self, infos=infos, panel=panel, oldparent=oldparent)
        
        if createNotebook:
            nb = NotebookCtrl(frame, -1, style=self._style, sizer=self._sizerstyle)
            nb.SetImageList(infos["imagelist"])
            self.ReparentToNotebook(nPage, nb)
        else:
            self.ReparentPage(nPage, frame)
            
            self.nb.DeletePage(nPage, False)

            self.bsizer.Detach(nPage)
            self.bsizer.Layout()
            self.sizer.Layout()

            self._notebookpages.pop(nPage)
            
            self.AdvanceSelection()

        if self.GetPageCount() == 0:
            self._ShowTabCtrl(False)

            self.sizer.Layout()

        custom = self.GetCustomPage()
        if custom is not None:
            self.SetCustomPage(custom)
            
        self.Thaw()

        frame.Show()

        
    def ReparentToNotebook(self, nPage, notebook, newPage=None):
        """ Reparents The NotebookCtrl Page nPage To A New NotebookCtrl. """
        
        if nPage < 0 or (self.GetSelection() >= 0 and nPage >= self.GetPageCount()):
            raise "\nERROR: Invalid Notebook Page In ReparentToNotebook: (" + str(nPage) + ")"

        if newPage is not None and newPage >= notebook.GetPageCount():
            raise "\nERROR: Invalid Notebook New Page In ReparentToNotebook: (" + str(nPage) + ")"

        self.Freeze()
        
        infos = self.GetPageInfo(nPage)
        panel = self.GetPage(nPage)

        self.ReparentPage(nPage, notebook)
        
        if newPage is None:
            notebook.AddPage(panel, infos["text"], False, infos["image"])
            notebook.SetPageInfo(0, infos)

        for attr in attrs:
            setattr(notebook, attr, getattr(self.nb, attr))
                    
        self.nb.DeletePage(nPage, False)

        self.bsizer.Detach(nPage)
        self.bsizer.Layout()
        self.sizer.Layout()

        self._notebookpages.pop(nPage)
        
        self.AdvanceSelection()

        if self.GetPageCount() == 0:
            self._ShowTabCtrl(False)

            self.sizer.Layout()

        self.Thaw()        
        

    def GetPageInfo(self, nPage):
        """ Returns All The Style Information For A Given Page. """

        if nPage < 0 or (self.GetSelection() >= 0 and nPage >= self.GetPageCount()):
            raise "\nERROR: Invalid Notebook Page In GetPageInfo: (" + str(nPage) + ")"
        
        text = self.GetPageText(nPage)
        image = self.GetPageImage(nPage)
        font1 = self.GetPageTextFont(nPage)
        font2 = self.GetPageTextSecondaryFont(nPage)
        fontcolour = self.GetPageTextColour(nPage)
        pagecolour = self.GetPageColour(nPage)
        enabled = self.IsPageEnabled(nPage)
        tooltip, ontime, winsize = self.GetPageToolTip(nPage)
        menu = self.GetPagePopupMenu(nPage)
        firstcol = self.GetPageFirstGradientColour(nPage)
        secondcol = self.GetPageSecondGradientColour(nPage)
        ishidden = self.nb._pages[nPage]._ishidden
            
        isanimated = 0
        timer = None

        if self.nb._timers[nPage].IsRunning():
            isanimated = 1
            timer = self.nb._timers[nPage].GetInterval()
            
        self.StopAnimation(nPage)
        animatedimages = self.GetAnimationImages(nPage)

        infos = {"text": text, "image": image, "font1": font1, "font2": font2,
                 "fontcolour": fontcolour, "pagecolour": pagecolour, "enabled": enabled,
                 "tooltip": tooltip, "ontime": ontime, "winsize": winsize,
                 "menu": menu, "isanimated": isanimated, "timer": timer,
                 "animatedimages": animatedimages, "imagelist": self.nb._imglist,
                 "firstcol": firstcol, "secondcol": secondcol, "ishidden": ishidden}

        return infos


    def SetPageInfo(self, nPage, infos):
        """ Sets All The Style Information For A Given Page. """

        if nPage < 0 or (self.GetSelection() >= 0 and nPage >= self.GetPageCount()):
            raise "\nERROR: Invalid Notebook Page In SetPageInfo: (" + str(nPage) + ")"

        self.SetPageTextFont(nPage, infos["font1"])
        self.SetPageTextSecondaryFont(nPage, infos["font2"])
        self.SetPageTextColour(nPage, infos["fontcolour"])
        self.SetPageColour(nPage, infos["pagecolour"])
        self.EnablePage(nPage, infos["enabled"])
        self.SetPageToolTip(nPage, infos["tooltip"], infos["ontime"], infos["winsize"])
        self.SetPagePopupMenu(nPage, infos["menu"])
        self.SetPageFirstGradientColour(nPage, infos["firstcol"])
        self.SetPageSecondGradientColour(nPage, infos["secondcol"])
        self.nb._pages[nPage]._ishidden = infos["ishidden"]
        
        if infos["isanimated"] and len(infos["animatedimages"]) > 1:
            self.SetAnimationImages(nPage, infos["animatedimages"])
            self.StartAnimation(nPage, infos["timer"])
    

    def SetCustomPage(self, panel):
        """ Sets A Custom Panel To Show When There Are No Pages Left In NotebookCtrl. """
        
        self.Freeze()
        
        if panel is None:
            if self._custompanel is not None:
                self.bsizer.Detach(self._custompanel)
                self._custompanel.Show(False)
                
            if self.GetPageCount() == 0:   
                self._ShowTabCtrl(False)
        else:
            if self.GetPageCount() == 0:
                if self._custompanel is not None:
                    self.bsizer.Detach(self._custompanel)
                    self._custompanel.Show(False)
                    
                self.bsizer.Add(panel, 1, wx.EXPAND | wx.ALL, 2)
                panel.Show(True)
                self._ShowTabCtrl(False)
            else:
                panel.Show(False)

        self._custompanel = panel
        
        self.bsizer.Layout()
        self.sizer.Layout()
        self.Thaw()
            

    def GetCustomPage(self):
        """ Gets A Custom Panel To Show When There Are No Pages Left In NotebookCtrl. """
        
        return self._custompanel
    

    def HideTab(self, nPage, hide=True):
        """ Hides A Tab In The NotebookCtrl. """

        self.nb.HideTab(nPage, hide)

        
    def HitTest(self, point, flags=0):
        """
        Standard NotebookCtrl HitTest() Method. If Called With 2 Outputs, It
        Returns The Page Clicked (If Any) And One Of These Flags:

        NC_HITTEST_NOWHERE = 0   ==> Hit Not On Tab 
        NC_HITTEST_ONICON  = 1   ==> Hit On Icon 
        NC_HITTEST_ONLABEL = 2   ==> Hit On Label 
        NC_HITTEST_ONITEM  = 4   ==> Hit Generic, On Item
        NC_HITTEST_ONX = 8       ==> Hit On Closing "X" On Every Page
        """
        
        return self.nb.HitTest(point, flags)

    def _AddMargin(self, style, margin):
        if style & NC_TOP or style & NC_BOTTOM:
            self.tabsizer.Add((0, margin), 0)
        elif style & NC_LEFT or style & NC_RIGHT:
            self.tabsizer.Add((margin, 0), 0)
        
    def _GetTabCtrlWindow(self):
        if self._style & NC_TOP or self._style & NC_LEFT:
            return self.tabsizer.GetItem(1)
        else:
            return self.tabsizer.GetItem(0)

    def _ShowTabCtrl(self, show):
        if self._style & NC_TOP:
            self.sizer.Show(0, show)
        else:
            self.sizer.Show(1, show)
        
    
# ---------------------------------------------------------------------------- #
# Class TransientTipWindow
# Auxiliary Help Class. Used To Build The Tip Window.
# ---------------------------------------------------------------------------- #

class _PopupWindow:
    
    def _Fill(self, tip, winsize):
        
        panel = wx.Panel(self, -1)
        colour = self.GetParent().GetToolTipBackgroundColour()
        
        panel.SetBackgroundColour(colour)

        # border from sides and top to text (in pixels)
        border = 5
        # how much space between text lines
        textPadding = 2
        max_len = len(tip)
        tw = winsize

        mylines = tip.split("\n")

        sts = wx.StaticText(panel, -1, "\n".join(mylines))
        sx, sy = sts.GetBestSize()
        sts.SetPosition((2, 2))
            
        panel.SetSize((sx+6, sy+6))
        self.SetSize(panel.GetSize())
        

class TransientTipWindow(_PopupWindow, wx.PopupWindow):
    
    def __init__(self, parent, tip, winsize):
        
        wx.PopupWindow.__init__(self, parent, flags=wx.SIMPLE_BORDER)
        self._Fill(tip,winsize)
        
        
    def ProcessLeftDown(self, evt):
        
        return False


    def OnDismiss(self):
        
        return False


class macPopupWindow(wx.Frame):
    
    def __init__(self, parent, flags):
        
        wx.Frame.__init__(self, parent, id=-1, style=flags|wx.FRAME_NO_TASKBAR|wx.STAY_ON_TOP)
        self._hideOnActivate = False
        #Get the parent frame: could be improved maybe?
        self._parentFrame = parent
        
        while True:
            
            parent = self._parentFrame.GetParent()

            if parent:
                self._parentFrame = parent
            else:
                break
            
        self.Bind(wx.EVT_ACTIVATE, self.OnActivate)

            
    def Show(self, show=True):
        
        wx.Frame.Show(self,show)
        
        if show:
            self._parentFrame.Raise()
            self._hideOnActivate = True
            
            
    def OnActivate(self, evt):
        """
        Let The User Hide The Tooltip By Clicking On It. 
        NotebookCtrl Will Destroy It Later.
        """
        
        if self._hideOnActivate:
            wx.Frame.Show(self,False)
        
            
class macTransientTipWindow(_PopupWindow, macPopupWindow):
    
    def __init__(self, parent, tip, winsize):
        
        macPopupWindow.__init__(self, parent, flags=wx.SIMPLE_BORDER)
        self._Fill(tip, winsize)
    

class NCFrame(wx.Frame):

    def __init__(self, parent, id=wx.ID_ANY, title="", pos=wx.DefaultPosition,
                 size=wx.DefaultSize, style=wx.DEFAULT_FRAME_STYLE, nb=None,
                 panel=None, infos=None, oldparent=None):

        wx.Frame.__init__(self, parent, id, title, pos, size, style)

        self._infos = infos
        self._nb = nb
        self._panel = panel
        self._oldparent = oldparent

        self.Bind(wx.EVT_CLOSE, self.OnClose)


    def OnClose(self, event):
        
        try:
            infos = self._infos
            self._panel.Reparent(self._oldparent)            
            self._nb.AddPage(self._panel, infos["text"], False, infos["image"])

            id = self._nb.GetPageCount() - 1
            
            self._nb.SetPageTextFont(id, infos["font1"])
            self._nb.SetPageTextSecondaryFont(id, infos["font2"])
            self._nb.SetPageTextColour(id, infos["fontcolour"])
            self._nb.SetPageColour(id, infos["pagecolour"])
            self._nb.EnablePage(id, infos["enabled"])
            self._nb.SetPageToolTip(id, infos["tooltip"], infos["ontime"], infos["winsize"])
            self._nb.SetPagePopupMenu(id, infos["menu"])
            self._nb.SetPageFirstGradientColour(id, infos["firstcol"])
            self._nb.SetPageSecondGradientColour(id, infos["secondcol"])
            self._nb._pages[id]._ishidden = infos["ishidden"]
            
            if infos["isanimated"] and len(infos["animatedimages"]) > 1:
                self._nb.SetAnimationImages(id, infos["animatedimages"])
                self._nb.StartAnimation(id, infos["timer"])

        except:
            self.Destroy()
            event.Skip()
            return

        self.Destroy()        

        event.Skip()

class NotebookCtrlWindowHandler(xrc.XmlResourceHandler):
    """
    Create L{NotebookCtrl} windows defined in Xrc resources.
    
    Below is an example of a resource definition::
      <?xml version="1.0" encoding="ISO-8859-1"?>
      <resource>
        <object class="wxPanel" name="appPanel">
          <object class="wxBoxSizer">
            <orient>wxVERTICAL</orient>
            <object class="sizeritem">
              <option>1</option>
              <flag>wxEXPAND</flag>
              <object class="NotebookCtrl" name="notebook">
                <style>wxNO_BORDER | NC_RIGHT | NC_ROTATE | NC_EXPANDABLE </style>
                <focus>0</focus>
                <highlight>1</highlight>
                <tabstyle>NC_GRADIENT_HORIZONTAL | NC_GRADIENT_SELECTION</tabstyle>
                <color1>#DCDCDC</color1>
                <color2>#F5F5F5</color2>
                <selectedcolor1>#C4DADB</selectedcolor1>
                <selectedcolor2>#FFFFFF</selectedcolor2>
                <custompagecolor>#C0C0C0</custompagecolor>
              </object>
            </object>
          </object>
        </object>
      </resource>
    
    @undocumented: CanHandle, DoCreateResource, SetupWindow
    """
    def __init__(self):
        """
        Create a NotebookCtrlWindowHandler instance.
        """
        xrc.XmlResourceHandler.__init__(self)
        # Specify the window styles recognized by objects of this type
        self.AddStyle("wxNO_BORDER", wx.NO_BORDER)
        self.AddStyle("wxTAB_TRAVERSAL", wx.TAB_TRAVERSAL)
        self.AddStyle("NC_TOP", NC_TOP)
        self.AddStyle("NC_BOTTOM", NC_BOTTOM)
        self.AddStyle("NC_LEFT", NC_LEFT)
        self.AddStyle("NC_RIGHT", NC_RIGHT)
        self.AddStyle("NC_FIXED_WIDTH", NC_FIXED_WIDTH)
        self.AddStyle("NC_ROTATE", NC_ROTATE)
        self.AddStyle("NC_EXPANDABLE", NC_EXPANDABLE)
        # More styles, used in the tabstyle parameter
        self.AddStyle("NC_AQUA_LIGHT", NC_AQUA_LIGHT)
        self.AddStyle("NC_AQUA_DARK", NC_AQUA_DARK)
        self.AddStyle("NC_AQUA", NC_AQUA)
        self.AddStyle("NC_METAL", NC_METAL)
        self.AddStyle("NC_SILVER", NC_SILVER)
        self.AddStyle("NC_KDE", NC_KDE)
        self.AddStyle("NC_GRADIENT_VERTICAL", NC_GRADIENT_VERTICAL)
        self.AddStyle("NC_GRADIENT_HORIZONTAL", NC_GRADIENT_HORIZONTAL)
        self.AddStyle("NC_GRADIENT_SELECTION", NC_GRADIENT_SELECTION)
        
        self.AddWindowStyles()

    def _CreateResourceInstance(self, parent, id, position, size, style, name):
        window = NotebookCtrl(parent, id, position, size=size, style=style, name=name)
        return window
    
    def _GetColorParamValue(self, paramName, defaultValue=wx.WHITE):
        paramValue = self.GetParamValue(paramName)
        if paramValue:
            return self.GetColour(paramName)
        else:
            return defaultValue
        
    def _GetCustomPage(self, window):
        customPage = wx.Window(window, -1, style = wx.STATIC_BORDER)
        customPage.SetBackgroundColour(self._GetColorParamValue('custompagecolor'))
        return customPage
        
    def _GetIntParamValue(self, paramName, defaultValue=0):
        paramValue = self.GetParamValue(paramName)
        if paramValue:
            return int(paramValue)
        else:
            return defaultValue
        
    def _GetTabTheme(self):
        tabstyle = self.GetStyle("tabstyle")
        
        if tabstyle:
            result = ThemeStyle()
            if tabstyle & NC_GRADIENT_VERTICAL or tabstyle & NC_GRADIENT_HORIZONTAL:
                result.EnableGradientStyle(True, tabstyle)
                result.SetFirstGradientColour(self._GetColorParamValue('color1'))
                result.SetSecondGradientColour(self._GetColorParamValue('color2'))
                result.SetFirstGradientColourSelected(self._GetColorParamValue('selectedcolor1'))
                result.SetSecondGradientColourSelected(self._GetColorParamValue('selectedcolor2'))
            elif tabstyle & NC_AQUA_LIGHT or tabstyle & NC_AQUA_DARK:
                result.EnableAquaTheme(True, tabstyle & NC_AQUA_LIGHT and 2 or 1)
            elif tabstyle & NC_METAL:
                result.EnableMetalTheme(True)
            elif tabstyle & NC_KDE:
                result.EnableKDETheme(True)
            elif tabstyle & NC_SILVER:
                result.EnableSilverTheme(True)
        else:
            result = GetDefaultTabStyle()
        return result

    # This method and the next one are required for XmlResourceHandlers
    def CanHandle(self, node):
        return self.IsOfClass(node, "NotebookCtrl")

    def DoCreateResource(self):
        # NOTE: wxWindows can be created in either a single-phase or
        # in a two-phase way.  Single phase is what you normally do,
        # and two-phase creates the instnace first, and then later
        # creates the actual window when the Create method is called.
        # (In wxPython the first phase is done using the wxPre*
        # function, for example, wxPreFrame, wxPrePanel, etc.)
        #
        # wxXmlResource supports either method, a premade instance can
        # be created and populated by xrc using the appropriate
        # LoadOn* method (such as LoadOnPanel) or xrc can create the
        # instance too, using the Load* method.  However this makes
        # the handlers a bit more complex.  If you can be sure that a
        # particular class will never be loaded using a pre-existing
        # instance, then you can make the handle much simpler.  I'll
        # show both methods below.

        # The simple method assumes that there is no existing
        # instance.  Be sure of that with an assert.
        assert self.GetInstance() is None

        # Now create the object
        window = self._CreateResourceInstance(self.GetParentAsWindow(),
                                              self.GetID(),
                                              self.GetPosition(),
                                              self.GetSize(),
                                              self.GetStyle("style", NC_DEFAULT_STYLE),
                                              self.GetName())

        # Set standard window attributes
        self.SetupWindow(window)
        # Create any child windows of this node
        self.CreateChildren(window)

        return window
    
    def SetupWindow(self, window):
        super(NotebookCtrlWindowHandler, self).SetupWindow(window)
        window.ApplyTabTheme(self._GetTabTheme())
        window.SetHighlightSelection(self._GetIntParamValue("highlight", 0) != 0)
        window.SetUseFocusIndicator(self._GetIntParamValue("focus", 1) != 0)
        window.SetCustomPage(self._GetCustomPage(window))
        