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

extends = None

with open(file) as f:
	for l in f:
		if l.find('#{extends') > -1:
			m = re.compile(r'#{extends\s*\'([^\']+)\'.*}').match(l)
			if m:
				extends = m.group(1)
				break
				
if extends:
	if extends.startswith('.'):
		extends = file[:file.rfind('/')] + '/' + extends
	else:
		extends = file[:file.rfind('/app/views/')+11] + extends
		
	if not os.path.exists(extends):

		ok = commands.getoutput(
			'%s/bin/CocoaDialog.app/Contents/MacOS/CocoaDialog ok-msgbox --title Message --text "%s" --informative-text "%s"' 
				% 
			(os.environ['TM_SUPPORT_PATH'], 'Do you want to create this template ?', extends[extends.find('/app/'):])
		)
		if ok == '1':
			os.system('mkdir -p %s' % os.path.dirname(extends))
			f = open(extends, 'w')
			f.close()
			os.system('osascript -e \'tell application "Dock" to activate\'; osascript -e \'tell application "TextMate" to activate\'')
		else:
			sys.exit()

	os.system('/usr/bin/open txmt://open?url=file://%s' % extends)
			
else:
	print 'No parent template found ...'
		

