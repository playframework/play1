# Show the computed classpath for the application

COMMANDS = ['cp', 'classpath']

HELP = {
    'classpath': 'Display the computed classpath'
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    print "~ Computed classpath is:"
    print "~ "
    print app.getClasspath()
    print "~ "
