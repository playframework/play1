wxGlade: a GUI builder for wxPython/wxWindows
version: 0.5
license: MIT (see license.txt)

THIS PROGRAM COMES WITH NO WARRANTY

* Requirements:
---------------
Python >= 2.3
wxPython >= 2.6


* Installation:
---------------
If you are reding this file, you already did all the necessary :-)
To start the program, enter ``python wxglade.py'' in your shell


* Documentation:
----------------
In the docs/ subdir there's a short introductory tutorial.  In the examples/
subdir there are some sample wxGlade apps (in xml format, .wxg file extension).


* Known Bugs/Issues:
--------------------
- If you have PyXML installed, be sure to use at least version 0.8.1, as
  otherwise you will not be able to generate code and load saved wxg
  files. This seems to be a PyXML bug, see
  http://sourceforge.net/tracker/?func=detail&atid=106473&aid=573011&group_id=6473 

- On Windows, selection tags may not be shown properly in some cases.

- On GTK, if your last operation before exiting is a paste from the clipboard,
  wxGlade exits with a segfault.

- On GTK, menubars give troubles: they produce a lot of Gtk-WARNING and 
  Gtk-FATAL messages and may cause segfaults.

- On GTK, notebooks can cause some Gtk-WARNING messages, but they seem to work 
  anyway.


For any kind of question, there's a mailing list at 
    https://lists.sourceforge.net/lists/listinfo/wxglade-general
If you don't want to follow the list, you can reach me at 
    agriggio@users.sourceforge.net

Enjoy!
Alberto Griggio

$Id: README.txt,v 1.14 2007/03/27 07:02:07 agriggio Exp $
