#!/usr/bin/env python

from __future__ import with_statement

import os
import sys
import re
import commands
import os.path
import MacOS

file_name = os.environ['TM_FILENAME']
file = os.environ['TM_FILEPATH']
line = int(os.environ['TM_LINE_NUMBER'])
current_line = os.environ['TM_CURRENT_LINE']
view_name = None

if current_line and current_line.find('render') > -1:
	m = re.compile('.*"(.*)".*').match(current_line)
	if m:
		view_name = m.group(1)
	else:
		lines = []
		with open(file) as f:
			for l in f:
				lines.append(l)
			
		while(line > 0):
			c_line = lines[line-1]
			line = line-1
			if c_line.find('public') > -1 and c_line.find('static') > -1 and c_line.find('void') > -1:
				m = re.compile(r'.*\s([a-zA-Z$0-9_]+)\s*\(.*').match(c_line)
				if m:
					view_name = file[file.find('/app/controllers/') + 17 : -len(file_name)] + file_name[0:-5] + '/' + m.group(1) + '.html'
					break
				
			
if view_name:
	view_file = file[:file.find('/app/controllers/') + 5] + 'views/' + view_name
	if not os.path.exists(view_file):
		
		ok = commands.getoutput(
			'%s/bin/CocoaDialog.app/Contents/MacOS/CocoaDialog ok-msgbox --title Message --text "%s" --informative-text "%s"' 
				% 
			(os.environ['TM_SUPPORT_PATH'], 'Do you want to create this template ?', view_file[view_file.find('/app/'):])
		)
		if ok == '1':
			os.system('mkdir -p %s' % os.path.dirname(view_file))
			f = open(view_file, 'w')
			f.close()
			os.system('osascript -e \'tell application "Dock" to activate\'; osascript -e \'tell application "TextMate" to activate\'')
		else:
			sys.exit()
		
	os.system('/usr/bin/open txmt://open?url=file://%s' % view_file)
	
else:
	print 'Use this command on a \'render\' line'
	MacOS.SysBeep()
		

