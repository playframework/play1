from pychecker2.util import parents

from compiler import ast

class File:
    def __init__(self, name):
        self.name = name
        self.parseTree = None
        self.scopes = {}
        self.root_scope = None
        self.warnings = []

    def __cmp__(self, other):
        return cmp(self.name, other.name)

    def warning(self, line, warn, *args):
        lineno = getattr(line, 'lineno', line)
        if not lineno and hasattr(line, 'parent'):
            for p in parents(line):
                if p.lineno:
                    lineno = p.lineno
                    break
        self.warnings.append( (lineno, warn, args) )

    def scope_filter(self, type):
        return [x for x in self.scopes.iteritems() if isinstance(x[0], type)]

    def function_scopes(self):
        return self.scope_filter(ast.Function)

    def class_scopes(self):
        return self.scope_filter(ast.Class)

    def not_class_scopes(self):
        result = []
        for n, s in self.scopes.iteritems():
            if not isinstance(n, ast.Class):
                result.append( (n, s) )
        return result
