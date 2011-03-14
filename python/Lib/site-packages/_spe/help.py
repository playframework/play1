#(c)www.stani.be
import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Content for context menu."""

__doc__=INFO['doc']%INFO

MENU=\
"""To add your own menu, see Help>Add menu.
See framework/menus/menu_User.py for an example.
"""

SHELL=\
"""Press 'Ctrl-SPACE' in the shell to jump to file and line number after error.
Press ESC to clear up the shell.
In the next tab 'Locals' you can browse the namespace of this shell.
Drag and drop here any files to run."""

LOCALS=\
"""Explore objects in the namespace tab.
Double click any item to browse in a seperate PyFilling window."""

SESSION=\
"""The session tab records the commands of the Shell."""

FIND=\
"""Tool to search recursively through text."""

BROWSER=\
"""This shows all file of a folder and subfolders. Set depth to the level of nested subfolders.
Left click a file to open it.
Right click to run it.
Drag and drop here any directory to add."""

RECENT=\
"""Left click a recent file to open it.
Right click to run it.
Drag and drop here any files to add."""

TODO=\
"""Overview of the todo tasks of all open files.  Click to jump to the source location.
To add a todo task anywhere in the source code, start a line with '# TODO:' and let it follow by a description of the task.
Press F5 or save to refresh."""

INDEX=\
"""Alphabetical listing of the defined methods and classes in all open files.  Click to jump to the source location.
Press F5 or save to refresh."""

NOTES=\
"""These notes will always be saved and opened in Spe."""

BLENDER=\
"""Blender browser. Choose an item to browse...
It is required that you launch spe from Blender and that you have blenpy installed to access this feature."""

CHILD_EXPLORE=\
"""Explore the class and method tree of the source code.
Right or double click to jump to source code.
See Help>Seperators about inserting separators.
Press F5 or save to refresh."""

CHILD_TODO=\
"""Every comment starting with '# TODO:' will appear here. 
By adding '!' you can give priority. 
Click to jump to the source code.
Press F5 or save to refresh."""

CHILD_INDEX=\
"""Alphabetical listing of the defined methods and classes.  Click to jump to the source location.
Press F5 or save to refresh."""

CHILD_NOTES=\
"""These notes will be saved as 'fileName.txt'.  If it is empty, this file will be deleted."""

CHILD_SOURCE=\
"""See Help>Shortcuts about keyboard shortcuts.
See Help>Seperators about inserting separators."""

