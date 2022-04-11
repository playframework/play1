from __future__ import print_function
from builtins import str
import os, os.path
import shutil
import getopt
import urllib.request, urllib.error, urllib.parse

from play.utils import *

COMMANDS = ['status', 'st']

HELP = {
    'status': 'Display the running application\'s status'
}

def execute(**kargs):
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    url = ''
    secret_key = ''

    try:
        optlist, args2 = getopt.getopt(args, '', ['url=', 'secret='])
        for o, a in optlist:
            if o in ('--url'):
                if a.endswith('/'):
                    url = a + '@status'
                else:
                    url = a + '/@status'
            if o in ('--secret'):
                secret_key = a
    except getopt.GetoptError as err:
        print("~ %s" % str(err))
        print("~ ")
        sys.exit(-1)

    if not url or not secret_key:
        app.check()
        if not url:
            http_port = int(app.readConf('http.port'))
            url = 'http://localhost:%s/@status' % http_port
        if not secret_key:
            secret_key = app.readConf('application.statusKey')

    try:
        proxy_handler = urllib.request.ProxyHandler({})
        req = urllib.request.Request(url)
        req.add_header('Authorization', secret_key)
        opener = urllib.request.build_opener(proxy_handler)
        status = opener.open(req)
        print('~ Status from %s,' % url)
        print('~')
        print(status.read())
        print('~')
    except urllib.error.HTTPError as e:
        print("~ Cannot retrieve the application status... (%s)" % (e.code))
        print("~")
        sys.exit(-1)
    except urllib.error.URLError as e:
        print("~ Cannot contact the application...")
        print("~")
        sys.exit(-1)
    print()
