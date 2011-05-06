import imp
import os
import warnings

def play_formatwarning(msg, *a):
    # ignore everything except the message
    # format the message in a play cmdline way
    return '~'+ '\n'+ '~ '+ str(msg) + '\n~\n'

warnings.formatwarning = play_formatwarning

class CommandLoader:
    def __init__(self, play_path):
        self.path = os.path.join(play_path, 'framework', 'pym', 'play', 'commands')
        self.commands = {}
        self.modules = {}
        self.load_core()

    def load_core(self):
        for filename in os.listdir(self.path):
            if filename != "__init__.py" and filename.endswith(".py"):
                try:
                    name = filename.replace(".py", "")
                    mod = load_python_module(name, self.path)
                    self._load_cmd_from(mod)
                except:
                    warnings.warn("!! Warning: could not load core command file " + filename, RuntimeWarning)

    def load_play_module(self, modname):
        commands = os.path.join(modname, "commands.py")
        if os.path.exists(commands):
            try:
                leafname = os.path.basename(modname).split('.')[0]
                mod = imp.load_source(leafname, os.path.join(modname, "commands.py"))
                self._load_cmd_from(mod)
            except Exception, e:
                print '~'
                print '~ !! Error while loading %s: %s' % (commands, e)
                print '~'
                pass # No command to load in this module

    def _load_cmd_from(self, mod):
        if 'COMMANDS' in dir(mod):
            for name in mod.COMMANDS:
                try:
                    if name in self.commands:
                        warnings.warn("Warning: conflict on command " + name)
                    self.commands[name] = mod
                except Exception:
                    warnings.warn("Warning: error loading command " + name)
        if 'MODULE' in dir(mod):
            self.modules[mod.MODULE] = mod

def load_python_module(name, location):
    mod_desc = imp.find_module(name, [location])
    mod_file = mod_desc[0]
    try:
        return imp.load_module(name, mod_desc[0], mod_desc[1], mod_desc[2])
    finally:
        if mod_file is not None and not mod_file.closed:
            mod_file.close()

