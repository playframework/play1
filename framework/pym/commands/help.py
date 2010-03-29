# Display help

import sys, os

NAMES = ['help']

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    if len(sys.argv) == 3:
        cmd = sys.argv[2]
    else:
        cmd = 'all'
    help_file = os.path.join(play_env["basedir"], 'documentation', 'commands', 'cmd-%s.txt' % cmd)
    if os.path.exists(help_file):
        print open(help_file, 'r').read()
    else:
        print '~ Oops, command \'%s\' not found. Try just \'play help\' to list all commands.' % cmd
        print '~'
        sys.exit(-1)
