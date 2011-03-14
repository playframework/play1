# tool.py: Tool objects
# $Id: tool.py,v 1.8 2007/03/27 07:01:51 agriggio Exp $
#
# Copyright (c) 2002-2007 Alberto Griggio <agriggio@users.sourceforge.net>
# License: MIT (see license.txt)
# THIS PROGRAM COMES WITH NO WARRANTY

from xml.sax.saxutils import escape, quoteattr
from common import _encode_to_xml

class Tool:
    def __init__(self, id='', label='', type=0, short_help='',
                 long_help='', bitmap1='', bitmap2='', handler=''):
        self.id = id
        self.label = label
        self.type = type
        self.short_help = short_help
        self.long_help = long_help
        self.bitmap1 = bitmap1
        self.bitmap2 = bitmap2
        self.handler = handler

    def write(self, outfile, tabs):
        fwrite = outfile.write
        fwrite("    " * tabs + '<tool>\n')
        tab_s = "    " * (tabs+1)
        fwrite(tab_s + '<id>%s</id>\n' % escape(_encode_to_xml(self.id)))
        fwrite(tab_s + '<label>%s</label>\n' % \
               escape(_encode_to_xml(self.label)))
        fwrite(tab_s + '<type>%s</type>\n' % escape(str(self.type)))
        fwrite(tab_s + '<short_help>%s</short_help>\n' % \
               escape(_encode_to_xml(self.short_help)))
        fwrite(tab_s + '<long_help>%s</long_help>\n' % \
               escape(_encode_to_xml(self.long_help)))
        fwrite(tab_s + '<bitmap1>%s</bitmap1>\n' % \
               escape(_encode_to_xml(self.bitmap1)))
        fwrite(tab_s + '<bitmap2>%s</bitmap2>\n' % \
               escape(_encode_to_xml(self.bitmap2)))
        if self.handler:
            fwrite(tab_s + '<handler>%s</handler>\n' % \
                   escape(_encode_to_xml(self.handler.strip())))
        fwrite("    " * tabs + '</tool>\n')

# end of class Tool
