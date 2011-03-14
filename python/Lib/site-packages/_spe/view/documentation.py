####(c)www.stani.be-------------------------------------------------------------

import _spe.info
INFO=_spe.info.copy()

INFO['description']=\
"""Index as tab."""

__doc__=INFO['doc']%INFO

####Panel class-----------------------------------------------------------------
def _(x):
    return x
    

import wx, sm.wxp
import os, nturl2path, sys
import pydoc

ERROR = "<h1>"+_("Error")+"</h1><br>"+\
        _("SPE could not generate documentation for ")+\
        _("<b>%s</b>:<br><br><i>%s</i>")

ERROR_UNNAMED = "<h1>"+_("Error")+"</h1><br>"+\
        _("SPE can't generate documentation of unnamed files.<br>")+\
        _("Please save your file first.")

def my_import(name):
    mod = __import__(name)
    components = name.split('.')
    for comp in components[1:]:
        mod = getattr(mod, comp)
    return mod

class Panel(sm.wxp.HtmlWindow):
    def __init__(self,parent,*args,**kwds):
        sm.wxp.HtmlWindow.__init__(self,parent=parent,id=-1)
        self.SetFonts(normal_face='helvetica',fixed_face='courier',sizes=[8,9,10,12,16,20,22])
        self.childPanel     = parent.childPanel
        self.open           = self.childPanel.parentPanel.openList
        self.moduleName     = None
        
    def main(self):
        if self.childPanel.confirmSave(_("""\
SPE will generate now documentation with pydoc.
This implies importing the file. If some files,
are not saved, it's recommended to press Cancel and
save them first before generating any documentation.
""")):
            try:
                fileName        = self.childPanel.fileName
                if fileName == 'unnamed':
                    self.SetPage(ERROR_UNNAMED)
                    return
                path            = os.path.dirname(fileName)
                moduleName      = os.path.splitext(os.path.basename(fileName))[0]
                if path:          os.chdir(path)
                self.loadDoc(moduleName,'')
            except Exception, message:
                self.error(moduleName,message)
            
    def OnLinkClicked(self, linkinfo):
        href                    = linkinfo.GetHref().split('.html')
        if len(href)==1:
            anchor              = href[0]
            if anchor == '.':
                self.index()
            elif anchor[:5]== 'file:':
                self.open(nturl2path.url2pathname(anchor[5:]))
            else:
                self.loadDoc(self.moduleName,anchor)
        elif len(href)==2:
            moduleName, anchor  = href
            self.loadDoc(moduleName,anchor)
        
    def loadDoc(self,moduleName,anchor):
        #generate html code
        if moduleName!= self.moduleName:
            try:
                module          = my_import(moduleName)
                reload(module)
                doc             = pydoc.html.page(    
                                    moduleName, 
                                    pydoc.html.document(module, moduleName)
                                )
                self.SetPage(doc)
                self.moduleName = moduleName
            except Exception, message:
                self.error(moduleName,message)
        #jump to anchor
        if anchor:
            self.LoadPage(anchor)
        
    def error(self,*args):
        self.SetPage(ERROR%tuple(args))
        
    def index(self):
        heading = pydoc.html.heading(
'<big><big><strong>Python: Index of Modules</strong></big></big>',
'#ffffff', '#7799ee')
        def bltinlink(name):
            return '<a href="%s.html">%s</a>' % (name, name)
        names = filter(lambda x: x != '__main__',
                       sys.builtin_module_names)
        contents = pydoc.html.multicolumn(names, bltinlink)
        indices = ['<p>' + pydoc.html.bigsection(
            'Built-in Modules', '#ffffff', '#ee77aa', contents)]
        seen = {}
        for dir in pydoc.pathdirs():
            indices.append(pydoc.html.index(dir, seen))
        contents = heading + pydoc.join(indices) + '''<p align=right>
<font color="#909090" face="helvetica, arial"><strong>
pydoc</strong> by Ka-Ping Yee &lt;ping@lfw.org&gt;</font>'''
        self.SetPage(contents)


