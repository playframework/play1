####(c)www.stani.be-------------------------------------------------------------
import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Session as tab.

pyChecker support by Nicodemus"""

__doc__=INFO['doc']%INFO

####Panel class-----------------------------------------------------------------

import wx, re, os, sys
import _spe.help

INTRO   = """Portions Copyright 2003-2007 www.stani.be - see credits in manual for further copyright information.
Please donate if you find this program useful (see help menu). Double click to jump to error source code."""

class Shell(wx.py.shell.Shell):
    def __init__(self,app,**keyw):
        self.locals = keyw['locals']  = {'SPEapp':app,'__name__':'__main__'}
        wx.py.shell.Shell.__init__(self,**keyw)
        self.SetUseAntiAliasing(True)
        self.Bind(wx.EVT_LEFT_DCLICK,self.jumpToSource) 
        
    def jumpToSource(self,event=None):
        line    = self.GetCurrentLine()
        text    = None
        while not text and line > 0:
            text=self.GetLine(line)
            if text.find('File')!=-1 and text.find('line')!=-1:
                text='{%s}'%text.split('in ')[0].strip().replace('File ','"file":r').replace('line','"line":')
                try:
                    text=eval(text)
                except:
                    text=None
            else: text=None
            line-=1
            
        #pyChecker support by Nicodemus
        if not text:
            # check if the line in the form filename:line: message
            current_line = self.GetLine(self.GetCurrentLine())
            m = re.match('(.*):(\d+): .*', current_line)
            if m:
                text = {}
                text['file'] = m.group(1)
                text['line'] = int(m.group(2))+1
            else:
                text = None
            
        if text:
            text['line']-=1
            self.open(r'%s'%text['file'],text['line'])
            self.setStatusText('Jumped to file "%s" (line %s)'%(text['file'],text['line']))
        else:
            self.setStatusText('Error: Impossible to locate file and line number.')
            
    def Execute(self,text):
        """Replace selection with clipboard contents, run commands."""
        ps1 = str(sys.ps1)
        ps2 = str(sys.ps2)
        endpos = self.GetTextLength()
        self.SetCurrentPos(endpos)
        startpos = self.promptPosEnd
        self.SetSelection(startpos, endpos)
        self.ReplaceSelection('')
        text = text.lstrip()
        text = self.fixLineEndings(text)
        text = self.lstripPrompt(text)
        text = text.replace(os.linesep + ps1, '\n')
        text = text.replace(os.linesep + ps2, '\n')
        text = text.replace(os.linesep, '\n')
        lines = text.split('\n')
        commands = []
        command = ''
        for line in lines:
            if line.strip() == ps2.strip():
                # If we are pasting from something like a
                # web page that drops the trailing space
                # from the ps2 prompt of a blank line.
                line = ''
            lstrip = line.lstrip()
            if line.strip() != '' and lstrip == line and \
                    lstrip[:4] not in ['else','elif'] and \
                    lstrip[:6] != 'except':
                # New command.
                if command:
                    # Add the previous command to the list.
                    commands.append(command)
                # Start a new command, which may be multiline.
                command = line
            else:
                # Multiline command. Add to the command.
                command += '\n'
                command += line
        commands.append(command)
        for command in commands:
            command = command.replace('\n', os.linesep + ps2)
            self.write(command)
            self.processLine()

class DropRun(wx.FileDropTarget):
    """Runs a file when dropped on shell."""
    def __init__(self,run):
        wx.FileDropTarget.__init__(self)
        self.run=run
    def OnDropFiles(self,x,y,fileNames):
        fileNames=[script for script in fileNames 
            if os.path.splitext(script)[-1].lower() in ['.py','.pyw']]
        if fileNames:
            for fileName in fileNames:
                self.run(fileName)
            return 1
        else:return 0

class Panel(Shell):
    def __init__(self,panel,*args,**kwds):
        Shell.__init__(self, panel.app, parent = panel, introText = INTRO)
        self.setStatusText  = panel.SetActiveStatusText
        self.open           = panel.openList
        self.SetDropTarget(DropRun(panel.runFile))
        self.SetHelpText(_spe.help.SHELL)
        
