# Display help

import sys, os

COMMANDS = ['help']

HELP = {
    'help': 'Display help on a specific command'
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")
    cmdloader = kargs.get("cmdloader")

    if len(sys.argv) == 3:
        cmd = sys.argv[2]
        help_file = os.path.join(play_env["basedir"], 'documentation', 'commands', 'cmd-%s.txt' % cmd)
        if os.path.exists(help_file):
            print open(help_file, 'r').read()
        else:
            print '~ Oops, command \'%s\' not found. Try just \'play help\' to list all commands.' % cmd
            print '~'
            sys.exit(-1)
    else:
        main_help(cmdloader.commands, play_env)

def main_help(commands, play_env):
    modules_commands = []
    print "~ For all commands, if the application is not specified, the current directory is used"
    print "~ Use 'play help cmd' to get more help on a specific command"
    print "~"
    print "~ Core commands:"
    print "~ ~~~~~~~~~~~~~~"
    for cmd in sorted(commands):
        if not isCore(commands[cmd], play_env):
            modules_commands.append(cmd)
            continue
        if 'HELP' in dir(commands[cmd]) and cmd in commands[cmd].HELP:
            print "~ " + cmd + (' ' * (16 - len(cmd))) + commands[cmd].HELP[cmd]
    if len(modules_commands) > 0:
        print "~"
        print "~ Modules commands:"
        print "~ ~~~~~~~~~~~~~~~~~"
        for cmd in modules_commands:
            if 'HELP' in dir(commands[cmd]) and cmd in commands[cmd].HELP:
                print "~ " + cmd + (' ' * (20 - len(cmd))) + commands[cmd].HELP[cmd]
    print "~"
    print "~ Also refer to documentation at http://www.playframework.org/documentation"
    print "~"

def isCore(mod, play_env):
    return mod.__file__.find(play_env["basedir"]) == 0
