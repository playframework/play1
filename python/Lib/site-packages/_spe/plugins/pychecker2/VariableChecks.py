from pychecker2.Check import Check
from pychecker2.Options import Opt, BoolOpt
from pychecker2.Warning import Warning
from pychecker2.util import *
from pychecker2 import symbols

from compiler import ast, walk

def _is_method(scope):
    return scope.__class__ is symbols.FunctionScope and \
           scope.parent.__class__ is symbols.ClassScope

def _is_self(scope, name):
    return _is_method(scope) and name in scope.node.argnames[:1]

def is_arg_and_defaulted_to_same_name(name, scope):
    if isinstance(scope, symbols.FunctionScope):
        if name in scope.node.argnames and scope.node.defaults:
            # compute default args
            args = scope.node.argnames[:]
            # knock off kwargs
            if scope.node.kwargs:
                args = args[:-1]
            # knock off varags 
            if scope.node.varargs:
                args = args[:-1]
            # take the last args as the defaults
            args = args[-len(scope.node.defaults):]
            try:
                # get the corresponding default arg value
                default = scope.node.defaults[args.index(name)]
                # must be a Name node of the same name
                if isinstance(default, ast.Name) and \
                   default.name == name:
                    return 1
            except ValueError:
                pass
    return None


class ShadowCheck(Check):
    """Use symbol information to check that no scope defines a name
    already known to a parent scope"""

    defineNone = Warning('Report any redefinition of None',
                         'Do not redefine None')
    shadowBuiltins = Warning('Report names that shadow builtins',
                            'Identifier (%s) shadows builtin', 0)
    shadowIdentifier = Warning('Report names already defined in outer scopes',
                               'Identifier (%s) shadows definition in scope %s')
    def check(self, file, unused_checker):
        # warn if any name defined in a scope is defined in a parent scope
        # or even the builtins
        for node, scope in file.not_class_scopes():
            for name in scope.defs:
                if name == 'None':
                    file.warning(scope.defs[name], self.defineNone)
                    continue
                if name in scope.globals:
                    continue
                if is_arg_and_defaulted_to_same_name(name, scope):
                    continue
                if _is_self(scope, name):
                    continue
                if __builtins__.has_key(name):
                    file.warning(scope.defs[name], self.shadowBuiltins, name)
                for p in parents(scope):
                    if p.defs.has_key(name) and \
                       not isinstance(p, symbols.ClassScope):
                        file.warning(scope.defs[name], \
                                     self.shadowIdentifier, name, `p`)

def _str_value(s):
    if type(s) == type(''):
        return eval(s)
    return s


def _empty_function(stmts):
    if not stmts:
        return 1
    stmt = stmts[0]
    # functions which only raise exceptions 
    if isinstance(stmt, ast.Raise):
        return 1
    # functions which do nothing 
    if len(stmts) == 1 and isinstance(stmt, ast.Pass):
        return 1
    # functions which just return a constant 
    if len(stmts) == 1 and \
       isinstance(stmt, ast.Return) and isinstance(stmt.value, ast.Const):
        return 1
    # functions which only assert falsehood 
    if isinstance(stmt, ast.Assert):
        if (isinstance(stmt.test, ast.Const) and not stmt.test.value) or \
            (isinstance(stmt.test, ast.Name) and stmt.test.name == 'None'):
            return 1
    return 0

class UnusedCheck(Check):
    """Use symbol information to check that no scope defines a name
    not used in this or any child scope"""

    unused = Warning('Report names not used', 'Identifier (%s) not used')

    def __init__(self):
        self.reportUnusedSelf = None
        self.unusedPrefixes = None

    def get_options(self, options):
        desc = 'Ignore unused identifiers that start with these values'
        default = ['unused', 'empty', 'dummy',
                   '__pychecker__', '__all__', '__version__', 'ignored']
        options.add(Opt(self, 'unusedPrefixes', desc, default))
        
        desc = 'Ignore unused method "self" parameter'
        options.add(BoolOpt(self, 'reportUnusedSelf', desc))

    def check(self, file, unused_checker):
        self.unusedPrefixes = _str_value(self.unusedPrefixes)

        def used(name, parent_scope):
            if parent_scope.uses.has_key(name):
                return 1
            for c in parent_scope.get_children():
                if used(name, c):
                    return 1
            return 0

        for nodes, scope in file.not_class_scopes():
            if isinstance(nodes, ast.Function):
                if _empty_function(nodes.code.nodes):
                    continue
            
            # ensure that every defined variable is used in some scope
            for var in scope.defs:
                # ignore '_'... just because
                if var == '_':
                    continue

                # check for method self
                if not self.reportUnusedSelf and _is_self(scope, var):
                    continue

                # ignore names in the root scope which are not imported:
                # class defs, function defs, variables, etc, unless
                # they start with '_'
                if scope == file.root_scope:
                    if not scope.imports.has_key(var):
                        if not var.startswith('_'):
                            continue

                for prefix in self.unusedPrefixes:
                    if var.startswith(prefix):
                        break
                else:
                    if not used(var, scope) and not scope.globals.has_key(var):
                        file.warning(scope.defs[var], self.unused, var)

def _importedName(scope, name):
    if scope.imports.has_key(name):
        return 1
    if scope.parent:
        return _importedName(scope.parent, name)
    return None

class UnknownCheck(Check):
    """Use symbol information to check that no scope uses a name
    not defined in a parent scope"""

    unknown = Warning('Report names that are not defined',
                      'Unknown identifier: %s')

    builtins = {}
    builtins.update(__builtins__)
    builtins['__builtins__'] = __builtins__
    builtins['WindowsError'] = getattr(__builtins__, 'WindowsError', None)

    def check(self, file, unused_checker):

        # if a name used is not found in the defined variables, complain
        for scope in file.scopes.values():
            unknown = dict_minus(scope.uses, scope.defs)
            unknown = dict_minus(unknown, self.builtins)
            for var in unknown:
                for p in parents(scope):
                    if p.defs.has_key(var):
                        break
                else:
                    if not _importedName(scope, var):
                        file.warning(scope.uses[var], self.unknown, var)

def _first_arg_defaulted(function_node):
    count = len(function_node.argnames)
    if function_node.varargs:
        count -= 1
    if function_node.kwargs:
        count -= 1
    if count > 0 and len(function_node.defaults) == count:
        return 1
    return None

class SelfCheck(Check):
    'Check for simple self parameter'
    
    selfName = Warning('Report any methods whose first argument is not self',
                       'First argument to method %s (%s) is not in %s')
    selfDefault = Warning('Report a self parameter with a default value',
                          'First argument to method %s (%s) has a default value')
    
    functionSelf = Warning('Report functions (not methods) with '
                           'arguments named "self"',
                           'Argument to function (%s) is "%s"')
    
    missingSelf = Warning('Report methods without "self"',
                          'Method %s is missing self parameter')
    
    def get_options(self, options):
        desc = 'Name of self parameter'
        default = ["self", "this", "s"]
        options.add(Opt(self, 'selfNames', desc, default))

        desc = 'Suspicious self parameters'
        self.selfSuspicious = ["self"]
        options.add(Opt(self, 'selfSuspicious', desc, self.selfSuspicious))

    def check(self, file, unused_checker):
        self.selfNames      = _str_value(self.selfNames)
        self.selfSuspicious = _str_value(self.selfSuspicious)

        for node, scope in file.function_scopes():
            args = node.argnames
            name = getattr(scope.node, 'name', 'lambda')
            if _is_method(scope):
                if not args:
                    file.warning(scope.node, self.missingSelf, name)
                else:
                    if args[0] not in self.selfNames:
                        file.warning(scope.node, self.selfName,
                                     name, args[0], `self.selfNames`)
                    if _first_arg_defaulted(scope.node):
                        file.warning(scope.node, self.selfDefault,
                                     name, args[0])
            else:
                for arg in args:
                    if arg in self.selfSuspicious:
                        file.warning(scope.defs[arg], self.functionSelf,
                                     name, arg)

                

class UnpackCheck(Check):
    'Mark all unpacked variables as used'

    def __init__(self):
        self.unpackedUsed = None

    def get_options(self, options):
        desc = 'Do not treat variables used in tuple assignment as used'
        options.add(BoolOpt(self, 'unpackedUsed', desc, 1))

    def check(self, file, unused_checker):
        if not self.unpackedUsed:
            return

        class Visitor:
            def visitAssTuple(self, node):
                for c in node.getChildNodes():
                    for n in parents(c):
                        try:
                            file.scopes[n].uses[c.name] = node
                            break
                        except (KeyError, AttributeError):
                            pass
            visitAssList = visitAssTuple

        # local args unpacked on the `def' line are used, too
        for scope_node, scope in file.function_scopes():
            for arg in type_filter(scope_node.argnames, tuple):
                for unpacked in ast.flatten(arg):
                    scope.uses[unpacked] = scope.uses.get(unpacked, scope_node)

        if file.root_scope:
            walk(file.root_scope.node, Visitor())

def intersect2(a, b):
    return [i for i in a if i in b]

def intersect(items):
    result = items[0]
    for item in items[1:]:
        result = intersect2(result, item)
    return result

class UsedBeforeSetCheck(Check):

    usedBeforeDefined = \
          Warning('Report variables that may be used before they are defined',
                  'The local %s may be used before defined')

    def check(self, file, unused_checker):
        class Visitor(BaseVisitor):
            def __init__(self, defines = None, uses = None):
                # list of vars defined
                self.defines = []
                # name->node vars used before defined                    
                self.uses = {}
                if defines is not None:
                    self.defines = defines[:]
                if uses is not None:
                    self.uses = uses.copy()
            def visitFunction(self, n):
                self.defines.append(n.name)
                
            visitClass = visitFunction
            visitAssName = visitFunction

            def visitGlobal(self, node):
                for name in node.names:
                    self.defines.append(name)
            
            def visitListComp(self, n):
                # visit qualifiers before expression
                children = ast.flatten_nodes(n.quals) + [n.expr]
                for c in children:
                    self.visit(c)
            
            def visitName(self, n):
                if n.name not in self.defines:
                    self.uses[n.name] = self.uses.get(n.name, n)

            def visitIf(self, n):
                if not n.else_:
                    return
                visits = []
                for test, code in n.tests:
                    visits.append(walk(code, Visitor(self.defines, self.uses)))
                visits.append(walk(n.else_, Visitor(self.defines, self.uses)))
                # compute the intersection of defines
                self.defines = intersect([v.defines for v in visits])
                # compute the union of uses, perserving first occurances
                union = {}
                visits.reverse()
                for v in visits:
                    union.update(v.uses)
                union.update(self.uses)
                self.uses = union
                        
        for node, scope in file.function_scopes():
            predefined = ast.flatten(scope.params) + scope.imports.keys()
            visitor = walk(node.code, Visitor(predefined))
            usedBefore = dict_intersect(visitor.uses, scope.defs)
            for name, use_node in usedBefore.items():
                file.warning(use_node, self.usedBeforeDefined, name)
