#!/usr/bin/env python

# Copyright (c) 2001-2004, MetaSlash Inc.  All rights reserved.
# Portions Copyright (c) 2005, Google, Inc.  All rights reserved.

"""
Python byte code operations.

Very similar to the dis module, but dis does not exist in Jython,
so recreate the small portion we need here.
"""

def LINE_NUM(op):              return op == 127
def LOAD_GLOBAL(op):           return op == 116
def LOAD_CONST(op):            return op == 100
def LOAD_FAST(op):             return op == 124
def LOAD_ATTR(op):             return op == 105
def LOAD_DEREF(op):            return op == 136
def STORE_ATTR(op):            return op == 95
def POP_TOP(op):               return op == 1
def IMPORT_FROM(op):           return op == 108
def IMPORT_STAR(op):           return op == 84
def UNARY_POSITIVE(op):        return op == 10
def UNARY_NEGATIVE(op):        return op == 11
def UNARY_INVERT(op):          return op == 15
def RETURN_VALUE(op):          return op == 83
def JUMP_FORWARD(op):          return op == 110
def JUMP_ABSOLUTE(op):         return op == 113
def FOR_ITER(op):              return op == 93
def FOR_LOOP(op):              return op == 114
def SETUP_LOOP(op):            return op == 120
def BREAK_LOOP(op):            return op == 80
def RAISE_VARARGS(op):         return op == 130
def POP_BLOCK(op):             return op == 87
def END_FINALLY(op):           return op == 88
def CALL_FUNCTION(op):         return op == 131

def UNPACK_SEQUENCE(op) :
    "Deal w/Python 1.5.2 (UNPACK_[LIST|TUPLE]) or 2.0 (UNPACK_SEQUENCE)"
    return op in (92, 93,)

def IS_CONDITIONAL_JUMP(op):
    return op in (111, 112)

def IS_NOT(op):
    return op == 12

HAVE_ARGUMENT = 90
EXTENDED_ARG = 143

_HAS_NAME = (90, 91, 95, 96, 97, 98, 101, 105, 107, 108, 116,)
_HAS_LOCAL = (124, 125, 126,)
_HAS_CONST = (100,)
_HAS_COMPARE = (106,)
_HAS_JREL = (110, 111, 112, 114, 120, 121, 122,)
_HAS_JABS = (113, 119,)

_CMP_OP =  ('<', '<=', '==', '!=', '>', '>=', 'in', 'not in', 'is',
            'is not', 'exception match', 'BAD')

EXCEPT_COMPARISON = 10
IS_COMPARISON = 8

def getOperand(op, func_code, oparg) :
    if op in _HAS_NAME :
        return func_code.co_names[oparg]
    elif op in _HAS_LOCAL :
        return func_code.co_varnames[oparg]
    elif op in _HAS_CONST :
        return func_code.co_consts[oparg]
    elif op in _HAS_COMPARE :
        return _CMP_OP[oparg]
    return None

def getLabel(op, oparg, i) :
    if op in _HAS_JREL :
        return i + oparg
    elif op in _HAS_JABS :
        return oparg
    return None

def getInfo(code, index, extended_arg) :
    """Returns (op, oparg, index, extended_arg) based on code
       this is a helper function while looping through byte code,
       refer to the standard module dis.disassemble() for more info"""

    # get the operation we are performing
    op = ord(code[index])
    index = index + 1
    if op >= HAVE_ARGUMENT :
        # get the argument to the operation
        oparg = ord(code[index]) + ord(code[index+1])*256 + extended_arg
        index = index + 2
        extended_arg = 0
        if op == EXTENDED_ARG :
            extended_arg = oparg * 65536L
    else :
        oparg, extended_arg = 0, 0
    return op, oparg, index, extended_arg

def initFuncCode(func) :
    """Returns (func_code, code, i, maxCode, extended_arg) based on func,
       this is a helper function to setup looping through byte code"""

    func_code = func.func_code
    code = func_code.co_code
    return func_code, code, 0, len(code), 0

def conditional(op):
    "returns true if the code results in conditional execution"
    return op in [83,                   # return
                  93,                   # for_iter
                  111, 112, 114,        # conditional jump
                  121,                  # setup_exec
                  130                   # raise_varargs
                  ]

# this code is here for debugging purposes.
# Jython doesn't support dis, so don't rely on it
try :
    import dis
    name = dis.opname
except ImportError :
    class Name:
        'Turn name[x] into x'
        def __getitem__(self, x):
            from pychecker import utils
            return utils.safestr(x)
    name = Name()
