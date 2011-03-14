# MenuTree.py: A class to represent a menu on a wxMenuBar
# $Id: MenuTree.py,v 1.12 2007/03/27 07:02:05 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

class MenuTree:
    """\
    A class to represent a menu on a wxMenuBar
    """
    class Node:
        def __init__(self, label="", id="", name="", help_str="",
                     checkable="", radio="", handler=""):
            self.label = label
            self.id = id
            self.name = name
            self.help_str = help_str
            self.checkable = checkable
            self.radio = radio
            self.handler = handler
            self.children = []
            self.parent = None
            
        def write(self, outfile, tabs, top=False):
            from xml.sax.saxutils import escape, quoteattr
            import common
            fwrite = outfile.write
            tstr = '    ' * (tabs+1)
            label = common._encode_to_xml(self.label)
            help_str = common._encode_to_xml(self.help_str)
            if not top and not self.children:
                fwrite('%s<item>\n' % ('    ' * tabs))
                label = escape(label)
                if label: fwrite('%s<label>%s</label>\n' % (tstr, label))
                id = escape(self.id)
                if id: fwrite('%s<id>%s</id>\n' % (tstr, id))
                name = escape(self.name)
                if name: fwrite('%s<name>%s</name>\n' % (tstr, name))
                help_str = escape(help_str)
                if help_str: fwrite('%s<help_str>%s</help_str>\n' % (tstr,
                                                                     help_str))
                try: checkable = int(self.checkable)
                except: checkable = 0
                if checkable:
                    fwrite('%s<checkable>%s</checkable>\n' % (tstr, checkable))
                try: radio = int(self.radio)
                except: radio = 0
                if radio:
                    fwrite('%s<radio>%s</radio>\n' % (tstr, radio))
                # ALB 2004-12-05
                handler = escape(self.handler.strip())
                if handler:
                    fwrite('%s<handler>%s</handler>\n' % (tstr, handler))
                fwrite('%s</item>\n' % ('    ' * tabs))
            else:
                name = quoteattr(self.name)
                fwrite('    ' * tabs + '<menu name=%s ' % name)
                if self.id:
                    fwrite('itemid=%s ' % quoteattr(self.id))
                if self.handler:
                    fwrite('handler=%s ' % quoteattr(self.handler))
                fwrite('label=%s>\n' % (quoteattr(label)))
                for c in self.children: c.write(outfile, tabs+1)
                fwrite('    ' * tabs + '</menu>\n')
                
    #end of class Node
    
    def __init__(self, name, label, id="", help_str="", handler=""):
        self.root = self.Node(label, id, name, help_str, handler=handler)

    def write(self, outfile, tabs):
        self.root.write(outfile, tabs, top=True)
        
#end of class MenuTree
