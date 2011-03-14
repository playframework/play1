#!/usr/bin/env python

# Copyright (c) 2001-2004, MetaSlash Inc.  All rights reserved.

"""
Print out warnings from Python source files.
"""

import sys
import string
import copy

from pychecker import msgs
from pychecker import Config
from pychecker.Warning import Warning


VAR_ARGS_BITS = 8
MAX_ARGS_MASK = ((1 << VAR_ARGS_BITS) - 1)

INIT = '__init__'
LAMBDA = '<lambda>'

# number of instructions to check backwards if it was a return
BACK_RETURN_INDEX = 4


_cfg = []

def cfg() :
    return _cfg[-1]

def initConfig(cfg) :
    _cfg.append(cfg)

def pushConfig() :
    newCfg = copy.copy(cfg())
    _cfg.append(newCfg)

def popConfig() :
    del _cfg[-1]


def shouldUpdateArgs(operand) :
    return operand == Config.CHECKER_VAR

def updateCheckerArgs(argStr, func, lastLineNum, warnings) :
    try :
        argList = string.split(argStr)
        # don't require long options to start w/--, we can add that for them
        for i in range(0, len(argList)) :
            if argList[i][0] != '-' :
                argList[i] = '--' + argList[i]

        cfg().processArgs(argList)
        return 1
    except Config.UsageError, detail :
        warn = Warning(func, lastLineNum, msgs.INVALID_CHECKER_ARGS % detail)
        warnings.append(warn)
        return 0
                       

def debug(*args) :
    if cfg().debug: print args


PYTHON_1_5 = 0x10502
PYTHON_2_0 = 0x20000
PYTHON_2_1 = 0x20100
PYTHON_2_2 = 0x20200
PYTHON_2_3 = 0x20300
PYTHON_2_4 = 0x20400

def pythonVersion() :
    return sys.hexversion >> 8

def startswith(s, substr) :
    "Ugh, supporting python 1.5 is a pain"
    return s[0:len(substr)] == substr

def endswith(s, substr) :
    "Ugh, supporting python 1.5 is a pain"
    return s[-len(substr):] == substr


# generic method that can be slapped into any class, thus the self parameter
def std_repr(self) :
    return "<%s at 0x%x: %s>" % (self.__class__.__name__, id(self), safestr(self))

try:
    unicode, UnicodeError
except NameError:
    class UnicodeError(Exception): pass

def safestr(value):
   try:
      return str(value)
   except UnicodeError:
      return unicode(value)
