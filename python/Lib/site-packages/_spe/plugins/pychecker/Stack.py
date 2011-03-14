#!/usr/bin/env python

# Copyright (c) 2001-2002, MetaSlash Inc.  All rights reserved.

"""
Module to hold manipulation of elements on the stack.
"""

import types
from pychecker import utils

DATA_UNKNOWN = "-unknown-"
LOCALS = 'locals'

# These should really be defined by subclasses
TYPE_UNKNOWN = "-unknown-"
TYPE_FUNC_RETURN = "-return-value-"
TYPE_ATTRIBUTE = "-attribute-"
TYPE_COMPARISON = "-comparison-"
TYPE_GLOBAL = "-global-"
TYPE_EXCEPT = "-except-"

class Item :
    "Representation of data on the stack"

    def __init__(self, data, dataType, const = 0, length = 0) :
        self.data = data
        self.type = dataType
        self.const = const
        self.length = length
        self.is_really_string = 0

    def __str__(self) :
        if type(self.data) == types.TupleType :
            value = '('
            for item in self.data :
                value = value + utils.safestr(item) + ', '
            # strip off the ', ' for multiple items
            if len(self.data) > 1 :
                value = value[:-2]
            return value + ')'
        return utils.safestr(self.data)

    def __repr__(self):
        return 'Stack Item: (%s, %s, %d)' % (self.data, self.type, self.const)

    def isNone(self) :
        return (self.type != TYPE_UNKNOWN and self.data is None or
                (self.data == 'None' and not self.const))

    def isImplicitNone(self) :
        return self.data is None and self.const

    def isMethodCall(self, c, methodArgName):
        return self.type == TYPE_ATTRIBUTE and c != None and \
               len(self.data) == 2 and self.data[0] == methodArgName

    def isLocals(self) :
        return self.type == types.DictType and self.data == LOCALS

    def setStringType(self, value = types.StringType):
        self.is_really_string = value == types.StringType

    def getType(self, typeMap) :
        if self.type != types.StringType or self.is_really_string:
            return self.type
        if self.const :
            return type(self.data)
        if type(self.data) == types.StringType :
            localTypes = typeMap.get(self.data, [])
            if len(localTypes) == 1 :
                return localTypes[0]
        return TYPE_UNKNOWN

    def getName(self) :
        if self.type == TYPE_ATTRIBUTE and type(self.data) != types.StringType:
            strValue = ""
            # convert the tuple into a string ('self', 'data') -> self.data
            for item in self.data :
                strValue = '%s.%s' % (strValue, utils.safestr(item))
            return strValue[1:]
        return utils.safestr(self.data)

    def addAttribute(self, attr) :
        if type(self.data) == types.TupleType :
            self.data = self.data + (attr,)
        else :
            self.data = (self.data, attr)
        self.type = TYPE_ATTRIBUTE


def makeDict(values = (), const = 1) :
    values = tuple(values)
    if not values:
        values = ('<on-stack>',)
    return Item(values, types.DictType, const, len(values))

def makeTuple(values = (), const = 1) :
    return Item(tuple(values), types.TupleType, const, len(values))

def makeList(values = [], const = 1) :
    return Item(values, types.ListType, const, len(values))

def makeFuncReturnValue(stackValue, argCount) :
    data = DATA_UNKNOWN
    # vars() without params == locals()
    if stackValue.type == TYPE_GLOBAL and \
       (stackValue.data == LOCALS or
        (argCount == 0 and stackValue.data == 'vars')) :
        data = LOCALS
    return Item(data, TYPE_FUNC_RETURN)

def makeComparison(stackItems, comparison) :
    return Item((stackItems[0], comparison, stackItems[1]), TYPE_COMPARISON)

