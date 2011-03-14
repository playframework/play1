from pychecker2.Check import Check
from pychecker2.Check import Warning
from pychecker2.util import BaseVisitor, type_filter
from pychecker2 import symbols

from compiler import ast, walk

class Returns(BaseVisitor):

    def __init__(self):
        self.result = []

    def visitReturn(self, node):
        self.result.append(node)

    # Don't descend into other scopes
    def visitFunction(self, node): pass
    visitClass = visitFunction
    visitLambda = visitFunction

def _is_implicit(node):
    if isinstance(node, ast.Const) and node.value is None:
        return 1
    return None

class MixedReturnCheck(Check):

    mixedReturns = \
        Warning('Report functions using "return" and "return value"',
                'Function %s uses both "return" and "return value"')

    def check(self, file, unused_checker):
        for scope in type_filter(file.scopes.values(), symbols.FunctionScope):
            returns = walk(scope.node.code, Returns()).result
            empty, value = [], []
            for node in returns:
                if _is_implicit(node.value):
                    empty.append(node)
                else:
                    value.append(node)
            if len(empty) > 0 and len(value) > 0:
                file.warning(empty[0], self.mixedReturns, scope.name)

