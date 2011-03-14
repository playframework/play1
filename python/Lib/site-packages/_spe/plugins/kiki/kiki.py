"""Kiki 0.5.6 - A Free Environment for Regular Expression Testing (ferret)

Copyright (C) 2003, 2004 Project 5

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

Contact info
web: http://come.to/project5
mail/msn: project5@wanadoo.nl
icq: 84243714
"""

import wx
import wx.html

__version__ = "0.5.6"

import re, os, os.path, cgi, sys

# string constants
WINDOWSIZE = "MainWindowSize"
WINDOWPOSITION = "MainWindowPosition"
SASHPOSITION = "SashPosition"
SEARCHTYPE = "SearchType"

MATCHINDEX = "MatchIndex"
MATCHTEMPLATE = "MatchTemplate"
FORMATTEDMATCH = "FormattedMatch"
STARTTEMPLATE = "StartTemplate"
ENDTEMPLATE = "EndTemplate"
GROUPCOLOR = "GroupColor"
GROUPNUMBER = "GroupNumber"

SAMPLETEXT = "SampleText"
REGEX = "Regex"

RESULTS_SHOWMATCHES = "ShowMatchesGroups"
RESULTS_SHOWNAMEDGROUPS = "ShowNamedGroups"
RESULTS_SHOWSAMPLE = "ShowSampleTextInResults"

# flags below are used in settings and in eval() function, so do NOT change!
FLAGIGNORE = "re.IGNORECASE"
FLAGMULTILINE = "re.MULTILINE"
FLAGLOCALE = "re.LOCALE"
FLAGDOTALL = "re.DOTALL"
FLAGUNICODE = "re.UNICODE"
FLAGVERBOSE = "re.VERBOSE"

# constants required by the GUI
ni = wx.NewId
ID_EVALUATE = ni()
ID_NOTEBOOK = ni()
ID_HELPCOMBOBOX = ni()

# global variable
settings = None

# templates
t_error = """<font color="maroon"><b>Error:</b></font><br /><ul>%s</ul>"""
t_nomatch = """<font color="teal"><b>No match found</b></font>"""

# colors used to highlight matches
colors = ["0000AA" , "00AA00" , "FFAA55" , "AA0000" , "00AAAA" , "AA00AA" , "AAAAAA" , 
          "0000FF" , "00FF00" , "00FFFF" , "FF0000" , "DDDD00" , "FF00FF" , 
          "AAAAFF" , "FF55AA" , "AAFF55" , "FFAAAA" , "55AAFF" , "FFAAFF" , 
          "000077" , "007700" , "770000" , "007777" , "770077" , "777700" ]

class Settings(object):
    """Stores and retrieves settings to a file as Python data
       structures which are eval()-ed. This is not by definition
       safe, but since the user has access to source code anyway
       and hence the ability to screw up anything, the danger 
       seems quite limited."""
    def __init__(self, savedir=None, dirname="", filename="settings.py", debugfile=""):
        """Initializes the object
           
           Arguments:
           savedir -- directory where to store data
                      For more info, see the docs of Settings.setSaveDir()
           dirname -- if savedir=None, dirname is used to determine which
                      subdirectory of $HOME data is stored it.
           filename -- name of file containing stored data
           debugfile -- used to allow the user to manually override the
                        default storage dir. Mor info in the docs of
                        Settings.setSaveDir()
        """
        self.__setSaveDir(savedir, dirname, debugfile)
        self.savefilename = filename
        self.__load()
        
    def shutdown(self):
        """Must be called before the program ends."""
        self.save()
        
    def __setSaveDir(self, savedir=None, dirname="", debugfile=""):
        """Sets self.savedir
           
           Arguments:
           savedir -- directory where to store data
                      if savedir==None, $HOME/dirname is used.
                      If necessary, the directory is created.
           dirname -- if savedir==None, dirname determines which
                      subdirectory of the home dir data is stored 
                      in
           debugfile -- this file may be a filename which is imported
                        and may contain a variable called savedir
                        If the debugfile is specified, imported
                        successfully and savedir exists, it is used
                        to override any other parameters. This allows
                        users to override the directory, bypassing
                        the application which needs the settings.
           
           In some Windows installations (seems to be mainly a Win2k
           problem), $HOME points to the root directory. In this case,
           this function will try to use $USERPROFILE/dirname instead.
           
           Overriding this default behaviour is possible by supplying the
           savedir variable in pearsdebug.py."""
        if savedir==None:
            try: # try to override using debugfile if present and contains savedir
                debugmodule = __import__(debugfile, globals())
                savedir = debugmodule.savedir
            except: # if no override, then perform default actions
                if savedir == None: # use $HOME/dirname
                    savedir = os.path.expanduser('~/'+dirname)
                    if len(savedir)<=len("c:\\/"+dirname): # sometimes $HOME points to root
                        # if this is the case, try using $USERPROFILE (see docstring)
                        temp = os.path.join(os.path.expandvars('$USERPROFILE'), dirname)
                        # if this is a different location, use it!
                        if temp > len("C:\\/"+dirname):
                            savedir = temp
            # create dir if it doesn't exist
        if not os.path.exists(savedir):
            os.makedirs(savedir)
        self.savedir = savedir
        
    def __load(self):
        """Loads the settings from a file. These settings are saved
           in Python code format which is eval()-ed, so it's not
           trustworthy."""
        try:
            settingsfile = file(os.path.join(self.savedir, self.savefilename), "r")
            self.settings = eval(settingsfile.read())
            settingsfile.close()
        except: # if file doesn't exist
            self.settings= {}
        
    def save(self):
        """Saves the settings to a file."""
        settingsfile = file(os.path.join(self.savedir, self.savefilename), "w")
        settingsfile.write(str(self.settings))
        settingsfile.close()
        
    def set(self, settingname, value):
        """Changes the value of a setting with settingname to value."""
        self.settings[settingname] = value
        
    def get(self, settingname, defaultval=None):
        """Returns the setting with settingname if present, otherwise
           returns defaultval and saves settingname with defaultval."""
        if self.settings.has_key(settingname):
            return self.settings[settingname]
        else:
            self.set(settingname, defaultval)
            return defaultval


class MyHtmlWindow(wx.html.HtmlWindow):
    """Adds OnLinkClicked"""
    def __init__(self, parent, id):
        wx.html.HtmlWindow.__init__(self, parent, id, style=wx.NO_FULL_REPAINT_ON_RESIZE)
    def OnLinkClicked(self, linkinfo):
        # there's a problem in KDE: the browser is not recognized properly
        # circumvent this problem
        import os
        if os.environ.has_key("BROWSER") and \
           os.environ["BROWSER"]=='kfmclient openProfile webbrowsing':
            print "Invalid browser detected : %s\nResetting to konqueror." % os.environ["BROWSER"]
            os.environ["BROWSER"] = 'konqueror' # set it to konqueror
        import webbrowser # MUST be imported only AFTER os.environ has been modified
        webbrowser.open(linkinfo.GetHref(), 1)
        
        
class MyFrame(wx.Frame):
    def __init__(self, *args, **kwds):
        # begin wxGlade: MyFrame.__init__
        kwds["style"] = wx.DEFAULT_FRAME_STYLE
        wx.Frame.__init__(self, *args, **kwds)
        self.SplitterWindow = wx.SplitterWindow(self, -1, style=wx.SP_3D|wx.SP_LIVE_UPDATE)
        self.BottomPane = wx.Panel(self.SplitterWindow, -1)
        self.Notebook = wx.Notebook(self.BottomPane, ID_NOTEBOOK, style=0)
        self.HelpPane = wx.Panel(self.Notebook, -1)
        self.SampleTextPane = wx.Panel(self.Notebook, -1)
        self.MatchesPane = wx.Panel(self.Notebook, -1, style=wx.SUNKEN_BORDER|wx.TAB_TRAVERSAL)
        self.TopPane = wx.Panel(self.SplitterWindow, -1, style=wx.RAISED_BORDER|wx.TAB_TRAVERSAL)
        self.RegexBox = wx.ComboBox(self.TopPane, -1, choices=["", "", "", "", "", "", ""], style=wx.CB_DROPDOWN)
        self.EvaluateButton = wx.Button(self.TopPane, ID_EVALUATE, "Evaluate")
        self.MethodBox = wx.RadioBox(self.TopPane, -1, "Methods", choices=["Find all", "Find first"], majorDimension=1, style=wx.RA_SPECIFY_COLS)
        self.IgnoreCheckBox = wx.CheckBox(self.TopPane, -1, "IGNORECASE (I)")
        self.LocaleCheckBox = wx.CheckBox(self.TopPane, -1, "LOCALE (L)")
        self.MultilineCheckBox = wx.CheckBox(self.TopPane, -1, "MULTILINE (M)")
        self.DotAllCheckBox = wx.CheckBox(self.TopPane, -1, "DOTALL (S)")
        self.UnicodeCheckBox = wx.CheckBox(self.TopPane, -1, "UNICODE (U)")
        self.VerboseCheckBox = wx.CheckBox(self.TopPane, -1, "VERBOSE (X)")
        self.MatchesWindow = wx.html.HtmlWindow(self.MatchesPane, -1)
        self.SampleText = wx.TextCtrl(self.SampleTextPane, -1, "", style=wx.TE_PROCESS_TAB|wx.TE_MULTILINE|wx.TE_RICH)
        self.HelpSelection = wx.ComboBox(self.HelpPane, ID_HELPCOMBOBOX, choices=["Working with Kiki", "Re - syntax", "Re - special characters: |, (, +, etc.", "Re - extensions, groups and lookahead/lookbehind: (?...)", "Re - special sequences: \...", "Re - flags", "About Kiki"], style=wx.CB_DROPDOWN|wx.CB_READONLY)
        self.HelpWindow = MyHtmlWindow(self.HelpPane, -1)

        self.__set_properties()
        self.__do_layout()
        # end wxGlade

    def __set_properties(self):
        # begin wxGlade: MyFrame.__set_properties
        self.SetTitle("Kiki")
        self.SetSize((640, 480))
        self.RegexBox.SetSelection(0)
        self.EvaluateButton.SetDefault()
        self.MethodBox.SetToolTipString("Find all returns all matches, find first only the first one")
        self.MethodBox.SetSelection(0)
        self.IgnoreCheckBox.SetToolTipString("Perform case-insensitive matching\nExpressions like [A-Z] will match lowercase letters too. This is not affected by the current locale.")
        self.LocaleCheckBox.SetToolTipString("Make \w, \W, \\b, and \B dependent on the current locale.")
        self.MultilineCheckBox.SetToolTipString("When specified, the pattern characters \"^\" and \"$\" match at the beginning respectively end of the string and at the beginning respectively end of each line (immediately following respectively preceding each newline).\nOtherwise \"^\" matches only at the beginning of the string, and \"$\" only at the end of the string and immediately before the newline (if any) at the end of the string.")
        self.DotAllCheckBox.SetToolTipString("Make the \".\" special character match any character at all, including a newline. Without this flag, \".\" will match anything except a newline.")
        self.UnicodeCheckBox.SetToolTipString("Make \w, \W, \\b, and \B dependent on the Unicode character properties database.")
        self.VerboseCheckBox.SetToolTipString("This flag allows you to write regular expressions that look nicer.\nWhitespace within the pattern is ignored, except when in a character class or preceded by an unescaped backslash, and, when a line contains a \"#\" neither in a character class or preceded by an unescaped backslash, all characters from the leftmost such \"#\" through the end of the line are ignored. ")
        self.HelpSelection.SetSelection(0)
        self.BottomPane.SetSize((623, 324))
        self.SplitterWindow.SplitHorizontally(self.TopPane, self.BottomPane, 120)
        # end wxGlade

    def __do_layout(self):
        # begin wxGlade: MyFrame.__do_layout
        MainSizer = wx.BoxSizer(wx.HORIZONTAL)
        TopPaneSizer = wx.BoxSizer(wx.HORIZONTAL)
        HelpPaneSizer = wx.FlexGridSizer(3, 1, 7, 7)
        SampleTextSizer = wx.BoxSizer(wx.HORIZONTAL)
        MatchesPaneSizer = wx.BoxSizer(wx.HORIZONTAL)
        BottomPaneSizer = wx.FlexGridSizer(5, 3, 0, 0)
        OptionsSizer = wx.FlexGridSizer(1, 2, 0, 7)
        FlagsSizer = wx.StaticBoxSizer(wx.StaticBox(self.TopPane, -1, "Flags"), wx.HORIZONTAL)
        FlagCheckSizer = wx.FlexGridSizer(4, 3, 7, 15)
        RegexSizer = wx.FlexGridSizer(1, 2, 0, 7)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        RegexSizer.Add(self.RegexBox, 0, wx.EXPAND, 0)
        RegexSizer.Add(self.EvaluateButton, 0, 0, 0)
        RegexSizer.AddGrowableCol(0)
        BottomPaneSizer.Add(RegexSizer, 1, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        OptionsSizer.Add(self.MethodBox, 0, wx.EXPAND, 0)
        FlagCheckSizer.Add((7, 2), 0, wx.EXPAND, 0)
        FlagCheckSizer.Add((7, 2), 0, wx.EXPAND, 0)
        FlagCheckSizer.Add((7, 2), 0, wx.EXPAND, 0)
        FlagCheckSizer.Add(self.IgnoreCheckBox, 0, 0, 0)
        FlagCheckSizer.Add(self.LocaleCheckBox, 0, 0, 0)
        FlagCheckSizer.Add(self.MultilineCheckBox, 0, 0, 0)
        FlagCheckSizer.Add(self.DotAllCheckBox, 0, 0, 0)
        FlagCheckSizer.Add(self.UnicodeCheckBox, 0, 0, 0)
        FlagCheckSizer.Add(self.VerboseCheckBox, 0, 0, 0)
        FlagCheckSizer.Add((7, 2), 0, wx.EXPAND, 0)
        FlagCheckSizer.Add((7, 2), 0, wx.EXPAND, 0)
        FlagCheckSizer.Add((7, 2), 0, wx.EXPAND, 0)
        FlagsSizer.Add(FlagCheckSizer, 1, wx.EXPAND, 0)
        OptionsSizer.Add(FlagsSizer, 1, 0, 0)
        BottomPaneSizer.Add(OptionsSizer, 1, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        BottomPaneSizer.Add((7, 7), 0, wx.EXPAND, 0)
        self.TopPane.SetAutoLayout(1)
        self.TopPane.SetSizer(BottomPaneSizer)
        BottomPaneSizer.Fit(self.TopPane)
        BottomPaneSizer.SetSizeHints(self.TopPane)
        BottomPaneSizer.AddGrowableCol(1)
        MatchesPaneSizer.Add(self.MatchesWindow, 1, wx.EXPAND, 0)
        self.MatchesPane.SetAutoLayout(1)
        self.MatchesPane.SetSizer(MatchesPaneSizer)
        MatchesPaneSizer.Fit(self.MatchesPane)
        MatchesPaneSizer.SetSizeHints(self.MatchesPane)
        SampleTextSizer.Add(self.SampleText, 1, wx.EXPAND, 0)
        self.SampleTextPane.SetAutoLayout(1)
        self.SampleTextPane.SetSizer(SampleTextSizer)
        SampleTextSizer.Fit(self.SampleTextPane)
        SampleTextSizer.SetSizeHints(self.SampleTextPane)
        HelpPaneSizer.Add((1, 1), 0, wx.EXPAND, 0)
        HelpPaneSizer.Add(self.HelpSelection, 0, wx.EXPAND, 0)
        HelpPaneSizer.Add(self.HelpWindow, 1, wx.EXPAND, 0)
        self.HelpPane.SetAutoLayout(1)
        self.HelpPane.SetSizer(HelpPaneSizer)
        HelpPaneSizer.Fit(self.HelpPane)
        HelpPaneSizer.SetSizeHints(self.HelpPane)
        HelpPaneSizer.AddGrowableRow(2)
        HelpPaneSizer.AddGrowableCol(0)
        self.Notebook.AddPage(self.MatchesPane, "Matches")
        self.Notebook.AddPage(self.SampleTextPane, "Sample text")
        self.Notebook.AddPage(self.HelpPane, "Help")
        TopPaneSizer.Add(self.Notebook, 1, wx.EXPAND, 0)
        self.BottomPane.SetAutoLayout(1)
        self.BottomPane.SetSizer(TopPaneSizer)
        MainSizer.Add(self.SplitterWindow, 1, wx.EXPAND, 0)
        self.SetAutoLayout(1)
        self.SetSizer(MainSizer)
        self.Layout()
        self.Centre()
        # end wxGlade

# end of class MyFrame

class MyFrameWithEvents(MyFrame):
    """Subclasses MyFrame - generated by wxGlade - and adds events."""
    def __init__(self, *args, **kwargs):
        MyFrame.__init__(self, *args, **kwargs)
            
        # map option flags to checkboxes
        self.flagmapper = {FLAGIGNORE: self.IgnoreCheckBox,
                           FLAGMULTILINE: self.MultilineCheckBox,
                           FLAGLOCALE: self.LocaleCheckBox,
                           FLAGDOTALL: self.DotAllCheckBox,
                           FLAGUNICODE: self.UnicodeCheckBox,
                           FLAGVERBOSE: self.VerboseCheckBox}
                               
        self.SetTitle(" " + self.GetTitle() + " " + __version__)
            
        # set empty pages for HTML windows
        self.MatchesWindow.SetPage("")
        self.HelpWindow.SetPage("")
        self.HelpWindow.SetWindowStyleFlag(wx.SUNKEN_BORDER)
            
        self.Notebook.SetSelection(0)
        self.RegexBox.Clear()
            
        # bind events
        wx.EVT_BUTTON(self, ID_EVALUATE, self.evaluate)
        wx.EVT_CLOSE(self, self.close)
        wx.EVT_NOTEBOOK_PAGE_CHANGED(self, ID_NOTEBOOK, self.changePage)
        wx.EVT_COMBOBOX(self, ID_HELPCOMBOBOX, self.showhelp)
            
        # apply settings
        self.loadSettings()
            
        # move focus to regex input field
        self.RegexBox.SetFocus()
            
        # initialize needed attribs
        self.matches = [] # list of found matches
            
        self.path = os.path.split(sys.argv[0])[0] or os.getcwd() # remembers where Kiki is located
        
    def icon(self, path=None):
        """Load and assign the icon
            
           Arguments:
           path -- path where kiki.ico is located. If
                   path==None, the current directory is
                   used
        """
        import sys
        if path==None:
            self.path = os.path.split(sys.argv[0])[0] or os.getcwd() # *MUST* be the directory where everything, including About data and the likes are located
        else:
            self.path = path
        iconfile = os.path.join(self.path, "kiki.ico")
        theicon = wx.Icon(iconfile, wx.BITMAP_TYPE_ICO)
        self.SetIcon(theicon)
        
    def changePage(self, event):
        """Handles notebook page changes"""
        if event.GetSelection()==2 and not self.HelpWindow.GetOpenedPageTitle().strip():
            self.HelpWindow.SetPage(file(os.path.join(self.path, "docs", "index.html"),"r").read())
        
    def showhelp(self, event):
        """Handles help combo box events"""
        sel = self.HelpSelection.GetStringSelection().lower() # must lower-case for comparisons
        id = self.HelpSelection.GetSelection()
        filename, anchor = "", ""
        simpleload = True
        if id==0: # show main help
            filename = "index.html"
        elif sel.find("syntax")>-1:
            filename, anchor = "re.html", "syntax"
        elif sel.find("special characters")>-1:
            filename, anchor = "re.html", "specialcharacters"
        elif sel.find("extensions")>-1:
            filename, anchor = "re.html", "extensions"
        elif sel.find("special sequences")>-1:
            filename, anchor = "re.html", "specialsequences"
        elif sel.find("flags")>-1:
            filename, anchor = "re.html", "flags"
        else:
            simpleload = False
        if simpleload:
            filename = os.path.join(self.path, "docs", filename)
            if anchor.strip():
                anchor = "#" + anchor
            else:
                anchor = ""
            self.HelpWindow.LoadPage(filename+anchor)
        else: # build about-screen
            f = file(os.path.join(self.path, "docs", "about.html"), "r")
            about = f.read()
            f.close()
            # build the dictionary needed to format the string
            if self.GetTitle().lower().find("spe")>-1:
                spe = "active"
            else: spe = "inactive (Kiki running standalone)"
            d = {"version": __version__,
                 "kikidir": self.path,
                 "datadir": settings.savedir,
                 "pythonversion": sys.version.split()[0],
                 "wxpythonversion": wx.__version__,
                 "spe": spe,
                 "website": "http://come.to/project5",
                 "mail": "project5@wanadoo.nl",
                 "icq": "84243714" }
            about = about % d
            self.HelpWindow.SetPage(about)
        
    def evaluate(self, event):
        """Actual functionality, triggered by Evaluate button press.
           
           The regex is compiled if possible.
           If compilation is successful, the regex is added to the
           history and it's matched against the sample text.
           If compilation is unsuccessful, the error message is 
           displayed.
        """
        self.saveSettings()
        self.Notebook.SetSelection(0) # display output pane
        # STEP 1: try to compile the regex
        # get flags to use in the compilation
        flags = 0
        for flag in self.flagmapper.keys():
            if self.flagmapper[flag].IsChecked():
                flags = flags|eval(flag)
        # compile the regex and stop with error message if invalid
        try:
            self.MatchesWindow.SetPage("")
            regex = re.compile(self.RegexBox.GetValue(), flags)
        except re.error, e:
            self.MatchesWindow.SetPage(t_error % e)
            return False # stop execution if error
            
        if self.RegexBox.GetValue().strip(): # append to history if non-empty
            # get current history items
            currentitems = [self.RegexBox.GetValue()]
            for i in range(self.RegexBox.GetCount()):
                t = self.RegexBox.GetString(0)
                if not t in currentitems: # no duplicates
                    currentitems.append(t)
                self.RegexBox.Delete(0)
            for item in currentitems:
                self.RegexBox.Append(item)
            self.RegexBox.SetSelection(0) # set selection again
            
        rawtext = self.SampleText.GetValue()
        # insider joke for the Sluggy fans
        if rawtext.strip()=="INSTANT FERRET-SHOCK!":
            import random
            shock = ["<b>"]
            #blinkychars = """+*-/\:;][{}|tyqfghjkl?>ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%"""
            rc = random.choice
            c = colors
            sa = shock.append
            t = """<font color="#%s">KIKI</font>"""
            [ sa(t % (rc(c))) for i in xrange(10000) ]
            shock.append("</b>")
            self.MatchesWindow.SetPage("".join(shock))
            return None
            
        self.matches = [] # empty the list of match objects
        output = []
        while 1:
            if len(self.matches)==0:
                start = 0
            else:
                start = self.matches[-1].end()
                if len(self.matches[-1].group(0))==0: 
                    # in case of expressions which return empty matches
                    # Without this condition, an endless loop would occur.
                    start += 1
            match = regex.search(rawtext, start)
            if (not match) or \
               (len(self.matches)>=1 and self.MethodBox.GetSelection()==1) or \
               start>=len(rawtext): # this last condition is also to prevent an endless loop
                break # stop execution
            else:
                self.matches.append(match)
        if not self.matches: # if no matches found
            output.append(t_nomatch)
        else: # matches found
            # show matches with parentheses
            if settings.get(RESULTS_SHOWMATCHES, True):
                output.append(self.showmatches())
                output.append("""<br /><br />""")
            # show tabular overview of named groups
            if settings.get(RESULTS_SHOWNAMEDGROUPS, True):
                # TODO: self.shownamedgroups()
                pass
        if settings.get(RESULTS_SHOWSAMPLE, True): 
            output.append("""<table cellpadding="0" cellspacing="0"><tr><td>
                               <b>Sample text matched against:</b>
                             </td></tr>
                             <tr><td bgcolor="#EEEEEE"><i>""")
            output.append(self.htmlize(rawtext) or "&nbsp;") # something must be in there, so if no raw text, at least force a space
            output.append("""</i></td></tr></table>""")
        self.MatchesWindow.SetPage("".join(output))
            
    def htmlize(self, text):
        """Converts the text to html (escapes HTML entities and tags,
           converts spaces to &nbsp's and enters to <br>'s."""
        result = cgi.escape(text)
        result = result.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
        result = result.replace("  ", "&nbsp;&nbsp;")
        result = result.replace("\r\n", "<br />")
        result = result.replace("\r", "<br />")
        result = result.replace("\n", "<br />")
        return result
        
    def formatmatch(self, match, index, matchtemplate, starttemplate, endtemplate):
        """Pretty-prints a match as HTML, with colors for groups, etc."""
        # first, make a dictionary of start and end positions 
        # (each char number in the match may be mapped to zero, one or more
        # group numbers which start/end there)
        # Groups function according to first-to-open, last-to-close.
        starts, ends = {}, {}
        # populate the dictionaries with empty lists 
        # A -1 key is necessary for groups which do not participate in the match.
        for pos in range(-1, len(match.string)+1):
            starts[pos], ends[pos] = [], []
        # now populate them with real values: each position key will contain
        # none or more group numbers
        #print match.groups()
        for groupnr in range(1+len(match.groups())):
            #print groupnr
            #print starts, ends
            starts[match.start(groupnr)].append(groupnr)
            ends[match.end(groupnr)].append(groupnr)
        # prepare result
        result = []
        # now, loop through the string matched and build the layout
        string = match.string
        opengroups = [] # keeps track of open groups
        for pos in range(match.start(), match.end()+1):
            while 1:
                # first, try to shut down any open groups
                if opengroups and opengroups[-1] in ends[pos]: # if opengroups available and the last one must be closed now
                    # shut down open group and remove it from the opengroups list
                    result.append(endtemplate % {GROUPCOLOR: colors[opengroups[-1] % len(colors)],
                                                 GROUPNUMBER: opengroups.pop(-1)})
                # secondly, try to open any new groups
                elif starts[pos]: # if any new groups must be opened now
                    result.append(starttemplate % {GROUPCOLOR: colors[starts[pos][0] % len(colors)],
                                                   GROUPNUMBER: starts[pos][0]})
                    opengroups.append(starts[pos].pop(0))
                else: # if no groups must be opened or closed, nothing special going on
                    if pos<match.end():
                        result.append(self.htmlize(string[pos]))
                    break # stop the while loop
        result = "".join(result)
        return matchtemplate % {MATCHINDEX: index, 
                                FORMATTEDMATCH: result}
        
    def showmatches(self):
        """Converts the results to html code and returns that.
           Is not capable of handling an empty self.matches list of matches."""
        if not self.matches:
            raise ValueError, "self.matches must be non-empty list of re match opbjects"
        index = -1 # number of current match that's being printed
        html = []
        html.append("""<table cellpadding="0" cellspacing="0"><tr><td bgcolor="#F8F8F8"><font color="#777777">""")
        # load match layout templates
        starttemplate = settings.get(STARTTEMPLATE, """<font color="#%%(%s)s"><b>(</b></font>""" % GROUPCOLOR)
        endtemplate = settings.get(ENDTEMPLATE, """<font color="#%%(%s)s"><b>)<font size="0">%%(%s)s</font></b></font>""" % (GROUPCOLOR, GROUPNUMBER))
        matchtemplate = settings.get(MATCHTEMPLATE, """<font color="#000000"><b><u><b><font size="0">%%(%s)s:</font></b></u>%%(%s)s</b></font>""" % (MATCHINDEX, FORMATTEDMATCH))
        # loop through matches and create output
        for match in self.matches:
            index += 1
            # determine what part of the string we're looking at
            if index>0:
                prevmatchend = self.matches[index-1].end()
            else: 
                prevmatchend = 0
            if index+1 < len(self.matches):
                nextmatchstart = self.matches[index+1].start()
            else:
                nextmatchstart = -1
            # append piece in between matches
            html.append(self.htmlize(match.string[prevmatchend:match.start()]))
            # append current match
            #html.append(self.htmlize(match.string[match.start():match.end()]))
            html.append(self.formatmatch(match, index, matchtemplate, starttemplate, endtemplate))
            # append end piece if necessary
            if index+1 >= len(self.matches): # if last match, print rest of string
                html.append(self.htmlize(match.string[match.end():]))
        html.append("""</font></td></tr></table>""")
        res = "".join(html)
        return res
        
    def loadSettings(self):
        """Loads GUI settings from the settings system."""
        # load some size settings
        # set window size and position; make sure it's on screen
        # and has a reasonable size
        system = wx.SystemSettings_GetMetric
        pos = list(settings.get(WINDOWPOSITION, (-1,-1)))
        x = system(wx.SYS_SCREEN_X)
        y = system(wx.SYS_SCREEN_Y)
        if pos[0]<=-1: pos[0] = (x/2)-320
        if pos[1]<=-1: pos[1] = (y/2)-240
        if pos[0]>system(wx.SYS_SCREEN_X)-50:
            pos[0] = 0
        if pos[1]>system(wx.SYS_SCREEN_Y)-50:
            pos[1] = 0
        self.SetPosition(pos)
        size = list(settings.get(WINDOWSIZE, (-1, -1)))
        if size[0]>system(wx.SYS_SCREEN_X):
            size[0] = 640
        if size[1]>system(wx.SYS_SCREEN_Y):
            size[1] = 480
        size[0] = max(size[0], 640)
        size[1] = max(size[1], 480)
        self.SetSize(size)
            
        # load the sample text and regex last used
        self.SampleText.SetValue(settings.get(SAMPLETEXT, ""))
        self.RegexBox.SetValue(settings.get(REGEX, ""))
            
        # load the flags and desired type of re functionality
        for flag in self.flagmapper.keys():
            self.flagmapper[flag].SetValue(settings.get(flag, False))
        self.MethodBox.SetSelection(settings.get(SEARCHTYPE, 0))
            
        # other settings
        self.SplitterWindow.SetSashPosition(settings.get(SASHPOSITION, 100))
        
    def saveSettings(self, dosave=True):
        """Puts all GUI settings in the settings system. 
           
           Arguments:
           dosave -- if True, the save() method of Settings is called
                     when done
        """
        # put all stuff that needs saving in the settings
        settings.set(WINDOWSIZE, self.GetSize())
        settings.set(WINDOWPOSITION, self.GetPosition())
        settings.set(SASHPOSITION, self.SplitterWindow.GetSashPosition())
            
        settings.set(SAMPLETEXT, self.SampleText.GetValue())
        settings.set(REGEX, self.RegexBox.GetValue())
            
        # save the selected flags
        for flag in self.flagmapper.keys():
            settings.set(flag, self.flagmapper[flag].GetValue())
        settings.set(SEARCHTYPE, self.MethodBox.GetSelection())
            
        if dosave: settings.save()
        
    def close(self, event):
        """Prepares for shutdown and then closes the app."""
        self.saveSettings()
            
        # shut down the settings system
        settings.shutdown()
            
        # shut down the app
        self.Destroy()
        
def speCreate(parent, info=None):
    """Integration of Kiki into spe (http://spe.pycs.net)"""
    global settings
    settings = Settings(dirname=".spe", filename="kikicfg.py", debugfile="kikidebug")        
    Kiki = MyFrameWithEvents(parent, -1, "")
    Kiki.SetTitle(Kiki.GetTitle() + " - the ferret in your Spe")
    if info and info.has_key('kikiPath'):
        kikipath = info['kikiPath']
    else:
        kikipath = os.path.join(os.path.dirname(sys.argv[0]), "framework/contributions")
    Kiki.icon(kikipath)
    Kiki.Show(1)
    return Kiki
    
def main():
    global settings
    settings = Settings(dirname=".kiki", filename="kikicfg.py", debugfile="kikidebug")        
    Kiki = wx.PySimpleApp()
    wx.InitAllImageHandlers()
    mw = MyFrameWithEvents(None, -1, "")
    mw.icon()
    Kiki.SetTopWindow(mw)
    mw.Show(1)
    Kiki.MainLoop()

if __name__ == "__main__":
    main()

