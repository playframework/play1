class Timer:
    def __init__(self):
        pass
    #---private
    def _synchronize(self):
        pass
    #---public
    def start(self):
        pass
    def stop(self):
        pass
        
class Frame:
    def __init__(self):
        pass
    #---output
    def clear(self):
        pass
    def write(self):
        pass
    #---events
    def onLeftMouseClick(self):
        pass    
    def onRightMouseClick(self):
        pass
        
class Dialog(Frame):
    def __init__(self):
        pass
    #---data
    def verify(self):
        pass
    def reset(self):
        pass
    #---events
    def onOk(self):
        pass    
    def onCancel(self):
        pass
        
class Warning(Dialog,Timer):
    #---separator
    def question(self):
        pass
        
class Window(Frame):
    def __init__(self):
        pass
    #---transform
    def move(self):
        pass
    def resize(self):
        pass
    #---events
    def onMaximize(self):
        pass    
    def onMinimize(self):
        pass
    def onClose(self):
        pass

class ScrolledWindow(Window):
    def __init__(self):
        pass
    #---scrollbars
    def setHscroll(self):
        pass
    def setVscroll(self):
        pass
        
class SplitWindow(Window):
    def split(self):
        pass
        
