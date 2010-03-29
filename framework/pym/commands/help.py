# Display help

import sys, os

NAMES = ['help']

def execute(command, app, args=[]):
    if len(sys.argv) == 3:
        cmd = args[2]
    else:
        cmd = 'all'
    help_file = os.path.join(app.play_base, 'documentation', 'commands', 'cmd-%s.txt' % cmd)
    if os.path.exists(help_file):
        print open(help_file, 'r').read()
    else:
        print '~ Oops, command \'%s\' not found. Try just \'play help\' to list all commands.' % cmd
        print '~'
        sys.exit(-1)
