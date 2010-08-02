import os, os.path
import shutil

from play.utils import *
import play.launchpad as launchpad

COMMANDS = ['check']

HELP = {
    'check': 'Check for a release newer than the current one'
}

def execute(**kargs):
    args = kargs.get("args")
    play_env = kargs.get("env")

    if len(sys.argv) == 3:
        version = sys.argv[2]
    else:
        version = playVersion(play_env)

    lpPlay = launchpad.Project("play")

    if len(version) > 2:
        series = lpPlay.getSeries(name=version[0:3])
    if series is None:
        print "~ Error: unable to determine series for version " + version + "."
        print "~"
        sys.exit(-1)

    lpSeries = launchpad.Entry(series["self_link"])
    releases = filter(lambda x: x["type"] == "release", lpSeries.get_timeline()["landmarks"])

    if len(releases) == 0:
        print "~ No release for the requested series. Are you on a development branch?"
    elif isRelease(version, releases):
        if releases[0]["name"] == version:
            print "~ You are using the latest version of the serie (" + version + ")."
        else:
            print "~  ***** NEW RELEASE: " + releases[0]["name"] + " *****"
            print "~      released on " + releases[0]["date"]
            print "~"
            print "~ Please upgrade: https://launchpad.net" + releases[0]["uri"]
    else:
        print "~ You don't seem to be using an official release."
        print "~ Latest release is " + releases[0]["name"] + ", released on " + releases[0]["date"]

    print "~"

def isRelease(version, releases):
    for release in releases:
        if release["name"] == version:
            return True
    return False

