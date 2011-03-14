from pychecker2.Check import Check
from pychecker2.Check import Warning
from pychecker2 import symbols
from pychecker2.util import *

from compiler.misc import mangle
from compiler import ast, walk

_ignorable = {}
for ignore in ['repr', 'dict', 'class', 'doc', 'str']:
    _ignorable['__%s__' % ignore] = 1

class GetDefs(BaseVisitor):
    "Record definitions of a attribute of self, who's name is provided"
    def __init__(self, name):
        self.selfname = name
        self.result = {}

    def visitAssAttr(self, node):
        if isinstance(node.expr, ast.Name) and \
           node.expr.name == self.selfname and \
           isinstance(node.parent, (ast.Assign, ast.AssTuple)):
            self.result[node.attrname] = node

    def visitClass(self, node):         # ignore nested classes
        pass

class GetRefs(BaseVisitor):
    "Record references to a attribute of self, who's name is provided"
    def __init__(self, name):
        self.selfname = name
        self.result = {}

    def visitAssAttr(self, node):
        if isinstance(node.expr, ast.Name) and \
           node.expr.name == self.selfname and \
           not isinstance(node.parent, (ast.Assign, ast.AssTuple)):
            self.result[node.attrname] = node
        self.visitChildren(node)

    def visitGetattr(self, node):
        if isinstance(node.expr, ast.Name) and \
           node.expr.name == self.selfname:
            self.result[node.attrname] = node
        self.visitChildren(node)

    def visitClass(self, node):         # ignore nested classes
        pass


def _get_methods(class_scope):
    return type_filter(class_scope.get_children(), symbols.FunctionScope)

class NotSimpleName(Exception): pass

# compress Getattr(Getattr(Name(x), y), z) -> "x.y.z"
def get_name(node):
    if isinstance(node, ast.Getattr):
        return get_name(node.expr) + (node.attrname, )
    elif isinstance(node, ast.Name):
        return (node.name,)
    else:
        raise NotSimpleName

def get_base_names(scope):
    names = []
    for b in scope.node.bases:
        try:
            names.append(get_name(b))
        except NotSimpleName:       # FIXME: hiding expressions
            pass
    return names

def find_in_module(package, remotename, names, checker):
    # No other names, must be a name from the module
    if not names:
        f = checker.check_module(package)
        if f:
            return find_scope_going_down(f.root_scope, [remotename], checker)
        return None

    # complex name lookup
    try:
        #  first, get the real name of the package
        name = package.__name__
        module = __import__(name, globals(), {}, [''])
    except AttributeError:
        #  ok, so its a fake module... go with that
        module = package
    if remotename:
        name += "." + remotename
    #  now import it, and chase down any other modules
    submodule = getattr(module, names[0], None)
    if type(submodule) == type(symbols):
        return find_in_module(submodule, None, names[1:], checker)

    #  object in the module is not another module, so chase down the source
    f = checker.check_module(submodule)
    if f:
        return find_scope_going_down(f.root_scope, names, checker)
    return None
                 
def find_scope_going_down(scope, names, checker):
    "Drill down scopes to find definition of x.y.z"
    for c in scope.get_children():
        if getattr(c, 'name', '') == names[0]:
            if len(names) == 1:
                return c
            return find_scope_going_down(c, names[1:], checker)
    # Not defined here, check for import
    return find_imported_class(scope.imports, names, checker)

def find_imported_class(imports, names, checker):
    # may be defined by import
    for i in range(1, len(names) + 1):
        # try x, then x.y, then x.y.z as imported names
        try:
            name = ".".join(names[:i])
            ref = imports[name]
            # now look for the rest of the name
            result = find_in_module(ref.module, ref.remotename, names[i:], checker)
            if result:
                return result
        except (KeyError, ImportError):
            pass
    return None

def find_scope_going_up(scope, names, checker):
    "Search up to find scope defining x of x.y.z"
    for p in parents(scope):
        if p.defs.has_key(names[0]):
            return find_scope_going_down(p, names, checker)
    # name imported via 'from module import *'
    try:
        return find_in_module(p.imports[names[0]].module, None, names, checker)
    except KeyError:
        return None

def get_base_classes(scope, checker):
    result = []
    for name in get_base_names(scope):
        base = find_scope_going_up(scope, name, checker)
        if base:
            result.append(base)
            result.extend(get_base_classes(base, checker))
    return result

def conformsTo(a, b):
    alen = len(a.node.argnames)
    blen = len(b.node.argnames)
    # f(a, *args, **kw) conforms to f(a, b, *args, **kw)
    # f(a, *args) conforms to f(a, b, *args)
    # f(a, *args) conforms to f(a, b, c)
    # f(a, b, c, *args) does not conform to f(a, b)
    if alen == blen:
        if a.node.kwargs == b.node.kwargs and a.node.varargs == b.node.varargs:
            return 1
    if a.node.varargs and alen - 1 <= blen:
        return a.node.kwargs == b.node.kwargs
    return None

class AttributeCheck(Check):
    "check `self.attr' expressions for attr"

    unknownAttribute = Warning('Report unknown object attributes in methods',
                           'Class %s has no attribute %s')
    unusedAttribute = Warning('Report attributes unused in methods',
                              'Attribute %s is not used in class %s')
    methodRedefined = Warning('Report the redefinition of class methods',
                              'Method %s in class %s redefined')
    signatureChanged = Warning('Report methods whose signatures do not '
                               'match base class methods',
                               'Signature does not match method '
                               '%s in base class %s')
    attributeInitialized = \
                 Warning('Report attributes not initialized in __init__',
                         'Attribute %s is not initialized in __init__')
                                   
    def check(self, file, checker):
        def visit_with_self(Visitor, method):
            if not method.node.argnames:
                return {}
            return walk(method.node, Visitor(method.node.argnames[0])).result

        # for all class scopes
        for node, scope in file.class_scopes():
            init_attributes = None      # attributes initilized in __init__
            attributes = {}             # "self.foo = " kinda things
            methods = {}                # methods -> scopes
            
            # get attributes defined on self
            for m in _get_methods(scope):
                defs = visit_with_self(GetDefs, m)
                if m.name == '__init__':
                    init_attributes = defs
                attributes.update(defs)
                methods[mangle(m.name, scope.name)] = m

            # complain about attributes not initialized in __init__
            if init_attributes is not None:
                for name, node in dict_minus(attributes, init_attributes).items():
                    file.warning(node, self.attributeInitialized, name)

            # collect inherited gunk: methods and attributes
            # check for non-conformant methods
            inherited_methods = scope.defs.copy()
            inherited_attributes = attributes.copy()
            for base in get_base_classes(scope, checker):
                for m in _get_methods(base):
                    inherited_attributes.update(visit_with_self(GetDefs, m))
                    mname = mangle(m.name, base.name)
                    if m.name != "__init__" and \
                       methods.has_key(mname) and \
                       not conformsTo(methods[mname], m):
                        file.warning(methods[mname].node,
                                     self.signatureChanged, m.name, base.name)
                    else:
                        methods[mname] = m
                inherited_methods.update(base.defs)

            # complain about attributes with the same name as methods
            both = dict_intersect(attributes, inherited_methods)
            for name, node in both.items():
                file.warning(node, self.methodRedefined, name, scope.name)

            # find refs on self
            refs = {}
            for m in _get_methods(scope):
                refs.update(visit_with_self(GetRefs, m))

            # Now complain about refs on self that aren't known
            unknown = dict_minus(refs, inherited_methods)
            unknown = dict_minus(unknown, _ignorable)
            unknown = dict_minus(unknown, scope.defs)
            unknown = dict_minus(unknown, inherited_attributes)
            for name, node in unknown.items():
                file.warning(node, self.unknownAttribute, scope.name, name)

            unused = dict_minus(attributes, refs)
            for name, node in unused.items():
                if name.startswith('__'):
                    file.warning(node, self.unusedAttribute, name, scope.name)

class GetReturns(BaseVisitor):

    def __init__(self):
        self.result = []

    def visitReturn(self, node):
        self.result.append(node)

    def visitFunction(self, node): pass
    visitClass = visitFunction

class InitCheck(Check):

    initReturnsValue = Warning('Report value returned from __init__',
                               'Method __init__ should not return a value')

    def check(self, file, unused_checker):

        for node, scope in file.class_scopes():
            for m in _get_methods(scope):
                if m.name == '__init__':
                    for r in walk(m.node.code, GetReturns()).result:
                        if isinstance(r.value, ast.Const) and \
                           r.value.value is None:
                            continue
                        if isinstance(r.value, ast.Name) and \
                           r.value.name == 'None':
                            continue
                        file.warning(r, self.initReturnsValue)

                            

special = {
    '__cmp__': 2,     '__del__': 1,     '__delitem__': 2, '__eq__': 2,
    '__ge__': 2,      '__getitem__': 2, '__gt__': 2,      '__hash__': 1,
    '__le__': 2,      '__len__': 1,     '__lt__': 2,      '__ne__': 2,
    '__nonzero__': 1, '__repr__': 1,    '__setitem__': 3, '__str__': 1,
    '__getattr__': 2, '__setattr__': 3,
    '__delattr__': 2, '__len__': 1,     '__delitem__': 2, '__iter__': 1,
    '__contains__': 2,'__setslice__': 4,'__delslice__': 3,
    '__add__': 2,     '__sub__': 2,     '__mul__': 2,     '__floordiv__': 2,
    '__mod__': 2,     '__divmod__': 2,  '__lshift__': 2,
    '__rshift__': 2,  '__and__': 2,     '__xor__': 2,     '__or__': 2,
    '__div__': 2,     '__truediv__': 2, '__radd__': 2,    '__rsub__': 2,
    '__rmul__': 2,    '__rdiv__': 2,    '__rmod__': 2,    '__rdivmod__': 2,
    '__rpow__': 2,    '__rlshift__': 2, '__rrshift__': 2, '__rand__': 2,
    '__rxor__': 2,    '__ror__': 2,     '__iadd__': 2,    '__isub__': 2,
    '__imul__': 2,    '__idiv__': 2,    '__imod__': 2,    '__ilshift__': 2,
    '__irshift__': 2, '__iand__': 2,    '__ixor__': 2,    '__ior__': 2,
    '__neg__': 1,     '__pos__': 1,     '__abs__': 1,     '__invert__': 1,
    '__complex__': 1, '__int__': 1,     '__long__': 1,    '__float__': 1,
    '__oct__': 1,     '__hex__': 1,     '__coerce__': 2,
    '__new__': None,
    '__getinitargs__': 1, '__reduce__': 1,
    '__getstate__': 1,'__setstate__': 2,
    '__copy__': 1,    '__deepcopy__': 1,
    '__pow__': 2,     '__ipow__': 2,    # 2 or 3
    '__call__': None,                   # any number > 1
    '__getslice__': 3,                  # deprecated
    '__getattribute__': 2,
    }

def check_special(scope):
    try:
        count = special[scope.name]
        max_args = len(scope.node.argnames)
        min_args = max_args - len(scope.node.defaults)
        if min_args > count or max_args < count or \
           scope.node.varargs or scope.node.kwargs:
            return special[scope.name]
    except KeyError:
        pass
    return None

class SpecialCheck(Check):

    specialMethod = Warning('Report special methods with incorrect '
                            'number of arguments',
                            'The %s method requires %d argument%s, '
                            'including self')

    notSpecial = Warning('Report methods with "__" prefix and suffix '
                         'which are not defined as special methods',
                         'The method %s is not a special method, '
                         'but is reserved.')

    def check(self, file, unused_checker):

        for node, scope in file.class_scopes():
            for m in _get_methods(scope):
                n = check_special(m)
                if n:
                    file.warning(m.node, self.specialMethod, m.name, n,
                                 n > 1 and "s" or "")
                name = m.name
                if name.startswith('__') and name.endswith('__') and \
                   name != '__init__' and not special.has_key(name):
                    file.warning(m.node, self.notSpecial, name)

class BackQuote(BaseVisitor):

    def __init__(self, selfname):
        self.results = []
        self.selfname = selfname

    def visitBackquote(self, node):
        if isinstance(node.expr, ast.Name) and node.expr.name == self.selfname:
            self.results.append(node)

class ReprCheck(Check):

    backquoteSelf = Warning('Report use of `self` in __repr__ methods',
                           'Using `self` in __repr__')
    def check(self, file, unused_checker):
        for node, scope in file.class_scopes():
            for m in _get_methods(scope):
                if m.name == '__repr__' and m.node.argnames:
                    visitor = BackQuote(m.node.argnames[0])
                    for n in walk(m.node.code, visitor).results:
                        file.warning(n, self.backquoteSelf)

