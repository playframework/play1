#!/usr/bin/env python

# Copyright (c) 2001-2004, MetaSlash Inc.  All rights reserved.
# Portions Copyright (c) 2005, Google, Inc.  All rights reserved.

"""
Warning Messages for PyChecker
"""

import UserString

class WarningClass:
  level = 0

  def __init__(self, msg):
    self.msg = msg

  def __mod__(self, args):
    result = UserString.UserString(self.msg % args)
    result.level = self.level
    return result

  def __str__(self):
    return self.msg

class Internal(WarningClass):
  level = 100

class Error(WarningClass):
  level = 90

class Security(WarningClass):
  level = 90

class Warning(WarningClass):
  level = 70

class Unused(WarningClass):
  level = 50

class Deprecated(WarningClass):
  level = 40

class Style(WarningClass):
  level = 10

TOO_MANY_WARNINGS = WarningClass("%d errors suppressed, use -#/--limit to increase the number of errors displayed")
CHECKER_BROKEN = Internal("INTERNAL ERROR -- STOPPED PROCESSING FUNCTION --\n\t%s")
INVALID_CHECKER_ARGS = Internal("Invalid warning suppression arguments --\n\t%s")

NO_MODULE_DOC = Style("No module doc string")
NO_CLASS_DOC = Style("No doc string for class %s")
NO_FUNC_DOC = Style("No doc string for function %s")

VAR_NOT_USED = Unused("Variable (%s) not used")
IMPORT_NOT_USED = Unused("Imported module (%s) not used")
UNUSED_LOCAL = Unused("Local variable (%s) not used")
UNUSED_PARAMETER = Unused("Parameter (%s) not used")
UNUSED_MEMBERS = Unused("Members (%s) not used in class (%s)")
NO_LOCAL_VAR = Unused("No local variable (%s)")
VAR_USED_BEFORE_SET = Warning("Variable (%s) used before being set")

REDEFINING_ATTR = Warning("Redefining attribute (%s) original line (%d)")

MODULE_IMPORTED_AGAIN = Warning("Module (%s) re-imported")
MODULE_MEMBER_IMPORTED_AGAIN = Warning("Module member (%s) re-imported")
MODULE_MEMBER_ALSO_STAR_IMPORTED = Warning("Module member (%s) re-imported with *")
MIX_IMPORT_AND_FROM_IMPORT = Warning("Using import and from ... import for (%s)")
IMPORT_SELF = Warning("Module (%s) imports itself")

NO_METHOD_ARGS = Error("No method arguments, should have %s as argument")
SELF_NOT_FIRST_ARG = Error("%s is not first %smethod argument")
SELF_IS_ARG = Error("self is argument in %s")
RETURN_FROM_INIT = Error("Cannot return a value from __init__")
NO_CTOR_ARGS = Error("Instantiating an object with arguments, but no constructor")

GLOBAL_DEFINED_NOT_DECLARED = Warning("Global variable (%s) not defined in module scope")
INVALID_GLOBAL = Error("No global (%s) found")
INVALID_METHOD = Error("No method (%s) found")
INVALID_CLASS_ATTR = Warning("No class attribute (%s) found")
INVALID_SET_CLASS_ATTR = Warning("Setting class attribute (%s) not set in __init__")
INVALID_MODULE_ATTR = Error("No module attribute (%s) found")

LOCAL_SHADOWS_GLOBAL = Warning("Local variable (%s) shadows global defined on line %d")
VARIABLE_SHADOWS_BUILTIN = Warning("(%s) shadows builtin")
USING_METHOD_AS_ATTR = Warning("Using method (%s) as an attribute (not invoked)")
OBJECT_HAS_NO_ATTR = Warning("Object (%s) has no attribute (%s)")
METHOD_SIGNATURE_MISMATCH = Warning("Overridden method (%s) doesn't match signature in class (%s)")

INVALID_ARG_COUNT1 = Error("Invalid arguments to (%s), got %d, expected %d")
INVALID_ARG_COUNT2 = Error("Invalid arguments to (%s), got %d, expected at least %d")
INVALID_ARG_COUNT3 = Error("Invalid arguments to (%s), got %d, expected between %d and %d")
FUNC_DOESNT_SUPPORT_KW = Error("Function (%s) doesn't support **kwArgs")
FUNC_DOESNT_SUPPORT_KW_ARG = Error("Function (%s) doesn't support **kwArgs for name (%s)")
FUNC_USES_NAMED_ARGS = Warning("Function (%s) uses named arguments")

BASE_CLASS_NOT_INIT = Warning("Base class (%s) __init__() not called")
NO_INIT_IN_SUBCLASS = Warning("No __init__() in subclass (%s)")
METHODS_NEED_OVERRIDE = Error("Methods (%s) in %s need to be overridden in a subclass")

FUNC_TOO_LONG = Style("Function (%s) has too many lines (%d)")
TOO_MANY_BRANCHES = Style("Function (%s) has too many branches (%d)")
TOO_MANY_RETURNS = Style("Function (%s) has too many returns (%d)")
TOO_MANY_ARGS = Style("Function (%s) has too many arguments (%d)")
TOO_MANY_LOCALS = Style("Function (%s) has too many local variables (%d)")
TOO_MANY_REFERENCES = Style('Law of Demeter violated, more than %d references for (%s)')

IMPLICIT_AND_EXPLICIT_RETURNS = Warning("Function returns a value and also implicitly returns None")
INCONSISTENT_RETURN_TYPE = Warning("Function return types are inconsistent")
INCONSISTENT_TYPE = Warning("Variable (%s) already has types %s and set to %s")
CODE_UNREACHABLE = Error("Code appears to be unreachable")
CONSTANT_CONDITION = Warning("Using a conditional statement with a constant value (%s)")
STRING_ITERATION = Warning("Iterating over a string (%s)")
DONT_RETURN_NONE = Error("%s should not return None, raise an exception if not found")
IS_LITERAL = Warning("Using is%s %s, may not always work")
INVALID_FORMAT = Error("Invalid format string, problem starts near: '%s'")
INVALID_FORMAT_COUNT = Error("Format string argument count (%d) doesn't match arguments (%d)")
TOO_MANY_STARS_IN_FORMAT = Error("Too many *s in format flags")
USING_STAR_IN_FORMAT_MAPPING = Error("Can't use * in formats when using a mapping (dictionary), near: '%s'")
CANT_MIX_MAPPING_IN_FORMATS = Error("Can't mix tuple/mapping (dictionary) formats in same format string")

INTEGER_DIVISION = Warning("Using integer division (%s / %s) may return integer or float")
MODULO_1 = Warning("... % 1 may be constant")
USING_TUPLE_ACCESS_TO_LIST = Error("Using a tuple instead of slice as list accessor for (%s)")
BOOL_COMPARE = Warning("Comparisons with %s are not necessary and may not work as expected")
SHOULDNT_ASSIGN_BUILTIN = Deprecated("Should not assign to %s, it is (or will be) a builtin")
SHOULDNT_ASSIGN_NAME = Deprecated("Should not assign to %s, it is similar to builtin %s")
SET_VAR_TO_ITSELF = Warning("Setting %s to itself has no effect")
MODIFY_VAR_NOOP = Warning("%s %s %s has no effect")
DIVIDE_VAR_BY_ITSELF = Warning("%s %s %s is always 1 or ZeroDivisionError")
XOR_VAR_WITH_ITSELF = Warning("%s %s %s is always 0")

STMT_WITH_NO_EFFECT = Error("Operator (%s) doesn't exist, statement has no effect")
POSSIBLE_STMT_WITH_NO_EFFECT = Error("Statement appears to have no effect")
UNARY_POSITIVE_HAS_NO_EFFECT = Error("Unary positive (+) usually has no effect")
LIST_APPEND_ARGS = Error("[].append() only takes 1 argument in Python 1.6 and above for (%s)")

LOCAL_DELETED = Error("(%s) cannot be used after being deleted on line %d")
LOCAL_ALREADY_DELETED = Error("Local variable (%s) has already been deleted on line %d")
VAR_DELETED_BEFORE_SET = Error("Variable (%s) deleted before being set")

CATCH_BAD_EXCEPTION = Warning("Catching a non-Exception object (%s)")
CATCH_STR_EXCEPTION = Deprecated("Catching string exceptions are deprecated (%s)")
RAISE_BAD_EXCEPTION = Warning("Raising an exception on a non-Exception object (%s)")
RAISE_STR_EXCEPTION = Deprecated("Raising string exceptions are deprecated (%s)")
SET_EXCEPT_TO_BUILTIN = Error("Setting exception to builtin (%s), consider () around exceptions")
USING_KEYWORD = Warning("Using identifier (%s) which will become a keyword in version %s")
MODIFYING_DEFAULT_ARG = Warning("Modifying parameter (%s) with a default value may have unexpected consequences")
USING_SELF_IN_REPR = Warning("Using `self` in __repr__ method")
USING_NONE_RETURN_VALUE = Error("Using the return value from (%s) which is always None")
WRONG_UNPACK_SIZE = Error("Unpacking %d values into %d variables")
WRONG_UNPACK_FUNCTION = Error("Unpacking function (%s) which returns %d values into %d variables")
UNPACK_NON_SEQUENCE = Error("Unpacking a non-sequence (%s) of type %s")

NOT_SPECIAL_METHOD = Warning("%s is not a special method")
USING_COERCE_IN_NEW_CLASS = Error("Using __coerce__ in new-style class (%s) will not work for binary operations")
USING_GETATTRIBUTE_IN_OLD_CLASS = Error("Using __getattribute__ in old-style class (%s) does not work")
USING_PROPERTIES_IN_CLASSIC_CLASS = Error("Using property (%s) in classic class %s may not work")
USING_SLOTS_IN_CLASSIC_CLASS = Error("Using __slots__ in classic class %s has no effect, consider deriving from object")
EMPTY_SLOTS = Warning("__slots__ are empty in %s")

USES_EXEC = Security("Using the exec statement")
USES_GLOBAL_EXEC = Security("Using the exec statement in global namespace")
USES_INPUT = Security("Using input() is a security problem, consider using raw_input()")

USING_DEPRECATED_MODULE = Deprecated("%s module is deprecated")
USING_DEPRECATED_ATTR = Deprecated("%s is deprecated")
USING_INSECURE_FUNC = Security("%s() is a security problem")
USE_INSTEAD = ", consider using %s"

USES_CONST_ATTR = Warning("Passing a constant string to %s, consider direct reference")

BAD_STRING_FIND = Error("string.find() returns an integer, consider checking >= 0 or < 0 for not found")
