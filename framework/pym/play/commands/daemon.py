import os, os.path
import subprocess
from play.utils import *
import time
if os.name == 'nt':
    import win32pdh, string, win32api

COMMANDS = ['start', 'stop', 'restart', 'pid', 'out']

HELP = {
    'start': 'Start the application in the background',
    'stop': 'Stop the running application',
    'restart': 'Restart the running application',
    'pid': 'Show the PID of the running application',
    'out': 'Follow logs/system.out file'
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    if command == 'start':
        start(app, args)
    if command == 'stop':
        stop(app)
    if command == 'restart':
        restart(app, args)
    if command == 'pid':
        pid(app)
    if command == 'out':
        out(app)

def start(app, args):
    app.check()
    if os.path.exists(app.pid_path()):
        pid = open(app.pid_path()).readline().strip()
        if process_running(pid):
            print "~ Oops. %s is already started (pid:%s)! (or delete %s)" % (os.path.normpath(app.path), pid, os.path.normpath(app.pid_path()))
            print "~"
            sys.exit(1)
        else:
            print "~ removing pid file %s for not running pid %s" % (os.path.normpath(app.pid_path()), pid)
            os.remove(app.pid_path())

    sysout = app.readConf('application.log.system.out')
    sysout = sysout!='false' and sysout!='off'
    if not sysout:
        sout = None
    else:
        sout = open(os.path.join(app.log_path(), 'system.out'), 'w')
    try:
        pid = subprocess.Popen(app.java_cmd(args), stdout=sout, env=os.environ).pid
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
    print "~ OK, %s is started" % os.path.normpath(app.path)
    if sysout:
      print "~ output is redirected to %s" % os.path.normpath(os.path.join(app.log_path(), 'system.out'))
    pid_file = open(app.pid_path(), 'w')
    pid_file.write(str(pid))
    print "~ pid is %s" % pid
    print "~"

def stop(app):
    app.check()
    if not os.path.exists(app.pid_path()):
        print "~ Oops! %s is not started (server.pid not found)" % os.path.normpath(app.path)
        print "~"
        sys.exit(-1)
    pid = open(app.pid_path()).readline().strip()
    kill(pid)
    os.remove(app.pid_path())
    print "~ OK, %s is stopped" % app.path
    print "~"


def restart(app, args):
    app.check()
    if not os.path.exists(app.pid_path()):
        print "~ Oops! %s is not started (server.pid not found)" % os.path.normpath(app.path)
        print "~"
    else:
        pid = open(app.pid_path()).readline().strip()
        os.remove(app.pid_path())
        kill(pid)

    sysout = app.readConf('application.log.system.out')
    sysout = sysout!='false' and sysout!='off'
    java_cmd = app.java_cmd(args)
    if not sysout:
      sout = None
    else:
      sout = open(os.path.join(app.log_path(), 'system.out'), 'w')
    try:
        pid = subprocess.Popen(java_cmd, stdout=sout, env=os.environ).pid
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
    print "~ OK, %s is restarted" % os.path.normpath(app.path)
    if sysout:
      print "~ output is redirected to %s" % os.path.normpath(os.path.join(app.log_path(), 'system.out'))
    pid_file = open(app.pid_path(), 'w')
    pid_file.write(str(pid))
    print "~ New pid is %s" % pid
    print "~"
    sys.exit(0)


def pid(app):
    app.check()
    if not os.path.exists(app.pid_path()):
        print "~ Oops! %s is not started (server.pid not found)" % os.path.normpath(app.path)
        print "~"
        sys.exit(-1)
    pid = open(app.pid_path()).readline().strip()
    print "~ PID of the running applications is %s" % pid
    print "~ "

def out(app):
    app.check()
    if not os.path.exists(os.path.join(app.log_path(), 'system.out')):
        print "~ Oops! %s not found" % os.path.normpath(os.path.join(app.log_path(), 'system.out'))
        print "~"
        sys.exit(-1)
    sout = open(os.path.join(app.log_path(), 'system.out'), 'r')
    try:
        sout.seek(-5000, os.SEEK_END)
    except IOError:
        sout.seek(0)
    while True:
        where = sout.tell()
        line = sout.readline().strip()
        if not line:
            time.sleep(1)
            sout.seek(where)
        else:
            print line

def kill(pid):
    if os.name == 'nt':
        import ctypes
        handle = ctypes.windll.kernel32.OpenProcess(1, False, int(pid))
        if not ctypes.windll.kernel32.TerminateProcess(handle, 0):
            print "~ Cannot kill the process with pid %s (ERROR %s)" % (pid, ctypes.windll.kernel32.GetLastError())
            print "~ "
            sys.exit(-1)
    else:
        try:
            os.kill(int(pid), 15)
        except OSError:
            print "~ Play was not running (Process id %s not found)" % pid
            print "~"
            sys.exit(-1)

def process_running(pid):
	if os.name == 'nt':
                return process_running_nt(pid)   
	else:
		try:
			os.kill(int(pid), 0)
			return True
		except OSError:
			return False

# loosely based on http://code.activestate.com/recipes/303339/
def process_list_nt():
    #each instance is a process, you can have multiple processes w/same name
    junk, instances = win32pdh.EnumObjectItems(None,None,'process', win32pdh.PERF_DETAIL_WIZARD)
    proc_ids={}
    proc_dict={}
    for instance in instances:
        if instance in proc_dict:
            proc_dict[instance] = proc_dict[instance] + 1
        else:
            proc_dict[instance]=0
    for instance, max_instances in proc_dict.items():
        for inum in xrange(max_instances+1):
            hq = win32pdh.OpenQuery() # initializes the query handle 
            path = win32pdh.MakeCounterPath( (None,'process',instance, None, inum,'ID Process') )
            counter_handle=win32pdh.AddCounter(hq, path) 
            win32pdh.CollectQueryData(hq) #collects data for the counter 
            type, val = win32pdh.GetFormattedCounterValue(counter_handle, win32pdh.PDH_FMT_LONG)
            proc_ids[str(val)]=instance;
            win32pdh.CloseQuery(hq) 

    return proc_ids


def process_running_nt(pid):
    if process_list_nt().get(pid,"") != "":
        return True
    else:
        return False
