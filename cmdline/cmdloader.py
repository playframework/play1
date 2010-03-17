import imp
import os

commands = {}

def load_all(play_path):
    for filename in os.listdir(os.path.join(play_path, 'cmdline', 'commands')):
        if filename != "__init__.py" and filename.endswith(".py"):
            print "import " + filename
            name = filename.replace(".py", "")
            mod = load_module(play_path, name)
            for name in mod.NAMES:
                commands[name] = mod

def load_module(base, name):
    mod_desc = imp.find_module(name, [os.path.join(base, 'cmdline', 'commands')])
    return imp.load_module(name, mod_desc[0], mod_desc[1], mod_desc[2])
