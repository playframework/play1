# Display help

import sys, os, re

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

    if len(args) == 1:
        cmd = args[0]
        help_file = os.path.join(play_env["basedir"], 'documentation', 'commands', 'cmd-%s.txt' % cmd)
        if os.path.exists(help_file):
            print open(help_file, 'r').read()
        else:
            exists = False
            slugCmd = re.sub('[-\s]+', '-', re.sub('[^\w\s-]', '', cmd.encode('ascii', 'ignore')).strip().lower())
            for module in app.modules():
                help_file = os.path\
                    .join(module, 'documentation', 'commands',
                        'cmd-%s.txt'
                          % slugCmd)
                exists = os.path.exists(help_file)
                if exists:
                    print open(help_file, 'r').read()
                    break
            if not exists:
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
    print "~ Also refer to documentation at https://www.playframework.com/documentation"
    print "~"

def isCore(mod, play_env):
    path = os.path.realpath(mod.__file__)
    directory = os.path.realpath(play_env["basedir"])

    isCore = True
    try:
        relpath = os.path.relpath(path, directory)
        if relpath.startswith(os.pardir):
            isCore = False
        else:
            if relpath.startswith('modules'):
                isCore = False
            else:
                isCore = mod.__file__.find(play_env["basedir"]) == 0
    except ValueError:
        isCore = False

    return isCore

