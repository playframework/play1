#!BPY

#"""
#Name: 'Attach Winpdb debugger'
#Blender: 243
#Group: 'System'
#Tooltip: 'Enables a Python debugger for Blender scripts'
#"""

#
# Copy this script to the .blender/scripts directory. or user scripts directory
#

__author__ = "Nir Aides"
__url__ = ("http://www.digitalpeers.com/pythondebugger")
__version__ = "1.3.1"
__email__ = ("witold-jaworski@poczta.neostrada.pl")
__bpydoc__ = """\
Winpdb, created by Nir Aides, is a platform independent GPL Python debugger,<br>
with support for:<br>
- multiple threads,<br>
- namespace modification,<br>
- embedded debugging <br>
  (and because of that it is suitable to use for Blender Python scripts),

and is up to 20 times faster than pdb.

This small script, written by Witold Jaworski, enables WinPdb to debug Python
scripts that are running inside Blender.<br>
It opens the Winpdb, and attaches it to the Blender Python environment.

IMPORTANT: <br>
When WinPdb window is opened for the first time in a Blender session, you have
to press F5 (Continue) in WinPdb, to let Blender to continue (to "unfreeze" its
 screen).

USAGE:<br>
To break into a script:<br>
    - press "Break" button in WinPdb;<br>
    - load the script to Blender's Text Editor;<br>
    - run the script, pressing Alt-P

The WindPdb window will appear. You can debug your script there.
You can control, wheter the code has or not have to be debugged, by pressing the WinPdb
"break" command button.

Remember: Use Winpdb by setting the break mode when it is needed, as long, as you
run Blender session. Always close Blender first, unless you have detached the WinPdb
before (using "Detach" command from the File menu - it is preffered way, but not
 required). When you close Winpdb WITHOUT detaching it, Blender.exe will crash
during shutdown. (Because in such case Winpdb tries to stop Blender's Python
engine).
"""

import os
import sys
import subprocess

#
# helper parameters - usually you do not need to change them:
#
WAIT_TIMEOUT  = 15        #time that this script will wait for attaching a Winpdb 
PASSWORD      = 'blender' #Winpdb requires a password between the client and 
                          #the server, so this is the one we will use.

#
# We will try to load rpdb2 from implicit path. It may work for a standalone 
# Winpdb installation. This way this script may work with newer version of 
# Winpdb, than this one that is shipped with SPE:
#
try:#looking for the default/standalone  winpdb installation:
    import rpdb2
except ImportError: #we will try localize it within SPE directories:
    import _spe.plugins.spe_winpdb #import this, to use winpdb packed with SPE

from rpdb2 import start_embedded_debugger, __file__ as rpdb2_file

def debug(what):
    #
    # We will form the argument list for Winpdb invocation:
    # 

    #
    # Argument 1: Python executable name. 
    # (We cannot utilize here os.executable, because it returns "Blender.exe", 
    # instead "python.exe)"
    #
    if os.name in ("nt","mac"): args=["pythonw"] # On Windows and Mac use this
    else: args = ["python"] #On Linux and other systems - standard python
    #
    # Argument 2: full path to Winpdb.py script name 
    #
    args.append(os.path.join(os.path.dirname(rpdb2_file),"winpdb.py"))
    #
    # Argument 3: -a(ttach) specified script
    #
    args.append("-a")
    #
    # Argument 4: -p(assword): it is only available on nt systems.
    #
    if os.name == "nt": 
        args.append("-p" + PASSWORD)
    #
    # Argument 5: name of the debugged script
    #
    args.append(what)
    #
    # Finally: run WinPdb...
    #
    if os.name == "nt":
        pid = subprocess.Popen(args) 
        
    else:
        #
        # On Linux systems: we have to pass the password with stdin!
        #
        pid = subprocess.Popen(args, stdin = subprocess.PIPE, stdout = subprocess.PIPE)
        pid.stdin.write(PASSWORD + "\n")
        pid.stdin.flush()
    #
    # ....and let it to connect, waiting for <timeout> seconds:
    #
    start_embedded_debugger(PASSWORD, timeout = WAIT_TIMEOUT)

#
# Every script in Blender is represented by the same name: <string>
#
debug("<string>")
