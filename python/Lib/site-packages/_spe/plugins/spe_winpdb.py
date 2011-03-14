# -*- coding: ISO-8859-1 -*-

__author__  = 'www.stani.be'
__license__ = 'GPL'
__doc__ = """
This module was originally developed for SPE - Stani's Python Editor
Pleave leave this header intact.

Homepage:   http://pythonide.stani.be
Email:      spe.stani.be@gmail.com
Copyright:  (c) 2005 www.stani.be
License:    GPL (contact me for other licenses)

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by the 
Free Software Foundation; either version 2 of the License, or any later 
version.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along 
with this program; if not, write to the Free Software Foundation, Inc., 
59 Temple Place, Suite 330, Boston, MA 02111-1307 USA    
"""

import os, thread
import wx
import _spe.info as info
import _spe.Menu as Menu

from dialogs import winpdbDialog

###import winpdb
##from _spe.plugins.winpdb.rpdb2 import CSimpleSessionManager
##from _spe.plugins.winpdb.winpdb import __file__ as WINPDB

#import winpdb
plugins_dir = os.path.dirname(__file__)
winpdb_dir  = os.path.join(plugins_dir, 'winpdb')
WINPDB      = os.path.join(winpdb_dir, 'winpdb.py')

import sys
sys.path.insert(0, winpdb_dir)
from rpdb2 import CSimpleSessionManager
from rpdb2 import start_embedded_debugger
del sys.path[0]

if info.WIN and ' ' in WINPDB:
    WINPDB = '"%s"'%WINPDB
    
def _(x): return x

class SessionManager(CSimpleSessionManager):
    
    command_line    = ''
    debugger        = False
    
    #---private
    def __init__(self,runner):
        self.runner             = runner
        self.app                = runner.app
        self.encrypted          = runner.app.fCrypto
        CSimpleSessionManager.__init__(self,fAllowUnencrypted = not self.encrypted)
        
    def _ask_to_launch_debugger(self,status,message,showDialog):
        if self.debugger: return 
        child               = self.app.childActive
        child.setStatus(status)
        if showDialog:
            dlg     = wx.MessageDialog(child.frame,
                        '%s\nDo you want to launch the debugger?\n\n'%message,
                        'SPE - %s'%self.command_line,
                        wx.YES_NO | wx.NO_DEFAULT | wx.ICON_QUESTION
                       )
            answer  = dlg.ShowModal()
            dlg.Destroy()
        else:
            answer  = wx.ID_CANCEL
        if answer == wx.ID_YES:
            self.runner.debug()
        else:
            self.request_go()

    def _feedback_terminate(self):
        #print "_feedback_terminate"
        self.runner.running = False
        self.runner._check_run(False)
        child               = self.app.childActive
        child.setStatus('Terminated "%s"'%self.command_line)
        child.statusBar.throbber.stop()

    #---exception callbacks
    def unhandled_exception_callback(self):
        #print "unhandled_exception_callback"
        wx.CallAfter(self._ask_to_launch_debugger,
            status  = 'Unhandled exception at "%s"'%self.command_line,
            message = 'An unhandled exception occurred.',
            showDialog  = self.runner.exceptionPrevious)
        
    def script_about_to_terminate_callback(self):
        #print "script_about_to_terminate_callback"
        child               = self.app.childActive
        wx.CallAfter(child.setStatus, 'Terminating "%s"'%self.command_line)
        self.request_go()
            
    def script_terminated_callback(self):
        #print "script_terminated_callback"
        wx.CallAfter(self._feedback_terminate)
        self.detach()
        
    #---public
    def debug(self):
        """Attach WinPdb to the running script."""
        #todo: encrypted
        child       = self.app.childActive
        (rid, pwd)  = self.prepare_attach()
        args        = [os.P_NOWAIT,
                       info.PYTHON_EXEC,
                       info.PYTHON_EXEC,
                       #WINPDB,
                       WINPDB]
        if not self.encrypted:
            args.append('-t')
        if info.WIN:
            args.extend(['-p"%s"'%pwd])
        args.extend(['-a',rid])
        try:
            os.spawnl(*args)
            self.debugger   = True
            child.setStatus('WinPdb Debugger is attached to "%s".'%self.command_line,1)
        except Exception, message:
            child.setStatus('WinPdb Debugger failed: "%s".'%message,1)        
        
    def launch(self, command_line, fchdir = True):
        """Launch a script with rpdb2."""
        self.command_line   = command_line
        #Give launching feedback
        child               = self.app.childActive
        child.statusBar.throbber.run()
        child.setStatus('Running "%s"'%command_line)
        self.runner._check_run(True)
        self.runner.running = True
        thread.start_new_thread(CSimpleSessionManager.launch,(self,fchdir,command_line))
                
class Runner:
    
    argumentsPrevious   = []
    dlg_arguments       = ''
    exceptionPrevious   = True
    #exitPrevious        = False
    running             = False
    
    def __init__(self,app):
        self.app        = app
        #initialize
        self.session    = None
        self.title      = 'SPE - Run file'
        if app.fCrypto:
            self.title  += ' (encrypted)'        
        #assing method for check run tool button
        child                   = self.app.childActive
        if child.frame.menuBar:
            self._check_run     = child.frame.menuBar.check_run_debug
        else:
            self._check_run     = child.parentFrame.menuBar.check_run_debug

    def _debug_childActive(self):
        child               = self.app.childActive
        if child.confirmSave() and not child.isNew():
            name            = child.fileName
            debugDialog     = winpdbDialog.dialog(self.app.parentFrame,name)
            answer          = debugDialog.ShowModal()
            debugDialog.Destroy()
            if answer != wx.ID_CANCEL:
                _info       = self.app.debugInfo
                args        = [os.P_NOWAIT,
                               info.PYTHON_EXEC,
                               info.PYTHON_EXEC]
                args.extend(_info['parameters'])
                if os.path.exists(name):
                    if info.WIN and ' ' in name:
                        name    = '"%s"'%name
                    args.append(name)
                    script_args = _info['arguments']
                    if script_args:
                        args.append(script_args)
                os.spawnl(*args)
                child.setStatus('WinPdb Debugger is succesfully started.',1)
            else:
                self._check_run(False)
                child.setStatus('WinPdb Debugger was cancelled.',1)
        else:
            self._check_run(False)
            child.setStatus('File must be saved before WinPdb Debugger is launched.',1)

    def switch(self):
        "Run/stop file"
        #todo: update toolbar
        if self.running:
            self.stop()
        else:
            child           = self.app.childActive
            if child.confirmSave() and not child.isNew(): 
                self.run(child)
            else:
                self.cancel()
            
    def run(self,child):
        """Show dialog for arguments and launch script."""
        fileName            = child.fileName
        from _spe.dialogs.runWinPdbDialog import RunWinPdbDialog
        runWinPdbDialog     = RunWinPdbDialog(fileName,
                                self.argumentsPrevious,
                                self.exceptionPrevious,
                                #self.exitPrevious,
                                parent=self.app.parentFrame,
                                id=-1)
        answer              = runWinPdbDialog.ShowModal()
        dlg_arguments       = runWinPdbDialog.arguments.GetValue()
        dlg_exception       = runWinPdbDialog.exception.GetValue()
        #dlg_exit            = runWinPdbDialog.exit.GetValue()
        runWinPdbDialog.Destroy()
        if answer == wx.ID_OK:
            self.argumentsPrevious.append(self.dlg_arguments)
            self.dlg_arguments      = dlg_arguments
            self.exceptionPrevious  = dlg_exception
            #self.exitPrevious       = dlg_exit
            command_line            = fileName
            if self.dlg_arguments:
                command_line        += ' ' + self.dlg_arguments
            self.session            = SessionManager(self)
            self.session.launch(command_line)
        else: self.cancel()
        
    def stop(self):
        """Stop script."""
        try:
            self.session.stop_debuggee()
        except Exception, message:
            if message:
                child       = self.app.childActive
                child.setStatus('WinPdb unhandled exception while stopping debuggee: "%s".'%message,1)
        
    def debug(self):
        """Debug running script or current script."""
        if self.running:
            self.session.debug()
        else:
            self._debug_childActive()
                
    def cancel(self):
        """Cancel running a script. (feedback on toolbar)"""
        self._check_run(False)
        child       = self.app.childActive
        child.setStatus('Running the script with WinPdb was cancelled.',1)
        

