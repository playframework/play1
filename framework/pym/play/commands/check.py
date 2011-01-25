import os, os.path
import shutil
import urllib, urllib2
import simplejson as json

from play.utils import *

COMMANDS = ['check']

HELP = {
    'check': 'Check for a release newer than the current one'
}

TAGS_URL = "http://github.com/api/v2/json/repos/show/playframework/play/tags"


def execute(**kargs):
    args = kargs.get("args")
    play_env = kargs.get("env")

    if len(sys.argv) == 3:
        version = sys.argv[2]
    else:
        version = playVersion(play_env)

    current = Release(version)
    releases = allreleases()

    if len(releases) == 0:
        print "~ No release found."
    elif current == max(releases):
        print "~ You are using the latest version."
    else:
        print "~  \tLatest release: " + str(max(releases))
        print "~  \tYour version  : " + str(current)
        print "~"
        print "~ Latest release download: " + max(releases).url()

    print "~"


def allreleases():
    try:
        req = urllib2.Request(TAGS_URL)
        req.add_header('Accept', 'application/json')
        opener = urllib2.build_opener()
        result = opener.open(req)
        return map(lambda x: Release(x), json.loads(result.read())["tags"])
    except urllib2.HTTPError, e:
        print "~ Oops,"
        print "~ Cannot contact github..."
        print "~"
        sys.exit(-1)
    except urllib2.URLError, e:
        print "~ Oops,"
        print "~ Cannot contact github..."
        print "~"
        sys.exit(-1)

class Release:

    # TODO: Be smarter at analysing the rest (ex: RC1 vs RC2)
    def __init__(self, strversion):
        self.strversion = strversion
        try:
            self.numpart = re.findall("\d+[\.\d+]+", strversion)[0]
        except:
            self.numpart = ''
        self.rest = strversion.replace(self.numpart, "")
        try:
            self.versions = map(lambda x: int(x), self.numpart.split("."))
        except:
            self.versions = [0,0]
        if not self.rest: self.rest = "Z"

    def url(self):
        return "http://download.playframework.org/releases/play-" + self.strversion + ".zip"

    def __eq__(self, other):
        return self.strversion == other.strversion
    def __lt__(self, other):
        try:
            if self == other:
                return False
            for i in range(len(self.versions)):
                if self.versions[i] < other.versions[i]:
                    return True # ex: 1.1 vs 1.2
                if self.versions[i] > other.versions[i]:
                    return False
            if len(self.versions) < len(other.versions):
                return True
            if len(self.versions) > len(other.versions):
                return False
            # From here, numeric part is the same - now having a rest means older version
            if len(other.numpart) > 0 and len(self.numpart) == 0:
                return False
            if len(self.numpart) > 0 and len(other.numpart) == 0:
                return True
            # Both have a rest, use a string comparison
            # alpha1 < beta1 < rc1 < rc2...
            return self.rest < other.rest
        except:
            return False
    def __le__(self, other):
        return self == other or self < other
    def __gt__(self, other):
        return not (self <= other)
    def __ge__(self, other):
        return not (self < other)
    def __ne__(self, other):
        return not (self == other)

    def __repr__(self):
        return self.strversion

    def __str__(self):
        return self.strversion
