"""SPE (c)www.stani.be 2003
This python script is useless. It just demonstrates
the functionality of the sidebar. Press the different tabs
to see how to add:
    - seperator: start a line at column 1 with '#---'
        (See Help>Seperators for more information.)
        ->the seperators will respect the hierarchy
    - todo: start a line anywhere with '# TODO:'
        ->the most important tasks will be highlighted
    - notes

Navigation:
    In all tabs of the sidebar right-clicking or double
    clicking will jump to location in the source code of
    the selected item.  In the todo, index and recent
    files (down) a normal mouse click also will work.
"""
####constants
#---math
ZERO=0

# TODO: Add more constants


####functions
#---general
def function(argument):
    return argument

#---crazy colors---#FF8040#8000FF-----------------------------------------------
def foo(x):
    return x

# TODO: Add more functions!!



####classes
class Example:
#---private---#8000FF#80FFFF----------------------------------------------------
    def __init__(self,name):
        self.name=name
    def __repr__(self):
        return name
#---public---#8000FF#80FFFF-----------------------------------------------------
    def hello():
        print hello,self.name



# TODO: Add more classes!
