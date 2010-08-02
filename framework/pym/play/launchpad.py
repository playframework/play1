# The official launchpad lib is too heavy with dependancies, let's just implement only what we need

from urllib2 import urlopen, Request
import simplejson as json

BASE_URL = 'https://api.edge.launchpad.net/1.0/'

class Entry:
    def __init__(self, baseUrl):
        self.baseUrl = baseUrl

    def __getattr__(self, method, *args, **kwargs):
        return lambda *args, **kwargs: _doCall(self.baseUrl, method, kwargs)

class Project(Entry):
    def __init__(self, name):
        Entry.__init__(self, BASE_URL + name)

def _doCall(baseUrl, method, parameters):
    url = baseUrl + "?ws.op=" + method
    for key in parameters:
        url += ("&" + key + "=" + parameters[key])
    return json.loads(urlopen(Request(url)).read())

