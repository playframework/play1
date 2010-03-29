# Show the computed classpath for the application

NAMES = ['cp', 'classpath']

def execute(command, app, args=[]):
    print "~ Computed classpath is:"
    print "~ "
    print app.getClasspath()
    print "~ "
