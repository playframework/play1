# kdefiledialog.py: support for native KDE file and dir dialog (using kdialog)
# $Id: kdefiledialog.py,v 1.5 2007/03/27 07:02:07 agriggio Exp $
# 
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

import wx
import signal, os, sys
import misc


__all__ = ['test_kde', 'kde_file_selector', 'kde_dir_selector']


def _check_for_kdialog():
    pth = os.environ.get('PATH', os.defpath).split(os.pathsep)
    for p in pth:
        name = os.path.join(p, 'kdialog')
        if os.access(name, os.X_OK):
            return True
    return False

_kdialog_ok = _check_for_kdialog()


def kde_file_selector(message, default_path="", default_filename="",
                      default_extension="", wildcard="*.*", flags=0,
                      *args, **kwds):
    """\
    Pops up the standard KDE file selector box, calling kdialog. The API is
    identical to that of wx.FileSelector. If kdialog can't be invoked,
    reverts to the standard wx.FileSelector. Note that at the moment not all
    the arguments are meaningful (for example, parent and initial position are
    ignored), and multiple selections are not supported.
    """
    if not _kdialog_ok:
        return wx.FileSelector(message, default_path, default_filename,
                               default_extension, wildcard, flags,
                               *args, **kwds)
    
    r, w = os.pipe()
    handler = _SigChldHandler()
    oldhandler = signal.signal(signal.SIGCHLD, handler)

    pid = os.fork()

    if pid == 0:
        os.close(r)
        os.dup2(w, sys.stdout.fileno())
        os.close(w)
        startdir = default_path
        if default_filename:
            if not os.path.isdir(startdir):
                startdir = os.path.dirname(startdir)
            startdir = os.path.join(startdir, default_filename)
        if flags & wx.SAVE:
            kind = '--getsavefilename'
        else:
            kind = '--getopenfilename'
        os.execlp('kdialog', 'kdialog', kind, startdir,
                  _wx_to_kde_wildcard(wildcard), '--title', message)
    elif pid > 0:
        disabler = wx.WindowDisabler()
        app = wx.GetApp()
        os.close(w)
        while not handler.done:
            app.Dispatch()
        if handler.status != 0:
            os.close(r)
            return ""

        filename = os.fdopen(r).readline().strip()
        signal.signal(signal.SIGCHLD, oldhandler or signal.SIG_DFL)
        if (flags & wx.SAVE) and (flags & wx.OVERWRITE_PROMPT) and \
               os.path.exists(filename):
            if wx.MessageBox(_("File '%s' already exists: do you really want "
                               "to overwrite it?") % misc.wxstr(filename),
                             _("Confirm"),
                             style=wx.YES_NO|wx.ICON_QUESTION) == wx.NO:
                return kde_file_selector(message, default_path,
                                         default_filename, default_extension,
                                         wildcard, flags)
        return filename
    else:
        raise OSError, _("Fork Error")


def kde_dir_selector(message="", default_path="", *args, **kwds):
    """\
    Pops up the standard KDE directory selector box, calling kdialog.
    The API is identical to that of wx.DirSelector. If kdialog can't be
    invoked, reverts to the standard wx.DirSelector. Note that at the moment
    not all the arguments are meaningful (for example, parent and initial
    position are ignored).
    """
    if not _kdialog_ok:
        return wx.DirSelector(message, default_path, *args, **kwds)
    
    r, w = os.pipe()
    handler = _SigChldHandler()
    oldhandler = signal.signal(signal.SIGCHLD, handler)
    pid = os.fork()

    if pid == 0:
        os.close(r)
        os.dup2(w, sys.stdout.fileno())
        os.close(w)
        if not default_path:
            default_path = os.getcwd()
        os.execlp('kdialog', 'kdialog', '--getexistingdirectory', default_path,
                  '--title', message)
    elif pid > 0:
        disabler = wx.WindowDisabler()
        app = wx.GetApp()
        os.close(w)
        while not handler.done:
            app.Dispatch()
        if handler.status != 0:
            os.close(r)
            return ""
        
        dirname = os.fdopen(r).readline().strip()
        signal.signal(signal.SIGCHLD, oldhandler or signal.SIG_DFL)
        return dirname
    else:
        raise OSError, _("Fork Error")


def test_kde():
    """\
    Checks whether KDE (actually, kdesktop) is running.
    """
    return os.system('dcop kdesktop > /dev/null 2>&1') == 0    


class _SigChldHandler:
    def __init__(self):
        self.done = False
        self.status = 0

    def __call__(self, signo, stackframe):
        pid, self.status = os.wait()
        self.done = True

# end of class _SigChldHandler


def _wx_to_kde_wildcard(wildcard):
    bits = wildcard.split('|')
    l = len(bits)
    ret = []
    for i in range(0, l, 2):
        if i+1 < l:
            ret.append(bits[i+1].replace(';', ' ') + '|' + bits[i])
        else:
            ret.append(bits[i].replace(';', ' '))
    return '\n'.join(ret)


if __name__ == '__main__':
    app = wx.PySimpleApp()
    frame = wx.Frame(None, -1, "Prova")
    b = wx.Button(frame, -1, "KDE File selector", pos=(0, 0))
    b2 = wx.Button(frame, -1, "wx File selector", pos=(0, 70))

    def on_click(event):
        filename = kde_file_selector(
            'Select file to save', '', 'prova.py',
            wildcard="Python files|*.py;*.pyc|All files|*",
            flags=wx.SAVE|wx.OVERWRITE_PROMPT)
        if filename:
            wx.MessageBox('You selected file: %s' % filename,
                          style=wx.OK|wx.ICON_INFORMATION)
        else:
            wx.MessageBox('No files selected!',
                          style=wx.OK|wx.ICON_EXCLAMATION)

    def on_click2(event):
        filename = wx.FileSelector(
            'Select file to save', '', 'prova.py',
            wildcard=_("Python files|*.py;*.pyc|All files|*"),
            flags=wx.SAVE|wx.OVERWRITE_PROMPT)
        if filename:
            wx.MessageBox(_('You selected file: %s') % filename,
                          style=wx.OK|wx.ICON_INFORMATION)
        else:
            wx.MessageBox(_('No files selected!'),
                          style=wx.OK|wx.ICON_EXCLAMATION)
            

    wx.EVT_BUTTON(b, -1, on_click)
    wx.EVT_BUTTON(b2, -1, on_click2)

    app.SetTopWindow(frame)
    frame.Show()
    app.MainLoop()
