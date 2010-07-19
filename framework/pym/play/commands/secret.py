from play.utils import *

COMMANDS = ['secret']

HELP = {
    'secret': 'Generate a new secret key'
}

def execute(**kargs):
    app = kargs.get("app")

    app.check()
    print "~ Generating the secret key..."
    sk = secretKey()
    replaceAll(os.path.join(app.path, 'conf', 'application.conf'), r'application.secret=.*', 'application.secret=%s' % sk, True)
    print "~ Keep the secret : %s" % sk
    print "~"
