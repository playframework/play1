#!/usr/bin/env python

# Copyright (c) 2001-2004, MetaSlash Inc.  All rights reserved.
# Portions Copyright (c) 2005, Google, Inc. All rights reserved.

"""
Setup a lot of info about Python builtin types, functions, methods, etc.
"""

import types
import sys

from pychecker import utils
from pychecker import Stack
from pychecker import Warning


BOOL = types.IntType
#                    name   (type,  args: min, max, kwArgs?
GLOBAL_FUNC_INFO = { '__import__': (types.ModuleType, 1, 4),
                     'abs': (Stack.TYPE_UNKNOWN, 1, 1),
                     'apply': (Stack.TYPE_UNKNOWN, 2, 3),
                     'buffer': (types.BufferType, 1, 3),
                     'callable': (BOOL, 1, 1),
                     'chr': (types.StringType, 1, 1),
                     'cmp': (types.IntType, 2, 2),
                     'coerce': ([ types.NoneType, types.TupleType ], 2, 2),
                     'compile': (types.CodeType, 3, 3),
                     'complex': (types.ComplexType, 1, 2, ['real', 'imag']),
                     'delattr': (types.NoneType, 2, 2),
                     'dir': (types.ListType, 0, 1),
                     'divmod': (types.TupleType, 2, 2),
                     'eval': (Stack.TYPE_UNKNOWN, 1, 3),
                     'execfile': (types.NoneType, 1, 3),
                     'filter': (types.ListType, 2, 2),
                     'float': (types.FloatType, 1, 1),
                     'getattr': (Stack.TYPE_UNKNOWN, 2, 3),
                     'globals': (types.DictType, 0, 0),
                     'hasattr': (BOOL, 2, 2),
                     'hash': (types.IntType, 1, 1),
                     'hex': (types.StringType, 1, 1),
                     'id': (types.IntType, 1, 1),
                     'input': (Stack.TYPE_UNKNOWN, 0, 1),
                     'int': (types.IntType, 1, 2, ['x']),
                     'intern': (types.StringType, 1, 1),
                     'isinstance': (BOOL, 2, 2),
                     'issubclass': (BOOL, 2, 2),
                     'len': (types.IntType, 1, 1),
                     'list': (types.ListType, 1, 1, ['sequence']),
                     'locals': (types.DictType, 0, 0),
                     'long': (types.LongType, 1, 2, ['x']),
                     'map': (types.ListType, 2, None),
                     'max': (Stack.TYPE_UNKNOWN, 1, None),
                     'min': (Stack.TYPE_UNKNOWN, 1, None),
                     'oct': (types.StringType, 1, 1),
                     'open': (types.FileType, 1, 3, ['name', 'mode', 'buffering']),
                     'ord': (types.IntType, 1, 1),
                     'pow': (Stack.TYPE_UNKNOWN, 2, 3),
                     'range': (types.ListType, 1, 3),
                     'raw_input': (types.StringType, 0, 1),
                     'reduce': (Stack.TYPE_UNKNOWN, 2, 3),
                     'reload': (types.ModuleType, 1, 1),
                     'repr': (types.StringType, 1, 1),
                     'round': (types.FloatType, 1, 2),
                     'setattr': (types.NoneType, 3, 3),
                     'slice': (types.SliceType, 1, 3),
                     'str': (types.StringType, 1, 1),
                     'tuple': (types.TupleType, 1, 1),
                     'type': (types.TypeType, 1, 1),
                     'vars': (types.DictType, 0, 1),
                     'xrange': (types.ListType, 1, 3),
                   }

if hasattr(types, 'UnicodeType') :
    GLOBAL_FUNC_INFO['unichr'] = (types.UnicodeType, 1, 1)
    GLOBAL_FUNC_INFO['unicode'] = (types.UnicodeType, 1, 3, ['string', 'encoding', 'errors'])

if utils.pythonVersion() >= utils.PYTHON_2_2 :
    GLOBAL_FUNC_INFO['compile'] = (types.CodeType, 3, 5)
    GLOBAL_FUNC_INFO['dict'] = (types.DictType, 0, 1, ['items'])
    GLOBAL_FUNC_INFO['file'] = GLOBAL_FUNC_INFO['open']
    GLOBAL_FUNC_INFO['float'] = (types.FloatType, 0, 1, ['x'])
    GLOBAL_FUNC_INFO['int'] = (types.IntType, 0, 2, ['x'])
    GLOBAL_FUNC_INFO['list'] = (types.ListType, 0, 1, ['sequence'])
    GLOBAL_FUNC_INFO['long'] = (types.LongType, 0, 2, ['x'])
    GLOBAL_FUNC_INFO['str'] = (types.StringType, 0, 1, ['object'])
    # FIXME: type doesn't take 2 args, only 1 or 3
    GLOBAL_FUNC_INFO['type'] = (types.TypeType, 1, 3, ['name', 'bases', 'dict'])
    GLOBAL_FUNC_INFO['tuple'] = (types.TupleType, 0, 1, ['sequence'])

    GLOBAL_FUNC_INFO['classmethod'] = (types.MethodType, 1, 1)
    GLOBAL_FUNC_INFO['iter'] = (Stack.TYPE_UNKNOWN, 1, 2)
    GLOBAL_FUNC_INFO['property'] = (Stack.TYPE_UNKNOWN, 0, 4, ['fget', 'fset', 'fdel', 'doc'])
    GLOBAL_FUNC_INFO['super'] = (Stack.TYPE_UNKNOWN, 1, 2)
    GLOBAL_FUNC_INFO['staticmethod'] = (types.MethodType, 1, 1)
    GLOBAL_FUNC_INFO['unicode'] = (types.UnicodeType, 0, 3, ['string', 'encoding', 'errors'])

    GLOBAL_FUNC_INFO['bool'] = (BOOL, 1, 1, ['x'])

if utils.pythonVersion() >= utils.PYTHON_2_3:
    GLOBAL_FUNC_INFO['dict'] = (types.DictType, 0, 1, [])

def tryAddGlobal(name, *args):
    if globals().has_key(name):
        GLOBAL_FUNC_INFO[name] = args

zipMinArgs = 1
if utils.pythonVersion() >= utils.PYTHON_2_4:
    zipMinArgs = 0
    
tryAddGlobal('zip', types.ListType, zipMinArgs, None)

tryAddGlobal('enumerate', types.TupleType, 1, 1, ['sequence'])
# sum() could also return float/long
tryAddGlobal('sum', types.IntType, 1, 2, ['start'])
# sorted() and reversed() always return an iterator  (FIXME: support iterator)
tryAddGlobal('sorted', Stack.TYPE_UNKNOWN, 1, 1)
tryAddGlobal('reversed', Stack.TYPE_UNKNOWN, 1, 1)

_STRING_METHODS = { 'capitalize': (types.StringType, 0, 0),
                    'center': (types.StringType, 1, 1),
                    'count': (types.IntType, 1, 1),
                    'encode': (types.StringType, 0, 2),
                    'endswith': (BOOL, 1, 3),
                    'expandtabs': (types.StringType, 0, 1),
                    'find': (types.IntType, 1, 3),
                    'index': (types.IntType, 1, 3),
                    'isalnum': (BOOL, 0, 0),
                    'isalpha': (BOOL, 0, 0),
                    'isdigit': (BOOL, 0, 0),
                    'islower': (BOOL, 0, 0),
                    'isspace': (BOOL, 0, 0),
                    'istitle': (BOOL, 0, 0),
                    'isupper': (BOOL, 0, 0),
                    'join': (types.StringType, 1, 1),
                    'ljust': (types.StringType, 1, 1),
                    'lower': (types.StringType, 0, 0),
                    'lstrip': (types.StringType, 0, 0),
                    'replace': (types.StringType, 2, 3),
                    'rfind': (types.IntType, 1, 3),
                    'rindex': (types.IntType, 1, 3),
                    'rjust': (types.StringType, 1, 1),
                    'rstrip': (types.StringType, 0, 0),
                    'split': (types.ListType, 0, 2),
                    'splitlines': (types.ListType, 0, 1),
                    'startswith': (BOOL, 1, 3),
                    'strip': (types.StringType, 0, 0),
                    'swapcase': (types.StringType, 0, 0),
                    'title': (types.StringType, 0, 0),
                    'translate': (types.StringType, 1, 2),
                    'upper': (types.StringType, 0, 0),
                  }

if utils.pythonVersion() >= utils.PYTHON_2_2 :
    _STRING_METHODS['decode'] = (types.UnicodeType, 0, 2)

if utils.pythonVersion() >= utils.PYTHON_2_4:
    _STRING_METHODS['rsplit'] = (types.StringType, 0, 2)
    _STRING_METHODS['center'] = (types.StringType, 1, 2),
    _STRING_METHODS['ljust'] = (types.StringType, 1, 2),
    _STRING_METHODS['rjust'] = (types.StringType, 1, 2),

BUILTIN_METHODS = { types.DictType :
                    { 'clear': (types.NoneType, 0, 0),
                      'copy': (types.DictType, 0, 0),
                      'get': (Stack.TYPE_UNKNOWN, 1, 2),
                      'has_key': (BOOL, 1, 1),
                      'items': (types.ListType, 0, 0),
                      'keys': (types.ListType, 0, 0),
                      'popitem': (types.TupleType, 0, 0),
                      'setdefault': (Stack.TYPE_UNKNOWN, 1, 2),
                      'update': (types.NoneType, 1, 1),
                      'values': (types.ListType, 0, 0),
                    },
                    types.ListType :
                    { 'append': (types.NoneType, 1, 1),
                      'count': (types.IntType, 1, 1),
                      'extend': (types.NoneType, 1, 1),
                      'index': (types.IntType, 1, 1),
                      'insert': (types.NoneType, 2, 2),
                      'pop': (Stack.TYPE_UNKNOWN, 0, 1),
                      'remove': (types.NoneType, 1, 1),
                      'reverse': (types.NoneType, 0, 0),
                      'sort': (types.NoneType, 0, 1),
                    },
                    types.FileType :
                    { 'close': (types.NoneType, 0, 0),
                      'fileno': (types.IntType, 0, 0),
                      'flush': (types.NoneType, 0, 0),
                      'isatty': (BOOL, 0, 0),
                      'read': (types.StringType, 0, 1),
                      'readinto': (types.NoneType, 1, 1),
                      'readline': (types.StringType, 0, 1),
                      'readlines': (types.ListType, 0, 1),
                      'seek': (types.NoneType, 1, 2),
                      'tell': (types.IntType, 0, 0),
                      'truncate': (types.NoneType, 0, 1),
                      'write': (types.NoneType, 1, 1),
                      'writelines': (types.NoneType, 1, 1),
                      'xreadlines': (types.ListType, 0, 0),
                    },
                  }

if utils.pythonVersion() >= utils.PYTHON_2_4:
    kwargs = ['cmp', 'key', 'reverse']
    BUILTIN_METHODS[types.ListType]['sort'] =(types.NoneType, 0, 3, kwargs)

if hasattr({}, 'pop'):
    BUILTIN_METHODS[types.DictType]['pop'] = (Stack.TYPE_UNKNOWN, 1, 2)

def _setupBuiltinMethods() :
    if utils.pythonVersion() >= utils.PYTHON_2_2 :
        PY22_DICT_METHODS = { 'iteritems': (types.ListType, 0, 0),
                              'iterkeys': (types.ListType, 0, 0),
                              'itervalues': (types.ListType, 0, 0),
                            }

        BUILTIN_METHODS[types.DictType].update(PY22_DICT_METHODS)

    try :
        BUILTIN_METHODS[types.ComplexType] = \
                                  { 'conjugate': (types.ComplexType, 0, 0), }
    except AttributeError :
        pass

    if len(dir('')) > 0 :
        BUILTIN_METHODS[types.StringType] = _STRING_METHODS

    try :
        BUILTIN_METHODS[types.UnicodeType] = _STRING_METHODS
    except AttributeError :
        pass

_setupBuiltinMethods()

MUTABLE_TYPES = (types.ListType, types.DictType, types.InstanceType,)

# identifiers which will become a keyword in a future version
FUTURE_KEYWORDS = { 'yield': '2.2' }

METHODLESS_OBJECTS = { types.NoneType : None, types.IntType : None,
                       types.LongType : None, types.FloatType : None,
                       types.BufferType : None, types.TupleType : None,
                       types.EllipsisType : None,
                     }

def _setupBuiltinAttrs() :
    item = Stack.Item(None, None)
    BUILTIN_ATTRS[types.MethodType] = dir(item.__init__)
    del item

    if utils.pythonVersion() >= utils.PYTHON_2_2 :
        # FIXME: I'm sure more types need to be added here
        BUILTIN_ATTRS[types.StringType] = dir(''.__class__)
        BUILTIN_ATTRS[types.ListType] = dir([].__class__)
        BUILTIN_ATTRS[types.DictType] = dir({}.__class__)

    try :
        import warnings
        _MSG = "xrange object's 'start', 'stop' and 'step' attributes are deprecated"
        warnings.filterwarnings('ignore', _MSG)
        del warnings, _MSG
    except (ImportError, AssertionError):
        pass
    BUILTIN_ATTRS[types.XRangeType] = dir(xrange(0))

    try: BUILTIN_ATTRS[types.ComplexType] = dir(complex(0, 1))
    except: pass

    try: BUILTIN_ATTRS[types.UnicodeType] = dir(unicode(''))
    except: pass

    try: BUILTIN_ATTRS[types.CodeType] = dir(_setupBuiltinAttrs.func_code)
    except: pass

    try: BUILTIN_ATTRS[types.FileType] = dir(sys.__stdin__)
    except: pass

    try:
        raise TypeError
    except TypeError :
        try:
            tb = sys.exc_info()[2]
            BUILTIN_ATTRS[types.TracebackType] = dir(tb)
            BUILTIN_ATTRS[types.FrameType] = dir(tb.tb_frame)
        except:
            pass
        tb = None

BUILTIN_ATTRS = { types.StringType : dir(''),
                  types.TypeType : dir(type(type)),
                  types.ListType : dir([]),
                  types.DictType : dir({}),
                  types.FunctionType : dir(_setupBuiltinAttrs),
                  types.BuiltinFunctionType : dir(len),
                  types.BuiltinMethodType : dir([].append),
                  types.ClassType : dir(Stack.Item),
                  types.UnboundMethodType : dir(Stack.Item.__init__),
                  types.LambdaType : dir(lambda: None),
                  types.SliceType : dir(slice(0)),
                }

# have to setup the rest this way to support different versions of Python
_setupBuiltinAttrs()

PENDING_DEPRECATED_MODULES = { 'string': None, 'types': None,
                             }
DEPRECATED_MODULES = { 'FCNTL': 'fcntl', 'gopherlib': None,
                       'macfs': 'Carbon.File or Carbon.Folder',
                       'posixfile': 'fcntl', 'pre': None, 'regsub': 're',
                       'statcache': 'os.stat()',
                       'stringold': None, 'tzparse': None,
                       'TERMIOS': 'termios', 'whrandom':'random',
                       'xmllib': 'xml.sax',

                       # C Modules
                       'mpz': None, 'pcre': None, 'pypcre': None,
                       'rgbimg': None, 'strop': None, 'xreadlines': 'file',
                     }
DEPRECATED_ATTRS = { 'array.read': None, 'array.write': None,
                     'operator.isCallable': None,
                     'operator.sequenceIncludes': None,
                     'pty.master_open': None, 'pty.slave_open': None,
                     'random.stdgamma': 'random.gammavariate',
                     'rfc822.AddrlistClass': 'rfc822.AddressList',
                     'string.atof': None, 'string.atoi': None,
                     'string.atol': None, 'string.zfill': None,
                     'sys.exc_traceback': None, 'sys.exit_thread': None,
                     'tempfile.mktemp': None, 'tempfile.template': None,
                   }

# FIXME: can't check these right now, maybe later
DEPRECATED_METHODS = {
                       'email.Message.get_type': 'email.Message.get_content_type',
                       'email.Message.get_subtype': 'email.Message.get_content_subtype',
                       'email.Message.get_main_type': 'email.Message.get_content_maintype',
                       'htmllib.HTMLParser.do_nextid': None,
                       'pstats.Stats.ignore': None,
                       'random.Random.cunifvariate': None,
                       'random.Random.stdgamma': 'Random.gammavariate',
                     }

_OS_AND_POSIX_FUNCS = { 'tempnam': None, 'tmpnam': None }
SECURITY_FUNCS = { 'os' : _OS_AND_POSIX_FUNCS, 'posix': _OS_AND_POSIX_FUNCS }

SPECIAL_METHODS = {
    '__call__': None,                   # any number > 1
    '__cmp__': 2,
    '__coerce__': 2,
    '__contains__': 2,
    '__del__': 1,
    '__hash__': 1,
    '__iter__': 1,
    '__len__': 1,
    '__new__': None,			# new-style class constructor
    '__nonzero__': 1,
    '__reduce__': 1,

    '__hex__': 1,
    '__oct__': 1,
    '__repr__': 1,
    '__str__': 1,

    '__invert__': 1,	'__neg__': 1,	'__pos__': 1,     '__abs__': 1,     
    '__complex__': 1,	'__int__': 1,	'__long__': 1,    '__float__': 1,

    '__eq__': 2,	'__ne__': 2,
    '__ge__': 2,	'__gt__': 2,
    '__le__': 2,	'__lt__': 2,

    '__getattribute__': 2,	# only in new-style classes
    '__getattr__': 2,		'__setattr__': 3,	'__delattr__': 2,
    '__getitem__': 2,		'__setitem__': 3,	'__delitem__': 2,
    '__getslice__': 3,		'__setslice__': 4,	'__delslice__': 3,
    # getslice is deprecated

    '__add__': 2,	'__radd__': 2,		'__iadd__': 2,    
    '__sub__': 2,	'__rsub__': 2,		'__isub__': 2,
    '__mul__': 2,	'__rmul__': 2,		'__imul__': 2,    
    '__div__': 2,	'__rdiv__': 2,		'__idiv__': 2,
    #  __pow__: 2 or 3                           __ipow__: 2 or 3
    '__pow__': 3,	'__rpow__': 2,		'__ipow__': 3,
    '__truediv__': 2,	'__rtruediv__': 2,	'__itruediv__': 2,	
    '__floordiv__': 2,	'__rfloordiv__': 2,	'__ifloordiv__': 2,	
    '__mod__': 2,	'__rmod__': 2,		'__imod__': 2,    
    '__divmod__': 2,	'__rdivmod__': 2,	# no inplace op for divmod()

    '__lshift__': 2,	'__rlshift__': 2,	'__ilshift__': 2,
    '__rshift__': 2,	'__rrshift__': 2,	'__irshift__': 2, 

    '__and__': 2,	'__rand__': 2,		'__iand__': 2,
    '__xor__': 2,	'__rxor__': 2,		'__ixor__': 2,
    '__or__': 2,	'__ror__': 2,		'__ior__': 2,

    # these are related to pickling 
    '__getstate__': 1,		'__setstate__': 2,
    '__copy__': 1,		'__deepcopy__': 2,
    '__getinitargs__': 1,	
    }
