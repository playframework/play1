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
action = None
rl = 0

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
			action = (file[file.find('/app/controllers/') + 17 : -len(file_name)] + file_name[0:-5]).replace('/', '.') + '.' + m.group(1)
			break
	
route_file = 	file[:file.find('/app/controllers/')] + '/conf/routes'
			
if action:
	i = 0
	with open(route_file) as f:
		for l in f:
			i = i + 1
			if l.find(action) > -1:
				rl = i
				break
			
if rl:
	os.system('/usr/bin/open "txmt://open?url=file://%s&line=%s"' % (route_file, rl) )
else:
	os.system('/usr/bin/open txmt://open?url=file://%s' % route_file)

