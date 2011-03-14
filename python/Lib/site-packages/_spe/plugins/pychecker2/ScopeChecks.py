
from pychecker2.Check import Check
from pychecker2.Warning import Warning
from pychecker2 import util

from compiler.ast import *

class RedefineCheck(Check):
    redefinedScope = Warning('Report redefined scopes',
                             'Scope (%s) is redefined at line %d')

    def check(self, file, unused_checker):
        names = {}                      # map name, parent to this scope
        for node, scope in file.scopes.items():
            if hasattr(node, 'name'):	# classes, functions
                key = (scope.parent, node.name)
                if names.has_key(key):
                    # oops, another scope has the same name and parent
                    first = node
                    second = names[key]
                    # but don't warn if the parent node is the same If or Try
                    if util.try_if_exclusive(first, second):
                        continue
                    if first.lineno > second.lineno:
                        second, first = first, second
                    file.warning(first, self.redefinedScope,
                                 first.name, second.lineno)
                names[key] = node
