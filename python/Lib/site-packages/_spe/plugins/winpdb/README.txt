


Winpdb - A GPL Python Debugger

Contact: Nir Aides
Email:   nir@winpdb.org
Website: http://www.winpdb.org/
         http://sourceforge.net/projects/winpdb/
Version: 1.3.4



Requirements

    CPython
    Winpdb is compatible with CPython 2.3 or later. Winpdb is NOT 
    compatible with Jython or IronPython. (http://www.python.org/download/)

    wxPython
    To use the Winpdb GUI you need wxPython 2.6.x or later 
    installed. You can still use rpdb2 which is the console version of the 
    debugger without wxPython.

    Most Linux distributions include wxPython as a package called python-wxgtk. 
    Use your distribution’s package manager (e.g. synaptic, aptitude or yum) 
    to find and install it.

    On Windows you need to install the wxPython runtime from 
    http://www.wxpython.org/ (The unicode version is preferred).

	
Installation

    In a console with admin privileges type:

        python setup.py install -f

    On Ubuntu you can type in a normal console:
        
        sudo python setup.py install -f

    Where do the files go? 

    The setup script copies rpdb2.py and winpdb.py modules to the Python 
    site-packages folder. The scripts rpdb2, winpdb are copied to the 
    Python binaries (scripts) folder:

    On Linux this folder is usually /usr/bin and is in the path by default. 

    On Windows this folder is %PYTHONHOME%\Scripts and is not in the path by
    default.


    Insufficient permissions?

    In the event of insufficient permissions, installation can be avoided 
    completely. To use Winpdb simply launch it from the folder in which it is 
    placed.



Launch Time

    On Linux systems start the debugger from a console with:

        winpdb

    On Windows systems start the debugger with:

        %PYTHONHOME%\Scripts\winpdb

    Note that the Python interpreter must be in the PATH for this to work.



Documentation

    Use the -h command-line flag for command-line help.

    Use the RPDB2 console 'help' command for detailed description of debugger 
    commands.

    Online documentation is available at:
    http://www.winpdb.org



Further Development

    Winpdb is open source. If you would like it to develop further you are
    welcome to contribute to development, send feedback or make a donation.

	

