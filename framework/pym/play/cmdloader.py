from __future__ import print_function
import importlib.util
import importlib.machinery
import os
import warnings
import traceback

def play_formatwarning(msg, *a):
    # ignore everything except the message
    # format the message in a play cmdline way
    return '~'+ '\n'+ '~ '+ str(msg) + '\n~\n'

warnings.formatwarning = play_formatwarning

class CommandLoader(object):
    def __init__(self, play_path):
        self.path = os.path.join(play_path, 'framework', 'pym', 'play', 'commands')
        self.commands = {}
        self.modules = {}
        self.load_core()

    def load_core(self):
        for filename in os.listdir(self.path):
            if filename != "__init__.py" and filename.endswith(".py"):
                try:
                    module_name = filename.replace(".py", "")
                    module_path = os.path.join(self.path, filename)
                    mod = load_python_module(module_name, module_path)
                    self._load_cmd_from(mod)
                except Exception as e:
                    print (e)
                    traceback.print_exc()
                    warnings.warn("!! Warning: could not load core command file " + filename, RuntimeWarning)

    def load_play_module(self, modname):
        commands = os.path.join(modname, "commands.py")
        if os.path.exists(commands):
            try:
                leafname = os.path.basename(modname).split('.')[0]
                # print(f"Loading commands for module \"{modname}\"")
                mod = load_source(leafname, os.path.join(modname, "commands.py"))
                self._load_cmd_from(mod)
            except Exception as e:
                print('~')
                print('~ !! Error while loading %s: %s' % (commands, e))
                print('~')
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
#    print(f"Loading module \"{name}\" at location \"{location}\"")
    spec = importlib.util.spec_from_file_location(name, location)
    if spec is None:
        raise ImportError(f"Could not find module {name} at {location}")

    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)

    return mod


# Obtained from https://docs.python.org/dev/whatsnew/3.12.html#imp
def load_source(modname, filename):
    loader = importlib.machinery.SourceFileLoader(modname, filename)
    spec = importlib.util.spec_from_file_location(modname, filename, loader=loader)
    module = importlib.util.module_from_spec(spec)
    # The module is always executed and not cached in sys.modules.
    # Uncomment the following line to cache the module.
    # sys.modules[module.__name__] = module
    loader.exec_module(module)
    return module
