#(c)www.stani.be (read __doc__ for more information)                            
import sm
INFO=sm.INFO.copy()

INFO['description']=\
"""General Python scripts."""

__doc__=INFO['doc']%INFO
#_______________________________________________________________________________

####IMPORT----------------------------------------------------------------------
import math,random,string,time

####CLASSES---------------------------------------------------------------------

class Keywords:
    """Filter out easily keywords from generic options list.
    
    See VideoControl for an example."""
    def filterKeywords(self,keyw,defaults):
        for key in defaults.keys():
            if keyw.has_key(key):
                self.__dict__[key] = keyw[key]
                del keyw[key]
            else:
                self.__dict__[key] = defaults[key]
            
class New:
    """Empty class of which properties can be set through keywords."""
    def __init__(self,**keywords):
        self.__dict__=keywords

class Str:
    """Use its own dictionary as a string representation."""
    def __str__(self):
        print self.__dict__()
        
class ValueRange:
    """Range class between minimum and max with features."""
    def __init__(self, minimum=0, maximum=None, step=1, random=0, lst = None):
        if lst:
            self.min = min(lst)
            self.max = max(lst)
        else:
            self.min    = minimum
            if max == None:
                self.max = self.min
            else:
                self.max    = maximum
        self.step   = step
        self.delta  = self.max-self.min
        #temp
        self._min   = self.min
        self._max   = self.max
        self._delta = self.delta
        
    def average(self):
        """Return the average of the range."""
        return (self._min+self._max)/2
     
    def choose(self):
        """Choose a random number out of the range."""
        if self._min==self._max:
            return self._min
        else:
            return random.randrange(self._min,self._max,self.step)
        
    def f2f(self,x):
        """fraction2rangeFloat"""
        return x*self._delta+self._min
        
    def f2r(self,x):
        """fraction2range"""
        return int(round(x*self._delta+self._min))
        
    def limit(self,n,min,max=None):
        """Limit/expand the scope of the range."""
        step        = float(self.delta)/n
        self._min   = int(round((min-1)*step))+self.min
        if max == None: max = min
        self._max   = int(round(max*step))+self.min
        self._delta = self._max-self._min
       
    def limitFraction(self,fraction=1,which='min',extra=None):
        #print self._min,self._max,fraction,self.delta*fraction
        if which == 'min':
            self._min=int(round(self.max-self.delta*fraction))
            if extra!=None: self._min = max(self._min,extra)
        else:
            self._max=int(round(self.min+self.delta*fraction))
            if extra!=None: self._max = min(self._max,extra)
        self._delta = self._max-self._min
        #print self._min,self._max,extra
        
    def r2f(self,x,invert=0):
        """range2fraction"""
        f = float(x-self._min)/self._delta
        if invert: f = 1-f
        return f
        
    def range(self, random = 0, backwards = 0):
        """Returns as a list with all values."""
        r = range(self._min,self._max+1,self.step)
        if random: random.shuffle(r)
        if backwards: reverse(r)
        return r
        
    def cycle(self,x):
        """Make sure a value fits within a range."""
        if x < self._min or x > self._max:
            return (x-self._min)%self._delta+self._min
        else:
            return x
        
class ValueRangeInOut:
    """Class to convert between value ranges."""
    def __init__(self,input,output):
        self.input      = input
        self.output     = output
        self.__call__   = self.i2o  
        
    def i2f(self,x):
        """in2out float"""
        return self.output.f2f(self.input.r2f(x))
        
    def i2o(self,x):
        """in2out"""
        return self.output.f2r(self.input.r2f(x))
        
    def o2f(self,x):
        """out2in float"""
        return self.input.f2f(self.output.r2f(x))
        
    def o2i(self,x):
        """out2in"""
        return self.input.f2r(self.output.r2f(x))
        

#---Stunt Class-----------------------------------------------------------------
APPEND='__www.stani.be__'#just an unique value
APPEND_METHODS=[]

class _StuntControlMethod:
    """Call with arguments and keywords.

    See Stunt class for more information.
    """
    def __init__(self,control,method,appendMethods=APPEND_METHODS):
        self.method=method
        self.appendMethods=appendMethods
        self.argKey=[]
    def __call__(self,*arguments,**keywords):
        try:
            appendArgument=(APPEND == arguments[-1])
        except:
            appendArgument=0
        if (self.method in self.appendMethods) or appendArgument:
            if appendArgument:
                arguments=arguments[:-1]
            self.argKey.append((arguments,keywords))
        else:self.argKey=[(arguments,keywords)]

class _StuntControl:
    """Buffer to register all calls to dialog control methods. Useful for threads.

    See Stunt class for more information.
    """
    def __init__(self,control):
        self.control=control
        self.methods={}
    def __getattr__(self,method):
        if public(method):
            if method not in self.methods.keys():
                self.methods[method]= _StuntControlMethod(self,method)
            return self.methods[method]
        else:return self.__dict__[method]
    def __call__(self,containerControl):
        for method,StuntMethod in self.methods.items():
            for arguments,keywords in StuntMethod.argKey:apply(getattr(containerControl,method),arguments,keywords)
        self.methods.clear()

class Stunt:
    """Buffer to register all calls to a dialog. Usefull for threads.

    These actions can be applied later through for example a dialog timer event.
    Example:
        >>> dialog=Stunt()
        >>> dialog.gauge.SetRange(100)
        >>> dialog.gauge.SetValue(25)
        >>> dialog.label.SetValue('Hello world')
        >>> dialog.controls
        {'gauge': <smNew._StuntControl instance at 0x0165BA18>, 'label': <smNew._StuntControl instance at 0x0165B980>}
        >>> dialog.controls['gauge'].methods
        {'SetValue': <smNew._StuntControlMethod instance at 0x0166FAF8>, 'SetRange': <smNew._StuntControlMethod instance at 0x0166F940>}
        >>> dialog.controls['gauge'].methods['SetValue'].argKey
        [((25,), {})]
        #>> dialog(wxDialog)
    """
    def __init__(self,container=None):
        self.controls={}
        self.__container=container
    def __getattr__(self,control):
        if public(control):
            if control not in self.controls.keys():
                self.controls[control]= _StuntControl(control)
            return self.controls[control]
        else:
            return self.__dict__[control]
    def __call__(self):
        "Apply Stunted methods of self to container."
        self.busy=1
        for control,StuntControl in self.controls.items():
            StuntControl(getattr(self.__container,control))
        self.controls.clear()
        self.busy=0
    def __nonzero__(self):
        return 1

####FUNCTIONS-------------------------------------------------------------------

def arange(start=0,stop=1,step=1):
    """Arbitrary range with floats"""
    return [x*step+start for x in range((stop-start)/step)]
    
def assertList(x):
    "Force x to a list."
    if type(x).__name__=='list': return x
    else: return [x]

def cgd(x,y):
    "Calculates common greatest denominator."
    max     = min(x,y)
    result  = 1
    d       = 2
    while d <= max:
        while x%d == 0 and y%d == 0:
            result  *= d
            x       /= d
            y       /=d
        d   +=1
    return result
    
def distance(p1,p2):
    "Calculates distance between two 2d points."
    px=math.fabs(p1[0]-p2[0])+1
    py=math.fabs(p1[1]-p2[1])+1
    return math.sqrt(px*px+py*py)

def flat(seq):
    """Flattens a sequence of sequences"""
    return [x for subseq in seq for x in subseq]

def flatten(s):
    "Flattens a list."
    result = []
    for i in s:
        try:
            result = result + flatten(i)
        except TypeError:
            result.append(i)
    return result

def fixHmsf(x,fps=25):
    """Make sure a number is in 'hh:mm:ss:ff' format."""
    return index2hmsf(hmsf2index(x,fps),fps)
    
def hmsf2index(x,fps=25):
    """Convert frame notation into frame number
    x='hh:mm:ss:ff' 
    """
    x=x.split(':')
    if x==['']:x=[]
    x=[0 for a in range(4-len(x))]+[int(b) for b in x]
    frame=((x[0]*60+x[1])*60+x[2])*fps+x[3]
    return frame
    
def index2hmsf(x,fps=25):
    """Convert frame notation into frame number
    returns 'hh:mm:ss:ff' 
    """
    return '%02i:%02i:%02i:%02i'%(x/(3600*fps),x/(60*fps)%60,x/fps%60,x%fps)
    
def irange(start=0,stop=1,step=1):
    """Arbitrary range with floats, inclusive endpoint"""
    return [x*step+start for x in range((stop-start)/step+1)]
    
def lrange(x):
    return range(len(x))
    
def limitRange(x,n):
    if n and len(x)>n:
        step = len(x)/float(n-2)
        return [x[0]]+[x[int(round(i*step)) + 1] for i in range(n-2)]+[x[-1]]
    else:
        return x
    
def minSec(seconds):
    "Converts seconds to minutes:seconds string."
    seconds=int(seconds)
    return str(seconds/60)+':'+str(seconds%60)

def public(x):
    "Returns true if string x doesn't start with '__'."
    return x[:2]!='__'

def ratio(x,y):
    d = cgd(x,y)
    return (x/d,y/d)
    
def rstrip(x,char=' '):
    try:
        return x.rstrip('-')
    except:
        index=len(x)
        try:
            while x>0 and x[index-1]==char:
                index-=1
            return x[:index]
        except:
            return x

def strFill(s,n):
    "Fills a string with n times the substring s."
    return ''.ljust(n).replace(' ',s)

def subtract(l,m):
    "Subtract list m from list l"
    def _notCommon(x):
        return not(x in m)
    return filter(_notCommon,l)

def timePassed(x):
    "Returns the time as a string in min and sec since x."
    return minSec(time.time()-x)

def transpose(x):
    return map(None,*x)
    
def unique(s):
    """Return a list of the elements in s, but without duplicates.

    For example, unique([1,2,3,1,2,3]) is some permutation of [1,2,3],
    unique("abcabc") some permutation of ["a", "b", "c"], and
    unique(([1, 2], [2, 3], [1, 2])) some permutation of
    [[2, 3], [1, 2]].

    For best speed, all sequence elements should be hashable.  Then
    unique() will usually work in linear time.

    If not possible, the sequence elements should enjoy a total
    ordering, and if list(s).sort() doesn't raise TypeError it's
    assumed that they do enjoy a total ordering.  Then unique() will
    usually work in O(N*log2(N)) time.

    If that's not possible either, the sequence elements must support
    equality-testing.  Then unique() will usually work in quadratic
    time.
    """

    n = len(s)
    if n == 0:
        return []

    # Try using a dict first, as that's the fastest and will usually
    # work.  If it doesn't work, it will usually fail quickly, so it
    # usually doesn't cost much to *try* it.  It requires that all the
    # sequence elements be hashable, and support equality comparison.
    u = {}
    try:
        for x in s:
            u[x] = 1
    except TypeError:
        del u  # move on to the next method
    else:
        return u.keys()

    # We can't hash all the elements.  Second fastest is to sort,
    # which brings the equal elements together; then duplicates are
    # easy to weed out in a single pass.
    # NOTE:  Python's list.sort() was designed to be efficient in the
    # presence of many duplicate elements.  This isn't true of all
    # sort functions in all languages or libraries, so this approach
    # is more effective in Python than it may be elsewhere.
    try:
        t = list(s)
        t.sort()
    except TypeError:
        del t  # move on to the next method
    else:
        assert n > 0
        last = t[0]
        lasti = i = 1
        while i < n:
            if t[i] != last:
                t[lasti] = last = t[i]
                lasti = lasti+1
            i = i+1
        return t[:lasti]

    # Brute force is all that's left.
    u = []
    for x in s:
        if x not in u:
            u.append(x)
    return u

def zfill(x,width):
    try:
        return string.zfill(x,width)
    except:
        x=str(x)
        l=len(x)
        if width>l:          
            return ('%0'+str(width-l)+'d%s')%(0,x)
        else:
            return x

####CONSTANTS-------------------------------------------------------------------
INCH2CM=2.54
CM2INCH=1/INCH2CM
MM2INCH=1/(INCH2CM*10)
MONTHS=('january','february','march','april','may','june','july','august','september','october','november','december')
