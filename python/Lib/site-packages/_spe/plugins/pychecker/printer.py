#!/usr/bin/env python

# Copyright (c) 2001, MetaSlash Inc.  All rights reserved.

"Helper functions for printing out info about objects"

from pychecker import utils

def printFunction(spaces, prefix, func, className = None) :
    params = ''
    argcount = func.func_code.co_argcount
    defaultArgStart = argcount
    if func.func_defaults != None :
        defaultArgStart = argcount - len(func.func_defaults)
    for i in range(0, argcount) :
        arg = func.func_code.co_varnames[i]
        if i >= defaultArgStart :
            arg = arg + " = %s" % utils.safestr(func.func_defaults[i - defaultArgStart])
        params = params + "%s, " % arg
    params = "(%s)" % params[:-2]
    if className == None :
        className = ""
    else :
        className = className + "."
    print "%s%s%s%s%s" % (spaces, prefix, className, func.func_name, params)


def module(module) :
    print "Module: ", module.moduleName
    if module.module == None :
        return

    print "  Imports:  ", module.modules.keys()
    print "  Variables:", module.variables.keys()
    print ""
    for function in module.functions.values() :
        printFunction("  ", "Function:  ", function.function)
    print ""
    for c in module.classes.values() :
        for method in c.methods.values() :
            if method != None :
                printFunction("  ", "", method.function, c.name)
        print ""

def attrs(object) :
    for attr in dir(object) :
        print " %s: %s" % (attr, `getattr(object, attr)`)
