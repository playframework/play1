#----------------------------------------------------------------------------
# Name:         ucode.py
# Purpose:      Single Instance App with passing arguments
# Usage:        see bottom of file
# Author:       Peter Damoc <pdamoc@gmx.net>
# Licence:      wxWindows license
#----------------------------------------------------------------------------
import wx
import thread
import time
import sys
import wx.lib.newevent
import SimpleXMLRPCServer
import xmlrpclib 

(PostArgsEvent, EVT_POST_ARGS) = wx.lib.newevent.NewEvent()

class PostAppServer:
    def __init__(self, app):
        self.app = app
    def PostArgs(self, args):
        evt = PostArgsEvent(data=args)
        wx.PostEvent(self.app, evt)
        return "OK"
    def Stop(self):
        return "OK"
        
class ArgsPosterThread:
    def __init__(self, app):
        self.app = app

    def Start(self):
        self.keepGoing = self.running = True
        thread.start_new_thread(self.Run, ())

    def Stop(self):
        self.keepGoing = False
        server = xmlrpclib.ServerProxy("http://localhost:%d"%self.app.port)
        server.Stop()
    def IsRunning(self):
        return self.running

    def Run(self):
        server = SimpleXMLRPCServer.SimpleXMLRPCServer(("localhost", self.app.port))
        server.register_instance(PostAppServer(self.app))
        while self.keepGoing:
            server.handle_request( ) 
        self.running = False
        
class SingleInstanceApp(wx.App):
    port = 50008
    def __init__(self,  name,*args, **kwargs):
        self.name = name
        self.instance = wx.SingleInstanceChecker(name+wx.GetUserId())
        try:
            self.port = kwargs.pop("port")
        except:
            pass
        if self.instance.IsAnotherRunning():
            self.active = True
            server = xmlrpclib.ServerProxy("http://localhost:%d"%self.port)
            server.PostArgs(sys.argv[1:])
            wx.App.__init__(self, *args, **kwargs)
        else:
            self.active = False
            self.args = sys.argv[1:]
            wx.App.__init__(self, *args, **kwargs)
            self.argsPosterThread = ArgsPosterThread(self)
            self.argsPosterThread.Start()
    
    def OnExit(self):
        if not self.active:
            wx.Yield()
            self.argsPosterThread.Stop()
            running = 1
            while running:
                running = 0
                print "_spe/sm/wxp/singleApp.py:78: running"
                running = running + self.argsPosterThread.IsRunning()
                time.sleep(0.1)
                
#-------------------------------- Usage ---------------------------------------
if __name__ == "__main__":
    class TestApp(SingleInstanceApp):
        def OnArgs(self, evt):
            self.tf.AppendText(u"\nReceived args: "+unicode(evt.data))
            self.GetTopWindow().Raise()
            self.GetTopWindow().Iconize(False)# thanks to Alexei for pointing it out
            
        def OnInit(self):
            if self.active:
                return False
            else:
                self.Bind(EVT_POST_ARGS, self.OnArgs)
                self.mainFrame = wx.Frame(None, title=self.name)
                self.tf = wx.TextCtrl(self.mainFrame, style=wx.TE_MULTILINE)
                self.tf.AppendText(u"Original args: "+unicode(self.args))
                self.SetTopWindow(self.mainFrame)
                self.mainFrame.Show()
                return True
    app = TestApp("A_simple_TestApp",0, port= 50009)
    app.MainLoop()