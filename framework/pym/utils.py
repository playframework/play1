import sys
import os, os.path
import re
import random
import fileinput


def replaceAll(file, searchExp, replaceExp):
	replaceExp = replaceExp.replace('\\', '\\\\')
	searchExp = searchExp.replace('$', '\\$')
	searchExp = searchExp.replace('{', '\\{')
	searchExp = searchExp.replace('}', '\\}')
	searchExp = searchExp.replace('.', '\\.')
	for line in fileinput.input(file, inplace=1):
		line = re.sub(searchExp, replaceExp, line)
		sys.stdout.write(line)

def secretKey():
	return ''.join([random.choice('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789') for i in range(64)])

def isParentOf(path1, path2):
	if len(path2) < len(path1) or len(path2) < 2:
		return False
	if (path1 == path2):
		return True
	return isParentOf(path1, os.path.dirname(path2))

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

def override(f, t):
	fromFile = None
	for module in modules:
		pc = os.path.join(module, f)
		if os.path.exists(pc): fromFile = pc
	if not fromFile:
		print "~ %s not found in any modules" % f
		print "~ "
		sys.exit(-1)
	toFile = os.path.join(application_path, t)
	if os.path.exists(toFile):
		response = raw_input("~ Warning! %s already exists and will be overriden (y/n)? " % toFile)
		if not response == 'y':
			return
	if not os.path.exists(os.path.dirname(toFile)):
		os.makedirs(os.path.dirname(toFile))
	shutil.copyfile(fromFile, toFile)
	print "~ Copied %s to %s " % (fromFile, toFile)

def isParentOf(path1, path2):
	if len(path2) < len(path1) or len(path2) < 2:
		return False
	if (path1 == path2):
		return True