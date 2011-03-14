from pychecker2.Check import Check
from pychecker2.Check import Warning
from pychecker2 import util

from compiler import walk

class ModuleReference:
    def __init__(self, localname, remotename, module, nodes):
        self.localname = localname
        self.remotename = remotename
        self.module = module
        self.nodes = nodes

    def __getstate__(self):
        return (self.localname, self.remotename, self.module.__name__, 
                self.nodes)

    def __setstate__(self, data):
        (self.localname, self.remotename, module, self.nodes) = data
        self.module = __import__(module, globals(), {}, [''])

class ImportCheck(Check):
    '''
Get 'from module import *' names hauled into the file and modules.
Figure out which names come from 'import name'.
    '''

    importError = Warning('Report/ignore imports that may fail',
                          'Error trying to import %s: %s')
    duplicateImport = Warning('Report/ignore duplicate imports',
                              'Import of "%s" is duplicate%s')
    shadowImport = Warning('Report imports which shadow names from '
                           'other imports',
                           'Import of "%s" duplicates import from '
                           'module %s at %d')

    def check(self, file, checker):
        def try_import(name, node):
            try:
                return __import__(name, globals(), {}, [''])
            except ImportError, detail:
                file.warning(node, ImportCheck.importError, name, str(detail))
            except Exception, detail:
                file.warning(node, ImportCheck.importError, name, str(detail))
            return None

        def add_import(node, local, remote, module):
            scopes = util.enclosing_scopes(file.scopes, node)
            for scope in scopes:
                try:
                    ref = scope.imports[local]
                    if not util.try_if_exclusive(ref.nodes, node):
                        if ref.module == module:
                            if scope == scopes[0]:
                                extra = " in current scope"
                            else:
                                extra = " of import in parent scope %s" % scope
                            file.warning(node, ImportCheck.duplicateImport,
                                         local, extra)
                        else:
                            file.warning(node, ImportCheck.shadowImport,
                                         local,
                                         ref.module.__name__,
                                         ref.nodes.lineno)
                except KeyError:
                    pass
            scopes[0].imports[local] = ModuleReference(local, remote,
                                                       module, node)
            checker.check_module(module)
            
        class FromImportVisitor:

            def visitFrom(self, node):
                m = try_import(node.modname, node)
                if m:
                    for module_name, local_name in node.names:
                        if module_name == '*':
                            for name in dir(m):
                                if not name.startswith('_'):
                                   add_import(node, name, name, m)
                        else:
                            add_import(node, local_name or module_name,
                                       module_name, m)

            def visitImport(self, node):
                for module, name in node.names:
                    m = try_import(module, node)
                    if m:
                        add_import(node, name or module, None, m)


        if file.root_scope:
            walk(file.root_scope.node, FromImportVisitor())
