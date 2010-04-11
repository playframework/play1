import imp
import os

class CommandLoader:
    def __init__(self, play_path):
        self.path = os.path.join(play_path, 'framework', 'pym', 'commands')
        self.commands = {}
        self.modules = {}
        self.load_core()

    def load_core(self):
        for filename in os.listdir(self.path):
            if filename != "__init__.py" and filename.endswith(".py"):
                name = filename.replace(".py", "")
                mod = load_python_module(name, self.path)
                self._load_cmd_from(mod)

    def load_play_module(self, modname):
        try:
            leafname = os.path.basename(modname).split('.')[0]
            mod = imp.load_source(leafname, os.path.join(modname, "commands.py"))
            self._load_cmd_from(mod)
        except:
            pass # No command to load in this module

    def _load_cmd_from(self, mod):
        try:
            for name in mod.COMMANDS:
                if name in self.commands:
                    print "~ Warning: conflict on command " + name
                self.commands[name] = mod
            if 'MODULE' in dir(mod):
                self.modules[mod.MODULE] = mod
        except Exception:
            print "~ Warning: error loading command " + name

def load_python_module(name, location):
    mod_desc = imp.find_module(name, [location])
    return imp.load_module(name, mod_desc[0], mod_desc[1], mod_desc[2])

