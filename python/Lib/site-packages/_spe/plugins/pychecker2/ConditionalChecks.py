from pychecker2.Check import Check
from pychecker2.util import BaseVisitor

from compiler import ast, walk

class ConstantFinder(BaseVisitor):

    def __init__(self):
        self.result = []

    def visitConst(self, node):
        if isinstance(node.parent, (ast.Or, ast.Not, ast.And)):
            self.result.append((node, `node.value`))

    def visitName(self, node):
        if node.name == 'None':
            if isinstance(node.parent, (ast.Or, ast.Not, ast.And)):
                self.result.append((node, 'None'))

class GetConditionalConstants(BaseVisitor):

    def __init__(self):
        self.result = []

    def visitIf(self, node):
        for test, code in node.tests:
            self.result.extend(walk(test, ConstantFinder()).result)

    def visitWhile(self, node):
        self.result.extend(walk(node.test, ConstantFinder()).result)
    visitListCompIf = visitWhile
    visitAssert = visitWhile

class ConstantCheck(Check):

    constantInConditional = \
                          Warning('Report constants used in conditionals',
                                  'Constant used in conditional %s')

    def check(self, file, unused_checker):
        if file.parseTree:
            v = GetConditionalConstants()
            for n, value in walk(file.parseTree, v).result:
                file.warning(n, self.constantInConditional, value)
