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
            mod = load_python_module("commands", modname)
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
        except Exception as ex:
            print "~ Warning: error loading command " + name
            print ex

def load_python_module(name, location):
    mod_desc = imp.find_module(name, [location])
    return imp.load_module(name, mod_desc[0], mod_desc[1], mod_desc[2])
