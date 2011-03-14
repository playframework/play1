#!BPY

#"""
#Name: 'Stani Python Editor'
#Blender:243
#Group: 'System'
#Tooltip: 'Python IDE for Blender'
#"""

__author__ = "Stani (www.stani.be)"
__url__ = ("http://pythonide.stani.be/")
__email__ = ("witold-jaworski@poczta.neostrada.pl")
__version__ = "0.8.4.b"
__bpydoc__ = """
This small script, written by Witold Jaworski, opens the SPE (Stani's Python Editor).

Spe is a python IDE with auto-indentation, auto completion,
call tips, syntax coloring, syntax highlighting, class
explorer, source index, auto todo list, sticky notes,
integrated pycrust shell, python file browser, recent file
browser, drag&drop, context help, ... Special is its blender
support with a blender 3d object browser and its ability to
run interactively inside blender. Spe ships with wxGlade (gui
designer), PyChecker (source code doctor) and Kiki (regular
expression console). Spe is extensible with wxGlade.

Spe can be used in Blender as an "side-kick" editor: once used, it
opens quickly during session. As long it is open, Blender redraws 
its window, but all its commands are not availabe. In fact, you can
control it by issuing a Blender API calls from Spe's shell console.<br>
Typical patter of usage:<br>
When you have finished to modify a script in SPE:<br>
	- transfer it into Blender's Text Editor (using Spe 'Load into Blender' menu command),<br>
	- close the SPE,<br>
	- select the script in Text Editor,<br>
	- run it to test (you can use to track it in the WinPdb debugger).<br>
Then, when you will have to make another non-trivial modification to script,<br>
	- open the Spe again.<br>
It will open with the same file, at the same position, where you have closed it.<br>
Make the change, transfer to Blender, and test again. 
"""

#copy this script to the .blender/scripts directory or user script directory
import sys
if sys.modules.has_key('_spe.SPE'):
   reload(sys.modules.get('_spe.SPE'))
else:  
   import _spe.SPE
