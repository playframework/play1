"""Panel to execute scripts and redirect their output for SPE"""

import os, re
from cgi import escape
import wx
import wx.stc as wx_stc
import  wx.html as  html

import _spe.info as info

def icon(x):
    return os.path.join(info.INFO['skinLocation'],x)

FIND_ICON   = icon('lookup.png').replace('\\','\\\\')
RUN_ICON   = icon('run.png')
RE_LINK     = re.compile('(\s*)(File "(.*))\n')
RE_LOCATION = re.compile('File "([^"]*)", line (\d+)')

KILL_ERROR  = {
    wx.KILL_OK              : 'ok',
    wx.KILL_BAD_SIGNAL      : 'bad signal',
    wx.KILL_ACCESS_DENIED   : 'access denied',
    wx.KILL_NO_PROCESS      : 'no such process',
    wx.KILL_ERROR           : 'unspecified error',
}

class Output(html.HtmlWindow):
    pid         = -1
    
    def __init__(self,parent=None,*args,**keyw):
        html.HtmlWindow.__init__(self,parent,*args,**keyw)
        self.SetFonts(normal_face='courier',fixed_face='courier',sizes=[8,9,10,12,16,20,22])
        self.app        = parent.app
    
    #---execute
    def _check_run(self,bool):
        #assing method for check run tool button
        if self.app.children:
            child                   = self.app.childActive
            if child.frame.menuBar:
                child.frame.menuBar.check_run(bool)
            else:
                child.parentFrame.menuBar.check_run(bool)
    
    def Execute(self, command, label = None, statustext = "Running script...",beep=False):
        """Executes a command of which the output will be redirected by OnIdle and OnEndProcess."""
        if self.pid is -1:
            if not label: label = command
            #give feedback
            self.AddText('<table bgcolor=#CCCCCC width=100%%><tr><td><TT><img src="%s">&nbsp;%s</TT></td></tr></table>'%(RUN_ICON,label))
            self.SetStatusText(statustext)
            self.UpdateToolbar()
            self.Raise()
            #bind events
            self.Bind(wx.EVT_IDLE,self.OnIdle)
            self.Bind(wx.EVT_END_PROCESS,self.OnEndProcess)
            #create process
            self.process        = wx.Process(self)
            self.process.Redirect()
            if info.WIN:
                self.pid        = wx.Execute(command, wx.EXEC_ASYNC | wx.EXEC_NOHIDE, self.process)
            else:
                self.pid        = wx.Execute(command, wx.EXEC_ASYNC | wx.EXEC_MAKE_GROUP_LEADER, self.process)
            self.inputstream    = self.process.GetInputStream()        
            self.errorstream    = self.process.GetErrorStream()
            self.outputstream   = self.process.GetOutputStream()
            self.inputstream.Write = Write
            self._check_run(True) 
            self.beep           = beep         
           
    def Kill(self):
        if wx.Process.Exists(self.pid) and self.pid != -1:
            result = wx.Process.Kill(self.pid, wx.SIGKILL, flags=wx.KILL_CHILDREN)
            self.OnEndProcess(event=None)
            message     = 'Script stopped by user (%s).'%KILL_ERROR.get(result,'unknown error')
            self.SetStatusText(message)
            self.AddText(message,error=True)
            
    def IsBusy(self):
        """Is the instance busy executing a command."""
        return not (self.pid is -1)
            
    #---user feedback
    def AddText(self,text,error=False):
        """Add text and in case of error, colour red and provide links."""
        text        = text.replace('\r\n','\n').replace("<string>","&lt;string&gt;")
        if error:
            text    = escape(text)
            text    = RE_LINK.sub(r"\g<1><img src='%s'>&nbsp;<a href='\g<2>'>\g<2></a><br>"%FIND_ICON,text)
            text    = '<font color=red>%s</font>'%text
        text        = text.replace('\n','<br>')
        self.AppendToPage(text)
        self.Scroll(0,self.GetVirtualSize()[1]/self.GetScrollPixelsPerUnit()[1])

    def SetStatusText(self,text):
        print text
        
    def UpdateToolbar(self):
        #print 'Updating toolbar...'
        pass
        
    #---view
    def Clear(self):
        self.SetPage('')

    #---event handlers
    def OnIdle(self, event):
            if self.inputstream.CanRead():
                text = self.inputstream.read()
                self.AddText(escape(text).replace(' ','&nbsp;').replace('\t','&nbsp;'))
            if self.errorstream.CanRead():
                text = self.errorstream.read()
                self.AddText(text,error=True)

    def OnEndProcess(self, event):
        #unbind events
        self.Unbind(wx.EVT_IDLE)
        self.Unbind(wx.EVT_END_PROCESS)
        #check for any leftover output.
        self.OnIdle(event)
        #destroy process
        if event != None:
            self.process.Destroy()
        self.process    = None
        self.pid        = -1
        #give feedback
        self.UpdateToolbar()
        self._check_run(False)
        if event:
            message     = "Script terminated."
            self.SetStatusText(message)
            self.AddText(message)#'</pre>'+
            wx.Bell()
        
    def OnLinkClicked(self,linkInfo):
        match   = RE_LOCATION.match(linkInfo.GetHref())
        try:
            fileName    = match.group(1)
            lineno      = int(match.group(2))
            self.OpenFile(fileName,lineno-1)
            self.SetStatusText('Jumped to file "%s" (line %s).'%(fileName,lineno))
        except Exception, message:
            self.SetStatusText('SPE could not locate source file. (%s)'%message)

    def OpenFile(self,fileName,lineno):
        print fileName,lineno
        
def Write(self,*args,**keyw):
    print args,keyw
    wx.OutputStream.Write(self,*args,**keyw)
        
class Panel(Output):
    def __init__(self,panel,*args,**keyw):
        Output.__init__(self,parent=panel,id=-1,*args,**keyw)
        self.app            = panel.app
        self.OpenFile       = panel.openList
        self.SetStatusText  = panel.SetActiveStatusText
        
    def Raise(self):
        self.GetParent().SetSelection(3)#todo: 3 should be determined dynamic

        
class TestFrame(wx.Frame):
    
    def __init__(self,parent=None,id=-1,*args,**keyw):
        wx.Frame.__init__(self,parent=parent,id=id,*args,**keyw)
        self.output = Output(parent=self,id=-1)
        self.Show()
        
    def test(self):
        import time
        for i in range (1):
            self.output.Execute('python -c "%s/0"'%i)
##            while self.output.IsBusy():
##                print self.output.pid
##                time.sleep(1)

def test():
    app = wx.PySimpleApp()
    frame = TestFrame()
    app.SetTopWindow(frame)
    wx.CallAfter(frame.test)
    app.MainLoop()
        
if __name__ == '__main__':
    print "<hello world>"
    print "             hello world"
