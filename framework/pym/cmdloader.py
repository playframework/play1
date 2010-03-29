import imp
import os

commands = {}

def load_all(play_path):
    for filename in os.listdir(os.path.join(play_path, 'framework', 'pym', 'commands')):
        if filename != "__init__.py" and filename.endswith(".py"):
            name = filename.replace(".py", "")
            mod = load_module(play_path, name)
            try:
                for name in mod.NAMES:
                    if name in commands:
                        print "Warning: conflict on command " + name
                    commands[name] = mod
            except:
                print "Warning: error loading command " + name

def load_module(base, name):
    mod_desc = imp.find_module(name, [os.path.join(base, 'framework', 'pym', 'commands')])
    return imp.load_module(name, mod_desc[0], mod_desc[1], mod_desc[2])
