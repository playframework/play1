"Main module for running pychecker a Tkinter GUI for all the options"

import sys
import os
import Tkinter, tkFileDialog

from OptionTypes import *
from string import capitalize, strip, rstrip, split

import Config

MAX_SUBBOX_ROWS = 8
MAX_BOX_COLS = 3
PAD = 10
EDITOR = "xterm -e vi -n +%(line)d %(file)s"
if sys.platform == 'win32':
    EDITOR = "notepad %(file)s"

def col_weight(grid):
    "Set column weights so that sticky grid settings actually work"
    unused, col = grid.grid_size()
    for c in range(col):
        grid.columnconfigure(c, weight=1)

def spawn(cmd_list):
    try:
        if os.fork():
            try:
                os.execvp(cmd_list[0], cmd_list)
            finally:
                sys.exit()
    except AttributeError:
        os.execvp(cmd_list[0], cmd_list)

def edit(file, line):
    "Fire up an external editor to see the file at the given line"
    unused = file, line
    list = split(EDITOR)
    cmd_list = []
    for word in list:
        cmd_list.append(word % locals())
    spawn(cmd_list)

def closeCB():
    sys.exit(0)

class Results:
    "Display the warnings produced by checker"

    def __init__(self, w):
        self.top = Tkinter.Toplevel(w, name="results")
        self.top.transient(w)
        self.top.bind('<Return>', self.hide)
        self.top.bind('<Escape>', self.hide)
        self.text = Tkinter.Text(self.top, name="text")
        self.text.grid()
        self.text.bind('<Double-Button-1>', self.showFile)
        close = Tkinter.Button(self.top,
                            name="close",
                            default=Tkinter.ACTIVE,
                            command=self.hide)
        close.grid()
        self.text.update_idletasks()

    def show(self, text):
        self.text.delete("0.1", "end")
        self.text.insert("0.1", text)
        self.top.deiconify()
        self.top.lift()

    def hide(self, *unused):
        self.top.withdraw()

    def line(self):
        return split(self.text.index(Tkinter.CURRENT), ".")[0]

    def showFile(self, unused):
        import re
        line = self.line()
        text = self.text.get(line + ".0", line + ".end")
        text = rstrip(text)
        result = re.search("(.*):([0-9]+):", text)
        if result:
            file, line = result.groups()
            edit(file, int(line))
            self.text.after(0, self.selectLine)

    def selectLine(self):
        line = self.line()
        self.text.tag_remove(Tkinter.SEL, "1.0", Tkinter.END)
        self.text.tag_add(Tkinter.SEL, line + ".0", line + ".end")

class ConfigDialog:
    "Dialog for editing options"
    
    def __init__(self, tk):
        self._tk = tk
        self._cfg, _, _ = Config.setupFromArgs(sys.argv)

        self._help = None
        self._optMap = {}
        self._opts = []
        self._file = Tkinter.StringVar()
        self._results = None

        if len(sys.argv) > 1:
            self._file.set(sys.argv[1])

        for name, group in Config._OPTIONS:
          opts = []
          for _, useValue, longArg, member, description in group:
              value = None
              if member:
                  value = getattr(self._cfg, member)
                  description = member + ": " + capitalize(description)
                  description = strip(description)
              tk.option_add('*' + longArg + ".help", description)
              if useValue:
                  if type(value) == type([]):
                      field = List(longArg, value)
                  elif type(value) == type(1):
                      field = Number(longArg, int(value))
                  elif type(value) == type(''):
                      field = Text(longArg, value)
                  else:
                      field = Boolean(longArg, value)
              else:
                  field = Boolean(longArg, value)
              self._optMap[longArg] = field
              opts.append(field)
          self._opts.append( (name, opts))

    def _add_fields(self, w, opts):
        count = 0
        for opt in opts:
            f = opt.field(w)
            c, r = divmod(count, MAX_SUBBOX_ROWS)
            f.grid(row=r, column=c, sticky=Tkinter.NSEW)
            count = count + 1

    def _add_group(self, w, name, opts):
        colFrame = Tkinter.Frame(w)
        
        label = Tkinter.Label(colFrame, text=name + ":")
        label.grid(row=0, column=0, sticky=Tkinter.NSEW)
        
        gframe = Tkinter.Frame(colFrame, relief=Tkinter.GROOVE, borderwidth=2)
        gframe.grid(row=1, column=0, sticky=Tkinter.NSEW)
        self._add_fields(gframe, opts)
        
        label = Tkinter.Label(colFrame)
        label.grid(row=2, column=0, sticky=Tkinter.NSEW)
        colFrame.rowconfigure(2, weight=1)
        return colFrame

    def main(self):
        frame = Tkinter.Frame(self._tk, name="opts")
        frame.grid()
        self._tk.option_readfile('Options.ad')
        self._fields = {}
        row, col = 0, 0
        rowFrame = Tkinter.Frame(frame)
        rowFrame.grid(row=row)
        row = row + 1
        for name, opts in self._opts:
            w = self._add_group(rowFrame, name, opts)
            w.grid(row=row, column=col, sticky=Tkinter.NSEW, padx=PAD)
            col = col + 1
            if col >= MAX_BOX_COLS:
                col_weight(rowFrame)
                rowFrame=Tkinter.Frame(frame)
                rowFrame.grid(row=row, sticky=Tkinter.NSEW)
                col = 0
                row = row + 1
        col_weight(rowFrame)

        self._help = Tkinter.Label(self._tk, name="helpBox")
        self._help.grid(row=row)
        self._help.config(takefocus=0)
        buttons = Tkinter.Frame(self._tk, name="buttons")
        ok = Tkinter.Button(buttons, name="ok", command=self.ok, default=Tkinter.ACTIVE)
        ok.grid(row=row, column=0)
        default = Tkinter.Button(buttons, name="default", command=self.default)
        default.grid(row=row, column=1)
        close = Tkinter.Button(buttons, name="close", command=closeCB)
        close.grid(row=row, column=2)
        buttons.grid()

        f = Tkinter.Frame(self._tk, name="fileStuff")
        Tkinter.Button(f, name="getfile", command=self.file).grid(row=0, column=1)
        fileEntry = Tkinter.Entry(f, name="fname", textvariable=self._file)
        fileEntry.grid(row=0, column=2)
        Tkinter.Button(f, name="check", command=self.check).grid(row=0, column=3)
        f.grid(sticky=Tkinter.EW)
        
        self._tk.bind_all('<FocusIn>', self.focus)
        self._tk.bind_all('<Enter>', self.focus)
        self._tk.bind_all('<ButtonPress>', self.click)
        fileEntry.bind('<Return>', self.check)
        self._tk.mainloop()

    #
    # Callbacks
    #

    def help(self, w):
        if type(w) == type(''):         # occurs with file dialog...
            return
        if self._help == w:             # ignore help events on help...
            return
        help = w.option_get("help", "help")
        self._help.configure(text=help)

    def focus(self, ev):
        self.help(ev.widget)

    def click(self, ev):
        self.help(ev.widget)

    def ok(self):
        opts = []
        # Pull command-line args
        for _, group in self._opts:
            for opt in group:
                arg = opt.arg()
                if arg:
                    opts.append(arg)

        # Calculate config
        self._cfg, _, _ = Config.setupFromArgs(opts)

        # Set controls based on new config
        for _, group in Config._OPTIONS:
            for _, _, longArg, member, _ in group:
                if member:
                    self._optMap[longArg].set(getattr(self._cfg, member))

    def default(self):
        self._cfg, _, _ = Config.setupFromArgs(sys.argv)
        for _, group in Config._OPTIONS:
            for _, _, longArg, member, _ in group:
                if member:
                    self._optMap[longArg].set(getattr(self._cfg, member))
                else:
                    self._optMap[longArg].set(0)

    def file(self):
        self._file.set(tkFileDialog.askopenfilename())

    def check(self, *unused):
        import checker
        import StringIO
        
        self.ok()                       # show effect of all settings

        checker._allModules = {}
        warnings = checker.getWarnings([self._file.get()], self._cfg)
        capture = StringIO.StringIO()
        
        if not self._results:
            self._results = Results(self._help)
        checker._printWarnings(warnings, capture)

        value = strip(capture.getvalue())
        if not value:
            value = "None"
        self._results.show(value)


if __name__=='__main__':
    dirs = os.path.join(os.path.split(os.getcwd())[:-1])
    sys.path.append(dirs[0])
    tk = Tkinter.Tk()
    tk.title('PyChecker')
    ConfigDialog(tk).main()
