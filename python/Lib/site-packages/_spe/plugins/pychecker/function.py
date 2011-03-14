#!/usr/bin/env python

# Copyright (c) 2001-2002, MetaSlash Inc.  All rights reserved.

"""
Object to hold information about functions.
Also contain a pseudo Python function object
"""

import string

_ARGS_ARGS_FLAG = 4
_KW_ARGS_FLAG = 8
_CO_FLAGS_MASK = _ARGS_ARGS_FLAG + _KW_ARGS_FLAG

class _ReturnValues:
    def __init__(self):
        self.returnValues = None

    def returnsNoValue(self):
        returnValues = self.returnValues
        # if unset, we don't know
        if returnValues is None:
            return 0
        # it's an empty list, that means no values
        if not returnValues:
            return 1
        # make sure each value is not None
        for rv in returnValues:
            if not rv[1].isNone():
                return 0
        return returnValues[-1][1].isImplicitNone()

class FakeCode :
    "This is a holder class for code objects (so we can modify them)"
    def __init__(self, code, varnames = None) :
        for attr in dir(code):
            try:
                setattr(self, attr, getattr(code, attr))
            except:
                pass
        if varnames is not None:
            self.co_varnames = varnames

class FakeFunction(_ReturnValues):
    "This is a holder class for turning code at module level into a function"

    def __init__(self, name, code, func_globals = {}, varnames = None) :
        _ReturnValues.__init__(self)
        self.func_name = self.__name__ = name
        self.func_doc  = self.__doc__  = "ignore"

        self.func_code = FakeCode(code, varnames)
        self.func_defaults = None
        self.func_globals = func_globals

    def __str__(self):
        return self.func_name

    def __repr__(self):
        return '%s from %s' % (self.func_name, self.func_code.co_filename)

class Function(_ReturnValues):
    "Class to hold all information about a function"

    def __init__(self, function, isMethod=0):
        _ReturnValues.__init__(self)
        self.function = function
        self.isMethod = isMethod
        self.minArgs = self.maxArgs = function.func_code.co_argcount
        if function.func_defaults is not None :
            self.minArgs = self.minArgs - len(function.func_defaults)
        # if function uses *args, there is no max # args
        try:
            if function.func_code.co_flags & _ARGS_ARGS_FLAG != 0 :
                self.maxArgs = None
            self.supportsKW = function.func_code.co_flags & _KW_ARGS_FLAG
        except AttributeError:
            # this happens w/Zope
            self.supportsKW = 0

    def __str__(self):
        return self.function.func_name

    def __repr__(self):
        return '%s from %s:%d' % (self.function.func_name,
                                  self.function.func_code.co_filename,
                                  self.function.func_code.co_firstlineno)

    def arguments(self) :
        numArgs = self.function.func_code.co_argcount
        if self.maxArgs is None :
            numArgs = numArgs + 1
        if self.supportsKW :
            numArgs = numArgs + 1
        return self.function.func_code.co_varnames[:numArgs]
        
    def isParam(self, name) :
        return name in self.arguments()

    def isStaticMethod(self):
        return self.isMethod and isinstance(self.function, type(create_fake))

    def isClassMethod(self):
        try:
            return self.isMethod and self.function.im_self is not None
        except AttributeError:
            return 0

    def defaultValue(self, name) :
        func_code = self.function.func_code
        arg_names = list(func_code.co_varnames[:func_code.co_argcount])
        i = arg_names.index(name)
        if i < self.minArgs :
            raise ValueError
        return self.function.func_defaults[i - self.minArgs]

    def varArgName(self) :
        if self.maxArgs is not None :
            return None
        func_code = self.function.func_code
        return func_code.co_varnames[func_code.co_argcount]

def create_fake(name, code, func_globals = {}, varnames = None) :
    return Function(FakeFunction(name, code, func_globals, varnames))

def create_from_file(file, filename, module) :
    # Make sure the file is at the beginning
    #   if python compiled the file, it will be at the end
    file.seek(0)

    # Read in the source file, see py_compile.compile() for games w/src str
    codestr = file.read()
    codestr = string.replace(codestr, "\r\n", "\n")
    codestr = string.replace(codestr, "\r", "\n")
    if codestr and codestr[-1] != '\n' :
        codestr = codestr + '\n'
    code = compile(codestr, filename, 'exec')
    return Function(FakeFunction('__main__', code, module.__dict__))

def _co_flags_equal(o1, o2) :
    return (o1.co_flags & _CO_FLAGS_MASK) == (o2.co_flags & _CO_FLAGS_MASK)
    
def same_signature(func, object) :
    '''Return a boolean value if the <func> has the same signature as
       a function with the same name in <object> (ie, an overriden method)'''

    try :
        baseMethod = getattr(object, func.func_name)
        base_func_code = baseMethod.im_func.func_code
    except AttributeError :
        return 1

    return _co_flags_equal(base_func_code, func.func_code) and \
           base_func_code.co_argcount == func.func_code.co_argcount

