
class BaseVisitor:

    def visit(self, unused_node):
        "method is really overridden by compiler.visitor.ASTVisitor"
        assert 0, 'Unreachable'

    def visitChildren(self, n):
        for c in n.getChildNodes():
            self.visit(c)     

def try_if_exclusive(stmt_node1, stmt_node2):
    from compiler import ast as ast
    
    """return true if the statements are in exclusive parts of if/elif/else
    or try/finally/else"""
    
    parent = stmt_node1.parent.parent
    if parent == stmt_node2.parent.parent:
        if isinstance(parent, ast.If):
            parts = [code for test, code in parent.tests]
            parts.append(parent.else_)
            for part in parts:
                if part and stmt_node1 in part.nodes:
                    return stmt_node2 not in part.nodes
        if isinstance(parent, ast.TryExcept):
            parts = []
            if parent.body:
                parts.extend(parent.body.nodes)
            if parent.else_:
                parts.extend(parent.else_.nodes)
            return not (stmt_node1 in parts and stmt_node2 in parts)
    return None

def parents(obj):
    class Parents:
        def __init__(self, start):
            self.next = start
        def __call__(self):
            retval = self.next.parent
            self.next = retval
            return retval
    return iter(Parents(obj), None)

def enclosing_scopes(scopes, node):
    result = []
    n = node
    while n:
        try:
            result.append(scopes[n])
        except KeyError:
            pass
        n = n.parent
    return result

def type_filter(seq, *classes):
    return [s for s in seq if isinstance(s, classes)]

    
def dict_minus(a, b):
    r = {}
    for k, v in a.iteritems():
        if not b.has_key(k):
            r[k] = v
    return r

def dict_intersect(a, b):
    r = {}
    for k, v in a.iteritems():
        if b.has_key(k):
            r[k] = v
    return r

