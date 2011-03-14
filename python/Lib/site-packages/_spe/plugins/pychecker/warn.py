#!/usr/bin/env python

# Copyright (c) 2001-2002, MetaSlash Inc.  All rights reserved.
# Portions Copyright (c) 2005, Google, Inc. All rights reserved.

"""
Print out warnings from Python source files.
"""

import os.path
import sys
import string
import types
import traceback
import imp
import re

from pychecker import OP
from pychecker import Stack
from pychecker import function
from pychecker import python

from pychecker import msgs
from pychecker import utils
from pychecker import CodeChecks
from pychecker.Warning import Warning


def cfg() :
    return utils.cfg()

def _checkSelfArg(method, warnings) :
    """Return a Warning if there is no self parameter or
       the first parameter to a method is not self."""

    if not cfg().methodArgName:
        return

    code = method.function.func_code
    err = None
    if method.isStaticMethod():
        if code.co_argcount > 0 and cfg().methodArgName == code.co_varnames[0]:
            err = msgs.SELF_IS_ARG % 'staticmethod'
    elif code.co_argcount < 1: 
        err = msgs.NO_METHOD_ARGS % cfg().methodArgName
    else:
        if method.isClassMethod():
            if code.co_varnames[0] not in cfg().classmethodArgNames:
                err = msgs.SELF_NOT_FIRST_ARG % \
                      (cfg().classmethodArgNames, 'class')
        elif code.co_varnames[0] != cfg().methodArgName:
            err = msgs.SELF_NOT_FIRST_ARG % (cfg().methodArgName, '')

    if err is not None :
        warnings.append(Warning(code, code, err))


def _checkNoSelfArg(func, warnings) :
    "Return a Warning if there is a self parameter to a function."

    code = func.function.func_code
    if code.co_argcount > 0 and cfg().methodArgName in code.co_varnames:
        warnings.append(Warning(code, code, msgs.SELF_IS_ARG % 'function'))


def _checkSubclass(c1, c2):
    try:
        return issubclass(c1.classObject, c2.classObject)
    except (TypeError, AttributeError):
        return 0


_IGNORE_RETURN_TYPES = ( Stack.TYPE_FUNC_RETURN, Stack.TYPE_ATTRIBUTE,
                         Stack.TYPE_GLOBAL, Stack.TYPE_COMPARISON,
                         Stack.TYPE_UNKNOWN)

def _checkReturnWarnings(code) :
    is_getattr = code.func_code.co_name in ('__getattr__', '__getattribute__')
    if is_getattr :
        for line, retval, dummy in code.returnValues :
            if retval.isNone() :
                err = msgs.DONT_RETURN_NONE % code.func_code.co_name
                code.addWarning(err, line+1)

    # there must be at least 2 real return values to check for consistency
    returnValuesLen = len(code.returnValues)
    if returnValuesLen < 2 :
        return

    # if the last return is implicit, check if there are non None returns
    lastReturn = code.returnValues[-1]

    # Python 2.4 optimizes the dead implicit return out, so we can't
    # distinguish implicit and explicit "return None"
    if utils.pythonVersion() < utils.PYTHON_2_4 and \
           not code.starts_and_ends_with_finally and \
           cfg().checkImplicitReturns and lastReturn[1].isImplicitNone():
        for line, retval, dummy in code.returnValues[:-1] :
            if not retval.isNone() :
                code.addWarning(msgs.IMPLICIT_AND_EXPLICIT_RETURNS,
                                lastReturn[0]+1)
                break

    # __get*__ funcs can return different types, don't warn about inconsistency
    if utils.startswith(code.func_code.co_name, '__get') and \
       utils.endswith(code.func_code.co_name, '__') :
        return

    returnType, returnData = None, None
    for line, value, dummy in code.returnValues :
        if not value.isNone() :
            valueType = value.getType(code.typeMap)
            if returnType is None and valueType not in _IGNORE_RETURN_TYPES :
                returnData = value
                returnType = valueType
                continue

            # always ignore None, None can be returned w/any other type
            # FIXME: if we stored func return values, we could do better
            if returnType is not None and not value.isNone() and \
               valueType not in _IGNORE_RETURN_TYPES and \
               returnData.type not in _IGNORE_RETURN_TYPES :
                ok = returnType in (type(value.data), valueType)
                if ok :
                    if returnType == types.TupleType :
                        # FIXME: this isn't perfect, if len == 0
                        # the length can really be 0 OR unknown
                        # we shouldn't check the lengths for equality
                        # ONLY IF one of the lengths is truly unknown
                        if returnData.length > 0 and value.length > 0:
                            ok = returnData.length == value.length
                else :
                    ok = _checkSubclass(returnType, valueType) or \
                         _checkSubclass(valueType, returnType)
                if not ok :
                    code.addWarning(msgs.INCONSISTENT_RETURN_TYPE, line)


def _checkComplex(code, maxValue, value, func, err) :
    if maxValue and value > maxValue :
        line = func.function.func_code.co_firstlineno
        code.addWarning(err % (func.function.__name__, value), line)


def _checkCode(code, codeSource) :
    while code.index < code.maxCode :
        op, oparg, operand = code.popNextOp()
        dispatch_func = CodeChecks.DISPATCH[op]
        if dispatch_func is not None :
            dispatch_func(oparg, operand, codeSource, code)

def _name_unused(var) :
    if var in cfg().unusedNames :
        return 0
    for name in cfg().unusedNames :
        if name != '_' and utils.startswith(var, name) :
            return 0
    return 1

def _checkUnusedParam(var, line, func, code) :
    if line is not None and line == 0 and _name_unused(var) :
        if ((cfg().ignoreSelfUnused or var != cfg().methodArgName) and
            (cfg().varArgumentsUsed or func.varArgName() != var)) :
            code.addWarning(msgs.UNUSED_PARAMETER % var, code.func_code)

def _handleNestedCode(func_code, code, codeSource):
    nested = not (codeSource.main or codeSource.in_class)
    if func_code.co_name == utils.LAMBDA or nested:
        utils.debug(' handling nested code')
        varnames = None
        if nested and func_code.co_name != utils.LAMBDA:
            varnames = func_code.co_varnames + \
                     codeSource.calling_code[-1].function.func_code.co_varnames
        # save the original return value and restore after checking
        returnValues = code.returnValues
        code.init(function.create_fake(func_code.co_name, func_code, {},
                                       varnames))
        _checkCode(code, codeSource)
        code.returnValues = returnValues

def _findUnreachableCode(code) :
    # code after RETURN or RAISE is unreachable unless there's a branch to it
    unreachable = {}
    terminals = code.returnValues[:-1] + code.raiseValues
    terminals.sort(lambda a, b: cmp(a[2], b[2]))
    for line, dummy, i in terminals :
        if not code.branches.has_key(i) :
            unreachable[i] = line

    # find the index of the last return
    lastLine = lastItem = lastIndex = None
    if code.returnValues:
        lastLine, lastItem, lastIndex = code.returnValues[-1]
    if len(code.returnValues) >= 2 :
        lastIndex = code.returnValues[-2][2]
    if code.raiseValues :
        lastIndex = max(lastIndex, code.raiseValues[-1][2])

    # remove last return if it's unreachable AND implicit
    if unreachable.get(lastIndex) == lastLine and lastItem and \
           lastItem.isImplicitNone():
        del code.returnValues[-1]
        del unreachable[lastIndex]

    if cfg().unreachableCode :
        for index in unreachable.keys() :
            try :
                if not OP.JUMP_FORWARD(ord(code.bytes[index])) :
                    code.addWarning(msgs.CODE_UNREACHABLE, unreachable[index])
            except IndexError :
                pass


def _checkFunction(module, func, c = None, main = 0, in_class = 0) :
    "Return a list of Warnings found in a function/method."

    # always push a new config object, so we can pop at end of function
    utils.pushConfig()

    code = CodeChecks.Code()
    code.init(func)
    if main:
        for key in func.function.func_globals.keys():
            code.unusedLocals[key] = -1
    codeSource = CodeChecks.CodeSource(module, func, c, main, in_class, code)
    try :
        _checkCode(code, codeSource)
        if not in_class :
            _findUnreachableCode(code)

        # handle lambdas and nested functions
        codeSource.calling_code.append(func)
        for func_code in code.codeObjects.values() :
            _handleNestedCode(func_code, code, codeSource)
        del codeSource.calling_code[-1]

    except (SystemExit, KeyboardInterrupt) :
        exc_type, exc_value, exc_tb = sys.exc_info()
        raise exc_type, exc_value
    except :
        exc_type, exc_value, exc_tb = sys.exc_info()
        exc_list = traceback.format_exception(exc_type, exc_value, exc_tb)
        for index in range(0, len(exc_list)) :
            exc_list[index] = string.replace(exc_list[index], "\n", "\n\t")
        code.addWarning(msgs.CHECKER_BROKEN % string.join(exc_list, ""))

    if cfg().checkReturnValues :
        _checkReturnWarnings(code)

    if cfg().localVariablesUsed :
        for var, line in code.unusedLocals.items() :
            if line is not None and line > 0 and _name_unused(var) :
                code.addWarning(msgs.UNUSED_LOCAL % var, line)

    if cfg().argumentsUsed :
        op = code.getFirstOp()
        if not (OP.RAISE_VARARGS(op) or OP.RETURN_VALUE(op)) :
            for var, line in code.unusedLocals.items() :
                _checkUnusedParam(var, line, func, code)

    # Check code complexity:
    #   loops should be counted as one branch, but there are typically 3
    #   branches in byte code to setup a loop, so subtract off 2/3's of them
    #    / 2 to approximate real branches
    branches = (len(code.branches.keys()) - (2 * code.loops)) / 2
    lines = (code.getLineNum() - code.func_code.co_firstlineno)
    returns = len(code.returnValues)
    if not main and not in_class :
        args = code.func_code.co_argcount
        locals = len(code.func_code.co_varnames) - args
        _checkComplex(code, cfg().maxArgs, args, func, msgs.TOO_MANY_ARGS)
        _checkComplex(code, cfg().maxLocals, locals, func, msgs.TOO_MANY_LOCALS)
        _checkComplex(code, cfg().maxLines, lines, func, msgs.FUNC_TOO_LONG)
    _checkComplex(code, cfg().maxReturns, returns, func, msgs.TOO_MANY_RETURNS)
    _checkComplex(code, cfg().maxBranches, branches, func, msgs.TOO_MANY_BRANCHES)

    if not (main or in_class) :
        utils.popConfig()
    func.returnValues = code.returnValues
    return (code.warnings, code.globalRefs, code.functionsCalled,
            code.codeObjects.values(), code.returnValues)


def _getUnused(module, globalRefs, dict, msg, filterPrefix = None) :
    "Return a list of warnings for unused globals"

    warnings = []
    for ref in dict.keys() :
        check = not filterPrefix or utils.startswith(ref, filterPrefix)
        if check and globalRefs.get(ref) == None :
            lineInfo = module.moduleLineNums.get(ref)
            if lineInfo:
                warnings.append(Warning(lineInfo[0], lineInfo[1], msg % ref))
    return warnings


def _get_func_info(method) :
    try:
        fc = getattr(method.im_func, 'func_code', None)
        if fc is not None :
            return fc.co_filename, fc.co_firstlineno
    except AttributeError:
        # if the object derives from any object in 2.2,
        # the builtin methods are wrapper_descriptors and
        # have no im_func attr
        pass
    return None, None

_DOT_INIT = '.' + utils.INIT

def _baseInitCalled(classInitInfo, base, functionsCalled) :
    baseInit = getattr(base, utils.INIT, None)
    if baseInit is None or _get_func_info(baseInit) == classInitInfo :
        return 1

    initName = utils.safestr(base) + _DOT_INIT
    if functionsCalled.has_key(initName) :
        return 1

    # ok, do this the hard way, there may be aliases, so check here
    names = string.split(initName, '.')
    try:
        # i think this can raise an exception if the module is a library (.so)
        obj = sys.modules[names[0]]
    except KeyError:
        return 1
    for i in range(1, len(names)) :
        obj = getattr(obj, names[i], None)
        if obj is None:
            return 0
        if functionsCalled.has_key(string.join(names[i:], '.')) :
            return 1

    return 0

def _checkBaseClassInit(moduleFilename, c, func_code, funcInfo) :
    """Return a list of warnings that occur
       for each base class whose __init__() is not called"""

    warnings = []
    functionsCalled, _, returnValues = funcInfo
    for line, stackItem, dummy in returnValues :
        if stackItem.data != None :
            if not stackItem.isNone() or cfg().returnNoneFromInit :
                warn = Warning(func_code, line, msgs.RETURN_FROM_INIT)
                warnings.append(warn)

    classInit = getattr(c.classObject, utils.INIT, None)
    if cfg().baseClassInitted and classInit is not None :
        classInitInfo = _get_func_info(classInit)
        for base in c.classObject.__bases__ :
            if not _baseInitCalled(classInitInfo, base, functionsCalled) :
                warn = Warning(moduleFilename, func_code,
                               msgs.BASE_CLASS_NOT_INIT % utils.safestr(base))
                warnings.append(warn)
    return warnings


def _checkOverridenMethods(func, baseClasses, warnings) :
    for baseClass in baseClasses :
        if func.func_name != utils.INIT and \
           not function.same_signature(func, baseClass) :
            err = msgs.METHOD_SIGNATURE_MISMATCH % (func.func_name, utils.safestr(baseClass))
            warnings.append(Warning(func.func_code, func.func_code, err))
            break


def _updateFunctionWarnings(module, func, c, warnings, globalRefs,
                            main = 0, in_class = 0) :
    "Update function warnings and global references"

    newWarnings, newGlobalRefs, funcs, codeObjects, returnValues = \
                 _checkFunction(module, func, c, main, in_class)
    warnings.extend(newWarnings)
    globalRefs.update(newGlobalRefs)

    return funcs, codeObjects, returnValues


def getBlackList(moduleList) :
    blacklist = []
    for badBoy in moduleList :
        if badBoy[-3:] == ".py":
            badBoy = badBoy[0:-3]
        try :
            file, path, flags = imp.find_module(badBoy)
            if file :
                file.close()
                blacklist.append(normalize_path(path))
        except ImportError :
            pass
    return blacklist

def getStandardLibrary() :
    if cfg().ignoreStandardLibrary :
        try :
            from distutils import sysconfig

            std_lib = sysconfig.get_python_lib()
            path = os.path.split(std_lib)
            if path[1] == 'site-packages' :
                std_lib = path[0]
            return std_lib
        except ImportError :
            return None

def normalize_path(path):
    return os.path.normpath(os.path.normcase(path))

def removeWarnings(warnings, blacklist, std_lib, cfg):
    if std_lib is not None:
        std_lib = normalize_path(std_lib)
    for index in range(len(warnings)-1, -1, -1) :
        filename = normalize_path(warnings[index].file)
        if filename in blacklist or (std_lib is not None and
                                     utils.startswith(filename, std_lib)) :
            del warnings[index]
        elif cfg.only:
            # ignore files not specified on the cmd line if requested
            if os.path.abspath(filename) not in cfg.files:
                del warnings[index]

        # filter by warning/error level if requested
        if cfg.level and warnings[index].level < cfg.level:
            del warnings[index]

    if cfg.limit:
        # sort by severity first, then normal sort (by file/line)
        warnings.sort(lambda a, b: cmp(a.level, b.level) or cmp(a, b))

        # strip duplicates
        lastWarning = None
        for index in range(len(warnings)-1, -1, -1):
            warning = warnings[index]

            # remove duplicate warnings
            if lastWarning is not None and cmp(lastWarning, warning) == 0:
                del warnings[index]
            else:
                lastWarning = warning

        num_ignored = len(warnings) - cfg.limit
        if num_ignored > 0:
            del warnings[:-cfg.limit]
            msg = msgs.TOO_MANY_WARNINGS % num_ignored
            warnings.append(Warning('', 0, msg))

    return warnings


class _SuppressionError(Exception) :
    pass

def _updateSuppressions(suppress, warnings) :
    if not utils.updateCheckerArgs(suppress, 'suppressions', 0, warnings) :
        utils.popConfig()
        raise _SuppressionError

_CLASS_NAME_RE = re.compile("<class '([A-Za-z0-9.]+)'>(\\..+)?")

def getSuppression(name, suppressions, warnings) :
    try :
        utils.pushConfig()

        # cheesy hack to deal with new-style classes.  i don't see a
        # better way to get the name, '<' is an invalid identifier, so
        # we can reliably check it and extract name from:
        # <class 'class-name'>[.identifier[.identifier]...]
        matches = _CLASS_NAME_RE.match(name)
        if matches:
            # pull out the names and make a complete identifier (ignore None)
            name = string.join(filter(None, matches.groups()), '')

        suppress = suppressions[0].get(name, None)
        if suppress is not None :
            _updateSuppressions(suppress, warnings)

        regexList = suppressions[1].keys()
        regexList.sort()
        for regex in regexList :
            match = regex.match(name)
            if match and match.group() == name :
                suppress = 1
                _updateSuppressions(suppressions[1][regex], warnings)

        if not suppress :
            utils.popConfig()

        return suppress
    except _SuppressionError :
        return None

def _findFunctionWarnings(module, globalRefs, warnings, suppressions) :
    for func in module.functions.values() :
        func_code = func.function.func_code
        utils.debug("function:", func_code)

        name = '%s.%s' % (module.moduleName, func.function.__name__)
        suppress = getSuppression(name, suppressions, warnings)
        if cfg().noDocFunc and func.function.__doc__ == None :
            err = msgs.NO_FUNC_DOC % func.function.__name__
            warnings.append(Warning(module.filename(), func_code, err))

        _checkNoSelfArg(func, warnings)
        _updateFunctionWarnings(module, func, None, warnings, globalRefs)
        if suppress is not None :
            utils.popConfig()

def _getModuleFromFilename(module, filename):
    if module.filename() != filename:
        for m in module.modules.values():
            if m.filename() == filename:
                return m
    return module

# Create object for non-2.2 interpreters, any class object will do
try:
    if object: pass
except NameError:
    object = _SuppressionError

# Create property for pre-2.2 interpreters
try :
    if property: pass
except NameError:
    property = None

def _findClassWarnings(module, c, class_code,
                       globalRefs, warnings, suppressions) :
    try:
        className = utils.safestr(c.classObject)
    except TypeError:
        # goofy __getattr__
        return
    classSuppress = getSuppression(className, suppressions, warnings)
    baseClasses = c.allBaseClasses()
    for base in baseClasses :
        baseModule = utils.safestr(base)
        if '.' in baseModule :
            # make sure we handle import x.y.z
            packages = string.split(baseModule, '.')
            baseModuleDir = string.join(packages[:-1], '.')
            globalRefs[baseModuleDir] = baseModule

    # handle class variables
    if class_code is not None :
        func = function.create_fake(c.name, class_code)
        _updateFunctionWarnings(module, func, c, warnings, globalRefs, 0, 1)

    filename = module.filename()
    func_code = None
    for method in c.methods.values() :
        if method == None :
            continue
        func_code = method.function.func_code
        utils.debug("method:", func_code)

        try:
            name = utils.safestr(c.classObject) + '.' + method.function.func_name
        except AttributeError:
            # func_name may not exist
            continue
        methodSuppress = getSuppression(name, suppressions, warnings)

        if cfg().checkSpecialMethods:
            funcname = method.function.func_name
            if funcname[:2] == '__' == funcname[-2:] and \
               funcname != '__init__':
                err = None
                argCount = python.SPECIAL_METHODS.get(funcname, -1)
                if argCount != -1:
                    # if the args are None, it can be any # of args
                    if argCount is not None:
                        minArgs = maxArgs = argCount
                        err = CodeChecks.getFunctionArgErr(funcname,
                                     func_code.co_argcount, minArgs, maxArgs)
                else:
                    err = msgs.NOT_SPECIAL_METHOD % funcname

                if err is not None:
                    warnings.append(Warning(filename, func_code, err))
                
        if cfg().checkOverridenMethods :
            _checkOverridenMethods(method.function, baseClasses, warnings)

        if cfg().noDocFunc and method.function.__doc__ == None :
            err = msgs.NO_FUNC_DOC % method.function.__name__
            warnings.append(Warning(filename, func_code, err))

        _checkSelfArg(method, warnings)
        tmpModule = _getModuleFromFilename(module, func_code.co_filename)
        funcInfo = _updateFunctionWarnings(tmpModule, method, c, warnings, globalRefs)
        if func_code.co_name == utils.INIT :
            if utils.INIT in dir(c.classObject) :
                warns = _checkBaseClassInit(filename, c, func_code, funcInfo)
                warnings.extend(warns)
            elif cfg().initDefinedInSubclass :
                err = msgs.NO_INIT_IN_SUBCLASS % c.name
                warnings.append(Warning(filename, c.getFirstLine(), err))
        if methodSuppress is not None :
            utils.popConfig()

    if c.memberRefs and cfg().membersUsed :
        memberList = c.memberRefs.keys()
        memberList.sort()
        err = msgs.UNUSED_MEMBERS % (string.join(memberList, ', '), c.name)
        warnings.append(Warning(filename, c.getFirstLine(), err))

    try:
        newStyleClass = issubclass(c.classObject, object)
    except TypeError:
        # FIXME: perhaps this should warn b/c it may be a class???
        newStyleClass = 0

    slots = c.statics.get('__slots__')
    if slots is not None and cfg().slots:
        lineNum = c.lineNums['__slots__']
        if not newStyleClass:
            err = msgs.USING_SLOTS_IN_CLASSIC_CLASS % c.name
            warnings.append(Warning(filename, lineNum, err))
        elif cfg().emptySlots:
            try:
                if len(slots.data) == 0:
                    err = msgs.EMPTY_SLOTS % c.name
                    warnings.append(Warning(filename, lineNum, err))
            except AttributeError:
                # happens when slots is an instance of a class w/o __len__
                pass

    if not newStyleClass and property is not None and cfg().classicProperties:
        for static in c.statics.keys():
            if type(getattr(c.classObject, static, None)) == property:
                err = msgs.USING_PROPERTIES_IN_CLASSIC_CLASS % (static, c.name)
                warnings.append(Warning(filename, c.lineNums[static], err))

    coerceMethod = c.methods.get('__coerce__')
    if newStyleClass and coerceMethod:
        lineNum = coerceMethod.function.func_code.co_firstlineno
        err = msgs.USING_COERCE_IN_NEW_CLASS % c.name
        warnings.append(Warning(filename, lineNum, err))

    gettroMethod = c.methods.get('__getattribute__')
    if not newStyleClass and gettroMethod:
        lineNum = gettroMethod.function.func_code.co_firstlineno
        err = msgs.USING_GETATTRIBUTE_IN_OLD_CLASS % c.name
        warnings.append(Warning(filename, lineNum, err))

    if cfg().noDocClass and c.classObject.__doc__ == None :
        method = c.methods.get(utils.INIT, None)
        if method != None :
            func_code = method.function.func_code
        # FIXME: check to make sure this is in our file,
        #        not a base class file???
        err = msgs.NO_CLASS_DOC % c.classObject.__name__
        warnings.append(Warning(filename, func_code, err))

    # we have to do this here, b/c checkFunction doesn't popConfig for classes
    # this allows us to have __pychecker__ apply to all methods
    # when defined at class scope
    if class_code is not None :
        utils.popConfig()

    if classSuppress is not None :
        utils.popConfig()


def find(moduleList, initialCfg, suppressions = None) :
    "Return a list of warnings found in the module list"

    if suppressions is None :
        suppressions = {}, {}

    utils.initConfig(initialCfg)

    warnings = []
    for module in moduleList :
        if module.moduleName in cfg().blacklist :
            continue

        modSuppress = getSuppression(module.moduleName, suppressions, warnings)
        globalRefs, classCodes = {}, {}

        # main_code can be null if there was a syntax error
        if module.main_code != None :
            funcInfo = _updateFunctionWarnings(module, module.main_code,
                                               None, warnings, globalRefs, 1)
            for code in funcInfo[1] :
                classCodes[code.co_name] = code

        _findFunctionWarnings(module, globalRefs, warnings, suppressions)

        for c in module.classes.values() :
                _findClassWarnings(module, c, classCodes.get(c.name),
                                   globalRefs, warnings, suppressions)

        if cfg().noDocModule and \
           module.module != None and module.module.__doc__ == None :
            warnings.append(Warning(module.filename(), 1, msgs.NO_MODULE_DOC))

        if cfg().allVariablesUsed or cfg().privateVariableUsed :
            prefix = None
            if not cfg().allVariablesUsed :
                prefix = "_"
            for ignoreVar in cfg().variablesToIgnore + cfg().unusedNames :
                globalRefs[ignoreVar] = ignoreVar
            warnings.extend(_getUnused(module, globalRefs, module.variables,
                                       msgs.VAR_NOT_USED, prefix))
        if cfg().importUsed :
            if module.moduleName != utils.INIT or cfg().packageImportUsed :
                # always ignore readline module, if [raw_]input() is used
                if globalRefs.has_key('input') or \
                   globalRefs.has_key('raw_input'):
                    globalRefs['readline'] = 0
                warnings.extend(_getUnused(module, globalRefs, module.modules,
                                           msgs.IMPORT_NOT_USED))

        if module.main_code != None :
            utils.popConfig()
        if modSuppress is not None :
            utils.popConfig()

    std_lib = None
    if cfg().ignoreStandardLibrary :
        std_lib = getStandardLibrary()
    return removeWarnings(warnings, getBlackList(cfg().blacklist), std_lib,
                          cfg())

if 0:
    # if you want to test w/psyco, include this
    import psyco
    psyco.bind(_checkCode)
