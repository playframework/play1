# Show the computed classpath for the application

NAMES = ['cp', 'classpath']

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    print "~ Computed classpath is:"
    print "~ "
    print app.getClasspath()
    print "~ "
