from play.utils import *

COMMANDS = ['secret']

def execute(**kargs):
    app = kargs.get("app")

    app.check()
    print "~ Generating the secret key..."
    sk = secretKey()
    replaceAll(os.path.join(app.path, 'conf', 'application.conf'), r'application.secret=.*', 'application.secret=%s' % sk)
    print "~ Keep the secret : %s" % sk
    print "~"
