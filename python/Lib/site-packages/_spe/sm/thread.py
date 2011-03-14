###(c)www.stani.be (read __doc__ for more information)                            
##import sm
##INFO=sm.INFO.copy()
##
##INFO['description']=\
##"""Thread related."""
##
##__doc__=INFO['doc']%INFO
###_______________________________________________________________________________

####IMPORT----------------------------------------------------------------------
import Queue,threading,types,time

####CLASSES---------------------------------------------------------------------
class Thread(threading.Thread):
    def __init__(self,function,*arguments,**keywords):
        threading.Thread.__init__(self)
        self.function=function
        self.arguments=arguments
        self.keywords=keywords
        self.start()
    def run(self):
        apply(self.function,self.arguments,self.keywords)

class _PoolThread(threading.Thread):
    """Private thread class used by the Pool class."""
    def __init__(self, pool):
        threading.Thread.__init__(self)
        self.pool=pool
        self.id=len(self.pool._pool)
    def __exit__(self):
        pass
    def run(self):
        print 'Thread:\t starting thread %s'%self.id
        while not self.pool.isAborted():
            self.process = self.pool._input.get()
            if self.process==SHUTDOWN or self.pool.isAborted():
                break
            else:
                self._time=time.time()
                self._timeOut=0
                function,arguments,keywords=self.process
                self.pool._current.put(self.process)
                self.pool._output.put(apply(function,arguments,keywords))
                self.pool._current.get()
        self.__exit__()
##        try:
##            self.__exit__()
##        except:
##            pass
        #empty input, so that isalive is correct
        self.pool._input=Queue.Queue()
        print 'Thread:\t Exiting thread %s'%self.id
    def time(self):
        """Running time of the current process (not of thread)."""
        return time.time()-self._time
    def timeOut(self,seconds):
        """Returns true if a time out occured."""
        if self.time()>seconds and not self._timeOut:
            self._timeOut=1
            return 1
        else: return 0
    def noTimeOut(self):
        self._timeOut=0

class Pool:
    """Pool of reusable threads, without polling."""
    def __init__(self, numThreads=5,timeOut=0,exit=None):
        """timeOut is only relevant for use together with the checkTimeOut method."""
        self.timeOut=timeOut
        if exit:self.__exit__=exit

        self._aborted=0
        self._current = Queue.Queue(numThreads)
        self._input = Queue.Queue()
        self._output = Queue.Queue()

        self._pool = []
        self.add(numThreads)
    def __exit__(self):
        """Will be executed everytime EACH thread finishes."""
        # TODO:Maybe it is better to do this only after ALL threads finish
        pass

    def __len__(self):
        """Returns how many threads are running."""
        return len(self._pool)

    def abort(self):
        """Let the threads stop as fast as possible."""
        self._aborted=1

    def add(self,n):
        """Adds one thread to the pool."""
        for i in range(n):
            t = _PoolThread(self)
            t.__exit__=self.__exit__
            t.start()
            self._pool.append(t)

    def checkTimeOut(self,timeOut=0):
        """Adds a thread to the pool if one of the threads times out."""
        if timeOut:self.timeOut=timeOut
        timeOuts=[]
        if self.timeOut:
            for t in self._pool:
                if t.timeOut(self.timeOut):
                    timeOuts.append(t)
                    self.add(1)

    def isAborted(self):
        """Returns true if the pool is trying to abort."""
        return self._aborted

    def get(self):
        """Gets the last output value."""
        return self._output.get()

    def getAll(self):
        """Gets all output values."""
        l=[]
        while not self._output.empty():l.append(self._output.get())
        return l

    def isAlive(self):
        """Returns true, if there are still processes scheduled or running."""
        return not (self._input.empty() and self._current.empty())

    def find(self,function,*arguments,**keywords):
        """Returns the thread which is executing this process."""
        ## TODO:Replace function,arg,keyw with process!!!!!!!
        index=0
        found=-1
        process=(function,arguments,keywords)
        while index<len(self) and found==-1:
            t=self._pool[index]
            if t.function==function and t.arguments==arguments and t.keywords==keywords:
                found=index
            else:index+=1
        if found: return t
        else: return 0

    def put(self,function,*arguments,**keywords):
        """Adds a process to the pool (function,arguments,keywords)"""
        self._input.put((function,arguments,keywords))

    def run(self,update=0,delay=1):
        """update(function to be executed every loop),delay(seconds)
        Only use this when you want the program to halt and to wait for the end of the tasklist."""
        while self.isAlive():
            time.sleep(delay)
            if update:update()

    def shutdown(self,join=0):
        """Finish all scheduled threads and stops the pool."""
        for t in self._pool:self._input.put(SHUTDOWN)
        if join:
            for t in self._pool:t.join(join)

class PausePool(Pool):
    """Use pool.isAborted"""
    def put(self,function,*arg,**keyw):
        keyw['pool']=self
        self._input.put((function,arg,keyw))
        
#---WXPYTHON EVENT SUPPORT------------------------------------------------------

def wxEnableEvents():
    """This will import wxPython and simplify the process of sending thread events.

    Probably it's better to use the Stunt class from this module with a timer event.
    """
    from wxPython import wx
    global wxEVT_THREAD_TYPE,connect,event,postEvent
    wxEVT_THREAD_TYPE=wx.wxNewEventType()

    def connect(win):
        """Connects the thread event of win with the onThread method.

        Make sure the onThread method is defined for this dialog win.
        """
        win.Connect(-1, -1, wxEVT_THREAD_TYPE, win.onThread)

    class Event(wx.wxPyEvent):
        """Makes an event of the wxEVT_THREAD_TYPE."""
        def __init__(self, **keywords):
            self.__dict__=keywords
            wx.wxPyEvent.__init__(self)
            self.SetEventType(wxEVT_THREAD_TYPE)

    def postEvent(win,**keywords):
        """Post an event from within a thread to the dialog win."""
        wx.wxPostEvent(win,Event(**keywords))

####CONSTANTS-------------------------------------------------------------------
SHUTDOWN=0

####TESTS-----------------------------------------------------------------------
def __test__():
    def anyFunction(x):
        print string.rjust(' ',x+1),'Start thread',x
        time.sleep(random.randint(0,5))
        print string.rjust(' ',x+1),'End thread',x
        return x
    import random,string
    pool=Pool(5)
    for i in range(10):pool.put(anyFunction,i)
    pool.run()
    print '\ninput\t',range(10)
    print 'output\t',pool.getAll()
    pool.shutdown()

if __name__ == '__main__':__test__()
