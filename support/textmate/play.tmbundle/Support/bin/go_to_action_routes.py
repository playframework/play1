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
method = None
controller_path = None

action = current_line.split()[2]
method = action.split('.')[-1]
controller = action[:action.rfind('.'+method)]
controller_path = file[:file.rfind('/conf/')] + '/app/controllers/' + controller.replace('.', '/') + '.java'

# Jump
if controller_path:
	
	if not os.path.exists(controller_path):

		ok = commands.getoutput(
			'%s/bin/CocoaDialog.app/Contents/MacOS/CocoaDialog ok-msgbox --title Message --text "%s" --informative-text "%s"' 
				% 
			(os.environ['TM_SUPPORT_PATH'], 'Do you want to create this controller ?', controller_path[controller_path.find('/app/'):])
		)
		if ok == '1':
			os.system('mkdir -p %s' % os.path.dirname(controller_path))
			f = open(controller_path, 'w')
			f.close()
			os.system('osascript -e \'tell application "Dock" to activate\'; osascript -e \'tell application "TextMate" to activate\'')
		else:
			sys.exit()

	line = 0
	i = 0
		
	with open(controller_path) as f:
		for l in f:
			i = i + 1
			if l.find('public') > -1 and l.find('static') > -1 and l.find('void') > -1 and l.find(method) > -1:
				line = i + 1
				break

	if line:
		os.system('/usr/bin/open "txmt://open?url=file://%s&line=%s"' % (controller_path, line) )
	else:
		os.system('/usr/bin/open txmt://open?url=file://%s' % controller_path) 

