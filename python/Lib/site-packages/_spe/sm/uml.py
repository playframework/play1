import os, re
import wx, wx.lib.ogl as ogl

INITIALIZED = False

BOTTOM      = 9999
DEFAULT     = '-'
SEPARATOR   = '+'

RE_PARENT   = re.compile("[^(]+[(]([^)]+)[)]")

####Utilities
def htmlColour(c):
    return ('#%2s%2s%2s'%tuple([hex(x)[2:] for x in (c.Red(),c.Green(),c.Blue())])).replace(' ','0').upper()
    
####Generic
class Class:
    def __init__(self,name='class',container=[],children=[],data=None):
        #passing values...
        self.name           = name
        self.container      = container[:]
        self.children       = children
        self.data           = data
        #initialize
        match               = RE_PARENT.match(self.name)
        if match:
            self.parents    = [x.strip() for x in match.group(1).split(',')]
            self.hierarchy  = BOTTOM
            self.verified   = False
        else:
            self.parents    = []
            self.hierarchy  = 0
            self.verified   = True
        
    def __str__(self):
        self.width      = max([len(x) for x in [self.name]+self.container])
        self.height     = len(self.container)+1
        entry           = '| %%-%ss |'%self.width
        line            = '+'+'-'*(self.width+2)+'+'
        return '\n'.join([line]+[entry%x for x in self.container]+[line])

    def append(self,x,t=DEFAULT):
        self.container.append('%s%s'%(t,x.replace(' ','')))
            
    def extend(self,l,t=DEFAULT):
        for x in l: self.append(x,t=t)
        
    def getHierarchy(self,classes):
        if not self.verified:
            parents = [classes[parent].getHierarchy(classes) 
                    for parent in self.parents if classes.has_key(parent)]
            if parents:
                self.hierarchy  = max(parents)+1
            else:
                self.hierarchy  = 1
            self.verified   = True
        return self.hierarchy
            
    #---wx
    def wx(self,dc,canvas):
        width = height = 10
        for x in [self.name]+self.container:
            w, h    = dc.GetTextExtent(x)
            width   = max(width,w)
            height  += h
        return _Class(width, height, canvas, self.name, self.container)
        
####WxPython
#---Printing support
ID_Setup    = wx.NewId()
ID_Preview  = wx.NewId()
ID_Print    = wx.NewId()

BITMAP_TYPE = {
                ".bmp": wx.BITMAP_TYPE_BMP,      # Save a Windows bitmap file.
                ".eps": None,
                ".gif": wx.BITMAP_TYPE_GIF,      # Save a GIF file.
                ".jpg": wx.BITMAP_TYPE_JPEG,     # Save a JPG file.
                ".pcx": wx.BITMAP_TYPE_PCX,      # Save a PCX file.
                ".png": wx.BITMAP_TYPE_PNM,      # Save a PNG file.
                ".pnm": wx.BITMAP_TYPE_PNM,      # Save a PNM file.
                ".tif": wx.BITMAP_TYPE_TIF,      # Save a TIF file.
                ".xbm": wx.BITMAP_TYPE_XBM,      # Save an X bitmap file.
                ".xpm": wx.BITMAP_TYPE_XPM,      # Save an XPM bitmap file.
            }

def wxTopLevelFrame(window):
    while not window.IsTopLevel():
        window = window.GetParent()
    return window
    
def doPrint(dc,canvas):
    # One possible method of setting scaling factors...
    maxX, maxY = canvas.GetVirtualSize()
    # Let's have at least 50 device units margin
    marginX = 50
    marginY = 50
    # Add the margin to the graphic size
    maxX = maxX + (2 * marginX)
    maxY = maxY + (2 * marginY)
    # Get the size of the DC in pixels
    (w, h) = dc.GetSizeTuple()
    # Calculate a suitable scaling factor
    scaleX = float(w) / maxX
    scaleY = float(h) / maxY
    # Use x or y scaling factor, whichever fits on the DC
    actualScale = min(scaleX, scaleY)
    # Calculate the position on the DC for centering the graphic
    posX = (w - (maxX * actualScale)) / 2.0
    posY = (h - (maxY * actualScale)) / 2.0
    # Set the scale and origin
    dc.SetUserScale(actualScale, actualScale)
    dc.SetDeviceOrigin(int(posX), int(posY))
    canvas.Redraw(dc)
    dc.DrawText("Drawn by SPE [http://pythonide.stani.be]", marginX/2, maxY-marginY)

class PrintCanvas(ogl.ShapeCanvas):
    def __init__(self, *args, **keyw):
        #initialize
        global INITIALIZED
        if not INITIALIZED: 
            ogl.OGLInitialize()
            INITIALIZED = True
        maxWidth  = 800
        maxHeight = 800
        #frame
        ogl.ShapeCanvas.__init__(self, size=(maxWidth,maxHeight), *args, **keyw)
        self.frame = wxTopLevelFrame(self)
        self.SetScrollbars(20, 20, maxWidth/20, maxHeight/20)
        #Print data
        self.printSetup = False
        self.printData = wx.PrintData()
        self.printData.SetPaperId(wx.PAPER_A4)
        self.printData.SetPrintMode(wx.PRINT_MODE_PRINTER)
        #events
        self.Bind(wx.EVT_LEFT_DCLICK, self.OnPrintPreview)
        self.Bind(wx.EVT_MIDDLE_DCLICK, self.OnDoSave)
        self.Bind(wx.EVT_RIGHT_DCLICK, self.OnPrintSetup)
    
    def _checkPrintSetup(self):
        if not self.printSetup: self.OnPrintSetup()

    def OnPrintSetup(self, event=None):
        data = wx.PageSetupDialogData(self.printData)
        printerDialog = wx.PageSetupDialog(self, data)
        if printerDialog.ShowModal() == wx.ID_OK:
            self.printData = wx.PrintData( printerDialog.GetPageSetupData().GetPrintData() )
        printerDialog.Destroy()
        self.pageSetup = True

    def OnPrintPreview(self, event=None):
        self._checkPrintSetup()        
        data = wx.PrintDialogData(self.printData)
        printout = Printout(self)
        printout2 = Printout(self)
        self.preview = wx.PrintPreview(printout, printout2, data)
        if not self.preview.Ok():
            return
        frame = wx.PreviewFrame(self.preview, self.frame, "SPE - Print Preview")
        frame.Initialize()
        frame.SetPosition(self.frame.GetPosition())
        frame.SetSize(self.frame.GetSize())
        frame.Show(True)

    def OnDoPrint(self, event=None):
        pdd = wx.PrintDialogData(self.printData)
        pdd.SetToPage(2)
        printer = wx.Printer(pdd)
        printout = Printout(self)
        if not printer.Print(self.frame, printout, True):
            wx.MessageBox("Printing was cancelled.\n\nIf you didn't cancel the print, perhaps\nyour current printer is not set correctly?", "Printing", wx.OK)
        else:
            self.printData = wx.PrintData( printer.GetPrintDialogData().GetPrintData() )
        printout.Destroy()
        
    def OnDoSave(self, event=None):
        self.SaveFile()#"c:\\test.png")
        
    def SaveFile(self, fileName= ''):
        """Saves the file to the type specified in the extension. If no file
        name is specified a dialog box is provided.  Returns True if sucessful,
        otherwise False.
        
        .bmp  Save a Windows bitmap file.
        .xbm  Save an X bitmap file.
        .xpm  Save an XPM bitmap file.
        .png  Save a Portable Network Graphics file.
        .jpg  Save a Joint Photographic Experts Group file.
        """
        fileTypes   = BITMAP_TYPE.keys()
        fileTypes.sort()
        ext         = fileName[-3:].lower()
        if ext not in fileTypes:
            dlg1    = wx.FileDialog(
                    self, 
                    "Save image as", ".", "",
                    "|".join(["%s files (*%s)|*%s"%(t.upper(),t,t) for t in fileTypes]),
                    wx.SAVE|wx.OVERWRITE_PROMPT
                    )
            if dlg1.ShowModal() == wx.ID_OK:
                fileName    = dlg1.GetPath()
                # Check for proper exension
                ext         = os.path.splitext(fileName)[-1]
                if ext not in fileTypes:
                    ext     = fileTypes[dlg1.GetFilterIndex()]
                    fileName+=ext
                dlg1.Destroy()
            else: # exit without saving
                dlg1.Destroy()
                return False
                
        tp          = BITMAP_TYPE[ext]
        # Save...
        w, h        = self.GetVirtualSize()
        if tp:
            #...as bitmap
            dc      = wx.MemoryDC()
            bitmap  = wx.EmptyBitmap(w+10,h+10)
            dc.SelectObject(bitmap)
            dc.SetBackground(wx.WHITE_BRUSH)
            dc.Clear()
            self.Redraw(dc)
            return bitmap.SaveFile(fileName, tp)
        else:
            #... as postscript
            printData   = wx.PrintData()
            printData.SetFilename(fileName)
            dc          = wx.PostScriptDC(printData)
            if dc.Ok():
                dc.StartDoc('Saving as postscript')
                doPrint(dc,self)
                #self.Redraw(dc)
                dc.EndDoc()
            
        
class Printout(wx.Printout):
    def __init__(self, canvas):
        wx.Printout.__init__(self)
        self.canvas = canvas

    def OnBeginDocument(self, start, end):
        return self.base_OnBeginDocument(start, end)

    def OnEndDocument(self):
        self.base_OnEndDocument()

    def OnBeginPrinting(self):
        self.base_OnBeginPrinting()

    def OnEndPrinting(self):
        self.base_OnEndPrinting()

    def OnPreparePrinting(self):
        self.base_OnPreparePrinting()

    def HasPage(self, page):
        if page <= 2:
            return True
        else:
            return False

    def GetPageInfo(self):
        return (1, 2, 1, 2)

    def OnPrintPage(self, page):
        dc = self.GetDC()
        doPrint(dc,self.canvas)
        return True

#---General
def wxAssertColour(c):
    name    = htmlColour(c)
    wx.TheColourDatabase.AddColour(name,c)
    return name
    
class _EvtHandler(ogl.ShapeEvtHandler):
    def __init__(self, frame):
        ogl.ShapeEvtHandler.__init__(self)
        self.statbarFrame = frame

    def OnLeftClick(self, x, y, keys=0, attachment=0):
        shape = self.GetShape()
        canvas = shape.GetCanvas()
        dc = wx.ClientDC(canvas)
        canvas.PrepareDC(dc)

        if shape.Selected():
            shape.Select(False, dc)
            canvas.Redraw(dc)
        else:
            shapeList = canvas.GetDiagram().GetShapeList()
            toUnselect = []
            for s in shapeList:
                if s.Selected():
                    # If we unselect it now then some of the objects in
                    # shapeList will become invalid (the control points are
                    # shapes too!) and bad things will happen...
                    toUnselect.append(s)
            shape.Select(True, dc)
            if toUnselect:
                for s in toUnselect:
                    s.Select(False, dc)
                canvas.Redraw(dc)

    def OnEndDragLeft(self, x, y, keys=0, attachment=0):
        shape = self.GetShape()
        ogl.ShapeEvtHandler.OnEndDragLeft(self, x, y, keys, attachment)
        if not shape.Selected():
            self.OnLeftClick(x, y, keys, attachment)

    def OnSizingEndDragLeft(self, pt, x, y, keys, attch):
        ogl.ShapeEvtHandler.OnSizingEndDragLeft(self, pt, x, y, keys, attch)

    def OnMovePost(self, dc, x, y, oldX, oldY, display):
        ogl.ShapeEvtHandler.OnMovePost(self, dc, x, y, oldX, oldY, display)

    def OnRightClick(self, dc, *dontcare):
        pass

class _Class(ogl.DividedShape):
    def __init__(self, width, height, canvas, name, container, 
            lineColour=wx.Colour(80,80,80), textColour=wx.Colour(0,0,0),
            pen=wx.BLACK_PEN,brush=wx.LIGHT_GREY_BRUSH):
        #initialize
        ogl.DividedShape.__init__(self, width, height)
        self.lineColour         = wxAssertColour(lineColour)
        self.textColour         = wxAssertColour(textColour)
        self.width              = width
        self.height             = height
        self.SetPen(pen)
        self.SetBrush(brush)
        #generate contents
        total                   = float(len(container))+1
        current                 = 0
        text                    = ''
        self.AddText(name,prop=1/total, textColour=wx.RED,format=ogl.FORMAT_CENTRE_HORIZ)
        for entry in container:
            if entry[0] == SEPARATOR:
                self.AddText(text,prop=current/total)
                self.AddText(entry[1:],prop=1/total, textColour=wx.Colour(0,0,200),format=ogl.FORMAT_CENTRE_HORIZ)
                text            = ''
                current    = 0
            else:
                text            = '%s%s\n'%(text,entry[1:])
                current     +=1
        self.AddText(text,prop=current/total)
        self.SetRegionSizes()
        self.ReformatRegions(canvas)
        
    def AddText(self,text,lineColour=None,textColour=None,prop=0.1,format=ogl.FORMAT_NONE):
        if text:
            region          = ogl.ShapeRegion()
            if lineColour:  region.SetPenColour(wxAssertColour(lineColour))
            else:           region.SetPenColour(self.lineColour)
            if textColour:  region.SetColour(wxAssertColour(textColour))
            else:           region.SetColour(self.textColour)
            region.SetText(text)
            region.SetProportions(0.0, prop)
            region.SetFormatMode(format)
            self.AddRegion(region)
            
    def Goto(self,x,y):
        self.SetX(x+self.width/2)
        self.SetY(y+self.height/2)

    def OnSizingEndDragLeft(self, pt, x, y, keys, attch):
        ogl.DividedShape.OnSizingEndDragLeft(self, pt, x, y, keys, attch)
        self.SetRegionSizes()
        self.ReformatRegions()
        self.GetCanvas().Refresh()

    def ReformatRegions(self, canvas=None):
        rnum = 0

        if canvas is None:
            canvas = self.GetCanvas()

        dc = wx.ClientDC(canvas)  # used for measuring

        for region in self.GetRegions():
            text = region.GetText()
            self.FormatText(dc, text, rnum)
            rnum += 1

#class Canvas(ogl.ShapeCanvas):
class Canvas(PrintCanvas):
    def __init__(self, parent,**keyw):
        PrintCanvas.__init__(self, parent,**keyw)

        self.parent = parent
        self.SetBackgroundColour(wx.WHITE)
        self.diagram = ogl.Diagram()
        self.SetDiagram(self.diagram)
        self.diagram.SetCanvas(self)
        self.shapes = []
        self.save_gdi = []
        self.__test__()
            
    def __test__(self):
        u = Class()
        u.append('mmmm')
        u.append('test')
        u.append('test')
        u.append('test')
        u.append('haha',SEPARATOR)
        u.append('test')
        u.append('test')
        u.append('test')
        self.DrawUml(classes={'u':u,'v':u})
        return True
        
##    def OnDoPrint(self,event=None):
##        self.GetParent().OnPrintPreview(None)
##        
    def DrawUml(self,classes={},between=20):
        """Draws the uml diagram"""
        #verify all hierachies
        rows        = [[] for x in range(len(classes)+2)]
        for name, u in classes.items():
            if not u.verified:
                u.getHierarchy(classes)
            rows[u.hierarchy].append(u)
        #draw uml
        shapes          = {}
        dc              = wx.ClientDC(self)
        self.PrepareDC(dc)
        
        self.diagram.DeleteAllShapes()
        total_height        = total_width   = y = between

        for row in rows:
            if row:
                x           = between
                height      = 0
                for u in row:
                    shape   = u.wx(dc,self)
                    shapes[u.name.split('(')[0]]  = shape
                    self.__addShape(shape, x, y, '')
                    x       += between+shape.width
                    height  = max(height,shape.height)
                    for parent in u.parents:
                        if shapes.has_key(parent):
                            line = ogl.LineShape()
                            line.SetCanvas(self)
                            line.SetPen(wx.BLACK_PEN)
                            line.SetBrush(wx.BLACK_BRUSH)
                            line.AddArrow(ogl.ARROW_ARROW)
                            line.MakeLineControlPoints(2)
                            shapes[parent].AddLine(line, shape)
                            self.diagram.AddShape(line)
                            line.Show(True)
                width       = int(x+between)
                height      = int(height+3*between)
                total_width = max(width,total_width)
                y           += height
                total_height+= height
        total_height        -= 3*between
        self.SetVirtualSize((total_width, total_height))
        self.SetScrollRate(20,20)

    def __addShape(self, shape, x, y, text):
        if isinstance(shape, ogl.CompositeShape):
            dc = wx.ClientDC(self)
            self.PrepareDC(dc)
            shape.Move(dc, x, y)
        else:
            shape.SetDraggable(True, True)
        shape.SetCanvas(self)
        shape.Goto(x,y)
        if text:
            for line in text.split('\n'):
                shape.AddText(line)
        shape.SetShadowMode(ogl.SHADOW_RIGHT)
        self.diagram.AddShape(shape)
        shape.Show(True)

        evthandler = _EvtHandler(self)
        evthandler.SetShape(shape)
        evthandler.SetPreviousHandler(shape.GetEventHandler())
        shape.SetEventHandler(evthandler)

        self.shapes.append(shape)
        return shape

    def OnBeginDragLeft(self, x, y, keys):
        pass

    def OnEndDragLeft(self, x, y, keys):
        pass

        
if __name__=='__main__':
    import wxp
    
    wxp.panelApp(Canvas)
