
from pychecker2.Warning import Warning
from pychecker2.File import File
from pychecker2 import Options

import time
import os
import stat

class WarningOpt(Options.BoolOpt):
    __pychecker__ = 'no-callinit'
    
    def __init__(self, longName, warning):
        self.warning = warning
        self.longName = longName
        self.default = warning.value

    def set_value(self, unused):
        self.warning.value = not self.warning.value
        
    def get_value(self):
        return self.warning.value

    def get_description(self):
        return self.warning.description

    def reset(self):
        self.warning.value = self.default

class CheckList:

    def __init__(self, checks):
        self.checks = checks
        self.modules = {}

    def check_file(self, f):
        for c in self.checks:
            c.check(f, self)

    def check_module(self, m):
        f = None
        try:
            f = self.modules[m]
        except KeyError:
            import inspect
            try:
                fname = inspect.getsourcefile(m)
                if fname:
                    f = File(fname)
            except TypeError:
                pass
            self.modules[m] = f
            if f:
                self.check_file(f)
        return f

    def __getstate__(self):
        modules = []
        for k, v in self.modules.items():
            modules.append( (k.__name__, v) )
        return (time.time(), self.checks, modules)

    def __setstate__(self, data):
        import inspect
        cache_time, self.checks, modules = data
        self.modules = {}
        for k, v in modules:
            module = __import__(k, globals(), {}, [''])
            # Don't recover files that are newer than the save time
            try:
                fname = inspect.getsourcefile(module)
                last_modified = os.stat(fname)[stat.ST_MTIME]
            except TypeError:
                last_modified = 0

            if last_modified < cache_time:
                self.modules[module] = v
                
class Check:

    def __str__(self):
        return self.__class__.__name__

    def get_warnings(self, options):
        for attr in vars(self.__class__):
            object = getattr(self, attr)
            if isinstance(object, Warning):
                options.add(WarningOpt(attr, object))
    
    def get_options(self, options):
        pass
    
    def check(self, file, checker):
        pass


