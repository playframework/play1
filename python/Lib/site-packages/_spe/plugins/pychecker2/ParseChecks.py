from pychecker2.Check import Check
from pychecker2.Warning import Warning
from pychecker2.Options import BoolOpt
from pychecker2 import symbols

from compiler import parseFile, walk
import parser

def _parent_link(node):
    for c in node.getChildNodes():
        c.parent = node
        _parent_link(c)

class ParseCheck(Check):

    syntaxErrors = Warning('Report/ignore syntax errors',
                           'Unable to parse: %s')

    def __init__(self):
        self.main = None
    
    def get_options(self, options):
        desc = 'Ignore module-level code protected by __name__ == "__main__"'
        options.add(BoolOpt(self, 'main', desc, 1))
    
    def check(self, file, unused_checker):
        try:
            file.parseTree = parseFile(file.name)
            # link each node to it's parent
            _parent_link(file.parseTree)
            file.parseTree.parent = None
        except parser.ParserError, detail:
            file.warning(1, self.syntaxErrors, detail.args[0])
        except IOError, detail:
            file.warning(0, self.syntaxErrors, detail.strerror)
        if not file.parseTree:
            return

        if not self.main:
            # remove __name__ == '__main__' code from module-level
            for n in file.parseTree.node.nodes:
                try:
                    test, code = n.tests[0]
                    comparison, value = test.ops[0]
                    if comparison == '==':
                        try:
                            if test.expr.name == '__name__' and \
                               value.value == '__main__':
                                file.parseTree.node.nodes.remove(n)
                                break
                        except AttributeError:
                            if test.expr.value == '__main__' and \
                               value.name == '__name__':
                                file.parseTree.node.nodes.remove(n)
                                break
                except (AttributeError, IndexError):
                    pass

        file.scopes = walk(file.parseTree, symbols.SymbolVisitor()).scopes
        file.root_scope = file.scopes[file.parseTree]

        # add starting lineno into scopes, since they don't have it
        for k, v in file.scopes.items():
            v.lineno = k.lineno

        # define the root of the scope tree (global scope, within
        # the module)
        file.root_scope.lineno = 1

        # create a mapping from scopes back to the nodes which made 'em
        for node, scope in file.scopes.items():
            scope.node = node

        # create a mapping from each scope back to it's enclosing scope
        for s in file.scopes.values():
            for c in s.get_children():
                c.parent = s
        file.root_scope.parent = None

        # initialize the mapping of imported names to modules
        for s in file.scopes.values():
            s.imports = {}

            
