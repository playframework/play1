KIKI 0.5.6 - regexes made nifty

Copyright (C) 2003, 2004 Project 5 (http://come.to/project5)
Licence: GPL (free)
Requires: Python, wxPython
System: any, provided (wx)Python runs on it

Installation
============
Make sure you install first (if not already 
present on your system):

- Python (2.3.x or newer) from:
  http://python.org

- wxPython (2.4.2.4 or later - preferably 2.5.1.5 or later) 
  from:
  http://wxpython.org

Then unpack this archive to some directory and run
"kiki.py" either by double-clicking on it or
by entering at the command line prompt 
"python kiki.py". Windows users might want to use
"Kiki.bat" to run it instead.

You might want to add it to your start menu too.
Windows users should open the Kiki directory in
the explorer. Right-click on "Kiki.bat"
and, keeping the right mouse button pressed, drag
and drop the file on the Start button.
If you want the icon in there too, proceed as follows:
- go to the Start menu and right-click on 
  "Kiki", then choose "Properties"
- go to the "Shortcut" tab and click on "Other icon"
  (not sure what it is on English systems). You will
  get a warning message, ignore it.
- click on the Browse button and go to the Kiki
  directory. Select the "kiki.ico" file and then
  click on the "Open" button.

Linux users should use whatever facilities their
distro provides to manipulate the menu.


Integration with Spe
====================
Spe is a very good Python editor, also written in 
Python/wxPython. It is available at 
http://spe.pycs.net. The distribution includes
an unmodified version of Kiki. You can access it
from the Tools menu.


Uninstalling
============
- remove the folder where you unpacked kiki.py

You can also remove the folder in your $HOME directory
called ".kiki". This is where Pears stores its stuff.

Under WinXP, the $HOME folder can be located at:
  C:\Documents and Settings\<USERNAME>


Help
====
For help, see the Help tab in the program.


Credits
=======
Once upon a time, there was a Tkinter-based application 
called "Recon - Regular expression test console", written 
by Brent Burley, which I found quite useful for writing 
regexes. I decided that I needed something with more
features and better looks. Kiki and Recon share no code
at all, but they share the design philosophy.

I decided to call my program "Ferret" (short for
"Free Environment for Regular Expression Testing"). On
second thought, I thought it would be better to name it
after the most famous (in fact after the *only* famous)
ferret I know: Kiki from the very funny Sluggy online
comic (http://sluggy.com).


Plans for future versions
=========================
 - overview of named groups


History
=======
The project history is present in "history.txt".